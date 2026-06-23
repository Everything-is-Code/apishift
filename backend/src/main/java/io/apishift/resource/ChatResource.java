package io.apishift.resource;

import io.apishift.ai.ApiShiftTools;
import io.apishift.ai.MigrationAgent;
import io.apishift.model.ChatMessage;
import io.apishift.service.ApiShiftMetrics;
import io.apishift.service.ThreeScaleService;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.runtime.StartupEvent;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.StringConfiguration;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Path("/api/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatResource {

    private static final Logger LOG = Logger.getLogger(ChatResource.class);
    private static final String FAQ_CACHE = "apishift-faq";

    private static final String[] FAQ_PROMPTS = {
            "List all 3scale Products in my cluster",
            "Analyze my 3scale config and create a migration plan",
            "Generate an AuthPolicy for API Key authentication",
            "Create a RateLimitPolicy for 100 req/min",
            "Compare shared vs dedicated gateway strategies",
            "Show kuadrantctl topology",
            "What is Connectivity Link?",
            "How does ApiShift migrate from 3scale to Kuadrant?",
            "What is the difference between AuthPolicy and RateLimitPolicy?",
            "How to configure OIDC authentication with Kuadrant?"
    };

    @Inject
    MigrationAgent migrationAgent;

    @Inject
    ApiShiftTools tools;

    @Inject
    ThreeScaleService threeScaleService;

    @Inject
    RemoteCacheManager cacheManager;

    @Inject
    ApiShiftMetrics apiShiftMetrics;

    private RemoteCache<String, String> getOrCreateFaqCache() {
        RemoteCache<String, String> cache = cacheManager.getCache(FAQ_CACHE);
        if (cache == null) {
            LOG.info("Creating FAQ cache: " + FAQ_CACHE);
            cache = cacheManager.administration()
                    .getOrCreateCache(FAQ_CACHE, new StringConfiguration(
                            "<distributed-cache name=\"" + FAQ_CACHE + "\">"
                            + "<encoding media-type=\"application/x-protostream\"/>"
                            + "</distributed-cache>"));
        }
        return cache;
    }

    void onStartup(@Observes StartupEvent ev) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Thread.sleep(15000);
                LOG.info("Starting FAQ cache warm-up...");
                RemoteCache<String, String> cache = getOrCreateFaqCache();
                if (cache == null) {
                    LOG.warn("FAQ cache not available, skipping warm-up");
                    return;
                }
                int loaded = 0;
                for (String prompt : FAQ_PROMPTS) {
                    String key = prompt.trim().toLowerCase();
                    if (cache.get(key) != null) {
                        loaded++;
                        continue;
                    }
                    if (cacheFaqPrompt(cache, prompt, loaded + 1)) {
                        loaded++;
                    }
                }
                LOG.infof("FAQ cache warm-up complete: %d/%d entries", loaded, FAQ_PROMPTS.length);
            } catch (Exception e) {
                LOG.warn("FAQ cache warm-up interrupted", e);
            }
        });
        executor.shutdown();
    }

    @POST
    public Response chat(ChatMessage userMessage) {
        try {
            String normalized = userMessage.content().trim().toLowerCase();
            RemoteCache<String, String> faqCache = null;
            try {
                faqCache = getOrCreateFaqCache();
            } catch (Exception e) {
                LOG.debug("FAQ cache not available");
            }

            if (faqCache != null) {
                String cached = faqCache.get(normalized);
                if (cached != null) {
                    LOG.infof("FAQ cache hit for: %s", userMessage.content());
                    apiShiftMetrics.recordChatRequest("cached");
                    return Response.ok(new ChatMessage("assistant", cached, true)).build();
                }
            }

            String contextEnriched = buildContextMessage(userMessage.content());
            String response = migrationAgent.chat(contextEnriched);
            response = cleanThinkingBlocks(response);
            apiShiftMetrics.recordChatRequest("llm");
            return Response.ok(new ChatMessage("assistant", response, false)).build();
        } catch (Exception e) {
            LOG.error("AI chat failed", e);
            String errorMsg = extractUserFriendlyError(e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(new ChatMessage("error", errorMsg))
                    .build();
        }
    }

    @POST
    @Path("/warm-faq")
    public Response warmFaqCache() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                RemoteCache<String, String> cache = getOrCreateFaqCache();
                if (cache == null) {
                    return;
                }
                int loaded = 0;
                for (String prompt : FAQ_PROMPTS) {
                    String key = prompt.trim().toLowerCase();
                    if (cache.get(key) != null) {
                        loaded++;
                        continue;
                    }
                    if (cacheFaqPrompt(cache, prompt, loaded + 1)) {
                        loaded++;
                    }
                }
                LOG.infof("FAQ cache refresh complete: %d/%d entries", loaded, FAQ_PROMPTS.length);
            } catch (Exception e) {
                LOG.warn("FAQ cache refresh failed", e);
            }
        });
        executor.shutdown();
        return Response.ok(Map.of("status", "warming", "count", FAQ_PROMPTS.length)).build();
    }

    @GET
    @Path("/faq-status")
    public Response faqStatus() {
        try {
            RemoteCache<String, String> cache = getOrCreateFaqCache();
            int cached = 0;
            if (cache != null) {
                for (String prompt : FAQ_PROMPTS) {
                    if (cache.get(prompt.trim().toLowerCase()) != null) cached++;
                }
            }
            return Response.ok(Map.of("total", FAQ_PROMPTS.length, "cached", cached)).build();
        } catch (Exception e) {
            return Response.ok(Map.of("total", FAQ_PROMPTS.length, "cached", 0, "error", e.getMessage())).build();
        }
    }

    private String extractUserFriendlyError(Exception e) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        Throwable cause = e.getCause();
        String causeMsg = cause != null && cause.getMessage() != null ? cause.getMessage() : "";

        if (msg.contains("401") || causeMsg.contains("401") || causeMsg.contains("auth_error")) {
            return "AI service authentication failed. The API key is invalid or not configured. Please contact the administrator.";
        }
        if (msg.contains("timeout") || msg.contains("Timeout") || causeMsg.contains("timeout")) {
            return "AI service timed out. The model is taking too long to respond. Please try again with a simpler question.";
        }
        if (msg.contains("ContextWindowExceeded") || causeMsg.contains("ContextWindowExceeded")) {
            return "The question context is too large for the AI model. Please ask about a specific product or topic.";
        }
        if (msg.contains("Connection refused") || causeMsg.contains("Connection refused")) {
            return "Cannot reach the AI service. Please verify the AI endpoint configuration.";
        }
        return "AI service is temporarily unavailable. Please try again later. (" + (causeMsg.isEmpty() ? msg : causeMsg) + ")";
    }

    private String cleanThinkingBlocks(String text) {
        if (text == null) return "";
        String cleaned = text.replaceAll("(?s)<think>.*?</think>\\s*", "");
        int closeIdx = cleaned.indexOf("</think>");
        if (closeIdx >= 0) {
            cleaned = cleaned.substring(closeIdx + "</think>".length());
        }
        return cleaned.trim();
    }

    @GET
    @Path("/status")
    public ChatMessage status() {
        return new ChatMessage("system", "ApiShift AI chat is active");
    }

    private static final int MAX_CONTEXT_PRODUCTS = 20;

    private boolean cacheFaqPrompt(RemoteCache<String, String> cache, String prompt, int displayIndex) {
        String key = prompt.trim().toLowerCase();
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                String response = migrationAgent.chat(buildFaqWarmupContext(prompt));
                response = cleanThinkingBlocks(response);
                if (response != null && !response.isBlank()) {
                    cache.put(key, response, 24, TimeUnit.HOURS);
                    LOG.infof("FAQ cached [%d/%d]: %s", displayIndex, FAQ_PROMPTS.length, prompt);
                    return true;
                }
                return false;
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    LOG.warnf("FAQ cache warm-up failed for: %s — %s", prompt, e.getMessage());
                } else {
                    LOG.infof("FAQ warm-up retry %d/%d for: %s — %s",
                            attempt, maxAttempts, prompt, e.getMessage());
                    try {
                        Thread.sleep(10_000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Smaller context for FAQ warm-up: skips full OpenShift namespace inventory to reduce
     * token load and upstream proxy timeouts on long answers.
     */
    private String buildFaqWarmupContext(String userQuestion) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("## Current Cluster State (summary)\n\n");
        try {
            ctx.append("### 3scale Status\n").append(tools.getThreeScaleStatus()).append("\n\n");
        } catch (Exception e) {
            LOG.debug("Failed to fetch 3scale status for FAQ warm-up context", e);
        }
        try {
            ctx.append("### 3scale Products (summary)\n").append(buildProductSummary(userQuestion)).append("\n\n");
        } catch (Exception e) {
            LOG.debug("Failed to fetch products for FAQ warm-up context", e);
        }
        ctx.append("---\n\n## User Question\n").append(userQuestion);
        return ctx.toString();
    }

    private String buildContextMessage(String userQuestion) {
        StringBuilder ctx = new StringBuilder();
        ctx.append("## Current Cluster State\n\n");

        try {
            ctx.append("### 3scale Status\n").append(tools.getThreeScaleStatus()).append("\n\n");
        } catch (Exception e) {
            LOG.debug("Failed to fetch 3scale status for context", e);
        }

        try {
            ctx.append("### 3scale Products (summary)\n").append(buildProductSummary(userQuestion)).append("\n\n");
        } catch (Exception e) {
            LOG.debug("Failed to fetch products for context", e);
        }

        try {
            ctx.append("### OpenShift Projects\n").append(tools.listProjects()).append("\n\n");
        } catch (Exception e) {
            LOG.debug("Failed to fetch projects for context", e);
        }

        try {
            ctx.append("### kuadrantctl\n").append(tools.getKuadrantctlVersion()).append("\n\n");
        } catch (Exception e) {
            LOG.debug("Failed to fetch kuadrantctl version for context", e);
        }

        ctx.append("---\n\n## User Question\n").append(userQuestion);
        return ctx.toString();
    }

    private String buildProductSummary(String userQuestion) {
        var products = threeScaleService.listProducts();
        if (products.isEmpty()) {
            return "No 3scale products found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Total products: %d\n".formatted(products.size()));

        String q = userQuestion != null ? userQuestion.toLowerCase() : "";
        var relevant = products.stream()
                .filter(p -> q.isEmpty()
                        || q.contains(p.name().toLowerCase())
                        || q.contains(p.systemName().toLowerCase())
                        || (p.namespace() != null && q.contains(p.namespace().toLowerCase())))
                .limit(MAX_CONTEXT_PRODUCTS)
                .toList();

        if (relevant.isEmpty() || relevant.size() == products.size()) {
            relevant = products.stream().limit(MAX_CONTEXT_PRODUCTS).toList();
        }

        sb.append("Showing %d relevant products:\n".formatted(relevant.size()));
        relevant.forEach(p -> sb.append("- %s (ns: %s, source: %s, %d rules, %d backends)\n".formatted(
                p.name(), p.namespace(), p.source(),
                p.mappingRules().size(), p.backendUsages().size())));

        if (products.size() > relevant.size()) {
            sb.append("... and %d more. Ask about a specific product by name for details.\n".formatted(
                    products.size() - relevant.size()));
        }
        return sb.toString();
    }
}

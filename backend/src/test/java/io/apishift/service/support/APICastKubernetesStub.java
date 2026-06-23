package io.apishift.service.support;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;

public final class APICastKubernetesStub {

    public record Options(
            List<GenericKubernetesResource> apiManagers,
            List<GenericKubernetesResource> products,
            List<Pod> apicastPods
    ) {
        public static Options empty() {
            return new Options(List.of(), List.of(), List.of());
        }
    }

    private APICastKubernetesStub() {}

    public static KubernetesClient create(Options options) {
        Options safe = options != null ? options : Options.empty();

        return (KubernetesClient) proxy(KubernetesClient.class, (proxy, method, args) -> switch (method.getName()) {
            case "genericKubernetesResources" -> createCrMixed(
                    pluralOf((CustomResourceDefinitionContext) args[0]),
                    safe);
            case "pods" -> createPodMixed(safe.apicastPods());
            case "close" -> null;
            default -> defaultValue(method);
        });
    }

    private static String pluralOf(CustomResourceDefinitionContext ctx) {
        return ctx.getPlural();
    }

    private static Object createCrMixed(String plural, Options options) {
        List<GenericKubernetesResource> items = "products".equals(plural)
                ? options.products()
                : options.apiManagers();

        return proxy(MixedOperation.class, (proxy, method, args) -> switch (method.getName()) {
            case "inAnyNamespace" -> createCrListable(null, items);
            case "inNamespace" -> createCrListable((String) args[0], items);
            default -> defaultValue(method);
        });
    }

    private static Object createCrListable(String namespace, List<GenericKubernetesResource> items) {
        return proxy(NonNamespaceOperation.class, (proxy, method, args) -> {
            if ("list".equals(method.getName())) {
                GenericKubernetesResourceList list = new GenericKubernetesResourceList();
                list.setItems(filterByNamespace(namespace, items));
                return list;
            }
            if ("withName".equals(method.getName())) {
                return createCrResource(namespace, (String) args[0], items);
            }
            return defaultValue(method);
        });
    }

    private static Object createCrResource(String namespace, String name, List<GenericKubernetesResource> items) {
        return proxy(Resource.class, (proxy, method, args) -> {
            if ("get".equals(method.getName())) {
                return items.stream()
                        .filter(item -> namespace == null
                                || Objects.equals(namespace, item.getMetadata().getNamespace()))
                        .filter(item -> Objects.equals(name, item.getMetadata().getName()))
                        .findFirst()
                        .orElse(null);
            }
            return defaultValue(method);
        });
    }

    private static Object createPodMixed(List<Pod> pods) {
        return proxy(MixedOperation.class, (proxy, method, args) -> {
            if ("inNamespace".equals(method.getName())) {
                return createPodNamespaceOps((String) args[0], pods);
            }
            return defaultValue(method);
        });
    }

    private static Object createPodNamespaceOps(String namespace, List<Pod> pods) {
        return proxy(NonNamespaceOperation.class, (proxy, method, args) -> {
            if ("withLabel".equals(method.getName())) {
                return createPodLabelOps(namespace, pods);
            }
            return defaultValue(method);
        });
    }

    private static Object createPodLabelOps(String namespace, List<Pod> pods) {
        return proxy(NonNamespaceOperation.class, (proxy, method, args) -> {
            if ("list".equals(method.getName())) {
                PodList list = new PodList();
                list.setItems(pods.stream()
                        .filter(pod -> Objects.equals(namespace, pod.getMetadata().getNamespace()))
                        .toList());
                return list;
            }
            return defaultValue(method);
        });
    }

    private static List<GenericKubernetesResource> filterByNamespace(
            String namespace,
            List<GenericKubernetesResource> items) {
        if (namespace == null) {
            return items;
        }
        return items.stream()
                .filter(item -> Objects.equals(namespace, item.getMetadata().getNamespace()))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> iface, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                new Class<?>[] { iface },
                handler);
    }

    private static Object defaultValue(Method method) {
        Class<?> returnType = method.getReturnType();
        if (returnType == void.class) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        return null;
    }
}

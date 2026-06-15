package io.gateforge.service.support;

import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClusterRegistryKubernetesStub {

    public enum NamespaceBehavior {
        OK,
        FAIL
    }

    public enum GatewayBehavior {
        OK,
        FAIL
    }

    public record Options(
            NamespaceBehavior namespaces,
            GatewayBehavior gateways,
            List<Secret> argocdSecrets
    ) {
        public static Options fullAccess() {
            return new Options(NamespaceBehavior.OK, GatewayBehavior.OK, List.of());
        }
    }

    private ClusterRegistryKubernetesStub() {}

    public static KubernetesClient create(Options options) {
        Object secretListable = proxy(NonNamespaceOperation.class, (proxy, method, args) -> {
            if ("withLabel".equals(method.getName())) {
                return proxy;
            }
            if ("list".equals(method.getName())) {
                SecretList list = new SecretList();
                list.setItems(options.argocdSecrets() != null ? options.argocdSecrets() : List.of());
                return list;
            }
            return defaultValue(method);
        });

        Object secretMixed = proxy(MixedOperation.class, (proxy, method, args) -> {
            if ("inNamespace".equals(method.getName())) {
                return secretListable;
            }
            return defaultValue(method);
        });

        Object gatewayListable = proxy(NonNamespaceOperation.class, (proxy, method, args) -> {
            if ("list".equals(method.getName())) {
                if (options.gateways() == GatewayBehavior.FAIL) {
                    throw new RuntimeException("Forbidden");
                }
                return new GenericKubernetesResourceList();
            }
            return defaultValue(method);
        });

        Object gatewayMixed = proxy(MixedOperation.class, (proxy, method, args) -> {
            if ("inAnyNamespace".equals(method.getName())) {
                return gatewayListable;
            }
            return defaultValue(method);
        });

        Object namespaceListable = proxy(NonNamespaceOperation.class, (proxy, method, args) -> {
            if ("list".equals(method.getName())) {
                if (options.namespaces() == NamespaceBehavior.FAIL) {
                    throw new RuntimeException("Forbidden");
                }
                return new NamespaceList();
            }
            return defaultValue(method);
        });

        return (KubernetesClient) proxy(KubernetesClient.class, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "namespaces" -> namespaceListable;
                case "genericKubernetesResources" -> gatewayMixed;
                case "secrets" -> secretMixed;
                case "close" -> null;
                default -> defaultValue(method);
            };
        });
    }

    public static Secret argocdClusterSecret(String name, String server, String bearerToken) {
        Map<String, String> data = new HashMap<>();
        data.put("name", encodeBase64(name));
        data.put("server", encodeBase64(server));
        if (bearerToken != null && !bearerToken.isBlank()) {
            data.put("config", encodeBase64("{\"bearerToken\":\"" + bearerToken + "\"}"));
        }

        Secret secret = new Secret();
        secret.setData(data);
        return secret;
    }

    private static String encodeBase64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
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

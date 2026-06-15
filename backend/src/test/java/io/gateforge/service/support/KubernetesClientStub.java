package io.gateforge.service.support;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.V1ApiextensionAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.ApiextensionsAPIGroupDSL;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class KubernetesClientStub {

    public enum Mode {
        SATISFIED,
        MISSING,
        FORBIDDEN
    }

    private KubernetesClientStub() {}

    public static KubernetesClient create(Mode mode) {
        Object crdResource = proxy(Resource.class, (proxy, method, args) -> {
            if ("get".equals(method.getName())) {
                return switch (mode) {
                    case SATISFIED -> new CustomResourceDefinition();
                    case MISSING -> null;
                    case FORBIDDEN -> throw new RuntimeException("Forbidden");
                };
            }
            return defaultValue(method);
        });

        Object crdOps = proxy(NonNamespaceOperation.class, (proxy, method, args) -> {
            if ("withName".equals(method.getName())) {
                return crdResource;
            }
            return defaultValue(method);
        });

        Object v1 = proxy(V1ApiextensionAPIGroupDSL.class, (proxy, method, args) -> {
            if ("customResourceDefinitions".equals(method.getName())) {
                return crdOps;
            }
            return defaultValue(method);
        });

        Object apiExtensions = proxy(ApiextensionsAPIGroupDSL.class, (proxy, method, args) -> {
            if ("v1".equals(method.getName())) {
                return v1;
            }
            return defaultValue(method);
        });

        return (KubernetesClient) proxy(KubernetesClient.class, (proxy, method, args) -> {
            if ("apiextensions".equals(method.getName())) {
                return apiExtensions;
            }
            return defaultValue(method);
        });
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
        if (returnType == CustomResourceDefinitionList.class) {
            return new CustomResourceDefinitionList();
        }
        return null;
    }
}

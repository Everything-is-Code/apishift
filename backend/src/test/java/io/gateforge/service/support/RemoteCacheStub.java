package io.gateforge.service.support;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.RemoteCacheManagerAdmin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RemoteCacheStub {

    private final Map<String, Map<String, String>> stores = new ConcurrentHashMap<>();
    private final RemoteCacheManager manager;

    private RemoteCacheStub() {
        this.manager = new InMemoryRemoteCacheManager();
    }

    public static RemoteCacheStub create() {
        return new RemoteCacheStub();
    }

    public RemoteCacheManager manager() {
        return manager;
    }

    public String get(String cacheName, String key) {
        Map<String, String> store = stores.get(cacheName);
        return store != null ? store.get(key) : null;
    }

    public boolean contains(String cacheName, String key) {
        return get(cacheName, key) != null;
    }

    @SuppressWarnings("unchecked")
    private <K, V> RemoteCache<K, V> cacheFor(String cacheName) {
        Map<String, String> store = stores.computeIfAbsent(cacheName, ignored -> new ConcurrentHashMap<>());
        InvocationHandler cacheHandler = (proxy, method, args) -> switch (method.getName()) {
            case "get" -> store.get((String) args[0]);
            case "put" -> {
                if (args.length == 2) {
                    store.put((String) args[0], (String) args[1]);
                } else if (args.length == 4) {
                    store.put((String) args[0], (String) args[1]);
                }
                yield null;
            }
            case "remove" -> store.remove((String) args[0]);
            default -> defaultValue(method);
        };
        return (RemoteCache<K, V>) Proxy.newProxyInstance(
                RemoteCache.class.getClassLoader(),
                new Class<?>[] { RemoteCache.class },
                cacheHandler);
    }

    private RemoteCacheManagerAdmin administrationProxy() {
        InvocationHandler adminHandler = (proxy, method, args) -> {
            if ("getOrCreateCache".equals(method.getName()) && args.length >= 1) {
                return cacheFor((String) args[0]);
            }
            return defaultValue(method);
        };
        return (RemoteCacheManagerAdmin) Proxy.newProxyInstance(
                RemoteCacheManagerAdmin.class.getClassLoader(),
                new Class<?>[] { RemoteCacheManagerAdmin.class },
                adminHandler);
    }

    private final class InMemoryRemoteCacheManager extends RemoteCacheManager {

        private InMemoryRemoteCacheManager() {
            super(false);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <K, V> RemoteCache<K, V> getCache(String name) {
            return (RemoteCache<K, V>) cacheFor(name);
        }

        @Override
        public RemoteCacheManagerAdmin administration() {
            return administrationProxy();
        }
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

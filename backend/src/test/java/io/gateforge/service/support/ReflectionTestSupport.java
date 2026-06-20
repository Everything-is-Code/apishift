package io.gateforge.service.support;

import java.lang.reflect.Field;

public final class ReflectionTestSupport {

    private ReflectionTestSupport() {}

    public static void inject(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to inject " + fieldName + " on " + target.getClass().getName(), e);
        }
    }

    public static Object getField(Object target, String fieldName) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            return field.get(target);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to read " + fieldName + " on " + target.getClass().getName(), e);
        }
    }

    public static Object invoke(Object target, String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            var method = findMethod(target.getClass(), methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Failed to invoke " + methodName + " on " + target.getClass().getName(), e);
        }
    }

    private static java.lang.reflect.Method findMethod(Class<?> type, String methodName, Class<?>[] paramTypes)
            throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(methodName, paramTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(methodName);
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}

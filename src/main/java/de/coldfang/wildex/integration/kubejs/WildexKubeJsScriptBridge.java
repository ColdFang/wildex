package de.coldfang.wildex.integration.kubejs;

import de.coldfang.wildex.Wildex;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class WildexKubeJsScriptBridge {

    private static final Map<String, List<Object>> LISTENERS = new ConcurrentHashMap<>();

    private WildexKubeJsScriptBridge() {
    }

    public static void onDiscoveryChanged(Object callback) {
        on(WildexKubeJsBridgeContract.EVENT_DISCOVERY_CHANGED, callback);
    }

    public static void onCompleted(Object callback) {
        on(WildexKubeJsBridgeContract.EVENT_COMPLETED, callback);
    }

    public static void on(String eventId, Object callback) {
        if (eventId == null || eventId.isBlank()) return;
        if (callback == null) return;
        LISTENERS.computeIfAbsent(eventId, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public static void clearAll() {
        LISTENERS.clear();
    }

    static boolean emitInternal(@NotNull String eventId, @NotNull Map<String, Object> payload) {
        boolean invoked = false;

        invoked |= invokeListeners(LISTENERS.get(eventId), eventId, payload);
        invoked |= invokeListeners(LISTENERS.get("*"), eventId, payload);

        return invoked;
    }

    private static boolean invokeListeners(List<Object> callbacks, String eventId, Map<String, Object> payload) {
        if (callbacks == null || callbacks.isEmpty()) return false;

        boolean invoked = false;
        for (Object callback : callbacks) {
            try {
                invokeCallback(callback, eventId, payload);
                invoked = true;
            } catch (Throwable t) {
                Wildex.LOGGER.warn("Wildex KubeJS callback failed for event '{}'", eventId, t);
            }
        }
        return invoked;
    }

    private static void invokeCallback(Object callback, String eventId, Map<String, Object> payload) throws Exception {
        if (callback instanceof WildexKubeJsEventEmitter emitter) {
            emitter.emit(eventId, payload);
            return;
        }

        if (invokeRhinoCallback(callback, eventId, payload)) {
            return;
        }

        Method m = findCallbackMethod(callback.getClass(), 2, "call", "accept", "apply", "handle");
        if (m != null) {
            m.invoke(callback, eventId, payload);
            return;
        }

        m = findCallbackMethod(callback.getClass(), 1, "call", "accept", "apply", "handle");
        if (m != null) {
            m.invoke(callback, payload);
            return;
        }

        throw new IllegalArgumentException("Unsupported callback type: " + callback.getClass().getName());
    }

    private static boolean invokeRhinoCallback(Object callback, String eventId, Map<String, Object> payload) {
        ClassLoader loader = callback.getClass().getClassLoader();
        Object context = null;
        try {
            Class<?> callableClass = Class.forName("dev.latvian.mods.rhino.Callable", false, loader);
            if (!callableClass.isInstance(callback)) return false;

            Class<?> scriptableClass = Class.forName("dev.latvian.mods.rhino.Scriptable", false, loader);
            Class<?> contextClass = Class.forName("dev.latvian.mods.rhino.Context", false, loader);
            Object scope = resolveRhinoScope(callback, scriptableClass);
            context = enterRhinoContext(loader);
            if (context == null) {
                throw new IllegalStateException("Rhino Context creation returned null");
            }

            Method callSync = contextClass.getMethod("callSync", callableClass, scriptableClass, scriptableClass, Object[].class);
            callSync.setAccessible(true);
            callSync.invoke(context, callback, scope, scope, buildRhinoArgs(callback, eventId, payload));
            return true;
        } catch (Throwable t) {
            throw new IllegalArgumentException("Failed to invoke Rhino callback: " + callback.getClass().getName(), t);
        } finally {
            exitRhinoContext(loader, context);
        }
    }

    private static Object resolveRhinoScope(Object callback, Class<?> scriptableClass) {
        Object scope = invokeNoArgIfPresent(callback, "getParentScope");
        if (scope == null && scriptableClass.isInstance(callback)) {
            scope = callback;
        }
        if (scope == null) {
            throw new IllegalArgumentException("Could not resolve Rhino scope for callback type: " + callback.getClass().getName());
        }
        return scope;
    }

    private static Object enterRhinoContext(ClassLoader loader) {
        Object factory = constructNoArg("dev.latvian.mods.rhino.ContextFactory", loader);
        if (factory != null) {
            Object contextFromFactory = invokeNoArgIfPresent(factory, "enter");
            if (contextFromFactory != null) return contextFromFactory;
        }

        Object context = invokeStaticNoArg("dev.latvian.mods.rhino.Context", "enter", loader);
        if (context != null) return context;

        return invokeStaticNoArg("dev.latvian.mods.rhino.Context", "getCurrentContext", loader);
    }

    private static void exitRhinoContext(ClassLoader loader, Object context) {
        if (context == null) return;
        if (invokeStaticNoArg("dev.latvian.mods.rhino.Context", "exit", loader) != null) return;
        if (context instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static Object invokeStaticNoArg(String className, String methodName, ClassLoader loader) {
        try {
            Class<?> type = Class.forName(className, false, loader);
            Method method = type.getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object constructNoArg(String className, ClassLoader loader) {
        try {
            Class<?> type = Class.forName(className, false, loader);
            return type.getDeclaredConstructor().newInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeNoArgIfPresent(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object[] buildRhinoArgs(Object callback, String eventId, Map<String, Object> payload) {
        Object len = invokeNoArgIfPresent(callback, "getLength");
        if (len instanceof Number n && n.intValue() >= 2) {
            return new Object[]{eventId, payload};
        }
        return new Object[]{payload};
    }

    private static Method findCallbackMethod(Class<?> type, int params, String... names) {
        for (String name : names) {
            for (Method method : type.getMethods()) {
                if (!method.getName().equals(name)) continue;
                if (method.getParameterCount() != params) continue;
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}

package dev.hamilo.client;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bootstrap for Pojav's PreMain-Class injector and standard Java-agent loaders.
 * Production Fabric runtimes generally expose intermediary Minecraft names,
 * while development environments expose Yarn names; support both.
 */
public final class HamiloAgent {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final String[] MINECRAFT_CLASSES = {
            "net.minecraft.class_310",
            "net.minecraft.client.MinecraftClient"
    };

    private HamiloAgent() {}

    public static void premain(String args) {
        start(null);
    }

    public static void premain(String args, Instrumentation instrumentation) {
        start(instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        start(instrumentation);
    }

    private static void start(Instrumentation instrumentation) {
        if (!STARTED.compareAndSet(false, true)) return;

        Thread worker = new Thread(() -> {
            try {
                Class<?> minecraftClass = waitForMinecraft(instrumentation);
                if (minecraftClass == null) {
                    System.err.println("[Hamilo] Minecraft client class was not found after 60 seconds.");
                    return;
                }

                ClassLoader gameLoader = minecraftClass.getClassLoader();
                Object minecraft = invokeStaticNoArgs(minecraftClass,
                        "method_1551", "getInstance");
                Object windowObject = invokeNoArgs(minecraftClass, minecraft,
                        "method_22683", "getWindow");
                long window = ((Number) invokeNoArgs(windowObject.getClass(), windowObject,
                        "method_4490", "getHandle")).longValue();

                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW", true, gameLoader);
                Method getKey = glfw.getMethod("glfwGetKey", long.class, int.class);
                Method setScreen = findMethod(minecraftClass,
                        new String[]{"method_1507", "setScreen"}, 1);
                Class<?> screenClass = Class.forName(
                        "dev.hamilo.client.HamiloClickGuiScreen", true, gameLoader);
                Field screenField = findField(minecraftClass,
                        "field_1755", "currentScreen");

                boolean wasDown = false;
                System.out.println("[Hamilo] Injected. Press Right Shift to toggle the GUI.");
                while (!Thread.currentThread().isInterrupted()) {
                    int state = ((Number) getKey.invoke(null, window, 344)).intValue();
                    boolean down = state == 1;
                    if (down && !wasDown) {
                        Object current = screenField.get(minecraft);
                        Object next = current != null && screenClass.isInstance(current)
                                ? null : screenClass.getDeclaredConstructor().newInstance();
                        setScreen.invoke(minecraft, next);
                    }
                    wasDown = down;
                    Thread.sleep(16L);
                }
            } catch (Throwable error) {
                System.err.println("[Hamilo] Injection bootstrap failed:");
                error.printStackTrace(System.err);
            }
        }, "Hamilo-Agent-Bootstrap");
        worker.setDaemon(true);
        worker.start();
    }

    private static Class<?> waitForMinecraft(Instrumentation instrumentation)
            throws InterruptedException {
        for (int attempt = 0; attempt < 600; attempt++) {
            if (instrumentation != null) {
                for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
                    if (isMinecraftClass(loaded.getName())) return loaded;
                }
            }

            for (ClassLoader loader : candidateLoaders()) {
                for (String className : MINECRAFT_CLASSES) {
                    try {
                        return Class.forName(className, false, loader);
                    } catch (ClassNotFoundException | LinkageError ignored) {
                        // Try the next mapping name and class loader.
                    }
                }
            }
            Thread.sleep(100L);
        }
        return null;
    }

    private static boolean isMinecraftClass(String name) {
        for (String candidate : MINECRAFT_CLASSES) {
            if (candidate.equals(name)) return true;
        }
        return false;
    }

    private static Set<ClassLoader> candidateLoaders() {
        Set<ClassLoader> loaders = new LinkedHashSet<>();
        addLoader(loaders, Thread.currentThread().getContextClassLoader());
        addLoader(loaders, HamiloAgent.class.getClassLoader());
        addLoader(loaders, ClassLoader.getSystemClassLoader());
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            addLoader(loaders, thread.getContextClassLoader());
        }
        return loaders;
    }

    private static void addLoader(Set<ClassLoader> loaders, ClassLoader loader) {
        if (loader == null) return;
        for (ClassLoader current = loader; current != null; current = current.getParent()) {
            loaders.add(current);
        }
    }

    private static Object invokeStaticNoArgs(Class<?> type, String... names)
            throws ReflectiveOperationException {
        return findMethod(type, names, 0).invoke(null);
    }

    private static Object invokeNoArgs(Class<?> type, Object target, String... names)
            throws ReflectiveOperationException {
        return findMethod(type, names, 0).invoke(target);
    }

    private static Method findMethod(Class<?> type, String[] names, int parameterCount)
            throws NoSuchMethodException {
        for (String name : names) {
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name)
                        && method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (method.getName().equals(name)
                            && method.getParameterCount() == parameterCount) {
                        method.setAccessible(true);
                        return method;
                    }
                }
            }
        }
        throw new NoSuchMethodException(type.getName() + " methods " + String.join("/", names));
    }

    private static Field findField(Class<?> type, String... names)
            throws NoSuchFieldException {
        for (String name : names) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                try {
                    Field field = current.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    // Continue through the class hierarchy.
                }
            }
        }
        throw new NoSuchFieldException(type.getName() + " fields " + String.join("/", names));
    }
}

package dev.hamilo.client;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bootstrap compatible with both standard Java-agent loading and launchers
 * that invoke the Pojav-style PreMain-Class entry point directly.
 */
public final class HamiloAgent {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private HamiloAgent() {}

    // Pojav-style custom launcher entry point (does not pass Instrumentation).
    public static void premain(String args) {
        start(null);
    }

    // Standard -javaagent entry point.
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
                    System.err.println("[Hamilo] MinecraftClient was not found after 60 seconds.");
                    return;
                }

                ClassLoader gameLoader = minecraftClass.getClassLoader();
                Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW", true, gameLoader);
                Object windowObject = minecraftClass.getMethod("getWindow").invoke(minecraft);
                long window = ((Number) windowObject.getClass().getMethod("getHandle")
                        .invoke(windowObject)).longValue();
                Method getKey = glfw.getMethod("glfwGetKey", long.class, int.class);
                Method setScreen = findSetScreen(minecraftClass);
                Class<?> screenClass = Class.forName(
                        "dev.hamilo.client.HamiloClickGuiScreen", true, gameLoader);
                Field screenField = findField(minecraftClass, "currentScreen");

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
                    if ("net.minecraft.client.MinecraftClient".equals(loaded.getName())) {
                        return loaded;
                    }
                }
            }

            for (ClassLoader loader : candidateLoaders()) {
                try {
                    return Class.forName("net.minecraft.client.MinecraftClient", false, loader);
                } catch (ClassNotFoundException | LinkageError ignored) {
                    // Try the next loader; launchers can isolate game and injected JARs.
                }
            }
            Thread.sleep(100L);
        }
        return null;
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

    private static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                Field field = current.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                // Continue through the superclass chain.
            }
        }
        throw new NoSuchFieldException(type.getName() + "." + name);
    }

    private static Method findSetScreen(Class<?> minecraftClass)
            throws NoSuchMethodException {
        for (Method method : minecraftClass.getMethods()) {
            if (method.getName().equals("setScreen") && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new NoSuchMethodException("MinecraftClient.setScreen");
    }
}

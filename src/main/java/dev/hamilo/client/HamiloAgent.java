package dev.hamilo.client;

import java.lang.instrument.Instrumentation;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Experimental standalone agent bootstrap. This intentionally does not implement
 * Fabric's ClientModInitializer and does not require fabric.mod.json.
 */
public final class HamiloAgent {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);

    private HamiloAgent() {}

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
                    System.err.println("[Hamilo] Minecraft was not detected; agent stopped.");
                    return;
                }

                Object minecraft = minecraftClass.getMethod("getInstance").invoke(null);
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW");
                Object windowObject = minecraftClass.getMethod("getWindow").invoke(minecraft);
                long window = ((Number) windowObject.getClass().getMethod("getHandle")
                        .invoke(windowObject)).longValue();
                java.lang.reflect.Method getKey = glfw.getMethod(
                        "glfwGetKey", long.class, int.class);
                java.lang.reflect.Method setScreen = findSetScreen(minecraftClass);
                Class<?> screenClass = Class.forName("dev.hamilo.client.HamiloClickGuiScreen",
                        true, HamiloAgent.class.getClassLoader());

                boolean wasDown = false;
                System.out.println("[Hamilo] Agent loaded. Press Right Shift to toggle the GUI.");
                while (!Thread.currentThread().isInterrupted()) {
                    int state = ((Number) getKey.invoke(null, window, 344)).intValue();
                    boolean down = state == 1;
                    if (down && !wasDown) {
                        java.lang.reflect.Field screenField = minecraftClass.getField("currentScreen");
                        Object current = screenField.get(minecraft);
                        Object next = current != null && screenClass.isInstance(current)
                                ? null : screenClass.getDeclaredConstructor().newInstance();
                        setScreen.invoke(minecraft, next);
                    }
                    wasDown = down;
                    Thread.sleep(16L);
                }
            } catch (Throwable error) {
                System.err.println("[Hamilo] Agent bootstrap failed:");
                error.printStackTrace(System.err);
            }
        }, "Hamilo-Agent-Bootstrap");
        worker.setDaemon(true);
        worker.start();
    }

    private static Class<?> waitForMinecraft(Instrumentation instrumentation)
            throws InterruptedException {
        for (int attempt = 0; attempt < 600; attempt++) {
            for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
                if ("net.minecraft.client.MinecraftClient".equals(loaded.getName())) {
                    return loaded;
                }
            }
            Thread.sleep(100L);
        }
        return null;
    }

    private static java.lang.reflect.Method findSetScreen(Class<?> minecraftClass)
            throws NoSuchMethodException {
        for (java.lang.reflect.Method method : minecraftClass.getMethods()) {
            if (method.getName().equals("setScreen") && method.getParameterCount() == 1) {
                return method;
            }
        }
        throw new NoSuchMethodException("MinecraftClient.setScreen");
    }
}

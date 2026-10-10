package dev.hamilo.client;

import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Experimental entry point for launchers that invoke a Java agent when injecting a JAR.
 * The regular Fabric entry point remains supported.
 */
public final class HamiloAgent {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static volatile URLClassLoader injectedLoader;

    private HamiloAgent() {}

    public static void premain(String args, Instrumentation instrumentation) {
        start(instrumentation);
    }

    public static void agentmain(String args, Instrumentation instrumentation) {
        start(instrumentation);
    }

    private static void start(Instrumentation instrumentation) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread worker = new Thread(() -> {
            try {
                ClassLoader gameLoader = waitForGameLoader(instrumentation);
                if (gameLoader == null) {
                    System.err.println("[Hamilo] MinecraftClient was not found; agent bootstrap stopped.");
                    return;
                }

                URL jarUrl = Path.of(HamiloAgent.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()).toUri().toURL();
                injectedLoader = new URLClassLoader(new URL[]{jarUrl}, gameLoader);

                Class<?> entrypoint = Class.forName(
                        "dev.hamilo.client.HamiloClient", true, injectedLoader);
                Object instance = entrypoint.getDeclaredConstructor().newInstance();
                entrypoint.getMethod("onInitializeClient").invoke(instance);
                System.out.println("[Hamilo] Agent bootstrap initialized the client entry point.");
            } catch (Throwable error) {
                System.err.println("[Hamilo] Agent bootstrap failed:");
                error.printStackTrace(System.err);
            }
        }, "Hamilo-Agent-Bootstrap");

        worker.setDaemon(true);
        worker.start();
    }

    private static ClassLoader waitForGameLoader(Instrumentation instrumentation)
            throws InterruptedException {
        // Minecraft may not have loaded yet when premain runs.
        for (int attempt = 0; attempt < 600; attempt++) {
            for (Class<?> loaded : instrumentation.getAllLoadedClasses()) {
                if ("net.minecraft.client.MinecraftClient".equals(loaded.getName())) {
                    return loaded.getClassLoader();
                }
            }
            Thread.sleep(100L);
        }
        return null;
    }
}

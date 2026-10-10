package dev.hamilo.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bootstrap for Pojav's PreMain-Class injector and standard Java-agent loaders.
 * Waits for the real Minecraft client to finish initializing, then opens Hamilo
 * automatically and keeps Right Shift as a toggle key.
 */
public final class HamiloAgent {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final String[] MINECRAFT_CLASSES = {
            "net.minecraft.class_310",
            "net.minecraft.client.MinecraftClient"
    };

    private HamiloAgent() {}

    /**
     * Execute .jar setup: create an independent Pojav/Fabric profile that loads
     * Hamilo through Fabric Loader's supported fabric.addMods property. The
     * JAR is stored outside the mods folder, and the user's normal profile is
     * left unchanged.
     */
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(
                        new java.io.File(System.getProperty("user.home", "/storage/emulated/0")));
                chooser.setDialogTitle("Select your Pojav .minecraft folder");
                chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showOpenDialog(null) != javax.swing.JFileChooser.APPROVE_OPTION) return;

                java.io.File minecraftDir = chooser.getSelectedFile();
                java.io.File versionsDir = new java.io.File(minecraftDir, "versions");
                if (!versionsDir.isDirectory()) {
                    throw new java.io.IOException("The selected folder has no versions directory. Select .minecraft.");
                }

                java.util.List<String> profiles = new java.util.ArrayList<>();
                java.io.File[] dirs = versionsDir.listFiles(java.io.File::isDirectory);
                if (dirs != null) {
                    java.util.Arrays.sort(dirs, java.util.Comparator.comparing(java.io.File::getName));
                    for (java.io.File dir : dirs) {
                        java.io.File json = new java.io.File(dir, dir.getName() + ".json");
                        if (!json.isFile()) continue;
                        String contents = new String(java.nio.file.Files.readAllBytes(json.toPath()),
                                java.nio.charset.StandardCharsets.UTF_8);
                        String id = jsonString(contents, "id");
                        String parent = jsonString(contents, "inheritsFrom");
                        boolean fabric = (id != null && id.startsWith("fabric-loader-"))
                                || dir.getName().startsWith("fabric-loader-");
                        boolean mc121 = "1.21".equals(parent)
                                || (id != null && id.contains("1.21"))
                                || dir.getName().contains("1.21");
                        if (fabric && mc121) profiles.add(dir.getName());
                    }
                }
                if (profiles.isEmpty()) {
                    throw new java.io.IOException("No Fabric Loader profile for Minecraft 1.21 was found. "
                            + "Install/launch Fabric 1.21 in Pojav once, then run this JAR again.");
                }

                javax.swing.JComboBox<String> profilePicker =
                        new javax.swing.JComboBox<>(profiles.toArray(new String[0]));
                int choice = javax.swing.JOptionPane.showConfirmDialog(null, profilePicker,
                        "Choose the existing Fabric 1.21 profile to clone",
                        javax.swing.JOptionPane.OK_CANCEL_OPTION, javax.swing.JOptionPane.QUESTION_MESSAGE);
                if (choice != javax.swing.JOptionPane.OK_OPTION) return;
                String parentId = (String) profilePicker.getSelectedItem();
                if (parentId == null) throw new java.io.IOException("No Fabric profile was selected.");

                java.io.File source = new java.io.File(
                        HamiloAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                if (!source.isFile() || !source.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                    throw new java.io.IOException("Run Hamilo from its packaged JAR.");
                }

                java.io.File hamiloDir = new java.io.File(minecraftDir, "hamilo");
                if (!hamiloDir.exists() && !hamiloDir.mkdirs()) {
                    throw new java.io.IOException("Could not create Hamilo profile directory: " + hamiloDir);
                }
                java.io.File targetJar = new java.io.File(hamiloDir, "hamilo-client.jar");
                if (!source.getCanonicalFile().equals(targetJar.getCanonicalFile())) {
                    java.nio.file.Files.copy(source.toPath(), targetJar.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                String profileId = "hamilo-fabric-1.21";
                java.io.File profileDir = new java.io.File(versionsDir, profileId);
                if (!profileDir.exists() && !profileDir.mkdirs()) {
                    throw new java.io.IOException("Could not create profile directory: " + profileDir);
                }
                String absoluteJar = targetJar.getAbsolutePath().replace("\\", "\\\\").replace("\"", "\\\"");
                String json = "{\n"
                        + "  \"id\": \"" + profileId + "\",\n"
                        + "  \"inheritsFrom\": \"" + parentId + "\",\n"
                        + "  \"type\": \"release\",\n"
                        + "  \"arguments\": {\n"
                        + "    \"jvm\": [\"-Dfabric.addMods=" + absoluteJar + "\"]\n"
                        + "  }\n"
                        + "}\n";
                java.nio.file.Files.write(new java.io.File(profileDir, profileId + ".json").toPath(),
                        json.getBytes(java.nio.charset.StandardCharsets.UTF_8));

                javax.swing.JOptionPane.showMessageDialog(null,
                        "Created separate profile: " + profileId + "\n\n"
                        + "Hamilo JAR: " + targetJar.getAbsolutePath() + "\n\n"
                        + "Select \"" + profileId + "\" in Pojav and launch it. "
                        + "Hamilo is loaded through Fabric Loader's fabric.addMods property; "
                        + "it is not copied into the mods folder. Press Right Shift in-game.",
                        "Hamilo Fabric Profile Created", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                System.out.println("[Hamilo] Created profile " + profileId + " inheriting " + parentId);
            } catch (Throwable error) {
                System.err.println("[Hamilo] Profile creation failed: " + error.getMessage());
                error.printStackTrace(System.err);
                javax.swing.JOptionPane.showMessageDialog(null,
                        "Could not create the Hamilo Fabric profile:\n"
                                + error.getClass().getSimpleName() + ": " + error.getMessage(),
                        "Hamilo Setup Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static String jsonString(String json, String key) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\\"" + java.util.regex.Pattern.quote(key) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .matcher(json);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static void launchStandaloneGui() {
        // Execute .jar uses Pojav's AWT/Caciocavallo runtime. Build the window
        // synchronously so startup errors are reported instead of disappearing
        // on the Swing event thread with no visible diagnostic.
        try {
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                javax.swing.JFrame frame = new javax.swing.JFrame("Hamilo Client");
                frame.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
                frame.setSize(380, 260);
                frame.setLocationRelativeTo(null);
                frame.setLayout(new java.awt.BorderLayout(12, 12));

                javax.swing.JLabel title = new javax.swing.JLabel("Hamilo Client", javax.swing.SwingConstants.CENTER);
                title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 22f));
                frame.add(title, java.awt.BorderLayout.NORTH);

                javax.swing.JTextArea info = new javax.swing.JTextArea(
                        "This window runs outside Minecraft.\n\n" +
                        "Execute .jar cannot inject into another running JVM.\n" +
                        "Install Hamilo as a Fabric mod, then launch your Fabric profile.\n" +
                        "In-game, press Right Shift to open or close the GUI.");
                info.setEditable(false);
                info.setOpaque(false);
                info.setLineWrap(true);
                info.setWrapStyleWord(true);
                info.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));
                frame.add(info, java.awt.BorderLayout.CENTER);

                javax.swing.JButton install = new javax.swing.JButton("Install as Fabric mod");
                install.addActionListener(event -> installForFabric(frame));

                javax.swing.JButton close = new javax.swing.JButton("Close");
                close.addActionListener(event -> frame.dispose());
                javax.swing.JPanel footer = new javax.swing.JPanel();
                footer.add(install);
                footer.add(close);
                frame.add(footer, java.awt.BorderLayout.SOUTH);
                frame.setVisible(true);
                frame.toFront();
            });
        } catch (Throwable error) {
            System.err.println("[Hamilo] Standalone GUI startup failed:");
            error.printStackTrace(System.err);
        }
    }

    /**
     * Execute .jar runs in a separate JVM, so it cannot attach this code to a
     * different Minecraft JVM. Provide a working Fabric install path instead:
     * copy this same mod JAR into Pojav's active .minecraft/mods directory.
     * It will load when that Fabric profile is next launched.
     */
    private static void installForFabric(javax.swing.JFrame owner) {
        try {
            java.io.File source = new java.io.File(
                    HamiloAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            if (!source.isFile() || !source.getName().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                throw new java.io.IOException("Hamilo must be run from its packaged JAR file.");
            }

            String home = System.getProperty("user.home", "");
            String userDir = System.getProperty("user.dir", "");
            java.io.File homeDir = new java.io.File(home);
            java.io.File userDirFile = new java.io.File(userDir);
            java.io.File modsDir = null;

            // Only auto-select a mods folder when its parent is demonstrably
            // a .minecraft directory. Never guess that an arbitrary "mods"
            // directory is the active Minecraft profile.
            java.io.File homeMinecraft = new java.io.File(homeDir, ".minecraft");
            java.io.File[] minecraftRoots = {
                    homeDir.getName().equals(".minecraft") ? homeDir : null,
                    userDirFile.getName().equals(".minecraft") ? userDirFile : null,
                    homeMinecraft,
                    new java.io.File("/storage/emulated/0/games/PojavLauncher/.minecraft"),
                    new java.io.File("/storage/emulated/0/Android/data/net.kdt.pojavlaunch/files/.minecraft")
            };
            for (java.io.File root : minecraftRoots) {
                if (root != null && root.isDirectory()) {
                    modsDir = new java.io.File(root, "mods");
                    break;
                }
            }

            if (modsDir == null) {
                javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(
                        homeDir.isDirectory() ? homeDir : userDirFile);
                chooser.setDialogTitle("Choose your Minecraft .minecraft folder");
                chooser.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
                chooser.setAcceptAllFileFilterUsed(false);
                if (chooser.showOpenDialog(owner) != javax.swing.JFileChooser.APPROVE_OPTION) return;
                modsDir = new java.io.File(chooser.getSelectedFile(), "mods");
            }

            if (!modsDir.exists() && !modsDir.mkdirs()) {
                throw new java.io.IOException("Could not create mods directory: " + modsDir);
            }
            java.io.File target = new java.io.File(modsDir, "hamilo-client.jar");
            if (source.getCanonicalFile().equals(target.getCanonicalFile())) {
                javax.swing.JOptionPane.showMessageDialog(owner,
                        "Hamilo is already installed here:\n" + target.getAbsolutePath(),
                        "Hamilo Client", javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            java.nio.file.Files.copy(source.toPath(), target.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            javax.swing.JOptionPane.showMessageDialog(owner,
                    "Hamilo was installed to:\n" + target.getAbsolutePath()
                    + "\n\nClose Minecraft completely, then launch the same Fabric profile again. "
                    + "Execute .jar cannot inject into a different running JVM.",
                    "Hamilo Client — Installed", javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } catch (Throwable error) {
            javax.swing.JOptionPane.showMessageDialog(owner,
                    "Install failed: " + error.getClass().getSimpleName() + ": " + error.getMessage(),
                    "Hamilo Client", javax.swing.JOptionPane.ERROR_MESSAGE);
            error.printStackTrace(System.err);
        }
    }

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
                    System.err.println("[Hamilo] Minecraft client class was not found before timeout.");
                    return;
                }

                // The class can load well before MinecraftClient and its window are ready.
                // Do not abort on a temporarily-null singleton during early startup.
                Object minecraft = null;
                Object windowObject = null;
                long window = 0L;
                Method getWindow = findMethod(minecraftClass,
                        new String[]{"method_22683", "getWindow"}, 0);
                Method getInstance = findMethod(minecraftClass,
                        new String[]{"method_1551", "getInstance"}, 0);

                for (int attempt = 0; attempt < 3000; attempt++) {
                    try {
                        minecraft = getInstance.invoke(null);
                        if (minecraft != null) {
                            windowObject = getWindow.invoke(minecraft);
                            if (windowObject != null) {
                                window = ((Number) invokeNoArgs(windowObject.getClass(),
                                        windowObject, "method_4490", "getHandle")).longValue();
                                if (window != 0L) break;
                            }
                        }
                    } catch (Throwable notReadyYet) {
                        // Minecraft or its window is still being initialized.
                    }
                    Thread.sleep(100L);
                }

                if (minecraft == null || windowObject == null || window == 0L) {
                    System.err.println("[Hamilo] Minecraft loaded, but its client window never became ready.");
                    return;
                }

                ClassLoader gameLoader = minecraftClass.getClassLoader();
                Class<?> glfw = Class.forName("org.lwjgl.glfw.GLFW", true, gameLoader);
                Method getKey = glfw.getMethod("glfwGetKey", long.class, int.class);
                Method setScreen = findMethod(minecraftClass,
                        new String[]{"method_1507", "setScreen"}, 1);
                Method execute = findMethod(minecraftClass,
                        new String[]{"method_18859", "execute"}, 1);
                Class<?> screenClass = loadScreenClass(gameLoader);
                Field screenField = findField(minecraftClass,
                        "field_1755", "currentScreen");

                // Show the GUI automatically as soon as the actual game client is ready.
                final Object readyMinecraft = minecraft;
                execute.invoke(minecraft, (Runnable) () -> {
                    try {
                        setScreen.invoke(readyMinecraft,
                                screenClass.getDeclaredConstructor().newInstance());
                    } catch (Throwable error) {
                        System.err.println("[Hamilo] Could not open GUI automatically:");
                        error.printStackTrace(System.err);
                    }
                });

                boolean wasDown = false;
                System.out.println("[Hamilo] Loaded into Minecraft. GUI opened; press Right Shift to toggle it.");
                while (!Thread.currentThread().isInterrupted()) {
                    int state = ((Number) getKey.invoke(null, window, 344)).intValue();
                    boolean down = state == 1;
                    if (down && !wasDown) {
                        execute.invoke(minecraft, (Runnable) () -> {
                            try {
                                Object current = screenField.get(readyMinecraft);
                                Object next = current != null && screenClass.isInstance(current)
                                        ? null : screenClass.getDeclaredConstructor().newInstance();
                                setScreen.invoke(readyMinecraft, next);
                            } catch (Throwable error) {
                                System.err.println("[Hamilo] Could not toggle GUI:");
                                error.printStackTrace(System.err);
                            }
                        });
                    }
                    wasDown = down;
                    Thread.sleep(16L);
                }
            } catch (Throwable error) {
                System.err.println("[Hamilo] Injection bootstrap failed:");
                error.printStackTrace(System.err);
            }
        }, "Hamilo-Agent-Bootstrap");
        // Keep the Execute .jar JVM alive while the bootstrap waits for Minecraft.
        worker.setDaemon(false);
        worker.start();
    }

    private static Class<?> waitForMinecraft(Instrumentation instrumentation)
            throws InterruptedException {
        for (int attempt = 0; attempt < 3000; attempt++) {
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

    /**
     * If the launcher loads the agent in a loader separate from Minecraft,
     * define the GUI in a child of the game loader so its Minecraft superclass
     * and method signatures resolve against the live game classes.
     */
    private static Class<?> loadScreenClass(ClassLoader gameLoader) throws Exception {
        String className = "dev.hamilo.client.HamiloClickGuiScreen";
        try {
            return Class.forName(className, true, gameLoader);
        } catch (ClassNotFoundException notVisibleToGameLoader) {
            try (InputStream input = HamiloAgent.class.getResourceAsStream(
                    "/dev/hamilo/client/HamiloClickGuiScreen.class")) {
                if (input == null) {
                    throw new ClassNotFoundException(
                            "GUI class resource is missing from the injected JAR", notVisibleToGameLoader);
                }
                byte[] bytes = readAllBytes(input);
                class ScreenClassLoader extends ClassLoader {
                    ScreenClassLoader(ClassLoader parent) {
                        super(parent);
                    }

                    Class<?> defineScreen(byte[] classBytes) {
                        return defineClass(className, classBytes, 0, classBytes.length);
                    }
                }
                return new ScreenClassLoader(gameLoader).defineScreen(bytes);
            }
        }
    }

    /** Java 8-compatible stream reader for broad injector compatibility. */
    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
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

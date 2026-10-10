# Hamilo Client

Hamilo Client is an experimental Minecraft Java Edition 1.21 client GUI packaged as a Java agent JAR for launchers that explicitly support loading/injecting Java agent JARs.

## Current features

- Press **Right Shift** to toggle the Hamilo Client GUI.
- Press **Escape** to close the GUI.
- The GUI does not pause single-player gameplay.
- GitHub Actions builds the agent JAR.

## Compatibility

- Minecraft Java Edition 1.21
- Java 21
- A launcher/runtime that supports this JAR's Java-agent entry point and compatible class/mapping access

**Important:** A Java agent JAR is not the same as a Fabric mod JAR. This project no longer requires a Fabric profile or Fabric API. However, Java-agent support varies by launcher. Pojav's “Inject .jar” option must support Java instrumentation agents for this artifact to work; the manifest alone cannot add that capability. If Pojav reports that it cannot read the file, make sure you selected the actual `.jar`, not the downloaded GitHub Actions artifact `.zip`.

## Download from GitHub Actions

1. Open the repository's **Actions** tab and select a successful build.
2. Download the `hamilo-client-jar` artifact.
3. Extract the downloaded ZIP archive.
4. Select the contained `hamilo-client-1.0.0.jar` file in the launcher's injection option. Do not rename or select the ZIP itself.

The artifact is a ZIP because GitHub Actions wraps uploaded artifacts in ZIP archives; the JAR is inside it.

## Build locally

Use Java 21 and Gradle 8.10:

```sh
gradle build
```

The agent JAR is written to `build/libs/hamilo-client-1.0.0.jar`.

## Loading model and limitations

The JAR declares `Premain-Class` and `Agent-Class` in its manifest. This is the Java instrumentation agent mechanism, not Fabric's mod entrypoint. It does not guarantee that a launcher supports injection or that the agent can resolve Minecraft classes in every runtime. The current bootstrap is experimental and must be validated against the exact Pojav version and Minecraft runtime.

## GitHub Actions

Push a commit or manually run the build workflow. Download the `hamilo-client-jar` artifact from a successful run and extract it before selecting the JAR.

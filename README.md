# Hamilo Client

Hamilo Client is an experimental Minecraft Java Edition 1.21 client GUI packaged as a Pojav-style `PreMain-Class` injection JAR.

## Current features

- Press **Right Shift** to toggle the Hamilo Client GUI.
- Press **Escape** to close the GUI.
- The GUI does not pause single-player gameplay.
- GitHub Actions builds the injection JAR.

## Compatibility

- Minecraft Java Edition 1.21
- Java 21
- A Pojav build that supports the custom `PreMain-Class` injection entry point.

**Execute .jar bootstrap:** The JAR now declares `Main-Class: dev.hamilo.client.HamiloAgent` and includes a Java 8-compatible `main(String[])` entry point, in addition to `PreMain-Class` and the standard Java-agent entries. This supports launchers that execute the JAR entry point inside the Minecraft JVM (or otherwise expose the live game's class loaders to it).

A standalone `java -jar` process cannot inject into a different Minecraft process just because the manifest contains `Main-Class`. The launcher's **Execute .jar** implementation must run the entry point in the game process or provide a real attach/injection bridge. If Pojav reports that its Execute .jar feature is incompatible with Java 21, this JAR change cannot override that launcher limitation. The bootstrap change is not yet verified against your exact Pojav build.

This is an injected client JAR, not a Fabric mod JAR. It does not require Fabric API. The GUI toggles with **Right Shift** once the bootstrap can see the running Minecraft client.

## Download from GitHub Actions

1. Open the repository's **Actions** tab and select the latest successful build.
2. Download the `hamilo-client-injectable-jar` artifact.
3. Extract the downloaded ZIP archive.
4. Use the launcher's JAR execution/injection option supported by your Pojav build. Do not select the artifact ZIP.

GitHub Actions wraps artifacts in ZIP archives; the actual JAR is inside.

## Build locally

Use Java 21 and Gradle 8.10:

```sh
gradle build
```

The agent JAR is written to `build/libs/hamilo-client-1.0.0.jar`.

## Validation

A successful GitHub Actions build confirms compilation and checks the injection manifest. It does not by itself prove compatibility with every Pojav build; test injection in the exact launcher version you use.
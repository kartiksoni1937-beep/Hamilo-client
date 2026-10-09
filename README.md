# Hamilo Client

Hamilo Client is a client-side Minecraft Java Edition 1.21 client built as a Fabric JAR. The same artifact can be used on Windows and Android launchers such as PojavLauncher when the selected Minecraft, Java, Fabric Loader, and device configuration are compatible.

## Current features

- Press **Right Shift** to open the Hamilo Client GUI.
- Press **Escape** to close the GUI.
- The GUI does not pause single-player gameplay.
- GitHub Actions builds the project and uploads the compiled JAR as a workflow artifact.

## Requirements

- Minecraft Java Edition 1.21
- Fabric Loader 0.16.5 or newer
- Fabric API for Minecraft 1.21
- Java 21
- Gradle 8.10 or compatible

## Build

From the repository root, run:

```sh
gradle build
```

The compiled JAR will be in `build/libs/`.

## Install on Windows

1. Create or select a Minecraft 1.21 Fabric profile with Java 21.
2. Download the `hamilo-client-jar` artifact from a successful GitHub Actions build, or build it locally.
3. Place the Hamilo Client JAR in that profile's `mods` folder.
4. Ensure Fabric API for Minecraft 1.21 is also in the `mods` folder, then launch that Fabric profile.

## Install on Android with PojavLauncher

1. Create or select a Minecraft 1.21 Fabric installation in PojavLauncher and use a compatible Java 21 runtime.
2. Download the same `hamilo-client-jar` artifact.
3. Put the JAR in the `mods` folder associated with that PojavLauncher game directory.
4. Add a compatible Fabric API JAR to the same folder and start the Fabric installation.

PojavLauncher configurations vary by device and runtime. If the game fails to start, check the launcher log for Java, Fabric Loader, or mod-version errors.

## GitHub Actions

Push a commit or open a pull request to run the build workflow. When the workflow succeeds, download the `hamilo-client-jar` artifact from the workflow run's **Artifacts** section.

## Loading model

Hamilo Client is packaged as a regular Fabric client JAR and loaded by Fabric at game startup. It does not inject code into an already-running Minecraft process. Fabric's normal loading path is the supported way to use the same client artifact on compatible Windows and PojavLauncher installations.

## Notes

This is an initial client GUI scaffold. It does not yet include configurable feature modules.

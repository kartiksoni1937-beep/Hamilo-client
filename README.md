# Hamilo Client

Hamilo Client is a Minecraft Java Edition 1.21 client GUI for Fabric Loader, with a Java-agent bootstrap retained for launchers that explicitly support it.

## Features

- Press **Right Shift** to toggle the Hamilo GUI.
- Press **Escape** to close the GUI.
- The GUI does not pause single-player gameplay.

## Recommended Pojav + Fabric setup

For Hamilo to load into a Minecraft instance that has not started yet, Fabric must load Hamilo when that instance launches. A JAR executed as a separate process cannot inject into a future Minecraft process by itself.

1. Build/download the JAR from the latest successful GitHub Actions run.
2. Put the actual `.jar` file (not the artifact ZIP) in the selected profile's `.minecraft/mods` folder.
3. Launch the Fabric 1.21 profile in Pojav.
4. Press **Right Shift** in-game to open Hamilo.

The JAR now includes `fabric.mod.json` and the Fabric client entrypoint. It also retains the agent manifest entries for Pojav builds that explicitly support `PreMain-Class`; those entries alone do not guarantee support for Pojav's separate **Execute .jar** flow.

## Compatibility

- Minecraft Java Edition 1.21
- Java 21
- Fabric Loader 0.16.5 or newer
- Fabric API for Minecraft 1.21

## Build locally

Use Java 21 and Gradle 8.10:

```sh
gradle build
```

The JAR is written to `build/libs/hamilo-client-1.0.0.jar`.

## Validation

A successful GitHub Actions build confirms compilation and metadata checks. It does not by itself prove runtime compatibility with every Pojav build; test it in the exact Fabric profile you use.

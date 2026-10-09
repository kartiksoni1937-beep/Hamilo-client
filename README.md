# Hamilo Client

Hamilo Client is a client-side Minecraft Java Edition 1.21 mod built with Fabric and Java 21.

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

The compiled mod JAR will be in `build/libs/`.

## GitHub Actions

Push a commit or open a pull request to run the build workflow. When the workflow succeeds, download the `hamilo-client-jar` artifact from the workflow run's **Artifacts** section.

## Notes

This is an initial client GUI scaffold. It does not yet include configurable feature modules.

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

**Important:** Do not choose **Execute .jar** for a running Java 21 game if your Pojav build reports that this feature is incompatible with Java 21. That message comes from the launcher's execution feature; changing the JAR manifest cannot override it. Launch Minecraft first, then use the launcher's **Inject .jar** option if your installed build provides it. If that option is unavailable or rejects the JAR, the launcher itself needs a compatible injection path.

This is an injected client JAR, not a Fabric mod JAR. It does not require Fabric API. The manifest includes `PreMain-Class` for Pojav-style injection and the standard Java-agent keys for compatible launchers. Runtime behavior still depends on the specific launcher build and Minecraft class mappings.

## Download from GitHub Actions

1. Open the repository's **Actions** tab and select the latest successful build.
2. Download the `hamilo-client-injectable-jar` artifact.
3. Extract the downloaded ZIP archive.
4. Select the `.jar` file inside it in **Inject .jar**. Do not select the artifact ZIP.

GitHub Actions wraps artifacts in ZIP archives; the actual JAR is inside.

## Build locally

Use Java 21 and Gradle 8.10:

```sh
gradle build
```

The agent JAR is written to `build/libs/hamilo-client-1.0.0.jar`.

## Validation

A successful GitHub Actions build confirms compilation and checks the injection manifest. It does not by itself prove compatibility with every Pojav build; test injection in the exact launcher version you use.
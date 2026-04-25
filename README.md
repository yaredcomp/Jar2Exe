# JavaPackagerFX

JavaPackagerFX is a desktop utility built with JavaFX that helps developers package Java applications into distributable desktop bundles (especially Windows `.exe` installers) using `jpackage`.

## Project Goal

The project aims to simplify Java desktop distribution by providing a guided UI for:

- selecting a Java project or JAR input
- detecting and configuring the main class
- defining app metadata (name, version, vendor, icon, license)
- choosing packaging options (installable vs portable, shortcuts, menu, console)
- executing packaging workflows without manually crafting long `jpackage` commands

## Key Features

- JavaFX UI with light/dark theme toggle
- Multi-step flow for source, identity, and output configuration
- Main-class auto-detection from project/JAR
- Build support for Maven, Gradle, and Ant projects
- Packaging through `jpackage`
- Installer options for Windows (desktop shortcut, start menu, console)
- Icon handling with Windows compatibility fallback/conversion
- Inline validation feedback for required fields
- Editable default EULA text, with automatic license file generation during packaging

## Tech Stack

- Java 17+
- JavaFX
- Maven
- Ikonli (icons)

## Prerequisites

- JDK 17 or newer
- `jpackage` available (comes with modern JDKs)
- **WiX Toolset (v3.11 or later)**: **Required** on Windows to create `.exe` or `.msi` installers. Ensure it is installed and the `bin` folder is added to your system `PATH`.
- Build tools depending on source project:
  - Maven (`mvn`)
  - Gradle (`gradle`)
  - or Ant (`ant`)

## Getting Started

1. Clone the repository.
2. Build the project:

```bash
mvn clean package
```

3. Run the application from your IDE or using your preferred Java launch command.

## How It Works

1. **Source Setup**  
   Select a Java project folder or JAR and confirm the main class.

2. **App Identity**  
   Set name, version, vendor, icon, and license text/file.

3. **Package Options**  
   Choose output directory and package type, then validate/build.

The app orchestrates project build (if needed), staging, and `jpackage` execution to produce final artifacts.

## Output

By default, packages are generated in a configurable output folder (for example under the user home directory), including app images/installers and helper generated files like license text when required.

## Notes

- On Windows installable targets, `.ico` is preferred for icons.  
  If another image format is provided, the app attempts conversion for compatibility.
- Some packaging behavior depends on the source project structure and available build tools.
- If you encounter errors regarding "light.exe" or "candle.exe", double-check your WiX Toolset installation and PATH configuration.

## License

No explicit open-source license file is currently included in this repository.  
Add a `LICENSE` file if you want to define distribution terms for the project itself.

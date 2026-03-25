# Branding and Distribution Guide

This guide details how to change the application logo and compile/package the game for Windows, macOS, and Linux.

## 1. Changing the Application Logo

The game window currently generates its own icon programmatically in `com.samarbhumi.ui.GameWindow` (inside the `buildAppIcon()` method). To use your own image instead of the programmatic one:

### Step 1: Add your App Logo
1. Create a `resources` folder in the root directory if it doesn't already exist (or use `src/resources`).
2. Place your logo there (e.g., `app_logo.png`). A resolution of 256x256 or 512x512 is recommended.

### Step 2: Update `GameWindow.java`
Replace the `setIconImage(buildAppIcon());` line with:
```java
try {
    java.net.URL imgURL = GameWindow.class.getResource("/app_logo.png");
    if (imgURL != null) {
        ImageIcon icon = new ImageIcon(imgURL);
        setIconImage(icon.getImage());
    } else {
        setIconImage(buildAppIcon()); // Fallback
    }
} catch (Exception e) {
    setIconImage(buildAppIcon());
}
```
*Note: Ensure the `resources` folder is included in your classpath during compilation.*

---

## 2. Packaging for Windows (.exe & Installer)

To apply changes to the `.exe` and the installer, you must re-wrap the `.jar` and re-compile the Inno Setup script.

### Step 1: Create the new JAR
1. Open a terminal in the project root.
2. Compile and package the JAR:
   ```cmd
   mkdir bin
   javac -d bin src/com/samarbhumi/core/*.java src/com/samarbhumi/entity/*.java src/com/samarbhumi/physics/*.java src/com/samarbhumi/weapon/*.java src/com/samarbhumi/ai/*.java src/com/samarbhumi/map/*.java src/com/samarbhumi/progression/*.java src/com/samarbhumi/audio/*.java src/com/samarbhumi/ui/*.java src/com/samarbhumi/exception/*.java src/Main.java
   jar cfe Samarbhumi.jar Main -C bin .
   ```

### Step 2: Update the `.exe` (Launch4j)
If you already have a `Samarbhumi.xml` (Launch4j config):
1. Download and open **Launch4j**.
2. Open the `Samarbhumi.xml` configuration file from the project root.
3. Go to the **Icon** tab and select your custom `.ico` file (convert your `app_logo.png` to `.ico`).
4. Click the gear icon ("Build Wrapper") to generate the new `Samarbhumi.exe`.

### Step 3: Update the Installer (Inno Setup)
1. Download and install **Inno Setup**.
2. Open `Samarbhumi_v1.1.iss`.
3. Locate `SetupIconFile=` under the `[Setup]` section and point it to your `.ico` file:
   ```ini
   SetupIconFile=app_logo.ico
   ```
4. Click **Compile**. This will generate `Samarbhumi_vX.X_Setup.exe` in the `Output` folder.

---

## 3. Packaging for macOS (.app)

Use `jpackage` (included with JDK 14+) to create a native macOS `.app` bundle and `.dmg` installer.
1. Convert your `app_logo.png` to a macOS icon format (`app_logo.icns`).
2. Run the following command from your Mac terminal in the project root:
   ```bash
   jpackage --type app-image --name Samarbhumi --input . --main-jar Samarbhumi.jar --icon app_logo.icns --mac-package-name "Samarbhumi"
   ```
3. To create a `.dmg` installer instead, change `--type app-image` to `--type dmg`.

---

## 4. Packaging for Linux (.deb / .rpm)

Use `jpackage` to create native Linux installers.
1. Place your `app_logo.png` in the project root.
2. Run the following command from your Linux terminal in the project root:
   ```bash
   jpackage --type deb --name samarbhumi --input . --main-jar Samarbhumi.jar --icon app_logo.png --app-version 1.1 --description "Samarbhumi - War Never Ends"
   ```
   *(Change `--type deb` to `--type rpm` if targeting Fedora/RHEL-based systems).*

---

## 5. Pushing to GitHub Releases

To distribute the new installers:
1. Go to your GitHub repository -> **Releases** -> **Draft a new release**.
2. Create a new tag (e.g., `v1.2`).
3. Set the Release title.
4. Drag and drop the following generated files into the "Attach binaries by dropping them here" area:
   - `Samarbhumi.jar` (Cross-platform)
   - `Samarbhumi_v1.2_Setup.exe` (Windows Installer)
   - `Samarbhumi.dmg` (macOS Installer)
   - `samarbhumi_1.2_amd64.deb` (Linux Installer)
5. Click **Publish release**.

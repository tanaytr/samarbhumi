# SAMARBHUMI — Master Deployment Guide `v1.2`
### The complete pipeline from source code to multi-platform distribution.

---

## 1. Project Organization
This guide assumes you are working from the project root.
- **Entry Point**: `src/Main.java`
- **Networking**: `com.samarbhumi.net` (Relay Server integration)
- **Built-in Docs**: Look at `BRANDING_GUIDE.md` for logo customization.

---

## 2. PART 1 — The GitHub Workflow (Command Line)
Since you are updating an existing project, follow these steps to ensure your GitHub matches your local code perfectly.

### Step 1: Force Update your Repository
If your local code is "better" than GitHub and you want to overwrite it:
```bash
git add .
git commit -m "Final Samarbhumi v1.2 Release — Networking & Leaderboard"
git push -f rocket master
```

### Step 2: Create a Version Tag
Tags are what GitHub uses to trigger "Releases".
```bash
git tag -a v1.2 -m "Samarbhumi Version 1.2"
git push rocket v1.2
```

---

## 3. PART 2 — Local Windows Build (Manual)
Use this for quick testing on your own machine.

### Step 1: Compile all Modules
Run this from the root folder:
```bash
mkdir bin
javac -encoding UTF-8 -d bin src\com\samarbhumi\exception\*.java src\com\samarbhumi\core\*.java src\com\samarbhumi\physics\*.java src\com\samarbhumi\map\*.java src\com\samarbhumi\weapon\*.java src\com\samarbhumi/entity\*.java src\com\samarbhumi\ai\*.java src\com\samarbhumi\progression\*.java src\com\samarbhumi\audio\*.java src\com\samarbhumi\net\*.java src\com\samarbhumi\ui\*.java src\Main.java
```

### Step 2: Create the JAR
```bash
copy resources\fonts\*.ttf bin\
copy resources\app_logo.png bin\
cd bin
echo Main-Class: Main > manifest.txt
jar cfm ..\Samarbhumi.jar manifest.txt .
cd ..
```

### Step 3: Wrap in .exe (Launch4j)
1. Open **Launch4j**.
2. Open `Samarbhumi.xml` (provided in root).
3. Click the **Gear Icon** to build `Samarbhumi.exe`.

### Step 4: Make Installer (Inno Setup)
1. Open **Inno Setup**.
2. Open `Samarbhumi_v1.2.iss`.
3. Click **Compile** to generate the `.exe` installer in the `Output/` folder.

---

## 4. PART 3 — The "Magic" Release (Mac/Linux from Windows)
Since you are on Windows, you cannot build macOS or Linux installers on your own machine. Instead, we use **GitHub Actions** (the file I added in `.github/workflows/build.yml`).

### How it works (The Explanation)
Think of GitHub Actions as a **High-End Packaging Factory**. When you push a **Tag**, GitHub runs professional industry tools to create:
1.  **Windows**: A professional **Setup Installer** (similar to Inno Setup). It has a Directory Chooser, License/Description, and adds the game to the Windows **Add/Remove Programs** list.
2.  **macOS**: A standard **Disk Image (.dmg)**. When users open it, they see the **Samarbhumi.app** bundle which they can drag to their Applications folder.
3.  **Linux**: A proper **Debian Package (.deb)**. Users can install it with a double-click or `sudo apt install`, and the game will appear in their system menu.

### Step-by-Step: From Windows to a Global Release
Follow these exact 5 steps to get your **Full Installers**:

1.  **Finalize your code**:
    ```bash
    git add .
    git commit -m "Final v1.2 Release"
    git push rocket master
    ```
2.  **Trigger the Magic Build (The Tag)**:
    Run these two commands. The tag is what "wakes up" the Mac/Linux build servers:
    ```bash
    git tag v1.2
    git push rocket v1.2
    ```
3.  **Watch the Build**:
    Go to your GitHub repo on your browser. Click the **"Actions"** tab at the top. You will see a workflow named **"Build and Release"** starting. Wait ~5 minutes until all three green checkmarks appear.
4.  **Grab the Files**:
    Click on the successful run -> scroll down. You will see these files under **"Artifacts"**:
    - **Windows**: `Samarbhumi-Setup.exe` (**Full Setup Installer**)
  - **macOS**: `Samarbhumi.dmg` (**Disk Image with .app bundle**)
  - **Linux**: `samarbhumi_1.2_amd64.deb` (**Debian/Ubuntu Package**)
  - **All**: `Samarbhumi.jar` (Cross-platform JAR)
    Download these to your Windows computer and unzip them.
5.  **Create the Release**:
    Go to **Releases** on GitHub -> **Draft a new release** -> Select tag `v1.2` -> Drag and drop these files (and your local Windows EXE) into the release assets.

---

## 5. PART 4 — Linking to the Landing Page (`index.html`)
To make your download buttons work, you must link them to the **Latest Release** URL pattern.

In `index.html`, use these exact link patterns (replace `YOUR_USERNAME`):

- **Windows**: `https://github.com/YOUR_USERNAME/samarbhumi/releases/latest/download/Samarbhumi-Setup.exe`
- **macOS**: `https://github.com/YOUR_USERNAME/samarbhumi/releases/latest/download/Samarbhumi.dmg`
- **Linux**: `https://github.com/YOUR_USERNAME/samarbhumi/releases/latest/download/samarbhumi_1.2_amd64.deb`
- **JAR**: `https://github.com/YOUR_USERNAME/samarbhumi/releases/latest/download/Samarbhumi.jar`

---

### Quick Reference Table

| Platform | File Format | Build Tool | Link Icon Class |
|----------|-------------|------------|-----------------|
| Windows  | `.exe`      | Inno Setup | `fab fa-windows` |
| macOS    | `.dmg`      | jpackage   | `fab fa-apple`   |
| Linux    | `.deb`      | jpackage   | `fab fa-linux`   |
| Universal| `.jar`      | javac/jar  | `fab fa-java`    |

*Master Guide v1.2 finalized for production distribution.*
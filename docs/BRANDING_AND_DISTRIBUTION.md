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

## 2. Keeping it Updated

Refer to [DEPLOY.md](DEPLOY.md) for current compilation commands and cross-platform packaging (Windows, macOS, Linux).

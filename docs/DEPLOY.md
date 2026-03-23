# SAMARBHUMI — Full Deployment Guide  `v1.1`
### From source code on your machine → landing page on the internet → downloadable Windows .exe

---

## Overview of what we're building

```
GitHub repo (code + releases)
       ↓
GitHub Pages (free landing page at username.github.io/samarbhumi)
       ↓
"Download Now" button → GitHub Release → Samarbhumi-Setup.exe
                                       → Samarbhumi.jar (cross-platform)
```

Everything here is **free**. No hosting costs, no domain needed (optional).

---

## PART 1 — Put the code on GitHub

### Step 1 — Create a GitHub account
Go to https://github.com and sign up if you don't have an account.

### Step 2 — Create a new repository
- Click the **+** button (top right) → **New repository**
- Name: `samarbhumi`
- Description: `2D action shooter — Mini Militia inspired, built in Java`
- Set to **Public**
- Do NOT tick "Add a README" (we already have one)
- Click **Create repository**

### Step 3 — Upload your code
GitHub will show you a page with commands. Open Command Prompt **inside your Samarbhumi folder** and run:

```bash
git init
git add .
git commit -m "Initial release — Samarbhumi v1.0"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/samarbhumi.git
git push -u origin main
```

Replace `YOUR_USERNAME` with your actual GitHub username.

If it asks for login: use your GitHub username and a **Personal Access Token** (not your password).
Get a token at: GitHub → Settings → Developer settings → Personal access tokens → Generate new token (classic) → tick `repo` → Generate.

---

## PART 2 — Create a Windows .exe installer

We'll use **Launch4j** (free, no install needed) to wrap the game into an `.exe`, then **Inno Setup** (free) to make a proper installer.

### Step 1 — Build a runnable JAR

Open Command Prompt in your Samarbhumi folder and run:

```bash
mkdir bin
javac -encoding UTF-8 -d bin src\com\samarbhumi\exception\*.java src\com\samarbhumi\core\Vec2.java src\com\samarbhumi\core\AABB.java src\com\samarbhumi\core\GameConstants.java src\com\samarbhumi\core\Enums.java src\com\samarbhumi\core\InputState.java src\com\samarbhumi\physics\PhysicsBody.java src\com\samarbhumi\map\GameMap.java src\com\samarbhumi\map\MapFactory.java src\com\samarbhumi\weapon\Weapon.java src\com\samarbhumi\weapon\Projectile.java src\com\samarbhumi\weapon\ParticleSystem.java src\com\samarbhumi\entity\Player.java src\com\samarbhumi\entity\PickupItem.java src\com\samarbhumi\ai\BotController.java src\com\samarbhumi\progression\PlayerProfile.java src\com\samarbhumi\core\GameSession.java src\com\samarbhumi\audio\AudioEngine.java src\com\samarbhumi\ui\PlayerRenderer.java src\com\samarbhumi\ui\MapRenderer.java src\com\samarbhumi\ui\HUDRenderer.java src\com\samarbhumi\ui\UIRenderer.java src\com\samarbhumi\ui\GameScreen.java src\com\samarbhumi\ui\Screens.java src\com\samarbhumi\ui\GameWindow.java src\Main.java
```

Then create the JAR (includes fonts):
```bash
copy resources\fonts\GameFont-Bold.ttf bin\
copy resources\fonts\GameFont-Regular.ttf bin\
cd bin
echo Main-Class: Main > manifest.txt
jar cfm ..\Samarbhumi.jar manifest.txt .
cd ..
```

Test it runs:
```bash
java -jar Samarbhumi.jar
```

### Step 2 — Wrap into .exe with Launch4j

1. Download **Launch4j** from: https://launch4j.sourceforge.net
2. Run `launch4j.exe` — no installation needed
3. Fill in these fields:

   **Basic tab:**
   - Output file: `C:\path\to\Samarbhumi\Samarbhumi.exe`
   - Jar: `C:\path\to\Samarbhumi\Samarbhumi.jar`

   **JRE tab:**
   - Min JRE version: `17.0.0`
   - Tick "Bundle JRE" if you want a self-contained exe (see note below)

   **Header tab:**
   - Header type: `GUI` (not console — hides the terminal window)

4. Click the **gear icon** (Build wrapper) → `Samarbhumi.exe` is created

> **Note on bundling JRE:** If you tick "Bundle JRE", your installer will be ~60MB but users don't need Java installed. Without it, the exe just checks Java is installed and tells users to install it if not.

### Step 3 — Make an installer with Inno Setup

1. Download **Inno Setup** from: https://jrsoftware.org/isdl.php
2. Install it, open it, click **"Create a new script using the Script Wizard"**
3. Fill in the wizard:
   - Application name: `Samarbhumi`
   - Application version: `1.0`
   - Application publisher: your name
   - Application folder name: `Samarbhumi`
4. On the **Files** step, add:
   - `Samarbhumi.exe`
   - `resources\fonts\` folder (for font fallback)
   - `saves\` folder (empty, creates the saves directory)
5. On **Icons** step: tick "Create a desktop shortcut"
6. Click **Finish** → then **Compile** → `Samarbhumi-Setup.exe` is created

### Step 4 — Upload the installer to GitHub Releases

1. Go to your GitHub repo page
2. Click **Releases** (right sidebar) → **Create a new release**
3. Tag: `v1.0`
4. Title: `Samarbhumi v1.0 — Initial Release`
5. Description (copy this):
   ```
   ## Samarbhumi — War Never Ends v1.0

   A Mini Militia-inspired 2D shooter built entirely in Java.

   ### Download
   - **Windows**: Download `Samarbhumi-Setup.exe` and run it
   - **All platforms**: Download `Samarbhumi.jar` (requires Java 17+)

   ### What's included
   - 3 hand-crafted maps
   - 9 weapons with real physics
   - AI bots (Easy / Medium / Hard)
   - Local 2-player support
   - Progression system — 30 levels, unlockable cosmetics
   ```
6. Drag and drop both files into the release:
   - `Samarbhumi-Setup.exe`
   - `Samarbhumi.jar`
7. Click **Publish release**

---

## PART 3 — Landing Page on GitHub Pages

### Step 1 — Create the landing page file

In your Samarbhumi repo folder, create a file called `index.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Samarbhumi — War Never Ends</title>
  <style>
    * { margin: 0; padding: 0; box-sizing: border-box; }
    body {
      background: #080c06;
      color: #e8e8dc;
      font-family: 'Segoe UI', Arial, sans-serif;
      min-height: 100vh;
    }
    /* Hero */
    .hero {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      padding: 40px 20px;
      background: radial-gradient(ellipse at center, #1a2e0e 0%, #080c06 70%);
      position: relative;
      overflow: hidden;
    }
    .hero::before {
      content: '';
      position: absolute;
      inset: 0;
      background-image: repeating-linear-gradient(
        60deg, transparent, transparent 30px,
        rgba(50,100,20,0.03) 30px, rgba(50,100,20,0.03) 31px
      );
    }
    .title {
      font-size: clamp(3rem, 10vw, 7rem);
      font-weight: 900;
      letter-spacing: 0.05em;
      background: linear-gradient(180deg, #ffd840 0%, #b87800 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      margin-bottom: 0.2em;
      position: relative;
    }
    .subtitle {
      font-size: clamp(1rem, 3vw, 1.5rem);
      color: #80c840;
      font-style: italic;
      letter-spacing: 0.2em;
      text-transform: uppercase;
      margin-bottom: 2.5rem;
    }
    .desc {
      max-width: 600px;
      font-size: 1.1rem;
      color: #a8b890;
      line-height: 1.7;
      margin-bottom: 3rem;
    }
    .buttons { display: flex; gap: 16px; flex-wrap: wrap; justify-content: center; }
    .btn {
      padding: 16px 40px;
      font-size: 1.1rem;
      font-weight: 700;
      border-radius: 8px;
      border: none;
      cursor: pointer;
      text-decoration: none;
      transition: transform 0.15s, box-shadow 0.15s;
      letter-spacing: 0.05em;
    }
    .btn:hover { transform: translateY(-2px); box-shadow: 0 8px 24px rgba(0,0,0,0.4); }
    .btn-primary {
      background: linear-gradient(135deg, #3a7810 0%, #205008 100%);
      color: #fff;
      border: 2px solid #60c030;
    }
    .btn-secondary {
      background: transparent;
      color: #a0c870;
      border: 2px solid #507830;
    }
    /* Features */
    .features {
      padding: 80px 20px;
      max-width: 1100px;
      margin: 0 auto;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 24px;
    }
    .feature-card {
      background: rgba(255,255,255,0.03);
      border: 1px solid rgba(80,130,40,0.3);
      border-radius: 12px;
      padding: 28px;
    }
    .feature-card h3 { color: #d4a820; font-size: 1.2rem; margin-bottom: 10px; }
    .feature-card p { color: #8a9870; line-height: 1.6; }
    /* Screenshots */
    .screens {
      background: rgba(255,255,255,0.02);
      padding: 60px 20px;
      text-align: center;
    }
    .screens h2 { color: #d4a820; font-size: 2rem; margin-bottom: 8px; }
    .screens p { color: #6a7858; margin-bottom: 30px; }
    .screen-grid {
      display: flex; gap: 16px; justify-content: center; flex-wrap: wrap;
    }
    .screen-placeholder {
      width: 340px; height: 200px;
      background: linear-gradient(135deg, #0d1a08, #182e0c);
      border: 1px solid rgba(80,130,40,0.3);
      border-radius: 8px;
      display: flex; align-items: center; justify-content: center;
      color: #405830; font-size: 0.9rem;
    }
    /* Download */
    .download {
      padding: 80px 20px;
      text-align: center;
      background: radial-gradient(ellipse at center, #142808 0%, #080c06 70%);
    }
    .download h2 { color: #d4a820; font-size: 2.2rem; margin-bottom: 12px; }
    .download p  { color: #8a9870; margin-bottom: 36px; font-size: 1.05rem; }
    .dl-options {
      display: flex; gap: 20px; justify-content: center; flex-wrap: wrap;
    }
    .dl-card {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(80,130,40,0.4);
      border-radius: 12px;
      padding: 28px 36px;
      min-width: 220px;
    }
    .dl-card h4 { color: #e8e8dc; font-size: 1.1rem; margin-bottom: 6px; }
    .dl-card p  { color: #6a7858; font-size: 0.9rem; margin-bottom: 18px; }
    /* Footer */
    footer {
      text-align: center;
      padding: 30px 20px;
      color: #405830;
      font-size: 0.85rem;
      border-top: 1px solid rgba(80,130,40,0.15);
    }
    footer a { color: #507840; text-decoration: none; }
  </style>
</head>
<body>

<section class="hero">
  <div class="title">SAMARBHUMI</div>
  <div class="subtitle">War Never Ends</div>
  <p class="desc">
    A fast-paced 2D action shooter inspired by Mini Militia.
    Fight across three battlegrounds with 9 weapons, jetpacks, AI bots,
    and a full progression system — all built from scratch in pure Java.
  </p>
  <div class="buttons">
    <a class="btn btn-primary"
       href="https://github.com/YOUR_USERNAME/samarbhumi/releases/latest/download/Samarbhumi-Setup.exe">
      &#9660; Download for Windows
    </a>
    <a class="btn btn-secondary"
       href="https://github.com/YOUR_USERNAME/samarbhumi">
      View on GitHub
    </a>
  </div>
</section>

<div class="features">
  <div class="feature-card">
    <h3>&#9654; 9 Weapons</h3>
    <p>Assault rifle, shotgun, sniper, SMG, rocket launcher, pistols, grenades and knife — each with real bullet physics and recoil.</p>
  </div>
  <div class="feature-card">
    <h3>&#9992; Jetpack Combat</h3>
    <p>Fly with limited fuel, recharge on the ground. Aerial battles add a vertical dimension most shooters ignore.</p>
  </div>
  <div class="feature-card">
    <h3>&#9733; Progression System</h3>
    <p>30 levels, XP from every kill, in-game coins, unlockable character skins, weapon camos, death effects and jet trails.</p>
  </div>
  <div class="feature-card">
    <h3>&#9876; Smart Bots</h3>
    <p>AI opponents that track you, take cover, use grenades, lead targets and retreat when low on health. Three difficulty levels.</p>
  </div>
  <div class="feature-card">
    <h3>&#9632; Three Maps</h3>
    <p>Warzone Alpha, Jungle Ruins, Steel Fortress — each with distinct terrain, platform layouts and tactical chokepoints.</p>
  </div>
  <div class="feature-card">
    <h3>&#9787; Local Multiplayer</h3>
    <p>Two players on the same keyboard. Player 1 uses WASD + mouse, Player 2 uses arrow keys + numpad.</p>
  </div>
</div>

<section class="screens">
  <h2>Screenshots</h2>
  <p>Replace these placeholders with actual screenshots once you run the game</p>
  <div class="screen-grid">
    <div class="screen-placeholder">Main Menu</div>
    <div class="screen-placeholder">Gameplay</div>
    <div class="screen-placeholder">Progression</div>
  </div>
</section>

<section class="download">
  <h2>Download &amp; Play</h2>
  <p>Requires Java 17+ — or use the Windows installer which bundles everything.</p>
  <div class="dl-options">
    <div class="dl-card">
      <h4>Windows Installer</h4>
      <p>Recommended. Installs the game and creates a desktop shortcut.</p>
      <a class="btn btn-primary"
         href="https://github.com/YOUR_USERNAME/samarbhumi/releases/latest/download/Samarbhumi-Setup.exe">
        &#9660; .exe Installer
      </a>
    </div>
    <div class="dl-card">
      <h4>Cross-Platform JAR</h4>
      <p>Works on Windows, Mac, Linux. Requires Java 17+ installed.</p>
      <a class="btn btn-secondary"
         href="https://github.com/YOUR_USERNAME/samarbhumi/releases/latest/download/Samarbhumi.jar">
        &#9660; .jar (All Platforms)
      </a>
    </div>
    <div class="dl-card">
      <h4>Source Code</h4>
      <p>Pure Java. No external libraries. Run with run.bat or run.sh.</p>
      <a class="btn btn-secondary"
         href="https://github.com/YOUR_USERNAME/samarbhumi">
        GitHub Repo
      </a>
    </div>
  </div>
</section>

<footer>
  <p>
    Samarbhumi &mdash; Built with pure Java, zero external libraries &mdash;
    <a href="https://github.com/YOUR_USERNAME/samarbhumi">GitHub</a>
  </p>
</footer>

</body>
</html>
```

> **Before pushing:** Replace every `YOUR_USERNAME` in `index.html` with your actual GitHub username.

### Step 2 — Enable GitHub Pages

1. Commit and push `index.html` to your repo:
   ```bash
   git add index.html
   git commit -m "Add landing page"
   git push
   ```
2. Go to your GitHub repo → **Settings** → **Pages** (left sidebar)
3. Under **Source**, select `main` branch, folder `/` (root)
4. Click **Save**
5. Wait ~2 minutes, then visit: `https://YOUR_USERNAME.github.io/samarbhumi`

Your landing page is now live.

---

## PART 4 — Custom Domain (Optional)

If you want `samarbhumi.com` instead of `username.github.io/samarbhumi`:

1. Buy a domain at Namecheap, GoDaddy, or Cloudflare Registrar (~$10/year)
2. In GitHub Pages settings → **Custom domain** → enter your domain
3. At your domain registrar, add these DNS records:
   ```
   Type: A    Name: @    Value: 185.199.108.153
   Type: A    Name: @    Value: 185.199.109.153
   Type: A    Name: @    Value: 185.199.110.153
   Type: A    Name: @    Value: 185.199.111.153
   Type: CNAME Name: www  Value: YOUR_USERNAME.github.io
   ```
4. Tick **Enforce HTTPS** in GitHub Pages settings
5. Wait up to 24 hours for DNS to propagate

---

## PART 5 — Keeping it updated

When you fix bugs or add features:

```bash
# Make your changes to source files
git add .
git commit -m "Fix: button clicks now work on all screen sizes"
git push
```

For a new release:
1. Rebuild `Samarbhumi.jar` (Step 2 above)
2. Rebuild `Samarbhumi-Setup.exe` (Launch4j + Inno Setup)
3. GitHub → Releases → **Draft a new release**
4. Tag: `v1.1`, upload new files, publish

---

## Quick Reference

| What you need    | Tool          | Cost | Link                                   |
|------------------|---------------|------|----------------------------------------|
| Code hosting     | GitHub        | Free | https://github.com                     |
| Landing page     | GitHub Pages  | Free | (built into GitHub)                    |
| .exe wrapper     | Launch4j      | Free | https://launch4j.sourceforge.net       |
| Windows installer| Inno Setup    | Free | https://jrsoftware.org/isdl.php        |
| Custom domain    | Namecheap etc | ~$10/yr | https://namecheap.com              |
| Java runtime     | Eclipse Temurin | Free | https://adoptium.net                 |

---

*The "Download Now" button on your landing page → GitHub Release → `.exe` installer → game on their desktop. Full pipeline, all free.*
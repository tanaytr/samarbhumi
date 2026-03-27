# SAMARBHUMI — War Never Ends
### A Java 2D Side-Scrolling Multiplayer Shooter  `v1.2.1`

---

## What's New in v1.2.1 (The Compatibility Update)
- **Maximum Linux Support:** Migrated the build system to `ubuntu-22.04` to ensure the `.deb` package flawlessly supports a vastly wider range of older and newer Linux distributions.
- **macOS Apple Silicon Support:** macOS bundles now natively build for Apple Silicon (M1/M2/M3/M4) while retaining full compatibility with older Intel Macs. 
- **Verified Distribution & Branding:** Windows Setup Installers now correctly identify the publisher as **"Shunya Labs"** in Add/Remove Programs. The build pipeline dynamically converts the app logo into native `.ico` (Windows) and `.icns` (macOS) formats so the game icon appears perfectly on desktop shortcuts and installers. (Note: UAC unknown publisher warnings natively require EV certs to completely bypass on Windows).
- **Flawless Multiplayer Hosting:** Added explicit Railway build configuration (`railway.json`) to guarantee the relay server launches perfectly for ultra-low latency combat.
- **UI Fixes:** Perfected the layout constraints of the "CREATE" button in both profile screens to eliminate visual overlap.

---

## What's New in v1.2 (The Multiplayer Update)
- **Online Multiplayer** — Full relay-based lobby system with 4-letter invite codes.
- **Lockstep Networking** — Deterministic synchronization for low-latency 2-4 player matches.
- **Enhanced Landing Page** — New starry sky background and premium UI animations.
- **Web Audio API BGM** — Real-time 8-bit theme music generated in-browser/in-app.
- **Packaging Tools** — Built-in support for native Windows/Mac/Linux distribution.

---

## What's New in v1.1
- **Home screen always loads on launch** — no forced profile selection screen on startup
- **Guest mode** — browse all menus freely; profile only required when you hit Play
- **No-profile popup** — clear, dismissible message guides you to Switch Profile if you try to play without one
- **Switch Profile fixed** — works in-app without restarting; single profile gives sign-out option instead of crashing
- **App icon fixed** — window/taskbar icon now displays correctly (no red rectangle)
- **Team leaderboard** — Vajra vs Pralay team scores show correctly in-game and post-match in all team modes

---

## Overview

Samarbhumi is a feature-complete, Mini Militia–inspired 2D action shooter built entirely in Java
with no external libraries. Every visual element is drawn programmatically via Java2D — no sprites,
no image assets. Music and sound effects are generated live through the Java MIDI synthesizer.

---

## Quick Start

### Requirements
- Java JDK 17 or later (OpenJDK or Oracle)
- Any OS: Windows, Linux, macOS

### Run — Windows
```
Double-click run.bat
```
Or in a terminal:
```
run.bat
```

### Run — Linux / macOS
```bash
chmod +x run.sh
./run.sh
```

The script compiles all sources into `bin/` then launches immediately.

---

## Controls

### Player 1 (Keyboard + Mouse)
| Action        | Key / Button         |
|---------------|----------------------|
| Move Left     | A                    |
| Move Right    | D                    |
| Jump          | W or Space           |
| Crouch        | S                    |
| Jetpack       | E (hold)             |
| Aim           | Mouse cursor         |
| Fire          | Left Mouse Button    |
| Alt Fire      | Right Mouse Button   |
| Melee / Knife | Q                    |
| Reload        | R                    |
| Swap Weapon   | Tab                  |
| Pick Up Item  | F                    |
| Pause         | Escape               |

### Player 2 (Local Co-op, same keyboard)
| Action     | Key            |
|------------|----------------|
| Move Left  | Left Arrow     |
| Move Right | Right Arrow    |
| Jump       | Up Arrow       |
| Crouch     | Down Arrow     |
| Jetpack    | Numpad 8       |
| Fire       | Numpad 0       |
| Melee      | Delete (Numpad)|
| Reload     | Numpad 5       |

---

## Game Modes

- **Solo vs Bots** — Face 1–5 AI opponents. Set difficulty (Easy / Medium / Hard).
- **Local Multiplayer** — Two human players on the same keyboard.
- **Deathmatch** — First to 20 kills wins. 5-minute time limit.

---

## Weapons

| Weapon          | Slot      | Ammo | Notes                              |
|-----------------|-----------|------|------------------------------------|
| Assault Rifle   | Primary   | 30   | Balanced — fast fire, good range   |
| Shotgun         | Primary   | 8    | 7 pellets per shot, huge close dmg |
| Sniper Rifle    | Primary   | 5    | One-shot heavy damage, slow reload |
| SMG             | Primary   | 35   | Very fast fire, slight spread      |
| Rocket Launcher | Primary   | 4    | Splash damage, physics knockback   |
| Pistol          | Secondary | 12   | Reliable sidearm                   |
| Dual Pistols    | Secondary | 14   | Unlocked at Level 6                |
| Grenade         | Secondary | 3    | Timed fuse, cooking arc throw      |
| Combat Knife    | Melee     | —    | Instant, no ammo, close range      |

---

## Maps

| Map             | Theme                        | Description                               |
|-----------------|------------------------------|-------------------------------------------|
| Warzone Alpha   | Open desert battleground     | Wide, central platforms, water trenches   |
| Jungle Ruins    | Vertical jungle ruins        | Ladders, dense cover, height advantage    |
| Steel Fortress  | Symmetric military base      | Metal terrain, tight corridors, two bases |

---

## Progression System

All progression is saved to `saves/profile.sav` between sessions.

- **XP** earned from kills (+50), match wins (+100)
- **Coins** earned from kills (+5), match wins (+30)
- **30 levels**, each unlocking one cosmetic item automatically
- **Store** — spend earned coins to unlock items early

### Level Unlock Tree (highlights)
| Level | Unlock            |
|-------|-------------------|
| 2     | Commando Skin     |
| 3     | Fire Jet Trail    |
| 5     | Explosion Death   |
| 6     | Dual Wield Pistols|
| 8     | Renegade Skin     |
| 10    | Arctic Camo       |
| 15    | Ghost Skin        |
| 25    | Rainbow Jet Trail |
| 30    | Legend Rank       |

---

## Architecture

```
com.samarbhumi
├── core/         GameConstants, Enums, Vec2, AABB, InputState, GameSession
├── physics/      PhysicsBody (gravity, integration, collision)
├── entity/       Player, PickupItem
├── weapon/       Weapon, Projectile (pooled), ParticleSystem (pooled)
├── ai/           BotController (state machine, LOS, leading)
├── net/          NetManager, NetworkSession (Lockstep Sync, Lobby)
├── map/          GameMap (tile collision, LOS raycasting), MapFactory
├── progression/  PlayerProfile (XP, unlocks, save/load)
├── audio/        AudioEngine (MIDI BGM + procedural SFX)
├── ui/           GameWindow, GameScreen, PlayerRenderer, MapRenderer,
│                 HUDRenderer, UIRenderer, Screens (all menu screens)
└── exception/    GameException, SaveException
```

---

## Technical Highlights

- **Custom physics engine** — gravity, jetpack thrust, friction, bounce, water buoyancy
- **Object pooling** — Projectile and Particle pools (zero GC during matches)
- **MIDI audio** — 4 BGM themes + 9 procedural SFX, all generated at runtime
- **BufferStrategy rendering** — triple-buffered active rendering at 60fps
- **Fixed-timestep game loop** — decoupled update (16.67ms) from render
- **Lockstep networking** — Frame-buffered deterministic input synchronization
- **Web Audio API** — Real-time 8-bit BGM synthesis
- **Persistent save** — Java ObjectOutputStream serialization

---

## Known Limitations

- Requires JDK 17+ for record syntax (UnlockEntry, StoreItem)

---

## Branding & Distribution

See `docs/BRANDING_AND_DISTRIBUTION.md` for detailed instructions on changing the application's logo, icons, and compiling packages for Windows, macOS, and Linux.

---
*Samarbhumi — Built with pure Java. No external libraries. No sprites. No compromises. | v1.2.1*
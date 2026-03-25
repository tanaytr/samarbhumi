# Samarbhumi — War Never Ends
## Complete Project Documentation

---

## Changelog

### v1.2 (current)
- **Online Multiplayer** — Full implementation of relay-based networking with 4-letter lobby codes.
- **Starry Landing Page** — Added dynamic starry sky and improved UI aesthetics.
- **8-bit BGM** — Integrated procedural background music using Web Audio API.
- **Maximized Window** — Application now launches in a maximized state for better UX.

### v1.1
- **Home page always loads first** — app starts on Main Menu, never forces profile select on launch
- **Guest mode** — players can browse menus without a profile; profile is only required to play
- **No-profile popup** — clicking PLAY without a profile shows a clear dismissible message with instructions
- **Switch Profile fixed** — single-profile: shows sign-out confirmation only; multiple profiles: switches in-app without closing; never kills the app
- **App icon** — window icon now renders correctly (programmatic, no missing resource/red-box)
- **Team leaderboard** — in-game scoreboard and post-match results correctly show Vajra (Blue) vs Pralay (Red) team breakdown in all team modes

### v1.0
- Initial release

---

## 1. Project Overview

**Samarbhumi** ("Battlefield" in Sanskrit) is a 2D multiplayer action shooter built entirely in Java using Swing/Java2D. Inspired by Mini Militia, it features real-time combat, AI bots, jetpacks, multiple weapons, and a full progression system.

- **Language:** Java 17+
- **Rendering:** Java2D (Swing Canvas with BufferStrategy)
- **Architecture:** Entity-Component pattern, fixed-timestep game loop, object pooling
- **Lines of Code:** ~5,500+

---

## 2. How to Run

### Windows (Recommended)
Double-click `run.bat`. It will:
1. Auto-detect Java on your system
2. If Java is missing, offer to download a portable JDK (~185 MB, no install required)
3. Compile all source files
4. Launch the game

### Linux / Mac
```bash
chmod +x run.sh
./run.sh
```

**Requirements:** JDK 17 or higher. Download from https://adoptium.net if needed.

---

## 3. Game Design

### Objective
**Deathmatch** — first player/bot to reach the kill target wins. The kill target scales with enemy count:
| Enemies | Kills to Win | Lives Before Losing |
|---------|-------------|---------------------|
| 1       | 3           | 5                   |
| 2       | 6           | 7                   |
| 3       | 10          | 9                   |
| 4       | 14          | 11                  |
| 5       | 18          | 12                  |

**Time limit:** 5 minutes. If time runs out, most kills wins.

### Maps
| Map | Theme | Features |
|-----|-------|----------|
| Warzone Alpha | Open grass field | Wide platforms, water trenches |
| Jungle Ruins | Dark stone ruins | Vertical ladders, dense cover |
| Steel Fortress | Metal bunker | Symmetric, tight corridors |

### Weapons
| Weapon | Speed | Damage | Notes |
|--------|-------|--------|-------|
| Assault Rifle | 780 px/s | 22 | Default primary |
| Shotgun | 600 px/s | 18×7 | Short range spread |
| Sniper Rifle | 1400 px/s | 90 | Long range, slow fire |
| SMG | 700 px/s | 12 | Fast rate of fire |
| Rocket Launcher | 380 px/s | 80 | Area damage |
| Pistol | 650 px/s | 18 | Default secondary |
| Combat Knife | Melee | 35 | Instant, no ammo |

---

## 4. Controls

### Player 1 — Arrow Keys
| Key | Action |
|-----|--------|
| ←/→ | Move left/right |
| ↑ | Jump (press twice quickly = double jump) |
| ↓ | Crouch / aim down |
| Arrow keys (held) | Also aim the gun |
| Num8 | Jetpack thrust |
| Num5 / Del | Melee attack |
| Num0 | Reload |
| End | Pick up item |
| PgDn | Swap weapon |
| Enter | Shoot (uses arrow key aim) |
| Mouse LMB | Fire (uses mouse aim — overrides arrows) |
| ESC | Pause |

### Player 2 — WASD (Local 2-Player mode)
| Key | Action |
|-----|--------|
| A/D | Move |
| W | Jump (double-tap = double jump) |
| S | Crouch |
| E | Jetpack |
| Q | Melee |
| R | Reload |
| F | Pickup |
| Tab | Swap weapon |
| Mouse LMB | Fire |

---

## 5. Architecture & Technical Design

### Core Systems

#### Game Loop (`GameWindow.java`)
Fixed-timestep loop at 60 FPS. The EDT (Swing thread) handles input events; the game loop runs on a dedicated thread. Mouse coordinates are back-projected through the scale/offset transform to logical 1280×720 coordinates before any game logic reads them.

**Profile / Guest Flow:**
- App always starts on Main Menu. `profile = null` means guest mode — menus render normally.
- PLAY requires a profile; clicking it as guest shows a non-blocking popup instead of crashing or forcing navigation.
- Switch Profile opens an overlay: single-profile case asks "sign out?"; multiple profiles switch in-app without restart.

#### Physics (`PhysicsBody.java`)
Every movable entity extends `PhysicsBody`. Forces accumulate per frame and integrate via semi-implicit Euler. Collision resolution uses AABB overlap with axis-separated response — resolves horizontal and vertical separately to avoid corner-catching.

#### Projectiles (`Projectile.java`)
Object pool of 256 projectiles — zero allocation during gameplay. Bullets have `gravScale=0` (straight flight). Grenades have full gravity + bounce timer. Rockets track velocity for rotation rendering.

#### AI (`BotController.java`)
State machine: `PATROL → CHASE → ATTACK → SEEK_HEALTH → FLEE`. Bots use ray-cast line-of-sight checks, remember last seen positions, lead-target bullet aim, and only flee at <12% HP. Difficulty controls reaction time and aim error.

#### Double Jump (`Player.java`)
`jumpsAvailable` counter (starts at 2) decremented on each edge-triggered jump press. Resets to 2 on landing. First jump: full `JUMP_VEL`. Second jump: 80% force. Clean edge detection via `pressed()` in `InputState` ensures each key-down fires exactly once regardless of hold duration.

#### Map Rendering (`MapRenderer.java`)
Tile palette chosen by `map.getMapStyle()` — each map has unique colors, textures, and surface details. Background uses 3-layer parallax scrolling with distinct themes (green sky, jungle canopy, grey fortress sky).

### Key Design Patterns
- **Object Pool** — Projectile reuse (zero GC during combat)
- **State Machine** — BotController AI, AppState in GameWindow
- **Observer-lite** — KillFeed and FloatText queues
- **Strategy** — Weapon types as enum with behavior fields
- **Fixed Timestep** — Accumulator-based game loop ensures deterministic physics

---

## 6. Progression System

- **XP:** +50 per kill, +100 for match win
- **Coins:** +5 per kill, +30 for win (spent in Store)
- **Levels:** 1–30, each unlocking cosmetics
- **Save file:** `saves/profile.sav` (Java serialization)

### Unlock Table
| Level | Unlock |
|-------|--------|
| 2 | Commando Skin |
| 3 | Fire Jet Trail |
| 5 | Explosion Death FX |
| 6 | Dual Wield |
| 8 | Renegade Skin |
| 12 | Ice Jet Trail |
| 15 | Ghost Skin |
| 25 | Rainbow Trail |
| 30 | MAX RANK — Legend |

---

## 7. Online Multiplayer (Architecture)

The `ONLINE` mode tab in Battle Setup is UI-complete. To activate it:

### Server Setup (Free)
1. Deploy `RelayServer.java` (included in root) to services like Railway.app or Render.com
2. Server uses plain TCP sockets — zero external dependencies
3. Update `NET_HOST` and `NET_PORT` in `GameConstants.java` to match your deployment

### How It Works (Lockstep Model)
- One player creates a lobby (gets a 4-letter code)
- Others join by entering the code
- Each client sends their inputs each frame to the relay server
- Server broadcasts all inputs to all clients
- Every client runs identical physics simulation
- Up to 4 players per lobby

### Latency Tolerance
The fixed-timestep loop + input delay of 1-2 frames makes the game playable at up to ~100ms RTT. For India-to-India connections this is typically 20-40ms.

---

## 8. File Structure

```
Samarbhumi/
├── run.bat                     Windows launcher (auto-installs JDK if needed)
├── run.sh                      Linux/Mac launcher
├── src/
│   ├── Main.java
│   └── com/samarbhumi/
│       ├── core/               GameConstants, Enums, InputState, GameSession, Vec2, AABB
│       ├── physics/            PhysicsBody
│       ├── entity/             Player, PickupItem
│       ├── weapon/             Weapon, Projectile, ParticleSystem
│       ├── map/                GameMap, MapFactory
│       ├── ai/                 BotController
│       ├── audio/              AudioEngine
│       ├── progression/        PlayerProfile
│       ├── net/                NetManager, NetworkSession
│       ├── ui/                 GameWindow, GameScreen, Screens, HUDRenderer, UIRenderer,
│       │                       MapRenderer, PlayerRenderer
│       └── exception/          GameException, SaveException
├── resources/
│   └── fonts/                  GameFont-Bold.ttf, GameFont-Regular.ttf
├── saves/                      Player save files (auto-created)
└── docs/
    └── PROJECT_COMPLETE.md     This file
```

---

## 9. Known Limitations & Future Work

- **Online multiplayer** — Fully functional relay-based system included.
- **Audio** — synthesized sounds via `AudioEngine` + Web Audio BGM.
- **Resolution** — fixed 1280×720 logical resolution with aspect-ratio letterboxing.
- **Platform** — Cross-platform support (Windows/Linux/Mac).

---

*Built with Java 17+ | Swing + Java2D | ~6,500 lines of code | v1.2*
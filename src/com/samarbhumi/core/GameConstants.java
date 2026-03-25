package com.samarbhumi.core;

import java.awt.*;
import java.awt.GraphicsEnvironment;

/**
 * Central constants for Samarbhumi.
 * All pixel sizes, physics values, colours, timing — one source of truth.
 */
public final class GameConstants {
    private GameConstants() {}

    // ── Window ─────────────────────────────────────────────────────────────
    public static final int   WIN_W       = 1280;
    public static final int   WIN_H       = 720;
    public static final int   TARGET_FPS  = 60;
    public static final long  FRAME_NS    = 1_000_000_000L / TARGET_FPS;

    // ── Physics ─────────────────────────────────────────────────────────────
    public static final float GRAVITY      = 900f;   // px/s²
    public static final float JETPACK_FORCE= 1400f;  // px/s² upward
    public static final float JETPACK_FUEL = 3.5f;   // seconds of full burn
    public static final float MOVE_SPEED   = 200f;   // px/s ground
    public static final float AIR_SPEED    = 160f;   // px/s in air
    public static final float JUMP_VEL     = -380f;  // px/s upward impulse
    public static final float FRICTION     = 0.82f;  // ground friction per frame
    public static final float AIR_FRICTION = 0.995f; // air friction per frame
    public static final float TERMINAL_VEL = 800f;   // px/s max fall

    // ── Player ─────────────────────────────────────────────────────────────
    public static final int   PLAYER_W     = 28;
    public static final int   PLAYER_H     = 44;
    public static final int   MAX_HP       = 100;
    public static final float RESPAWN_TIME = 3f;     // seconds
    public static final int   MAX_PLAYERS  = 4;      // 1 human + 3 bots max per team

    // ── Weapons ─────────────────────────────────────────────────────────────
    public static final int   BULLET_POOL_SIZE  = 256;
    public static final int   PARTICLE_POOL_SIZE= 512;
    public static final float MELEE_RANGE       = 50f;
    public static final float MELEE_DAMAGE      = 30f;

    // ── Map / World ─────────────────────────────────────────────────────────
    public static final int   TILE_SIZE    = 32;
    public static final int   MAP_W_TILES  = 80;
    public static final int   MAP_H_TILES  = 30;
    public static final int   MAP_W_PX     = MAP_W_TILES * TILE_SIZE;
    public static final int   MAP_H_PX     = MAP_H_TILES * TILE_SIZE;

    // ── Progression ─────────────────────────────────────────────────────────
    public static final int   XP_PER_KILL   = 50;
    public static final int   XP_PER_ASSIST = 20;
    public static final int   XP_PER_WIN    = 100;
    public static final int   COINS_PER_KILL= 5;
    public static final int   COINS_PER_WIN = 30;
    public static final int   MAX_LEVEL     = 30;

    // ── Game Modes ─────────────────────────────────────────────────────────
    public static final int   DEATHMATCH_KILLS  = 20;  // first to X kills wins
    public static final int   MATCH_TIME_SEC    = 300; // 5 minutes

    // ── Online Multiplayer ──────────────────────────────────────────────────
    public static final String NET_HOST = "crossover.proxy.rlwy.net";
    public static final int    NET_PORT = 50588;

    // ── UI Colours (Mini Militia style — dark military green+orange theme) ─
    public static final Color C_BG          = new Color(  8,  12,   6);
    public static final Color C_BG2         = new Color( 15,  22,  10);
    public static final Color C_PANEL       = new Color( 20,  28,  14, 230);
    public static final Color C_PANEL2      = new Color( 30,  42,  18, 220);
    public static final Color C_BORDER      = new Color( 80, 120,  40);
    public static final Color C_BORDER2     = new Color( 55,  85,  25);
    public static final Color C_ACCENT      = new Color(220, 140,  20);   // orange
    public static final Color C_ACCENT2     = new Color(255, 175,  40);   // bright orange
    public static final Color C_RED         = new Color(200,  45,  35);
    public static final Color C_RED2        = new Color(240,  70,  55);
    public static final Color C_GREEN       = new Color( 55, 185,  55);
    public static final Color C_BLUE        = new Color( 45, 130, 210);
    public static final Color C_GOLD        = new Color(210, 170,  40);
    public static final Color C_GOLD2       = new Color(255, 210,  60);
    public static final Color C_WHITE       = new Color(235, 235, 220);
    public static final Color C_DIM         = new Color(130, 130, 100);
    public static final Color C_HP_GREEN    = new Color( 50, 210,  50);
    public static final Color C_HP_YELLOW   = new Color(230, 200,  30);
    public static final Color C_HP_RED      = new Color(220,  50,  40);
    public static final Color C_FUEL        = new Color( 50, 160, 240);

    // Player team colours
    public static final Color C_TEAM_BLUE   = new Color( 50, 130, 220);
    public static final Color C_TEAM_RED    = new Color(220,  50,  50);
    public static final Color C_TEAM_GREEN  = new Color( 50, 200,  80);
    public static final Color C_TEAM_YELLOW = new Color(220, 200,  40);

    // ── Map Tile Colours ───────────────────────────────────────────────────
    public static final Color T_GROUND      = new Color( 58,  88,  40);
    public static final Color T_GROUND_TOP  = new Color( 75, 115,  50);
    public static final Color T_GROUND_DARK = new Color( 38,  58,  25);
    public static final Color T_ROCK        = new Color( 95,  90,  75);
    public static final Color T_ROCK_DARK   = new Color( 65,  62,  50);
    public static final Color T_METAL       = new Color( 85,  95, 100);
    public static final Color T_METAL_DARK  = new Color( 55,  62,  68);
    public static final Color T_LADDER      = new Color(160, 120,  50);
    public static final Color T_WATER       = new Color( 30,  80, 150, 160);
    public static final Color T_CRATE       = new Color(140, 110,  55);
    public static final Color T_SKY_TOP     = new Color( 20,  38,  75);
    public static final Color T_SKY_BOT     = new Color( 38,  68, 115);

    // ── Particle Colours ───────────────────────────────────────────────────
    public static final Color P_BLOOD       = new Color(180,  25,  15);
    public static final Color P_BLOOD2      = new Color(220,  50,  30);
    public static final Color P_SPARK       = new Color(255, 200,  60);
    public static final Color P_SMOKE       = new Color(140, 135, 120, 120);
    public static final Color P_MUZZLE      = new Color(255, 220, 100);
    public static final Color P_EXPLOSION   = new Color(255, 140,  20);
    public static final Color P_SHELL       = new Color(180, 155,  40);
    public static final Color P_DUST        = new Color(130, 115,  85, 100);
    public static final Color P_JET_FIRE    = new Color(100, 180, 255);
    public static final Color P_JET_FIRE2   = new Color( 50, 120, 230);

    // ── Fonts ──────────────────────────────────────────────────────────────
    // ── Font loading: Arial Black → bundled Poppins → SansSerif fallback ──────
    // loadFont() tries Arial Black first (built-in on Windows/macOS),
    // then falls back to our bundled Poppins-Bold.ttf, then to SansSerif.
    private static Font loadFont(boolean bold, float size) {
        // 1. Try Arial Black / Arial (system fonts, best appearance)
        String systemFont = bold ? "Arial Black" : "Arial";
        Font f = new Font(systemFont, bold ? Font.BOLD : Font.PLAIN, (int)size);
        // Java never returns null - check if it actually resolved (not just mapped to default)
        if (!f.getFamily().equalsIgnoreCase("Dialog")) return f;
        // 2. Try bundled Poppins
        try {
            String res = bold ? "resources/fonts/GameFont-Bold.ttf"
                              : "resources/fonts/GameFont-Regular.ttf";
            java.io.File file = new java.io.File(res);
            if (!file.exists()) {
                // Also try relative to JAR location
                java.net.URL url = GameConstants.class.getResource("/" +
                    (bold ? "GameFont-Bold.ttf" : "GameFont-Regular.ttf"));
                if (url != null) {
                    f = Font.createFont(Font.TRUETYPE_FONT,
                            url.openStream()).deriveFont(bold?Font.BOLD:Font.PLAIN, size);
                    GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                    return f;
                }
            } else {
                f = Font.createFont(Font.TRUETYPE_FONT, file)
                        .deriveFont(bold ? Font.BOLD : Font.PLAIN, size);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                return f;
            }
        } catch (Exception ignored) {}
        // 3. Last resort: guaranteed logical font
        return new Font("SansSerif", bold ? Font.BOLD : Font.PLAIN, (int)size);
    }

    public static final Font F_TITLE   = loadFont(true,  42f);
    public static final Font F_HEAD    = loadFont(true,  18f);
    public static final Font F_SUBHEAD = loadFont(true,  15f);
    public static final Font F_BODY    = loadFont(false, 13f);
    public static final Font F_SMALL   = loadFont(false, 11f);
    public static final Font F_HUD     = loadFont(true,  12f);
    public static final Font F_HUD_BIG = loadFont(true,  22f);
    public static final Font F_MONO    = new Font("Monospaced", Font.BOLD, 12);
    public static final Font F_NUM     = loadFont(true,  14f);
}
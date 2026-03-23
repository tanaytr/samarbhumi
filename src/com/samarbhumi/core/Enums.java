package com.samarbhumi.core;

/** All enumerations for the game. */
public final class Enums {
    private Enums() {}

    public enum GameState  { MAIN_MENU, PROFILE, STORE, SETTINGS, LOBBY, PLAYING, PAUSED, SCOREBOARD }
    public enum Team       { BLUE, RED, NONE }
    public enum MoveDir    { LEFT, RIGHT, NONE }
    public enum WeaponSlot { PRIMARY, SECONDARY, MELEE }

    /**
     * Weapon definitions.
     * Constructor order: name, slot, clipSize, maxAmmo, bulletSpeed(px/s), damage, range(px), fireDelay(s), reloadTime(s), unlockLevel
     */
    public enum WeaponType {
        ASSAULT_RIFLE  ("Assault Rifle",   WeaponSlot.PRIMARY,    30, 90,  780f, 22, 900f,  0.08f, 2.0f, 1),
        SHOTGUN        ("Shotgun",         WeaponSlot.PRIMARY,     8, 24,  600f, 18, 320f,  0.75f, 2.0f, 1),
        SNIPER         ("Sniper Rifle",    WeaponSlot.PRIMARY,     5, 15, 1400f, 90,1800f,  1.20f, 2.5f, 1),
        SMG            ("SMG",             WeaponSlot.PRIMARY,    35,105,  700f, 12, 600f,  0.07f, 1.8f, 1),
        ROCKET_LAUNCHER("Rocket Launcher", WeaponSlot.PRIMARY,     4, 12,  380f, 80, 900f,  1.50f, 3.0f, 1),
        PISTOL         ("Pistol",          WeaponSlot.SECONDARY,  12, 36,  650f, 18, 700f,  0.20f, 1.5f, 1),
        DUAL_PISTOLS   ("Dual Pistols",    WeaponSlot.SECONDARY,  14, 42,  650f, 16, 700f,  0.18f, 1.5f, 1),
        GRENADE        ("Grenade",         WeaponSlot.SECONDARY,   3,  3,  260f, 70, 400f,  1.00f, 0.0f, 1),
        KNIFE          ("Combat Knife",    WeaponSlot.MELEE,       -1, -1,   0f, 35,   0f,  0.40f, 0.0f, 1);

        public final String     displayName;
        public final WeaponSlot slot;
        public final int        clipSize;
        public final int        maxAmmo;
        public final float      bulletSpeed;
        public final int        damage;
        public final float      range;
        public final float      fireDelay;
        public final float      reloadTime;
        public final int        unlockLevel;

        WeaponType(String n, WeaponSlot s, int clip, int ammo,
                   float bSpeed, int dmg, float range,
                   float fireDelay, float reload, int unlock) {
            displayName=n; slot=s; clipSize=clip; maxAmmo=ammo;
            bulletSpeed=bSpeed; damage=dmg; this.range=range;
            this.fireDelay=fireDelay; reloadTime=reload; unlockLevel=unlock;
        }
    }

    public enum MapId { WARZONE_ALPHA, JUNGLE_RUINS, STEEL_FORTRESS, CITY_RUINS }

    public enum ParticleType { BLOOD, SPARK, SMOKE, DUST, SHELL, MUZZLE, EXPLOSION, JET, PICKUP }

    public enum TileType {
        AIR(false, false), SOLID(true, false), PLATFORM(true, false),
        LADDER(false, true), WATER(false, false), SPAWN_BLUE(false, false), SPAWN_RED(false, false);
        public final boolean solid, ladder;
        TileType(boolean s, boolean l) { solid=s; ladder=l; }
    }

    public enum AiState { PATROL, CHASE, ATTACK, SEEK_HEALTH, SEEK_AMMO, FLEE, DEAD }

    public enum Difficulty { EASY, MEDIUM, HARD }

    public enum SkinId {
        WARRIOR, COMMANDO, RENEGADE, GHOST,
        DEFAULT, DESERT_CAMO, ARCTIC, URBAN_CAMO, CHROME, GOLD_PLATED,
        DEATH_BASIC, DEATH_EXPLODE, DEATH_STAR, DEATH_COINS,
        JET_BASIC, JET_FIRE, JET_ICE, JET_RAINBOW
    }

    public enum UnlockCategory { CHARACTER, WEAPON_SKIN, DEATH_EFFECT, JET_TRAIL, DUAL_WIELD }
}

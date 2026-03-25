package com.samarbhumi.progression;

import com.samarbhumi.core.*;
import com.samarbhumi.core.Enums.*;
import java.io.*;
import java.util.*;

/**
 * Persistent player profile. Saved to disk between sessions.
 * Tracks XP, level, coins, unlocked items, equipped cosmetics, match stats.
 * Demonstrates: serialization, collections (HashSet for unlocks), file I/O.
 */
public class PlayerProfile implements Serializable {
    private static final long serialVersionUID = 3L;
    // Save path is now per-profile: saves/<name>.sav
    private static final String SAVE_DIR = "saves/";
    
    public record LeaderboardEntry(String name, int level, int xp, int coins) {}

    /** Returns file path for this profile */
    public String getSavePath() {
        String safe = playerName.replaceAll("[^a-zA-Z0-9_-]", "_");
        return SAVE_DIR + safe + ".sav";
    }

    // Identity
    private String playerName;

    // Progression
    private int    totalXP;
    private int    level;        // 1-30
    private int    coins;

    // Stats (lifetime)
    private int    totalKills, totalDeaths, totalMatches, totalWins;

    // Unlocks
    private final Set<SkinId> unlockedSkins   = new HashSet<>();
    private final Set<SkinId> unlockedTrails  = new HashSet<>();
    private final Set<SkinId> unlockedDeaths  = new HashSet<>();
    private boolean dualWieldUnlocked = false;

    // Equipped
    private SkinId  equippedSkin   = SkinId.WARRIOR;
    private SkinId  equippedTrail  = SkinId.JET_BASIC;
    private SkinId  equippedDeath  = SkinId.DEATH_BASIC;

    // Per-weapon equipped skin

    // ── Unlock table — one unlock per level ──────────────────────────────
    public record UnlockEntry(int level, SkinId skin, UnlockCategory cat, String label, int storeCost) {}

    public  static final List<UnlockEntry> UNLOCK_TABLE = List.of(
        new UnlockEntry( 2, SkinId.COMMANDO,     UnlockCategory.CHARACTER,    "Commando Skin",       0),
        new UnlockEntry( 3, SkinId.JET_FIRE,     UnlockCategory.JET_TRAIL,   "Fire Jet Trail",      0),
        new UnlockEntry( 4, SkinId.DESERT_CAMO,  UnlockCategory.WEAPON_SKIN, "Desert Camo Weapons", 0),
        new UnlockEntry( 5, SkinId.DEATH_EXPLODE,UnlockCategory.DEATH_EFFECT,"Explosion Death",     0),
        new UnlockEntry( 6, null,                UnlockCategory.DUAL_WIELD,  "Dual Wield Pistols",  0),
        new UnlockEntry( 8, SkinId.RENEGADE,     UnlockCategory.CHARACTER,   "Renegade Skin",       0),
        new UnlockEntry(10, SkinId.ARCTIC,        UnlockCategory.WEAPON_SKIN,"Arctic Camo",         0),
        new UnlockEntry(12, SkinId.JET_ICE,      UnlockCategory.JET_TRAIL,   "Ice Jet Trail",       0),
        new UnlockEntry(14, SkinId.DEATH_STAR,   UnlockCategory.DEATH_EFFECT,"Star Burst Death",    0),
        new UnlockEntry(15, SkinId.GHOST,        UnlockCategory.CHARACTER,   "Ghost Skin",          0),
        new UnlockEntry(18, SkinId.URBAN_CAMO,   UnlockCategory.WEAPON_SKIN, "Urban Camo",          0),
        new UnlockEntry(20, SkinId.DEATH_COINS,  UnlockCategory.DEATH_EFFECT,"Coin Rain Death",     0),
        new UnlockEntry(22, SkinId.CHROME,       UnlockCategory.WEAPON_SKIN, "Chrome Weapons",      0),
        new UnlockEntry(25, SkinId.JET_RAINBOW,  UnlockCategory.JET_TRAIL,   "Rainbow Trail",       0),
        new UnlockEntry(28, SkinId.GOLD_PLATED,  UnlockCategory.WEAPON_SKIN, "Gold Plated",         0),
        new UnlockEntry(30, null,                UnlockCategory.CHARACTER,   "MAX RANK — Legend",   0)
    );

    // ── Store items (purchasable for coins at any time) ──────────────────
    public record StoreItem(SkinId skin, UnlockCategory cat, String label, int cost) {}

    public static final List<StoreItem> STORE = List.of(
        new StoreItem(SkinId.COMMANDO,     UnlockCategory.CHARACTER,    "Commando",        200),
        new StoreItem(SkinId.RENEGADE,     UnlockCategory.CHARACTER,    "Renegade",        350),
        new StoreItem(SkinId.GHOST,        UnlockCategory.CHARACTER,    "Ghost",           500),
        new StoreItem(SkinId.DESERT_CAMO,  UnlockCategory.WEAPON_SKIN,  "Desert Camo",     150),
        new StoreItem(SkinId.ARCTIC,       UnlockCategory.WEAPON_SKIN,  "Arctic Camo",     200),
        new StoreItem(SkinId.URBAN_CAMO,   UnlockCategory.WEAPON_SKIN,  "Urban Camo",      250),
        new StoreItem(SkinId.CHROME,       UnlockCategory.WEAPON_SKIN,  "Chrome",          400),
        new StoreItem(SkinId.GOLD_PLATED,  UnlockCategory.WEAPON_SKIN,  "Gold Plated",     600),
        new StoreItem(SkinId.JET_FIRE,     UnlockCategory.JET_TRAIL,    "Fire Trail",      150),
        new StoreItem(SkinId.JET_ICE,      UnlockCategory.JET_TRAIL,    "Ice Trail",       200),
        new StoreItem(SkinId.JET_RAINBOW,  UnlockCategory.JET_TRAIL,    "Rainbow Trail",   400),
        new StoreItem(SkinId.DEATH_EXPLODE,UnlockCategory.DEATH_EFFECT, "Explosion",       120),
        new StoreItem(SkinId.DEATH_STAR,   UnlockCategory.DEATH_EFFECT, "Star Burst",      180),
        new StoreItem(SkinId.DEATH_COINS,  UnlockCategory.DEATH_EFFECT, "Coin Rain",       250)
    );

    public PlayerProfile(String name) {
        this.playerName = name;
        this.level      = 1;
        this.coins      = 100; // starter coins
        // Default unlocks
        unlockedSkins.add(SkinId.WARRIOR);
        unlockedTrails.add(SkinId.JET_BASIC);
        unlockedDeaths.add(SkinId.DEATH_BASIC);
    }

    // ── XP & Levelling ────────────────────────────────────────────────────

    /** Add XP. Returns list of new unlock labels if leveled up. */
    public List<String> addXP(int xp) {
        totalXP += xp;
        List<String> newUnlocks = new ArrayList<>();
        int newLevel = calcLevel(totalXP);
        while (level < newLevel && level < GameConstants.MAX_LEVEL) {
            level++;
            String ul = checkLevelUnlock(level);
            if (ul != null) newUnlocks.add(ul);
        }
        return newUnlocks;
    }

    private int calcLevel(int xp) {
        // XP thresholds: each level needs 200 more XP than the last
        int lv = 1, needed = 200;
        while (xp >= needed && lv < GameConstants.MAX_LEVEL) { xp -= needed; lv++; needed += 200; }
        return lv;
    }

    public int xpForCurrentLevel() {
        int lv=1, needed=200, rem=totalXP;
        while (lv < level && lv < GameConstants.MAX_LEVEL) { rem-=needed; lv++; needed+=200; }
        return rem;
    }

    public int xpNeededForNextLevel() {
        int needed=200;
        for (int i=1; i<level && i<GameConstants.MAX_LEVEL; i++) needed+=200;
        return needed;
    }

    public float levelProgress() {
        return Math.min(1f, (float)xpForCurrentLevel() / xpNeededForNextLevel());
    }

    private String checkLevelUnlock(int lv) {
        for (UnlockEntry e : UNLOCK_TABLE) {
            if (e.level() == lv) {
                applyUnlock(e);
                return e.label();
            }
        }
        return null;
    }

    private void applyUnlock(UnlockEntry e) {
        if (e.cat() == UnlockCategory.DUAL_WIELD) { dualWieldUnlocked=true; return; }
        if (e.skin() == null) return;
        switch (e.cat()) {
            case CHARACTER   -> unlockedSkins.add(e.skin());
            case WEAPON_SKIN -> unlockedSkins.add(e.skin());
            case JET_TRAIL   -> unlockedTrails.add(e.skin());
            case DEATH_EFFECT-> unlockedDeaths.add(e.skin());
            default -> {}
        }
    }

    // ── Store purchases ───────────────────────────────────────────────────

    public boolean buy(StoreItem item) {
        if (coins < item.cost()) return false;
        if (isUnlocked(item.skin(), item.cat())) return true; // already owned
        coins -= item.cost();
        applyUnlock(new UnlockEntry(0, item.skin(), item.cat(), item.label(), item.cost()));
        return true;
    }

    public boolean isUnlocked(SkinId s, UnlockCategory cat) {
        if (s == null) return dualWieldUnlocked;
        return switch(cat) {
            case CHARACTER, WEAPON_SKIN -> unlockedSkins.contains(s);
            case JET_TRAIL   -> unlockedTrails.contains(s);
            case DEATH_EFFECT-> unlockedDeaths.contains(s);
            case DUAL_WIELD  -> dualWieldUnlocked;
        };
    }

    // ── Post-match update ─────────────────────────────────────────────────

    public List<String> addMatchResult(int kills, int deaths, boolean won, int coins) {
        totalKills   += kills;
        totalDeaths  += deaths;
        totalMatches++;
        if (won) { totalWins++; this.coins += GameConstants.COINS_PER_WIN; }
        this.coins += coins;
        int xp = kills * GameConstants.XP_PER_KILL + (won ? GameConstants.XP_PER_WIN : 0);
        return addXP(xp);
    }

    // ── Save / Load ───────────────────────────────────────────────────────

    public void save() throws IOException {
        new File(SAVE_DIR).mkdirs();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getSavePath()))) {
            oos.writeObject(this);
        }
    }

    public static PlayerProfile load(String name) throws IOException, ClassNotFoundException {
        String safe = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(SAVE_DIR + safe + ".sav"))) {
            return (PlayerProfile) ois.readObject();
        }
    }

    public static boolean hasSave(String name) {
        String safe = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        return new File(SAVE_DIR + safe + ".sav").exists();
    }

    public static PlayerProfile loadOrCreate(String name) {
        try {
            String safe = name.replaceAll("[^a-zA-Z0-9_-]", "_");
            java.io.File f = new java.io.File(SAVE_DIR + safe + ".sav");
            if (f.exists()) return load(name);
        } catch (Exception ignored) {}
        return new PlayerProfile(name);
    }

    /** List all saved profile names */
    public static java.util.List<String> listProfiles() {
        java.util.List<String> names = new java.util.ArrayList<>();
        java.io.File dir = new java.io.File(SAVE_DIR);
        if (!dir.exists()) return names;
        java.io.File[] files = dir.listFiles();
        if (files == null) return names;
        for (java.io.File f : files) {
            if (f.getName().endsWith(".sav")) {
                names.add(f.getName().replace(".sav", ""));
            }
        }
        return names;
    }

    public static List<LeaderboardEntry> getLeaderboard() {
        List<LeaderboardEntry> list = new ArrayList<>();
        for (String name : listProfiles()) {
            try {
                PlayerProfile p = load(name);
                list.add(new LeaderboardEntry(p.playerName, p.level, p.totalXP, p.coins));
            } catch (Exception ignored) {}
        }
        list.sort((a,b) -> b.xp() - a.xp());
        if (list.size() > 5) return list.subList(0, 5);
        return list;
    }

    /** Delete this profile's save file */
    public void deleteSave() {
        new java.io.File(getSavePath()).delete();
    }

    // ── Getters ───────────────────────────────────────────────────────────

    public String  getPlayerName()      { return playerName; }
    public void    setPlayerName(String n){ playerName=n; }
    public int     getLevel()           { return level; }
    public int     getTotalXP()         { return totalXP; }
    public int     getCoins()           { return coins; }
    public void    addCoins(int c)      { coins=Math.max(0,coins+c); }
    public int     getTotalKills()      { return totalKills; }
    public int     getTotalDeaths()     { return totalDeaths; }
    public int     getTotalMatches()    { return totalMatches; }
    public int     getTotalWins()       { return totalWins; }
    public float   getKD()             { return totalDeaths==0?totalKills:(float)totalKills/totalDeaths; }
    public SkinId  getEquippedSkin()   { return equippedSkin; }
    public void    setEquippedSkin(SkinId s){ if(unlockedSkins.contains(s)) equippedSkin=s; }
    public SkinId  getEquippedTrail()  { return equippedTrail; }
    public void    setEquippedTrail(SkinId s){ if(unlockedTrails.contains(s)) equippedTrail=s; }
    public SkinId  getEquippedDeath()  { return equippedDeath; }
    public void    setEquippedDeath(SkinId s){ if(unlockedDeaths.contains(s)) equippedDeath=s; }
    public boolean isDualWieldUnlocked(){ return dualWieldUnlocked; }
    public Set<SkinId> getUnlockedSkins()  { return Collections.unmodifiableSet(unlockedSkins); }
    public Set<SkinId> getUnlockedTrails() { return Collections.unmodifiableSet(unlockedTrails); }
    public Set<SkinId> getUnlockedDeaths() { return Collections.unmodifiableSet(unlockedDeaths); }
    public String  getRankTitle()       {
        if (level>=30) return "Legend";  if (level>=25) return "Commander";
        if (level>=20) return "Major";   if (level>=15) return "Sergeant";
        if (level>=10) return "Corporal";if (level>=5)  return "Private";
        return "Recruit";
    }
}

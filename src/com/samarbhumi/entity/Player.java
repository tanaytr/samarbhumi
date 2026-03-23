package com.samarbhumi.entity;

import com.samarbhumi.core.*;
import com.samarbhumi.core.Enums.*;
import com.samarbhumi.physics.PhysicsBody;
import com.samarbhumi.weapon.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A player (human or AI) in the game.
 * Double-jump: two distinct key-presses of the jump key while not jetpacking.
 */
public class Player extends PhysicsBody {

    // Identity
    private final int     playerIndex;
    private final String  name;
    private final Team    team;
    private final boolean isHuman;

    // Health
    private int     hp;
    private boolean alive;
    private float   respawnTimer;
    private int     deaths, kills, assists;

    // Jetpack
    private float   jetpackFuel;
    private boolean jetpackActive;

    // Weapons
    private final List<Weapon> weapons   = new ArrayList<>();
    private int                activeSlot = 0;

    // Grenades — separate from weapons, thrown with Shift
    private int     grenadeCount   = 2;   // default supply

    // Aim
    private float   aimAngle   = 0f;
    private boolean facingRight= true;
    private boolean crouching  = false;

    // Double jump — fully self-contained state machine inside Player
    private int     jumpsAvailable  = 2;
    private boolean wasOnGroundLast = true;  // landing-edge detection
    private boolean jumpKeyWasHeld  = false; // edge detection for jump key

    // Animation
    public enum AnimState { IDLE, RUN, JUMP, FALL, CROUCH, DEAD, SHOOT }
    private AnimState animState  = AnimState.IDLE;
    private float     animTimer  = 0f;
    private float     animFrame  = 0f;
    private float     deadTimer  = 0f;
    private float     shootFlash = 0f;
    private float     hitFlash   = 0f;
    private float     reloadSpin = 0f;

    // Cosmetics
    private SkinId characterSkin = SkinId.WARRIOR;
    private SkinId jetTrail      = SkinId.JET_BASIC;
    private SkinId deathEffect   = SkinId.DEATH_BASIC;
    private Color  bodyColor;
    private Color  accentColor;

    // Combat
    private float  meleeCooldown   = 0f;
    private float  invincibleTimer = 0f;
    private Player lastDamager     = null;

    private String  killerName     = "";   // name of last player who killed us (for HUD)

    // Stats
    private int coinsEarned = 0;
    private int xpEarned    = 0;


    public Player(int idx, String name, Team team, float x, float y, boolean isHuman) {
        super(x, y, GameConstants.PLAYER_W, GameConstants.PLAYER_H);
        this.playerIndex = idx;
        this.name        = name;
        this.team        = team;
        this.isHuman     = isHuman;
        this.hp          = GameConstants.MAX_HP;
        this.alive       = true;
        this.jetpackFuel = GameConstants.JETPACK_FUEL;

        bodyColor   = team==Team.BLUE ? GameConstants.C_TEAM_BLUE : GameConstants.C_TEAM_RED;
        accentColor = team==Team.BLUE ? new Color(80,160,255)     : new Color(255,100,80);

        weapons.add(new Weapon(WeaponType.ASSAULT_RIFLE));
        weapons.add(new Weapon(WeaponType.PISTOL));
        weapons.add(new Weapon(WeaponType.KNIFE));
    }

    // ── Update ────────────────────────────────────────────────────────────

    public void update(float dt) {
        if (!alive) {
            deadTimer += dt;
            vel.x *= 0.95f;
            vel.y += GameConstants.GRAVITY * dt;
            pos.x += vel.x * dt;
            pos.y += vel.y * dt;
            return;
        }

        for (Weapon w : weapons) w.update(dt);

        if (invincibleTimer > 0) invincibleTimer -= dt;
        if (meleeCooldown   > 0) meleeCooldown   -= dt;
        if (shootFlash      > 0) shootFlash       -= dt;
        if (hitFlash        > 0) hitFlash         -= dt;

        // Jetpack fuel recharges only while on ground
        if (!jetpackActive && isOnGround()) {
            jetpackFuel = Math.min(GameConstants.JETPACK_FUEL, jetpackFuel + dt * 0.9f);
        }
        // Restore double jump on landing (air→ground transition only)
        if (isOnGround() && !wasOnGroundLast) {
            jumpsAvailable = 2;
        }
        wasOnGroundLast = isOnGround();

        jetpackActive = false;

        Weapon w = getActiveWeapon();
        if (w != null && w.isReloading()) reloadSpin += dt * 5f;
        else reloadSpin = 0f;

        updateAnimation(dt);
    }

    private void updateAnimation(float dt) {
        animTimer += dt;
        if (!isOnGround() && vel.y < -10) animState = AnimState.JUMP;
        else if (!isOnGround())            animState = AnimState.FALL;
        else if (crouching)               animState = AnimState.CROUCH;
        else if (Math.abs(vel.x) > 20)   animState = AnimState.RUN;
        else                              animState = AnimState.IDLE;
        if (shootFlash > 0.02f)           animState = AnimState.SHOOT;
        animFrame = (animTimer * 8f) % 4f;
    }

    // ── Movement ──────────────────────────────────────────────────────────

    public void moveLeft() {
        float speed = isOnGround() ? GameConstants.MOVE_SPEED : GameConstants.AIR_SPEED;
        applyForce(-speed * 12f, 0);
        facingRight = false;
    }

    public void moveRight() {
        float speed = isOnGround() ? GameConstants.MOVE_SPEED : GameConstants.AIR_SPEED;
        applyForce(speed * 12f, 0);
        facingRight = true;
    }

    /**
     * Call every frame with the raw "jump key held" boolean.
     * Internally detects rising edge (key just pressed) for double-jump.
     * This avoids all issues with multiple update() calls per tick or held keys.
     */
    public void updateJump(boolean keyHeld) {
        boolean justPressed = keyHeld && !jumpKeyWasHeld;
        jumpKeyWasHeld = keyHeld;

        if (!justPressed) return;          // nothing to do if not a fresh press
        if (jumpsAvailable <= 0) return;   // no jumps left

        if (isOnGround() || isOnLadder()) {
            // Ground jump
            vel.y = GameConstants.JUMP_VEL;
            setOnGround(false);
        } else {
            // Air / double jump
            vel.y = GameConstants.JUMP_VEL * 0.82f;
        }
        jumpsAvailable--;
    }

    /** Legacy single-call jump (used by AI bots) */
    public void jump() {
        if (jumpsAvailable <= 0 || !isOnGround()) return;
        vel.y = GameConstants.JUMP_VEL;
        setOnGround(false);
        jumpsAvailable = Math.max(0, jumpsAvailable - 1);
    }

    public void activateJetpack(float dt) {
        if (jetpackFuel > 0) {
            applyForce(0, -GameConstants.JETPACK_FORCE);
            jetpackFuel -= dt;
            jetpackActive = true;
            if (vel.y > 0) vel.y *= 0.82f;
        }
    }

    public void setCrouch(boolean c) {
        crouching = c;
        h = c ? GameConstants.PLAYER_H * 2 / 3 : GameConstants.PLAYER_H;
    }

    public void climbLadder(float dy) {
        if (isOnLadder()) { vel.y = dy * 120f; vel.x *= 0.5f; }
    }

    // ── Combat ────────────────────────────────────────────────────────────

    public boolean takeDamage(int dmg, Player source) {
        if (!alive || invincibleTimer > 0) return false;
        hp -= dmg;
        hitFlash    = 0.14f;
        lastDamager = source;
        if (hp <= 0) { hp = 0; die(source); return true; }
        return false;
    }

    public void die(Player killer) {
        alive        = false;
        deadTimer    = 0f;
        respawnTimer = GameConstants.RESPAWN_TIME;   // SET HERE — not in game loop
        killerName   = (killer != null && killer != this) ? killer.getName() : "the environment";
        deaths++;
        if (killer != null && killer != this) {
            killer.kills++;
            killer.xpEarned    += GameConstants.XP_PER_KILL;
            killer.coinsEarned += GameConstants.COINS_PER_KILL;
        }
        vel.x += (Math.random() - 0.5) * 200;
        vel.y  = -180f;
        animState = AnimState.DEAD;
    }

    public void respawn(float x, float y) {
        pos.set(x, y);
        vel.set(0, 0);
        hp              = GameConstants.MAX_HP;
        alive           = true;
        deadTimer       = 0f;
        jetpackFuel     = GameConstants.JETPACK_FUEL;
        invincibleTimer = 2.0f;
        crouching       = false;
        h               = GameConstants.PLAYER_H;
        jumpsAvailable  = 2;
        wasOnGroundLast = true;
        jumpKeyWasHeld  = false;
        for (Weapon w : weapons) w.refillAmmo();
    }

    public boolean tryMelee() {
        if (meleeCooldown > 0) return false;
        meleeCooldown = 0.4f;
        return true;
    }

    // ── Weapons ───────────────────────────────────────────────────────────

    public Weapon getActiveWeapon() {
        if (weapons.isEmpty()) return null;
        return weapons.get(Math.min(activeSlot, weapons.size()-1));
    }

    public void swapWeapon() { activeSlot = (activeSlot+1) % weapons.size(); }
    public void setActiveSlot(int s) { activeSlot = Math.max(0, Math.min(s, weapons.size()-1)); }

    public void replaceWeapon(Weapon w) {
        for (int i=0;i<weapons.size();i++) {
            if (weapons.get(i).getType().slot == w.getType().slot) {
                weapons.set(i, w); activeSlot=i; return;
            }
        }
        weapons.add(w); activeSlot=weapons.size()-1;
    }

    public boolean tryFire() {
        Weapon w = getActiveWeapon();
        if (w == null) return false;
        boolean fired = w.tryFire();
        if (fired) shootFlash = 0.1f;
        return fired;
    }

    public void triggerReload() {
        Weapon w = getActiveWeapon();
        if (w != null) w.startReload();
    }

    public void heal(int amount) { hp = Math.min(GameConstants.MAX_HP, hp+amount); }

    // ── Getters ───────────────────────────────────────────────────────────

    public int       getPlayerIndex()    { return playerIndex; }
    public String    getName()           { return name; }
    public Team      getTeam()           { return team; }
    public boolean   isHuman()           { return isHuman; }
    public int       getHp()             { return hp; }
    public void      setHp(int h)        { this.hp = Math.max(0, Math.min(GameConstants.MAX_HP,h)); }
    public boolean   isAlive()           { return alive; }
    public float     getJetpackFuel()    { return jetpackFuel; }
    public boolean   isJetpackActive()   { return jetpackActive; }
    public float     getAimAngle()       { return aimAngle; }
    public void      setAimAngle(float a){ aimAngle=a; facingRight=Math.cos(a)>=0; }
    public boolean   isFacingRight()     { return facingRight; }
    public boolean   isCrouching()       { return crouching; }
    public AnimState getAnimState()      { return animState; }
    public float     getAnimFrame()      { return animFrame; }
    public float     getShootFlash()     { return shootFlash; }
    public float     getHitFlash()       { return hitFlash; }
    public float     getReloadSpin()     { return reloadSpin; }
    public int       getKills()          { return kills; }
    public int       getDeaths()         { return deaths; }
    public int       getAssists()        { return assists; }
    public Player    getLastDamager()    { return lastDamager; }
    public float     getRespawnTimer()   { return respawnTimer; }
    public String    getKillerName()     { return killerName; }
    public void      setRespawnTimer(float t){ respawnTimer=t; }
    public float     getDeadTimer()      { return deadTimer; }
    public boolean   isInvincible()      { return invincibleTimer > 0; }
    public List<Weapon> getWeapons()     { return weapons; }
    public int       getActiveSlot()     { return activeSlot; }
    public SkinId    getCharacterSkin()  { return characterSkin; }
    public void      setCharacterSkin(SkinId s){ characterSkin=s; }
    public SkinId    getJetTrail()       { return jetTrail; }
    public void      setJetTrail(SkinId s){ jetTrail=s; }
    public SkinId    getDeathEffect()    { return deathEffect; }
    public void      setDeathEffect(SkinId s){ deathEffect=s; }
    public Color     getBodyColor()      { return bodyColor; }
    public void      setBodyColor(Color c){ bodyColor=c; }
    public Color     getAccentColor()    { return accentColor; }
    public int       getCoinsEarned()    { return coinsEarned; }
    public int       getXpEarned()       { return xpEarned; }
    public float     getMeleeCooldown()  { return meleeCooldown; }
    public float     getHpFraction()     { return (float)hp/GameConstants.MAX_HP; }
    public float     getFuelFraction()   { return jetpackFuel/GameConstants.JETPACK_FUEL; }
    public int       getJumpsAvailable() { return jumpsAvailable; }
    public int       getGrenadeCount()   { return grenadeCount; }
    public void      addGrenades(int n)  { grenadeCount = Math.min(grenadeCount+n, 9); }
    public boolean   useGrenade()        { if(grenadeCount<=0) return false; grenadeCount--; return true; }
}

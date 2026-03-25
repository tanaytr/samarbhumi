package com.samarbhumi.ai;

import com.samarbhumi.core.*;
import com.samarbhumi.core.Enums.*;
import com.samarbhumi.entity.*;
import com.samarbhumi.map.GameMap;
import com.samarbhumi.weapon.Weapon;
import java.util.*;

/**
 * Aggressive AI — state machine: PATROL → CHASE → ATTACK → FLEE
 * Bots target the nearest enemy (human or other bot) and fight until dead.
 * They jump over obstacles, strafe unpredictably, and lead targets.
 */
public class BotController {

    private final Player     bot;
    private boolean firedThisFrame = false;
    private AiState state = AiState.PATROL;
    private Player  target;

    private float thinkTimer  = 0f;
    private float jumpTimer   = 0f;
    private float fireTimer   = 0f;
    private float strafeTimer = 0f;
    private float strafeDir   = 1f;
    private float patrolDir   = 1f;
    private float patrolTimer = 0f;
    private float stuckTimer  = 0f;
    private float lastX       = 0f;

    // Per-difficulty tuning
    private final float REACT_TIME;   // seconds between shots
    private final float AIM_ERROR;    // radians of aim spread
    private final float THINK_RATE;   // AI decision frequency

    public BotController(Player b, GameSession s) {
        this.bot = b;
        // ALL difficulties shoot fast enough to be dangerous
        switch (s.getDifficulty()) {
            case EASY   -> { REACT_TIME=0.35f; AIM_ERROR=0.28f; THINK_RATE=0.18f; }
            case MEDIUM -> { REACT_TIME=0.18f; AIM_ERROR=0.12f; THINK_RATE=0.12f; }
            default     -> { REACT_TIME=0.08f; AIM_ERROR=0.04f; THINK_RATE=0.07f; }
        }
    }

    public void update(float dt, List<Player> allPlayers, List<PickupItem> pickups, GameMap map) {
        firedThisFrame = false;
        if (!bot.isAlive()) { state = AiState.DEAD; return; }

        // Stuck detection — if barely moving for 1s, change direction
        stuckTimer += dt;
        if (stuckTimer > 1.0f) {
            if (Math.abs(bot.cx() - lastX) < 5f && state != AiState.ATTACK) patrolDir = -patrolDir;
            lastX = bot.cx();
            stuckTimer = 0f;
        }

        thinkTimer += dt;
        if (thinkTimer >= THINK_RATE) { thinkTimer = 0f; think(allPlayers, map); }

        execute(dt, map);
    }

    private void think(List<Player> allPlayers, GameMap map) {
        // Always flee below 12% HP regardless of difficulty
        if (bot.getHpFraction() < 0.12f) { state = AiState.FLEE; return; }

        // Find nearest enemy (any team that isn't mine) — including other bots
        target = null;
        float best = Float.MAX_VALUE;
        for (Player p : allPlayers) {
            if (p == bot || !p.isAlive() || p.getTeam() == bot.getTeam()) continue;
            float d = dist(bot, p);
            // Slightly prefer human targets to create challenge, but attack anyone
            float w = p.isHuman() ? d * 0.7f : d;
            if (w < best) { best = w; target = p; }
        }

        if (target == null) { state = AiState.PATROL; return; }

        boolean los = map.lineOfSight(bot.cx(), bot.cy(), target.cx(), target.cy());
        state = los ? AiState.ATTACK : AiState.CHASE;
    }

    private void execute(float dt, GameMap map) {
        strafeTimer -= dt;
        jumpTimer   -= dt;
        fireTimer   -= dt;

        switch (state) {
            case PATROL -> patrol(dt, map);
            case CHASE  -> chase(dt, map);
            case ATTACK -> attack(dt, map);
            case FLEE   -> flee(dt, map);
            default -> {}
        }

        avoidEdge(map);
        // Auto-jump if stuck against a wall
        autoJumpObstacle(map);
    }

    private void patrol(float dt, GameMap map) {
        patrolTimer -= dt;
        if (patrolTimer <= 0) { patrolDir = -patrolDir; patrolTimer = 1.5f + (float)Math.random() * 2f; }
        if (patrolDir > 0) bot.moveRight(); else bot.moveLeft();
        if (jumpTimer <= 0 && bot.isOnGround() && Math.random() < 0.006f) { bot.jump(); jumpTimer = 1.2f; }
        bot.setAimAngle(patrolDir > 0 ? 0 : (float)Math.PI);
    }

    private void chase(float dt, GameMap map) {
        if (target == null) { patrol(dt, map); return; }
        float dx = target.cx() - bot.cx();
        if (dx > 0) bot.moveRight(); else bot.moveLeft();
        if (jumpTimer <= 0 && bot.isOnGround() && Math.random() < 0.12f) { bot.jump(); jumpTimer = 0.7f; }
        bot.setAimAngle(aimAt(target));
        // Shoot even during chase (spray and pray)
        Weapon w = bot.getActiveWeapon();
        if (w != null) {
            if (w.isEmpty()) bot.triggerReload();
            else if (fireTimer <= 0 && Math.random() < 0.4f) {
                if (bot.tryFire()) firedThisFrame = true;
                fireTimer = REACT_TIME * 1.5f;
            }
        }
    }

    private void attack(float dt, GameMap map) {
        if (target == null) { chase(dt, map); return; }
        float d  = dist(bot, target);
        float dx = target.cx() - bot.cx();

        // Movement: always push toward target unless at sweet spot (100-200px)
        if (d > 200) {
            if (dx > 0) bot.moveRight(); else bot.moveLeft();
        } else if (d < 80) {
            // Too close — back off slightly and keep shooting
            if (dx > 0) bot.moveLeft(); else bot.moveRight();
        } else {
            // Strafe left/right for unpredictability
            if (strafeTimer <= 0) { strafeDir = -strafeDir; strafeTimer = 0.3f + (float)Math.random() * 0.5f; }
            if (strafeDir > 0) bot.moveRight(); else bot.moveLeft();
        }

        // Jump to dodge and reach elevated targets
        if (jumpTimer <= 0 && bot.isOnGround()) {
            float prob = target.cy() < bot.cy() - 40 ? 0.12f : 0.04f;
            if (Math.random() < prob) { bot.jump(); jumpTimer = 0.9f; }
        }

        // Aim with small error
        float angle = aimAt(target);
        bot.setAimAngle(angle + (float)(Math.random() - 0.5) * AIM_ERROR);

        // Fire
        Weapon w = bot.getActiveWeapon();
        if (w != null) {
            if (w.isEmpty()) { bot.triggerReload(); }
            else if (fireTimer <= 0) {
                if (bot.tryFire()) firedThisFrame = true;
                fireTimer = REACT_TIME + (float)(Math.random() * 0.04);
            }
        }
    }

    private void flee(float dt, GameMap map) {
        if (target == null) { patrol(dt, map); return; }
        float dx = bot.cx() - target.cx();
        if (dx > 0) bot.moveRight(); else bot.moveLeft();
        if (bot.isOnGround() && jumpTimer <= 0) { bot.jump(); jumpTimer = 0.6f; }
        if (bot.getJetpackFuel() > 0.2f) bot.activateJetpack(dt);
        // Occasionally transition back to attack after fleeing
        if (bot.getHpFraction() > 0.25f) state = AiState.CHASE;
    }

    private void avoidEdge(GameMap map) {
        // Check if ground ahead disappears — if so, reverse patrol direction
        float ahead = bot.cx() + (bot.isFacingRight() ? 20 : -20);
        float foot  = bot.getPos().y + bot.getH() + 8;
        if (bot.isOnGround() && !map.isSolid(ahead, foot)) {
            patrolDir = bot.isFacingRight() ? -1 : 1;
        }
    }

    private void autoJumpObstacle(GameMap map) {
        // If a wall is directly in front and bot is on ground, jump over it
        float fwdX = bot.cx() + (bot.isFacingRight() ? bot.getW()/2 + 4 : -bot.getW()/2 - 4);
        if (bot.isOnGround() && map.isSolid(fwdX, bot.cy()) && jumpTimer <= 0) {
            bot.jump(); jumpTimer = 0.5f;
        }
    }

    private float aimAt(Player p) {
        float dx = p.cx() - bot.cx();
        float dy = p.cy() - bot.cy();
        // Lead target based on bullet speed
        Weapon w = bot.getActiveWeapon();
        if (w != null && w.getType().bulletSpeed > 0) {
            float d = (float)Math.sqrt(dx*dx + dy*dy);
            float t = d / w.getType().bulletSpeed;
            dx += p.getVel().x * t * 0.6f;
            dy += p.getVel().y * t * 0.2f;
        }
        return (float)Math.atan2(dy, dx);
    }

    private float dist(Player a, Player b) {
        float dx = a.cx() - b.cx(), dy = a.cy() - b.cy();
        return (float)Math.sqrt(dx*dx + dy*dy);
    }

    public Player   getBot()           { return bot; }
    public boolean  hasFiredThisFrame(){ return firedThisFrame; }
    public AiState  getState()         { return state; }
}

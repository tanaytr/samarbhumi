package com.samarbhumi.core;
import java.awt.Color;

import com.samarbhumi.ai.BotController;
import com.samarbhumi.entity.*;
import com.samarbhumi.map.*;
import com.samarbhumi.weapon.*;
import com.samarbhumi.progression.PlayerProfile;

import java.util.*;

/**
 * A single match session. Owns all entities, physics, projectiles, pickups.
 * Updated by the game loop at a fixed timestep.
 *
 * Demonstrates: collections (ArrayList, LinkedList), generics, command pattern (killFeed).
 */
public class GameSession {

    private final GameMap          map;
    private final List<Player>     players    = new ArrayList<>();
    private final List<BotController> bots   = new ArrayList<>();
    private final List<Projectile> bullets    = new ArrayList<>();
    private final List<PickupItem> pickups    = new ArrayList<>();
    private final ParticleSystem   particles  = new ParticleSystem();
    private final LinkedList<KillFeedEntry> killFeed = new LinkedList<>();
    private final LinkedList<FloatText>     floatTexts = new LinkedList<>();

    private float  matchTimer;
    private boolean matchOver;
    private Player winner;
    private int    humanPlayerIdx;
    private final Random RNG = new Random();
    private float pickupSpawnTimer = 0f;

    // Explosion splash damage radius
    private static final float EXPLOSION_RADIUS = 120f;

    public static class KillFeedEntry {
        public final String killer, victim, weapon;
        public float life = 5f;
        public KillFeedEntry(String k, String v, String w){ killer=k; victim=v; weapon=w; }
    }
    public static class FloatText {
        public String text; public float x, y, vy, life, maxLife;
        public java.awt.Color color;
        public FloatText(String t, float x, float y, java.awt.Color c){
            text=t; this.x=x; this.y=y; vy=-80f; life=1.2f; maxLife=1.2f; color=c;
        }
    }

    // killsToWin scales with enemy count — set after players list is built
    private int killsToWin = 3;

    private void fireWeapon(Player shooter, com.samarbhumi.weapon.Weapon w) {
        if (w == null) return;
        Enums.WeaponType wt = w.getType();
        float angle = shooter.getAimAngle();
        float ox = shooter.cx() + (float)Math.cos(angle)*20f;
        float oy = shooter.cy() + (float)Math.sin(angle)*6f - (shooter.isCrouching()?8:0);

        // Muzzle flash particle
        particles.spawnMuzzleFlash(ox, oy, (float)Math.cos(angle), (float)Math.sin(angle));

        switch (wt) {
            case ASSAULT_RIFLE, SMG, PISTOL, DUAL_PISTOLS -> {
                Projectile p = Projectile.obtain();
                float spread = (wt==Enums.WeaponType.SMG) ? 0.06f : 0.02f;
                float a2 = angle + (float)(RNG.nextGaussian()*spread);
                p.initBullet(ox, oy, (float)Math.cos(a2)*wt.bulletSpeed, (float)Math.sin(a2)*wt.bulletSpeed,
                             wt.damage, wt.range, shooter.getPlayerIndex(), shooter.getTeam(), wt);
                bullets.add(p);
                particles.spawnShellCasing(shooter.cx(), shooter.cy()+10, shooter.isFacingRight());
            }
            case SHOTGUN -> {
                for (int i=0; i<7; i++) {
                    Projectile p = Projectile.obtain();
                    float a2 = angle + (float)(RNG.nextGaussian()*0.15f);
                    p.initBullet(ox, oy, (float)Math.cos(a2)*wt.bulletSpeed, (float)Math.sin(a2)*wt.bulletSpeed,
                                 wt.damage, wt.range, shooter.getPlayerIndex(), shooter.getTeam(), wt);
                    bullets.add(p);
                }
                particles.spawnShellCasing(shooter.cx(), shooter.cy()+10, shooter.isFacingRight());
            }
            case SNIPER -> {
                Projectile p = Projectile.obtain();
                p.initBullet(ox, oy, (float)Math.cos(angle)*wt.bulletSpeed, (float)Math.sin(angle)*wt.bulletSpeed,
                             wt.damage, wt.range, shooter.getPlayerIndex(), shooter.getTeam(), wt);
                bullets.add(p);
                particles.spawnShellCasing(shooter.cx(), shooter.cy()+10, shooter.isFacingRight());
            }
            case ROCKET_LAUNCHER -> {
                Projectile p = Projectile.obtain();
                p.initRocket(ox, oy, (float)Math.cos(angle)*wt.bulletSpeed, (float)Math.sin(angle)*wt.bulletSpeed,
                             shooter.getPlayerIndex(), shooter.getTeam());
                bullets.add(p);
            }
            case GRENADE -> {
                Projectile p = Projectile.obtain();
                float gvx = (float)Math.cos(angle)*280f;
                float gvy = (float)Math.sin(angle)*280f - 100f;
                p.initGrenade(ox, oy, gvx, gvy, shooter.getPlayerIndex(), shooter.getTeam());
                bullets.add(p);
            }
            case KNIFE -> {
                // Melee — check proximity
                if (shooter.tryMelee()) doMeleeHit(shooter);
            }
            default -> {}
        }
    }

    private void doMeleeHit(Player attacker) {
        float ax = attacker.cx(), ay = attacker.cy();
        for (Player p : players) {
            if (p==attacker || !p.isAlive()) continue;
            // In team mode skip teammates; in FFA everyone is a valid target
            if (teamMode && p.getTeam()==attacker.getTeam()) continue;
            float dist = (float)Math.sqrt((p.cx()-ax)*(p.cx()-ax)+(p.cy()-ay)*(p.cy()-ay));
            if (dist < GameConstants.MELEE_RANGE) {
                boolean dead = p.takeDamage((int)GameConstants.MELEE_DAMAGE, attacker);
                particles.spawnBlood(p.cx(), p.cy(), 0, -1, 6);
                addFloat("-"+(int)GameConstants.MELEE_DAMAGE, p.cx(), p.cy(), GameConstants.P_BLOOD2);
                if (dead) onKill(attacker, p, "Knife");
            }
        }
    }

    private final boolean twoPlayerMode;
    private final boolean teamMode;        // true = team-based win condition

    // ── Constructors ──────────────────────────────────────────────────────

    /**
     * @param teamMode  true = Team Blue (human + allies) vs Team Red (enemies)
     *                  false = free-for-all, every player for themselves
     */
    public GameSession(GameMap map, PlayerProfile profile, int numBots,
                       Enums.Difficulty diff, boolean twoPlayer, boolean teamMode) {
        this.map          = map;
        this.matchTimer   = GameConstants.MATCH_TIME_SEC;
        this.twoPlayerMode= twoPlayer;
        this.teamMode     = teamMode;

        Vec2 spawn0 = map.getSpawnBlue(0);
        Player human = new Player(0, profile.getPlayerName(), Enums.Team.BLUE, spawn0.x, spawn0.y, true);
        human.setCharacterSkin(profile.getEquippedSkin());
        human.setJetTrail(profile.getEquippedTrail());
        human.setDeathEffect(profile.getEquippedDeath());
        players.add(human);
        humanPlayerIdx = 0;

        String[] botNames = {"Ramesh","Priya","Arjun","Kavya","Dev","Nisha","Vikram","Meena"};

        if (twoPlayer) {
            if (teamMode) {
                // Co-op: P1+P2 both BLUE vs all bots RED
                Vec2 sp2 = map.getSpawnBlue(1);
                Player p2 = new Player(1, "Player 2", Enums.Team.BLUE, sp2.x, sp2.y, false);
                players.add(p2);
                for (int i=0; i<numBots; i++) {
                    Vec2 sp = map.getSpawnRed(i % map.spawnRedCount());
                    Player bot = new Player(i+2, botNames[i%botNames.length], Enums.Team.RED, sp.x, sp.y, false);
                    players.add(bot); bots.add(new BotController(bot, diff));
                }
            } else {
                // Personal / duel: P1=BLUE vs P2=RED, no bots
                Vec2 sp2 = map.getSpawnRed(0);
                Player p2 = new Player(1, "Player 2", Enums.Team.RED, sp2.x, sp2.y, false);
                players.add(p2);
            }
        } else {
            if (teamMode) {
                // Team mode: human + floor(numBots/2) bots = BLUE, rest = RED
                int blueAllies = numBots / 2;   // bots on human's team
                int redEnemies = numBots - blueAllies;
                int idx = 1;
                for (int i=0; i<blueAllies; i++) {
                    Vec2 sp = map.getSpawnBlue((i+1) % map.spawnBlueCount());
                    Player bot = new Player(idx++, botNames[i%botNames.length], Enums.Team.BLUE, sp.x, sp.y, false);
                    players.add(bot); bots.add(new BotController(bot, diff));
                }
                for (int i=0; i<redEnemies; i++) {
                    Vec2 sp = map.getSpawnRed(i % map.spawnRedCount());
                    Player bot = new Player(idx++, botNames[(blueAllies+i)%botNames.length], Enums.Team.RED, sp.x, sp.y, false);
                    players.add(bot); bots.add(new BotController(bot, diff));
                }
            } else {
                // Free-for-all: human=BLUE, ALL bots=RED (fixes melee/bullet team-skip bug)
                for (int i=0; i<numBots; i++) {
                    Vec2 sp = map.getSpawnRed(i % map.spawnRedCount());
                    Player bot = new Player(i+1, botNames[i%botNames.length], Enums.Team.RED, sp.x, sp.y, false);
                    players.add(bot); bots.add(new BotController(bot, diff));
                }
            }
        }

        spawnInitialPickups();
        int enemies = players.size() - 1;
        killsToWin = Math.max(1, enemies * 3);
    }

    /** Backwards-compatible constructors */
    public GameSession(GameMap map, PlayerProfile profile, int numBots, Enums.Difficulty diff, boolean twoPlayer) {
        this(map, profile, numBots, diff, twoPlayer, false);
    }
    public GameSession(GameMap map, PlayerProfile profile, int numBots, Enums.Difficulty diff) {
        this(map, profile, numBots, diff, false, false);
    }

    private void spawnInitialPickups() {
        // Drop some weapons and health across the map
        int tw = map.getW()*GameConstants.TILE_SIZE;
        for (Enums.WeaponType wt : new Enums.WeaponType[]{
                Enums.WeaponType.SHOTGUN, Enums.WeaponType.SNIPER, Enums.WeaponType.SMG,
                Enums.WeaponType.ROCKET_LAUNCHER}) {
            PickupItem item = new PickupItem(tw/5f + (float)Math.random()*tw*3/5f, 60, wt);
            pickups.add(item);
        }
        for (int i=0; i<6; i++) {
            float px = 100f + (float)Math.random()*(tw-200);
            pickups.add(new PickupItem(px, 60, PickupItem.PickupType.HEALTH, 40));
        }
        for (int i=0; i<4; i++) {
            float px = 100f + (float)Math.random()*(tw-200);
            pickups.add(new PickupItem(px, 60, PickupItem.PickupType.AMMO, 30));
        }
    }

    // ── Main update ───────────────────────────────────────────────────────

    public void update(float dt, InputState input) {
        if (matchOver) return;

        matchTimer -= dt;
        if (matchTimer <= 0) { matchTimer=0; endMatch(); return; }

        // Human input → Player 0
        Player human = players.get(humanPlayerIdx);
        if (human.isAlive()) applyHumanInput(human, dt, input);

        // P2 input if 2-player mode
        if (twoPlayerMode && players.size() > 1) {
            Player p2 = players.get(1);
            if (p2.isAlive()) applyP2Input(p2, dt, input);
        }

        // AI bots — update AI logic, then create projectile if they fired
        for (BotController bc : bots) {
            bc.update(dt, players, pickups, map);
            if (bc.hasFiredThisFrame()) {
                Player bot = bc.getBot();
                Weapon w   = bot.getActiveWeapon();
                if (w != null) fireWeapon(bot, w);
            }
        }

        // Integrate all players
        for (Player p : players) {
            if (!p.isAlive()) { p.update(dt); continue; }
            p.integrate(dt);
            map.resolve(p, dt);
            p.update(dt);
            // NOTE: respawn timer is set inside Player.die() - not here
        }

        // Count down respawn timers and respawn
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (!p.isAlive() && p.getRespawnTimer() > 0) {
                p.setRespawnTimer(p.getRespawnTimer() - dt);
                if (p.getRespawnTimer() <= 0) {
                    Vec2 sp = (p.getTeam()==Enums.Team.BLUE)
                        ? map.getSpawnBlue(i % map.spawnBlueCount())
                        : map.getSpawnRed(i % map.spawnRedCount());
                    p.respawn(sp.x, sp.y);
                }
            }
        }

        // Integrate + resolve bullets
        for (int i=bullets.size()-1; i>=0; i--) {
            Projectile proj = bullets.get(i);
            proj.integrate(dt);
            map.resolve(proj, dt);

            // Check grenade timer / rocket hit tile
            boolean hit = false;
            if (proj.getProjType()==Projectile.ProjType.GRENADE && proj.isExploded()) hit=true;
            if (proj.getProjType()==Projectile.ProjType.ROCKET && proj.isOnGround()) hit=true;

            if (hit) {
                doExplosion(proj);
                Projectile.release(bullets.remove(i));
                continue;
            }
            if (!proj.isAlive()) { Projectile.release(bullets.remove(i)); continue; }

            // Bullet hit players
            for (Player p : players) {
                if (!p.isAlive() || p.getPlayerIndex()==proj.getOwner()) continue;
                // In team mode skip teammates; in FFA hit anyone
                if (teamMode && p.getTeam()==proj.getOwnerTeam()) continue;
                if (proj.getBounds().overlaps(p.getBounds())) {
                    int dmg = proj.getDamage();
                    Player shooter = getPlayerByIndex(proj.getOwner());
                    boolean dead = p.takeDamage(dmg, shooter);
                    particles.spawnBlood(p.cx(), p.cy(), proj.getVel().x, proj.getVel().y, 8);
                    addFloat("-"+dmg, p.cx(), p.cy(), GameConstants.P_BLOOD2);
                    if (dead && shooter != null) onKill(shooter, p, proj.getWeapType().displayName);
                    proj.kill();
                    particles.spawnSpark(proj.getPos().x, proj.getPos().y, -proj.getVel().x*0.1f, -proj.getVel().y*0.1f, 5);
                    break;
                }
            }

            // Bullet hit tile
            if (map.isSolid(proj.cx(), proj.cy())) {
                particles.spawnSpark(proj.getPos().x, proj.getPos().y, -proj.getVel().x*0.1f, 0, 4);
                proj.kill();
            }

            // Jetpack particles for human
            if (human.isAlive() && human.isJetpackActive()) spawnJetParticles(human);
        }

        // Integrate pickups
        for (PickupItem item : pickups) {
            if (!item.isAlive()) continue;
            item.integrate(dt);
            map.resolve(item, dt);
            item.update(dt);
            // Check collection by players
            for (Player p : players) {
                if (!p.isAlive()) continue;
                if (item.getBounds().overlaps(p.getBounds())) {
                    collectPickup(p, item);
                }
            }
        }
        pickups.removeIf(i -> !i.isAlive());

        // Respawn pickups
        pickupSpawnTimer -= dt;
        if (pickupSpawnTimer <= 0 && pickups.size() < 8) {
            pickupSpawnTimer = 15f;
            spawnRandomPickup();
        }

        // Jet particles for all active jetpackers
        for (Player p : players) {
            if (p.isAlive() && p.isJetpackActive()) spawnJetParticles(p);
            if (p.isAlive() && p.isOnGround() && Math.abs(p.getVel().x) > 80) {
                particles.spawnDust(p.cx(), p.getPos().y+p.getH());
            }
        }

        particles.update(dt);
        floatTexts.removeIf(ft -> { ft.life-=dt; ft.y+=ft.vy*dt; return ft.life<=0; });
        killFeed.removeIf(kf -> { kf.life-=dt; return kf.life<=0; });

        // Win condition
        checkWinCondition();
    }

    private void applyHumanInput(Player p, float dt, InputState input) {
        if (input.p1Left())    p.moveLeft();
        if (input.p1Right())   p.moveRight();
        p.updateJump(input.p1JumpHeld());
        if (input.p1Down())    p.setCrouch(true); else p.setCrouch(false);
        if (input.p1Jetpack()) p.activateJetpack(dt);
        if (input.p1Reload())  p.triggerReload();
        if (input.p1Swap())    p.swapWeapon();
        if (input.p1Pickup())  tryPickup(p);
        if (input.p1Melee())   doMeleeHit(p);

        // Grenade throw — Shift key, uses separate grenade inventory
        if (input.p1Grenade()) throwGrenade(p);

        // Aiming
        float keyAim = input.p1KeyAimAngle();
        float angle;
        if (input.mouseMoved || Float.isNaN(keyAim)) {
            float worldMouseX = input.mouseX + getCamX(p);
            float worldMouseY = input.mouseY + getCamY(p);
            angle = (float) Math.atan2(worldMouseY - p.cy(), worldMouseX - p.cx());
        } else {
            angle = keyAim;
        }
        p.setAimAngle(angle);

        // Fire — LMB (mouse aim) OR Enter (key aim)
        if (input.p1FireMouse() || input.p1FireKey()) {
            if (p.tryFire()) {
                Weapon w = p.getActiveWeapon();
                if (w != null) fireWeapon(p, w);
            }
        }
    }

    private void applyP2Input(Player p, float dt, InputState input) {
        if (input.p2Left())    p.moveLeft();
        if (input.p2Right())   p.moveRight();
        p.updateJump(input.p2JumpHeld());
        if (input.p2Down())    p.setCrouch(true); else p.setCrouch(false);
        if (input.p2Jetpack()) p.activateJetpack(dt);
        if (input.p2Reload())  p.triggerReload();
        if (input.p2Swap())    p.swapWeapon();
        if (input.p2Pickup())  tryPickup(p);
        if (input.p2Melee())   doMeleeHit(p);

        // Grenade throw — Ctrl key
        if (input.p2Grenade()) throwGrenade(p);

        float worldMouseX = input.mouseX + getCamX(p);
        float worldMouseY = input.mouseY + getCamY(p);
        p.setAimAngle((float) Math.atan2(worldMouseY - p.cy(), worldMouseX - p.cx()));

        // Fire — LMB (mouse aim) OR Tab (key aim)
        if (input.p2FireMouse() || input.p2FireKey()) {
            if (p.tryFire()) {
                Weapon w = p.getActiveWeapon();
                if (w != null) fireWeapon(p, w);
            }
        }
    }

    /** Throw a grenade from inventory (Shift/Ctrl key) */
    private void throwGrenade(Player p) {
        if (!p.useGrenade()) {
            addFloat("No grenades!", p.cx(), p.cy()-20, GameConstants.C_RED);
            return;
        }
        float angle = p.getAimAngle();
        float ox = p.cx() + (float)Math.cos(angle)*20f;
        float oy = p.cy() + (float)Math.sin(angle)*6f;
        float gvx = (float)Math.cos(angle)*320f;
        float gvy = (float)Math.sin(angle)*320f - 120f;
        Projectile proj = Projectile.obtain();
        proj.initGrenade(ox, oy, gvx, gvy, p.getPlayerIndex(), p.getTeam());
        bullets.add(proj);
        addFloat("GRENADE!", p.cx(), p.cy()-30, GameConstants.C_GOLD2);
    }

    // Camera follow offset
    public float getCamX(Player p) {
        int mw = map.getW()*GameConstants.TILE_SIZE;
        float cx = p.cx() - GameConstants.WIN_W/2f;
        return Math.max(0, Math.min(mw - GameConstants.WIN_W, cx));
    }
    public float getCamY(Player p) {
        int mh = map.getH()*GameConstants.TILE_SIZE;
        float cy = p.cy() - GameConstants.WIN_H/2f + 80;
        return Math.max(0, Math.min(mh - GameConstants.WIN_H, cy));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void doExplosion(Projectile proj) {
        float ex = proj.cx(), ey = proj.cy();
        particles.spawnExplosion(ex, ey, EXPLOSION_RADIUS);
        for (Player p : players) {
            if (!p.isAlive()) continue;
            float dist = (float)Math.sqrt((p.cx()-ex)*(p.cx()-ex)+(p.cy()-ey)*(p.cy()-ey));
            if (dist < EXPLOSION_RADIUS) {
                int dmg = (int)(proj.getDamage() * (1f - dist/EXPLOSION_RADIUS));
                if (dmg > 0) {
                    float dx = p.cx()-ex, dy = p.cy()-ey;
                    p.applyImpulse(dx*2, dy*2 - 200);
                    Player shooter = getPlayerByIndex(proj.getOwner());
                    boolean dead = p.takeDamage(dmg, shooter);
                    addFloat("-"+dmg, p.cx(), p.cy(), GameConstants.P_EXPLOSION);
                    if (dead && shooter!=null) onKill(shooter, p, "Rocket");
                }
            }
        }
    }

    private void collectPickup(Player p, PickupItem item) {
        item.collect();
        switch (item.getPickupType()) {
            case WEAPON -> p.replaceWeapon(new com.samarbhumi.weapon.Weapon(item.getWeaponType()));
            case HEALTH -> { p.heal(item.getValue()); addFloat("+"+item.getValue()+" HP", p.cx(), p.cy(), GameConstants.C_HP_GREEN); }
            case AMMO   -> { com.samarbhumi.weapon.Weapon w=p.getActiveWeapon(); if(w!=null) w.addAmmo(30); addFloat("+Ammo", p.cx(), p.cy(), GameConstants.C_FUEL); }
            case ARMOR  -> { p.heal(item.getValue()/2); addFloat("+Armor", p.cx(), p.cy(), GameConstants.C_FUEL); }
            case GRENADE_PACK -> { p.addGrenades(item.getValue()); addFloat("+"+item.getValue()+" Grenades", p.cx(), p.cy(), GameConstants.C_GOLD2); }
        }
    }

    private void spawnJetParticles(Player p) {
        Color jc1 = switch(p.getJetTrail()) {
            case JET_FIRE    -> new Color(255,100,20);
            case JET_ICE     -> new Color(100,200,255);
            case JET_RAINBOW -> new Color((int)(Math.random()*200+55),(int)(Math.random()*200+55),(int)(Math.random()*200+55));
            default          -> GameConstants.P_JET_FIRE;
        };
        Color jc2 = switch(p.getJetTrail()) {
            case JET_FIRE    -> new Color(255,200,50);
            case JET_ICE     -> new Color(200,240,255);
            default          -> GameConstants.P_JET_FIRE2;
        };
        particles.spawnJetTrail(p.cx(), p.getPos().y+p.getH(), jc1, jc2);
    }

    private void tryPickup(Player p) {
        for (PickupItem item : pickups) {
            if (!item.isAlive()) continue;
            if (item.getBounds().overlaps(p.getBounds())) { collectPickup(p, item); break; }
        }
    }

    private void onKill(Player killer, Player victim, String weapon) {
        killFeed.addFirst(new KillFeedEntry(killer.getName(), victim.getName(), weapon));
        if (killFeed.size() > 5) killFeed.removeLast();
        addFloat("+Kill", killer.cx(), killer.cy()-30, GameConstants.C_GOLD2);
        // Death effect
        switch (victim.getDeathEffect()) {
            case DEATH_EXPLODE -> particles.spawnExplosion(victim.cx(), victim.cy(), 60);
            case DEATH_STAR    -> particles.spawnSpark(victim.cx(), victim.cy(), 0, -1, 20);
            case DEATH_COINS   -> {
                for (int i=0; i<8; i++) particles.spawnSpark(victim.cx(), victim.cy(), (float)(Math.random()-0.5)*200, -100f, 3);
            }
            default -> particles.spawnBlood(victim.cx(), victim.cy(), 0, -2, 12);
        }
    }

    private void spawnRandomPickup() {
        int tw=map.getW()*GameConstants.TILE_SIZE;
        float px=100f+(float)Math.random()*(tw-200);
        boolean health = Math.random()<0.6f;
        pickups.add(new PickupItem(px, 60, health?PickupItem.PickupType.HEALTH:PickupItem.PickupType.AMMO, 40));
    }

    private void checkWinCondition() {
        if (teamMode) {
            // Team mode: sum kills per team; first team to killsToWin total wins
            int blueKills = players.stream().filter(p->p.getTeam()==Enums.Team.BLUE).mapToInt(Player::getKills).sum();
            int redKills  = players.stream().filter(p->p.getTeam()==Enums.Team.RED) .mapToInt(Player::getKills).sum();
            if (blueKills >= killsToWin) {
                winner = players.stream().filter(p->p.getTeam()==Enums.Team.BLUE)
                                .max((a,b)->a.getKills()-b.getKills()).orElse(null);
                endMatch(); return;
            }
            if (redKills >= killsToWin) {
                winner = players.stream().filter(p->p.getTeam()==Enums.Team.RED)
                                .max((a,b)->a.getKills()-b.getKills()).orElse(null);
                endMatch(); return;
            }
        } else {
            // Free-for-all: first individual to killsToWin wins
            for (Player p : players) {
                if (p.getKills() >= killsToWin) { winner = p; endMatch(); return; }
            }
        }
        // Human out of lives → lose
        Player human = players.get(humanPlayerIdx);
        if (human.getDeaths() >= getDeathsToLose()) {
            winner = players.stream()
                .filter(p -> p != human)
                .max((a, b) -> a.getKills() - b.getKills())
                .orElse(null);
            endMatch();
        }
    }

    /** Blue team kills total (for HUD) */
    public int getBlueTeamKills() {
        return players.stream().filter(p->p.getTeam()==Enums.Team.BLUE).mapToInt(Player::getKills).sum();
    }
    public int getRedTeamKills() {
        return players.stream().filter(p->p.getTeam()==Enums.Team.RED).mapToInt(Player::getKills).sum();
    }
    public boolean isTeamMode() { return teamMode; }

    public int getKillsToWin()   { return killsToWin; }
    public int getDeathsToLose() {
        int enemies = players.size() - 1;
        return Math.max(3, enemies * 3);  // 1 enemy=3 lives, 2=6, etc.
    }
    public int getEffectiveKillsNeeded() { return killsToWin; }

    private void endMatch() { matchOver=true; }

    private Player getPlayerByIndex(int idx) {
        for (Player p : players) if (p.getPlayerIndex()==idx) return p;
        return null;
    }

    private void addFloat(String text, float x, float y, java.awt.Color c) {
        floatTexts.add(new FloatText(text, x, y, c));
    }

    // ── Getters ───────────────────────────────────────────────────────────
    public List<Player>          getPlayers()     { return players; }
    public List<Projectile>      getBullets()      { return bullets; }
    public List<PickupItem>      getPickups()      { return pickups; }
    public ParticleSystem        getParticles()    { return particles; }
    public GameMap               getMap()          { return map; }
    public Player                getHumanPlayer()  { return players.get(humanPlayerIdx); }
    public LinkedList<KillFeedEntry> getKillFeed() { return killFeed; }
    public LinkedList<FloatText> getFloatTexts()   { return floatTexts; }
    public float                 getMatchTimer()   { return matchTimer; }
    public boolean               isMatchOver()     { return matchOver; }
    public Player                getWinner()       { return winner; }
}

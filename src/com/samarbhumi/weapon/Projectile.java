package com.samarbhumi.weapon;

import com.samarbhumi.core.*;
import com.samarbhumi.core.Enums.*;
import com.samarbhumi.physics.PhysicsBody;

import java.awt.*;

/**
 * A fired projectile. Pooled for performance (no GC pressure during matches).
 * Bullets have zero gravity; grenades have full gravity + bounce.
 */
public class Projectile extends PhysicsBody {

    public enum ProjType { BULLET, ROCKET, GRENADE }

    private ProjType projType;
    private int      ownerIndex;   // player index who fired
    private Team     ownerTeam;
    private int      damage;
    private float    range;
    private float    distTravelled;
    private boolean  alive;
    private boolean  exploded;
    private float    explodeTimer; // for grenades
    private float    angle;        // visual rotation for rockets
    private Color    trailColor;
    private WeaponType weapType;


    // Object pool support
    private static final int POOL_SIZE = 256;
    private static final Projectile[] pool = new Projectile[POOL_SIZE];
    private static int poolTop = 0;

    static {
        for (int i=0;i<POOL_SIZE;i++) pool[i] = new Projectile();
    }

    private Projectile() { super(0,0,4,4); }

    /** Obtain a bullet from the pool. Never allocates during gameplay. */
    public static Projectile obtain() {
        if (poolTop > 0) { Projectile p = pool[--poolTop]; p.alive=true; p.distTravelled=0; p.exploded=false; return p; }
        Projectile p = new Projectile(); p.alive=true; return p;
    }

    /** Return projectile to pool. */
    public static void release(Projectile p) {
        p.alive = false;
        if (poolTop < POOL_SIZE) pool[poolTop++] = p;
    }

    public void initBullet(float x, float y, float vx, float vy, int damage, float range,
                           int ownerIdx, Team team, WeaponType wt) {
        pos.set(x,y); vel.set(vx,vy); accel.set(0,0);
        this.damage=damage; this.range=range;
        ownerIndex=ownerIdx; ownerTeam=team; weapType=wt;
        projType=ProjType.BULLET;
        gravScale=0f; // bullets fly straight
        w=4; h=4;
        trailColor = GameConstants.P_SPARK;
        alive=true; exploded=false;
    }

    public void initRocket(float x, float y, float vx, float vy, int ownerIdx, Team team) {
        pos.set(x,y); vel.set(vx,vy); accel.set(0,0);
        damage=80; range=600f;
        ownerIndex=ownerIdx; ownerTeam=team; weapType=WeaponType.ROCKET_LAUNCHER;
        projType=ProjType.ROCKET;
        gravScale=0.05f; w=10; h=5;
        trailColor=GameConstants.P_MUZZLE;
        alive=true; exploded=false;
        angle=(float)Math.atan2(vy,vx);
    }

    public void initGrenade(float x, float y, float vx, float vy, int ownerIdx, Team team) {
        pos.set(x,y); vel.set(vx,vy); accel.set(0,0);
        damage=90; range=150f;
        ownerIndex=ownerIdx; ownerTeam=team; weapType=WeaponType.GRENADE;
        projType=ProjType.GRENADE;
        gravScale=1f; w=8; h=8; explodeTimer=2.5f;
        alive=true; exploded=false;
    }

    @Override public void integrate(float dt) {
        if (!alive) return;
        super.integrate(dt);
        distTravelled += vel.len() * dt;
        if (projType == ProjType.GRENADE) {
            explodeTimer -= dt;
            if (explodeTimer <= 0 && !exploded) exploded=true;
            // Bounce
            if (isOnGround() && Math.abs(vel.y) < 30) vel.y = 0;
            else if (isOnGround()) vel.y = -vel.y * 0.4f;
            vel.x *= 0.98f;
            // Rotate visual
            angle += vel.x * dt * 0.1f;
        }
        if (projType == ProjType.ROCKET) angle = (float)Math.atan2(vel.y, vel.x);
        if (distTravelled > range && projType==ProjType.BULLET) alive=false;
    }

    public ProjType  getProjType()     { return projType; }
    public int       getOwner()        { return ownerIndex; }
    public Team      getOwnerTeam()    { return ownerTeam; }
    public int       getDamage()       { return damage; }
    public float     getRange()        { return range; }
    public boolean   isAlive()         { return alive; }
    public void      kill()            { alive=false; }
    public boolean   isExploded()      { return exploded; }
    public void      setExploded()     { exploded=true; }
    public float     getAngle()        { return angle; }
    public Color     getTrailColor()   { return trailColor; }
    public WeaponType getWeapType()    { return weapType; }
}

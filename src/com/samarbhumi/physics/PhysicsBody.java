package com.samarbhumi.physics;

import com.samarbhumi.core.*;

/**
 * A physics-simulated body.
 * Stores position, velocity, forces. Integrates each frame with fixed timestep.
 * Abstract base for all moving entities (Player, Bullet, Grenade, Pickup).
 */
public abstract class PhysicsBody {

    protected Vec2 pos;        // top-left corner (world space pixels)
    protected Vec2 vel;        // velocity px/s
    protected Vec2 accel;      // accumulated forces this frame
    protected float w, h;      // bounding box size

    protected boolean onGround  = false;
    protected boolean onLadder  = false;
    protected boolean inWater   = false;
    protected boolean awake     = true;
    protected float   gravScale = 1f;   // 0 = no gravity (bullets), 0.3 = water

    public PhysicsBody(float x, float y, float w, float h) {
        pos   = new Vec2(x, y);
        vel   = new Vec2();
        accel = new Vec2();
        this.w = w; this.h = h;
    }

    /** Apply force (px/s²) this frame — accumulates until integrate() */
    public void applyForce(float fx, float fy) { accel.x+=fx; accel.y+=fy; }

    /** Apply instant velocity impulse (px/s) */
    public void applyImpulse(float ix, float iy) { vel.x+=ix; vel.y+=iy; }

    /** Integrate velocity and position, apply gravity, drag. Call with dt in seconds. */
    public void integrate(float dt) {
        if (!awake) return;

        // Gravity
        if (!onLadder) accel.y += GameConstants.GRAVITY * gravScale;

        // Water drag
        if (inWater) {
            accel.y -= GameConstants.GRAVITY * 0.75f; // buoyancy
            vel.x *= 0.90f;
            vel.y *= 0.90f;
        }

        // Integrate acceleration → velocity
        vel.x += accel.x * dt;
        vel.y += accel.y * dt;

        // Ground / air friction
        if (onGround) {
            vel.x *= GameConstants.FRICTION;
        } else if (!onLadder) {
            vel.x *= GameConstants.AIR_FRICTION;
        }

        // Terminal velocity
        if (vel.y > GameConstants.TERMINAL_VEL) vel.y = GameConstants.TERMINAL_VEL;

        // Integrate velocity → position
        pos.x += vel.x * dt;
        pos.y += vel.y * dt;

        // Clear accumulated forces
        accel.set(0, 0);
        onGround = false; // reset — map collision will set this back
    }

    public AABB getBounds()      { return new AABB(pos.x, pos.y, w, h); }
    public float cx()            { return pos.x + w/2; }
    public float cy()            { return pos.y + h/2; }
    public Vec2  getPos()        { return pos; }
    public Vec2  getVel()        { return vel; }
    public float getW()          { return w; }
    public float getH()          { return h; }
    public boolean isOnGround()  { return onGround; }
    public boolean isOnLadder()  { return onLadder; }
    public boolean isInWater()   { return inWater; }
    public void setOnGround(boolean b) { onGround = b; }
    public void setOnLadder(boolean b) { onLadder = b; }
    public void setInWater(boolean b)  { inWater  = b; }
    public void setGravScale(float g)  { gravScale = g; }
    public boolean isAwake()     { return awake; }
    public void setAwake(boolean b){ awake = b; }
}

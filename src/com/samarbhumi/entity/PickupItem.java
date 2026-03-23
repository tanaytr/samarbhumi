package com.samarbhumi.entity;


import com.samarbhumi.core.Enums.*;
import com.samarbhumi.physics.PhysicsBody;

/** A collectible item dropped on the map. Physics-enabled (falls, rests on ground). */
public class PickupItem extends PhysicsBody {
    public enum PickupType { WEAPON, HEALTH, AMMO, ARMOR, GRENADE_PACK }

    private PickupType  pickupType;
    private WeaponType  weaponType; // if WEAPON
    private int         value;      // health/ammo amount
    private boolean     alive;
    private float       bobTimer;   // visual bobbing
    private float       glowPulse;

    public PickupItem(float x, float y, WeaponType wt) {
        super(x, y, 24, 24);
        this.pickupType = PickupType.WEAPON;
        this.weaponType = wt;
        this.alive      = true;
        gravScale       = 1f;
    }

    public PickupItem(float x, float y, PickupType type, int value) {
        super(x, y, 20, 20);
        this.pickupType = type;
        this.value      = value;
        this.alive      = true;
        gravScale       = 1f;
    }

    public void update(float dt) {
        if (!alive) return;
        bobTimer  += dt * 2f;
        glowPulse += dt * 3f;
    }

    public float getBobOffset() { return (float)Math.sin(bobTimer) * 3f; }
    public float getGlowAlpha() { return 0.5f + 0.5f*(float)Math.sin(glowPulse); }

    public PickupType getPickupType() { return pickupType; }
    public WeaponType getWeaponType() { return weaponType; }
    public int        getValue()      { return value; }
    public boolean    isAlive()       { return alive; }
    public void       collect()       { alive = false; }
}

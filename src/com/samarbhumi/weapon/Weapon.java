package com.samarbhumi.weapon;


import com.samarbhumi.core.Enums.*;
import java.io.Serializable;

/**
 * Instance of a weapon held by a player.
 * Manages ammo, reload state, fire rate cooldown, dual-wield flag.
 * Demonstrates encapsulation and method overloading.
 */
public class Weapon implements Serializable {
    private static final long serialVersionUID = 1L;

    private final WeaponType type;
    private int   ammoInClip;
    private int   ammoReserve;
    private float fireCooldown   = 0f;
    private float reloadTimer    = 0f;
    private boolean reloading    = false;
    private boolean dualWield    = false;
    private SkinId  skin         = SkinId.DEFAULT;

    public Weapon(WeaponType type) {
        this.type        = type;
        this.ammoInClip  = type.clipSize;
        this.ammoReserve = type.maxAmmo - type.clipSize;
    }

    public Weapon(WeaponType type, SkinId skin) {
        this(type); this.skin = skin;
    }

    /** @return true if a shot was fired */
    public boolean tryFire() {
        if (reloading || fireCooldown > 0) return false;
        if (type == WeaponType.KNIFE) { fireCooldown = type.fireDelay; return true; }
        if (ammoInClip <= 0) { if (ammoReserve > 0) startReload(); return false; }
        ammoInClip--;
        fireCooldown = type.fireDelay;
        if (ammoInClip == 0 && ammoReserve > 0) startReload();
        return true;
    }

    public void startReload() {
        if (reloading || ammoReserve == 0 || ammoInClip == type.clipSize) return;
        reloading   = true;
        reloadTimer = type.reloadTime;
    }

    public void update(float dt) {
        if (fireCooldown > 0) fireCooldown -= dt;
        if (reloading) {
            reloadTimer -= dt;
            if (reloadTimer <= 0) {
                int needed  = type.clipSize - ammoInClip;
                int taken   = Math.min(needed, ammoReserve);
                ammoInClip   += taken;
                ammoReserve  -= taken;
                reloading    = false;
            }
        }
    }

    public void addAmmo(int amount) { ammoReserve = Math.min(type.maxAmmo, ammoReserve + amount); }
    public void refillAmmo()        { ammoInClip = type.clipSize; ammoReserve = type.maxAmmo - type.clipSize; }

    public WeaponType getType()      { return type; }
    public int  getClipAmmo()        { return ammoInClip; }
    public int  getReserveAmmo()     { return ammoReserve; }
    public int  getTotalAmmo()       { return ammoInClip + ammoReserve; }
    public boolean isReloading()     { return reloading; }
    public float getReloadProgress() { return reloading ? 1f - reloadTimer/type.reloadTime : 1f; }
    public float getFireCooldown()   { return fireCooldown; }
    public boolean isDualWield()     { return dualWield; }
    public void setDualWield(boolean d) { dualWield = d; }
    public SkinId getSkin()          { return skin; }
    public void setSkin(SkinId s)    { skin = s; }
    public boolean isEmpty()         { return ammoInClip == 0 && ammoReserve == 0; }
    public String ammoString()       { return ammoInClip + " / " + ammoReserve; }
}

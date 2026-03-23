package com.samarbhumi.ui;

import com.samarbhumi.core.*;
import com.samarbhumi.core.Enums.*;
import com.samarbhumi.entity.Player;
import com.samarbhumi.weapon.Weapon;

import java.awt.*;
import java.awt.geom.*;

/**
 * Draws a fully-animated stick-figure player.
 * Body parts: head, torso, arms (upper+lower), legs (upper+lower), jetpack.
 * All rotated via AffineTransform — genuine skeletal animation.
 * Weapon rendered separately at aim angle with skin colour applied.
 */
public class PlayerRenderer {

    private static final RenderingHints RH;
    static {
        RH = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RH.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    public static void draw(Graphics2D g2, Player p, float camX, float camY) {
        if (!p.isAlive() && p.getDeadTimer() > 3f) return;

        g2.addRenderingHints(RH);

        float wx = p.cx() - camX;
        float wy = p.cy() - camY + p.getH()/2f;

        // Invincibility flash after spawn
        if (p.isInvincible() && (int)(System.currentTimeMillis()/80) % 2 == 0) return;

        // Hit flash — red tint overlay after taking damage
        if (p.getHitFlash() > 0) {
            g2.setColor(new Color(255,60,40,(int)(180*p.getHitFlash()/0.12f)));
        }

        Composite origComp = g2.getComposite();
        if (!p.isAlive()) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                Math.max(0f, 1f - p.getDeadTimer()/2f)));
        }

        // Skin colour system
        Color body    = p.getBodyColor();
        Color accent  = p.getAccentColor();
        Color skin    = skinTone(p.getCharacterSkin());
        Color equip   = equipColor(p.getCharacterSkin());
        Color outline = new Color(0,0,0,160);

        float frame  = p.getAnimFrame();
        boolean dead = !p.isAlive();
        boolean fr   = p.isFacingRight();
        float cr     = p.isCrouching() ? 0.7f : 1f;
        float aim    = p.getAimAngle();

        // ── SHADOW ────────────────────────────────────────────────────────
        if (p.isAlive()) {
            g2.setColor(new Color(0,0,0,45));
            g2.fillOval((int)(wx-12), (int)(wy+14*cr), 24, 6);
        }

        // ── JETPACK ───────────────────────────────────────────────────────
        if (p.isAlive()) drawJetpack(g2, wx, wy, cr, fr, p.isJetpackActive(), p.getFuelFraction(), equip);

        // ── LEGS ──────────────────────────────────────────────────────────
        if (!dead) {
            float legSwing = 0f;
            if (p.getAnimState() == Player.AnimState.RUN) {
                legSwing = (float)Math.sin(frame * Math.PI/2) * 0.5f;
            } else if (!p.isOnGround()) {
                legSwing = 0.3f;
            }
            // Upper legs
            drawLimb(g2, wx-4, wy+4*cr, 4, 12*cr, legSwing,  body, outline, fr);
            drawLimb(g2, wx+4, wy+4*cr, 4, 12*cr, -legSwing, body, outline, !fr);
            // Lower legs / boots
            drawLimb(g2, wx-4+legSwing*6, wy+16*cr, 4, 10*cr, legSwing*0.5f, equip, outline, fr);
            drawLimb(g2, wx+4-legSwing*6, wy+16*cr, 4, 10*cr, -legSwing*0.5f, equip, outline, !fr);
        } else {
            // Ragdoll legs
            drawLimb(g2, wx-4, wy+4,   4, 12, 1.2f,  body,  outline, true);
            drawLimb(g2, wx+4, wy+4,   4, 12, -0.4f, body,  outline, false);
            drawLimb(g2, wx-4+7, wy+14, 4, 10, 0.8f, equip, outline, true);
            drawLimb(g2, wx+4-2, wy+14, 4, 10, -1.0f,equip, outline, false);
        }

        // ── TORSO ─────────────────────────────────────────────────────────
        float torsoH = 18*cr;
        drawTorso(g2, wx, wy-6*cr, 14, (int)torsoH, dead, body, accent, outline);

        // ── ARMS ──────────────────────────────────────────────────────────
        if (!dead) {
            // Aim arm — follows aim angle
            drawAimArm(g2, wx, wy-2*cr, aim, fr, skin, outline);
        } else {
            drawLimb(g2, wx-6, wy-2, 4, 14, -1.8f, skin, outline, false);
            drawLimb(g2, wx+6, wy-2, 4, 14,  0.5f, skin, outline, true);
        }

        // ── HEAD ──────────────────────────────────────────────────────────
        float headY = wy - 18*cr;
        if (dead) headY = wy - 8;
        drawHead(g2, wx, headY, skin, equip, p.getCharacterSkin(), dead, aim, fr);

        // ── WEAPON ───────────────────────────────────────────────────────
        if (p.isAlive()) {
            Weapon w = p.getActiveWeapon();
            if (w != null && w.getType() != WeaponType.KNIFE) {
                drawWeapon(g2, wx, wy-2*cr, aim, fr, w, p);
            }
            // Reload animation — spinning indicator
            if (w != null && w.isReloading()) {
                drawReloadIndicator(g2, wx, headY-14, w.getReloadProgress());
            }
        }

        g2.setComposite(origComp);

        // ── NAME TAG ──────────────────────────────────────────────────────
        if (p.isAlive()) drawNameTag(g2, wx, wy-26*cr, p);
    }

    // ── Body part drawers ────────────────────────────────────────────────

    private static void drawTorso(Graphics2D g, float x, float y, int w, int h,
                                   boolean tilted, Color body, Color accent, Color outline) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        if (tilted) g.rotate(1.5f);
        // Body vest
        g.setColor(body);
        g.fillRoundRect(-w/2, 0, w, h, 4, 4);
        // Accent stripe
        g.setColor(accent);
        g.fillRect(-w/2+2, h/4, w-4, h/3);
        // Outline
        g.setColor(outline); g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(-w/2, 0, w, h, 4, 4);
        g.setTransform(old);
    }

    private static void drawLimb(Graphics2D g, float x, float y, int w, float h,
                                  float angle, Color col, Color outline, boolean left) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(angle);
        g.setColor(col);
        g.fillRoundRect(-w/2, 0, w, (int)h, w, w);
        g.setColor(outline); g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(-w/2, 0, w, (int)h, w, w);
        g.setTransform(old);
    }

    private static void drawAimArm(Graphics2D g, float x, float y, float aimAngle, boolean fr,
                                    Color skin, Color outline) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        g.rotate(aimAngle);
        // Upper arm
        g.setColor(skin);
        g.fillRoundRect(-2, -2, 14, 5, 3, 3);
        // Forearm slightly bent
        g.rotate(0.3f, 14, 2);
        g.fillRoundRect(12, -2, 12, 5, 3, 3);
        g.setColor(outline); g.setStroke(new BasicStroke(0.8f));
        g.drawRoundRect(-2, -2, 14, 5, 3, 3);
        g.drawRoundRect(12, -2, 12, 5, 3, 3);
        g.setTransform(old);
    }

    private static void drawHead(Graphics2D g, float x, float y, Color skin, Color equip,
                                  SkinId skinId, boolean dead, float aim, boolean fr) {
        AffineTransform old = g.getTransform();
        g.translate(x, y);
        if (dead) g.rotate(1.6f);
        else {
            // Slight head tilt toward aim
            float tilt = (float)Math.sin(aim) * 0.15f;
            g.rotate(tilt);
        }

        // Neck
        g.setColor(skin); g.fillRect(-2, 0, 4, 5);

        // Helmet / hat based on skin
        int hw=20, hh=16;
        g.setColor(equip);
        switch (skinId) {
            case WARRIOR   -> g.fillRoundRect(-hw/2, -hh, hw, hh, 4, 4);    // standard helmet
            case COMMANDO  -> { g.fillRoundRect(-hw/2,-hh,hw,hh,2,2); // beret
                                g.setColor(skin.darker()); g.fillOval(-hw/2+2,-hh+2,hw-4,6); }
            case RENEGADE  -> { g.fillRoundRect(-hw/2,-hh+2,hw,hh-2,2,2); // no top
                                g.setColor(new Color(30,30,30)); g.fillRect(-hw/2,-hh,hw,5); } // headband
            case GHOST     -> { g.setColor(new Color(220,220,220)); // white mask
                                g.fillRoundRect(-hw/2,-hh,hw,hh,8,8); }
            default        -> g.fillRoundRect(-hw/2,-hh,hw,hh,4,4);
        }

        // Face / visor
        g.setColor(skin);
        g.fillRoundRect(-hw/2+3, -hh+hh/3, hw-6, hh*2/3, 4, 4);

        // Eye
        int eyeX = fr ? 3 : -5;
        g.setColor(new Color(30,30,30)); g.fillOval(eyeX, -hh+hh/2, 4, 4);
        g.setColor(Color.WHITE);         g.fillOval(eyeX+1, -hh+hh/2+1, 2, 2);

        // Outline
        g.setColor(new Color(0,0,0,150)); g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(-hw/2, -hh, hw, hh, 4, 4);
        g.drawRoundRect(-hw/2+3, -hh+hh/3, hw-6, hh*2/3, 4, 4);
        g.setTransform(old);
    }

    private static void drawJetpack(Graphics2D g, float x, float y, float cr,
                                     boolean fr, boolean active, float fuel, Color col) {
        int jx = fr ? (int)(x-14) : (int)(x+6);
        int jy = (int)(y-14*cr);
        // Pack body
        g.setColor(col.darker());
        g.fillRoundRect(jx, jy, 8, (int)(22*cr), 3, 3);
        // Nozzles
        g.setColor(new Color(50,50,50));
        g.fillRect(jx+1, jy+(int)(18*cr), 6, 4);
        // Fuel gauge
        g.setColor(new Color(30,30,30));     g.fillRect(jx+2, jy+2, 4, (int)(14*cr));
        g.setColor(GameConstants.C_FUEL);    g.fillRect(jx+2, jy+2+(int)((14*cr)*(1-fuel)), 4, (int)(14*cr*fuel));
        // Flame when active
        if (active) {
            float fz = 6f + (float)Math.random()*8f;
            g.setColor(new Color(100,180,255,200));
            g.fillOval(jx, jy+(int)(22*cr), 8, (int)fz);
            g.setColor(new Color(200,240,255,150));
            g.fillOval(jx+2, jy+(int)(22*cr), 4, (int)(fz*0.6f));
        }
    }

    private static void drawWeapon(Graphics2D g, float x, float y, float aimAngle, boolean fr,
                                    Weapon w, Player p) {
        AffineTransform old = g.getTransform();
        g.translate(x + (fr?4:-4), y);
        g.rotate(aimAngle);
        if (!fr) g.scale(1, -1); // flip for left-facing

        Color wCol  = weaponBodyColor(w);
        Color wDark = wCol.darker();

        switch (w.getType()) {
            case ASSAULT_RIFLE -> {
                g.setColor(wDark);  g.fillRect(0,  -3, 28, 6);  // receiver
                g.setColor(wCol);   g.fillRect(2,  -2, 24, 4);
                g.setColor(wDark);  g.fillRect(18,  -3, 10, 3); // barrel
                g.setColor(wCol);   g.fillRect(-4, -2, 6, 8);   // grip
                g.setColor(wDark);  g.fillRect(4,  2, 10, 4);   // mag
            }
            case SHOTGUN -> {
                g.setColor(wDark);  g.fillRect(0, -4, 30, 7);
                g.setColor(new Color(120,80,40)); g.fillRect(0, -2, 14, 4); // stock wood
                g.setColor(wCol);   g.fillRect(12, -4, 20, 5);
                g.setColor(wDark);  g.fillRect(28, -3, 8, 3);  // barrel tip
            }
            case SNIPER -> {
                g.setColor(wDark);  g.fillRect(0, -3, 36, 5);
                g.setColor(wCol);   g.fillRect(2, -2, 32, 3);
                g.setColor(wDark);  g.fillRect(30, -2, 8, 2);  // muzzle
                g.setColor(new Color(80,80,100)); g.fillRect(10, -7, 10, 4); // scope
                g.setColor(new Color(60,60,80)); g.fillRect(11, -5, 8, 2);
                g.setColor(wCol);   g.fillRect(-4, -2, 5, 7);  // grip
            }
            case SMG -> {
                g.setColor(wDark);  g.fillRect(0, -3, 20, 5);
                g.setColor(wCol);   g.fillRect(2, -2, 16, 3);
                g.setColor(wDark);  g.fillRect(-3, -1, 5, 7);  // grip
                g.setColor(wCol);   g.fillRect(4, 2, 6, 6);    // mag (angled)
            }
            case ROCKET_LAUNCHER -> {
                g.setColor(wDark);
                g.fillRoundRect(0, -5, 34, 10, 4, 4);
                g.setColor(wCol);
                g.fillOval(26, -4, 8, 8);  // rocket
                g.setColor(new Color(80,80,80)); g.fillOval(27, -3, 6, 6);
            }
            case PISTOL, DUAL_PISTOLS -> {
                g.setColor(wDark);  g.fillRect(0, -3, 16, 5);
                g.setColor(wCol);   g.fillRect(1, -2, 14, 3);
                g.setColor(wDark);  g.fillRect(-3, -1, 5, 7);
                g.setColor(wCol);   g.fillRect(2, 2, 4, 5);
            }
            case GRENADE -> {
                g.setColor(new Color(60,120,60));
                g.fillOval(-4, -5, 10, 10);
                g.setColor(wDark); g.setStroke(new BasicStroke(0.8f)); g.drawOval(-4,-5,10,10);
                g.setColor(new Color(80,80,80)); g.fillRect(3,-6,3,4);
            }
            default -> {
                g.setColor(wDark); g.fillRect(0,-2,18,4);
                g.setColor(wCol);  g.fillRect(-3,-1,5,6);
            }
        }
        // Muzzle flash
        if (p.getShootFlash() > 0.03f) {
            float sz = 6f + (float)Math.random()*8f;
            float blen = weaponBarrelLength(w.getType());
            g.setColor(new Color(255,240,100,200));
            g.fillOval((int)blen, -(int)(sz/2), (int)sz, (int)sz);
            g.setColor(new Color(255,255,255,150));
            g.fillOval((int)blen+2, -(int)(sz/4), (int)(sz/2), (int)(sz/2));
        }

        g.setTransform(old);
    }

    private static float weaponBarrelLength(WeaponType wt) {
        return switch (wt) {
            case ASSAULT_RIFLE -> 28f;
            case SHOTGUN       -> 36f;
            case SNIPER        -> 38f;
            case SMG           -> 22f;
            case ROCKET_LAUNCHER-> 34f;
            default            -> 16f;
        };
    }

    private static Color weaponBodyColor(Weapon w) {
        return switch (w.getSkin()) {
            case DESERT_CAMO -> new Color(160,130,70);
            case ARCTIC      -> new Color(200,220,230);
            case URBAN_CAMO  -> new Color(90,100,85);
            case CHROME      -> new Color(180,190,200);
            case GOLD_PLATED -> new Color(200,170,40);
            default          -> new Color(55,58,60);
        };
    }

    private static Color skinTone(SkinId sk) {
        return switch (sk) {
            case GHOST   -> new Color(220,215,210);
            case RENEGADE-> new Color(160,105,65);
            default      -> new Color(200,155,105);
        };
    }

    private static Color equipColor(SkinId sk) {
        return switch (sk) {
            case WARRIOR  -> new Color(60,85,50);
            case COMMANDO -> new Color(50,70,100);
            case RENEGADE -> new Color(80,55,35);
            case GHOST    -> new Color(190,195,200);
            default       -> new Color(60,70,55);
        };
    }

    private static void drawReloadIndicator(Graphics2D g, float x, float y, float progress) {
        g.setColor(new Color(0,0,0,130));
        g.fillArc((int)x-10,(int)y-10,20,20,90,360);
        g.setColor(GameConstants.C_GOLD2);
        g.setStroke(new BasicStroke(2.5f));
        g.drawArc((int)x-10,(int)y-10,20,20,90,-(int)(360*progress));
    }

    private static void drawNameTag(Graphics2D g, float x, float y, Player p) {
        String name = p.getName();
        g.setFont(GameConstants.F_SMALL);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(name);
        // Background
        g.setColor(new Color(0,0,0,130));
        g.fillRoundRect((int)x-tw/2-3, (int)y-13, tw+6, 13, 3, 3);
        // HP-based name colour
        Color nc = p.getHpFraction() > 0.6f ? GameConstants.C_WHITE
                 : p.getHpFraction() > 0.3f ? GameConstants.C_HP_YELLOW : GameConstants.C_HP_RED;
        g.setColor(nc);
        g.drawString(name, (int)x-tw/2, (int)y-2);
        // HP bar below name
        int bw=36, bh=3;
        int bx=(int)x-bw/2, by=(int)y;
        g.setColor(new Color(30,30,30,180)); g.fillRect(bx, by, bw, bh);
        Color hc = p.getHpFraction()>0.6f?GameConstants.C_HP_GREEN:p.getHpFraction()>0.3f?GameConstants.C_HP_YELLOW:GameConstants.C_HP_RED;
        g.setColor(hc); g.fillRect(bx, by, (int)(bw*p.getHpFraction()), bh);
    }
}

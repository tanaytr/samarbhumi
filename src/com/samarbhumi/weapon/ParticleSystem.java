package com.samarbhumi.weapon;

import com.samarbhumi.core.*;
import com.samarbhumi.core.Enums.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Manages all visual particles: blood, sparks, smoke, shell casings, muzzle flash.
 * Uses object pooling (ArrayList recycle) to avoid GC during gameplay.
 */
public class ParticleSystem {

    private static final Random RNG = new Random();
    private final List<Particle> active  = new ArrayList<>(512);
    private final List<Particle> pool    = new ArrayList<>(512);

    private Particle obtain() {
        if (!pool.isEmpty()) return pool.remove(pool.size()-1);
        return new Particle();
    }

    private void emit(float x, float y, float vx, float vy,
                      float life, float gravity, Color c1, Color c2,
                      float size, ParticleType type) {
        Particle p = obtain();
        p.init(x, y, vx, vy, life, gravity, c1, c2, size, type);
        active.add(p);
    }

    // ── Emitter helpers ───────────────────────────────────────────────────

    public void spawnBlood(float x, float y, float dirX, float dirY, int count) {
        for (int i=0; i<count; i++) {
            float speed = 80f + RNG.nextFloat()*220f;
            float a = (float)Math.atan2(dirY,dirX) + (RNG.nextFloat()-0.5f)*1.8f;
            float size = 2f + RNG.nextFloat()*3f;
            emit(x, y, (float)Math.cos(a)*speed, (float)Math.sin(a)*speed,
                 0.4f+RNG.nextFloat()*0.5f, 600f,
                 GameConstants.P_BLOOD, GameConstants.P_BLOOD2, size, ParticleType.BLOOD);
        }
    }

    public void spawnSpark(float x, float y, float dirX, float dirY, int count) {
        for (int i=0; i<count; i++) {
            float speed = 120f + RNG.nextFloat()*300f;
            float a = (float)Math.atan2(dirY,dirX) + (RNG.nextFloat()-0.5f)*1.0f;
            emit(x, y, (float)Math.cos(a)*speed, (float)Math.sin(a)*speed,
                 0.1f+RNG.nextFloat()*0.2f, 400f,
                 GameConstants.P_SPARK, GameConstants.P_MUZZLE, 2f, ParticleType.SPARK);
        }
    }

    public void spawnMuzzleFlash(float x, float y, float dirX, float dirY) {
        for (int i=0; i<5; i++) {
            float speed = 50f+RNG.nextFloat()*100f;
            float a = (float)Math.atan2(dirY,dirX) + (RNG.nextFloat()-0.5f)*0.4f;
            emit(x, y, (float)Math.cos(a)*speed, (float)Math.sin(a)*speed,
                 0.05f+RNG.nextFloat()*0.06f, 0f,
                 GameConstants.P_MUZZLE, new Color(255,255,200), 3f+RNG.nextFloat()*4f, ParticleType.MUZZLE);
        }
    }

    public void spawnShellCasing(float x, float y, boolean facingRight) {
        float vx = facingRight ? -(40f+RNG.nextFloat()*60f) : (40f+RNG.nextFloat()*60f);
        float vy = -(50f + RNG.nextFloat()*80f);
        emit(x, y, vx, vy, 0.8f+RNG.nextFloat()*0.5f, 600f,
             GameConstants.P_SHELL, GameConstants.P_SHELL, 3f, ParticleType.SHELL);
    }

    public void spawnExplosion(float x, float y, float radius) {
        // Fireballs
        for (int i=0; i<20; i++) {
            float speed = 50f+RNG.nextFloat()*radius*2;
            float a = RNG.nextFloat()*(float)(Math.PI*2);
            float size = 4f+RNG.nextFloat()*12f;
            emit(x, y, (float)Math.cos(a)*speed, (float)Math.sin(a)*speed-80f,
                 0.3f+RNG.nextFloat()*0.5f, 200f,
                 GameConstants.P_EXPLOSION, GameConstants.P_SPARK, size, ParticleType.EXPLOSION);
        }
        // Smoke
        for (int i=0; i<12; i++) {
            float speed = 20f+RNG.nextFloat()*60f;
            float a = RNG.nextFloat()*(float)(Math.PI*2);
            emit(x, y, (float)Math.cos(a)*speed, (float)Math.sin(a)*speed-30f,
                 0.8f+RNG.nextFloat()*0.6f, -50f,
                 GameConstants.P_SMOKE, new Color(180,175,165,80), 8f+RNG.nextFloat()*14f, ParticleType.SMOKE);
        }
        // Debris sparks
        spawnSpark(x, y, 0, -1, 15);
    }

    public void spawnDust(float x, float y) {
        for (int i=0; i<4; i++) {
            float vx = (RNG.nextFloat()-0.5f)*60f;
            emit(x, y, vx, -20f-RNG.nextFloat()*30f, 0.3f+RNG.nextFloat()*0.2f, -100f,
                 GameConstants.P_DUST, GameConstants.P_DUST, 5f+RNG.nextFloat()*5f, ParticleType.DUST);
        }
    }

    public void spawnJetTrail(float x, float y, Color c1, Color c2) {
        Particle p = obtain();
        p.init(x + (RNG.nextFloat()-0.5f)*6f, y,
               (RNG.nextFloat()-0.5f)*20f, 30f+RNG.nextFloat()*50f,
               0.1f+RNG.nextFloat()*0.12f, -60f, c1, c2, 4f+RNG.nextFloat()*5f, ParticleType.JET);
        active.add(p);
    }

    // ── Update / Draw ─────────────────────────────────────────────────────

    public void update(float dt) {
        for (int i = active.size()-1; i >= 0; i--) {
            Particle p = active.get(i);
            p.update(dt);
            if (!p.isAlive()) { pool.add(active.remove(i)); }
        }
    }

    public void draw(Graphics2D g, float camX, float camY) {
        for (Particle p : active) p.draw(g, camX, camY);
    }

    public int count() { return active.size(); }

    // ── Inner Particle class ───────────────────────────────────────────────

    private static class Particle {
        float x, y, vx, vy, life, maxLife, gravity, size;
        Color c1, c2;
        ParticleType type;
        float rot, rotSpeed;
        boolean alive;
        static final Random R = new Random();

        void init(float x, float y, float vx, float vy, float life, float gravity,
                  Color c1, Color c2, float size, ParticleType type) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy;
            this.life=life; this.maxLife=life; this.gravity=gravity;
            this.c1=c1; this.c2=c2; this.size=size; this.type=type;
            this.alive=true;
            rot=R.nextFloat()*(float)(Math.PI*2);
            rotSpeed=(R.nextFloat()-0.5f)*8f;
        }

        void update(float dt) {
            vy += gravity * dt;
            x  += vx * dt;
            y  += vy * dt;
            vx *= 0.98f;
            rot += rotSpeed * dt;
            life -= dt;
            if (life <= 0) alive = false;
        }

        boolean isAlive() { return alive; }

        void draw(Graphics2D g, float camX, float camY) {
            float t = 1f - life/maxLife;
            // Interpolate color
            int r = (int)(c1.getRed()  *(1-t) + c2.getRed()  *t);
            int gv= (int)(c1.getGreen()*(1-t) + c2.getGreen()*t);
            int b = (int)(c1.getBlue() *(1-t) + c2.getBlue() *t);
            int a = (int)(255 * Math.max(0, life/maxLife));
            a = Math.min(255, Math.max(0, a));
            g.setColor(new Color(r,gv,b,a));

            int sx = (int)(x - camX);
            int sy = (int)(y - camY);
            int sz = Math.max(1, (int)(size * (1f - t*0.5f)));

            if (type == ParticleType.SHELL || type == ParticleType.BLOOD) {
                // Draw as rotated rectangle
                var old = g.getTransform();
                g.translate(sx, sy);
                g.rotate(rot);
                g.fillRect(-sz/2, -sz/2, sz, sz/2);
                g.setTransform(old);
            } else {
                g.fillOval(sx - sz/2, sy - sz/2, sz, sz);
            }
        }
    }
}

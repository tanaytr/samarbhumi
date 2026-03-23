package com.samarbhumi.ui;

import com.samarbhumi.core.*;
import com.samarbhumi.core.GameSession.*;
import com.samarbhumi.entity.Player;
import com.samarbhumi.weapon.Projectile;

import java.awt.*;
import java.awt.geom.*;
import java.util.LinkedList;

/**
 * Renders the active match world + HUD.
 * Camera follows human player with smooth lerp.
 */
public class GameScreen {

    private final GameSession session;
    private float camX = 0, camY = 0;

    private float damageVignette = 0f;
    private int   lastHp;

    public GameScreen(GameSession session) {
        this.session = session;
        Player h = session.getHumanPlayer();
        camX  = session.getCamX(h);
        camY  = session.getCamY(h);
        lastHp = h.getHp();
    }

    public void render(Graphics2D g, int mouseX, int mouseY) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        Player human = session.getHumanPlayer();

        // Smooth camera
        float targetCX = session.getCamX(human);
        float targetCY = session.getCamY(human);
        camX += (targetCX - camX) * 0.12f;
        camY += (targetCY - camY) * 0.12f;

        int SW = GameConstants.WIN_W, SH = GameConstants.WIN_H;

        MapRenderer.drawBackground(g, camX, camY, SW, SH, session.getMap().getMapStyle());
        MapRenderer.drawTiles(g, session.getMap(), camX, camY, SW, SH);
        MapRenderer.drawPickups(g, session.getPickups(), camX, camY);

        drawProjectiles(g, camX, camY);

        for (Player p : session.getPlayers())
            PlayerRenderer.draw(g, p, camX, camY);

        session.getParticles().draw(g, camX, camY);

        // Float texts — convert world → screen
        LinkedList<FloatText> screenTexts = new LinkedList<>();
        for (FloatText ft : session.getFloatTexts()) {
            FloatText copy = new FloatText(ft.text, ft.x - camX, ft.y - camY, ft.color);
            copy.life = ft.life; copy.maxLife = ft.maxLife;
            screenTexts.add(copy);
        }

        // Damage vignette
        int hp = human.getHp();
        if (hp < lastHp) damageVignette = 0.85f;
        lastHp = hp;
        damageVignette = Math.max(0, damageVignette - 0.03f);
        if (damageVignette > 0 || human.getHpFraction() < 0.25f) drawVignette(g, SW, SH, human);

        HUDRenderer.draw(g, human, session.getKillFeed(), screenTexts,
                         session.getMatchTimer(), session.getPlayers(),
                         session.getEffectiveKillsNeeded(), session.getDeathsToLose(),
                         session.isTeamMode(), session.getBlueTeamKills(), session.getRedTeamKills());
        HUDRenderer.drawMinimap(g, session.getMap(), session.getPlayers(), camX, camY);
        HUDRenderer.drawCrosshairAt(g, mouseX, mouseY, human.getShootFlash() > 0.05f);

        if (session.isMatchOver()) drawMatchOver(g, SW, SH);
    }

    private void drawProjectiles(Graphics2D g, float camX, float camY) {
        for (Projectile proj : session.getBullets()) {
            if (!proj.isAlive()) continue;
            int px = (int)(proj.cx() - camX);
            int py = (int)(proj.cy() - camY);

            switch (proj.getProjType()) {
                case BULLET -> {
                    float vLen = proj.getVel().len();
                    if (vLen == 0) continue;
                    float dx = proj.getVel().x / vLen * 10;
                    float dy = proj.getVel().y / vLen * 10;
                    g.setColor(new Color(255, 240, 180, 185));
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(px-(int)dx, py-(int)dy, px+(int)dx, py+(int)dy);
                    g.setColor(new Color(255, 255, 220, 225));
                    g.fillOval(px-2, py-2, 4, 4);
                }
                case ROCKET -> {
                    AffineTransform old = g.getTransform();
                    g.translate(px, py); g.rotate(proj.getAngle());
                    g.setColor(new Color(120,80,40)); g.fillRoundRect(-14,-4,28,8,4,4);
                    g.setColor(new Color(200,60,30)); g.fillOval(12,-3,6,6);
                    g.setColor(new Color(255,140,30,205)); g.fillOval(-20,-4,10,8);
                    g.setTransform(old);
                }
                case GRENADE -> {
                    AffineTransform old = g.getTransform();
                    g.translate(px, py); g.rotate(proj.getAngle());
                    g.setColor(new Color(60,120,60)); g.fillRoundRect(-5,-5,10,10,4,4);
                    g.setColor(new Color(100,100,100)); g.fillRect(0,-6,3,4);
                    g.setTransform(old);
                }
            }
        }
    }

    private void drawVignette(Graphics2D g, int W, int H, Player human) {
        float intensity = Math.max(damageVignette, 1f - human.getHpFraction() * 2f);
        if (intensity <= 0) return;
        float alpha = Math.min(0.75f, intensity * 0.82f);
        RadialGradientPaint vg = new RadialGradientPaint(W/2f, H/2f, W * 0.55f,
            new float[]{0.5f, 1f},
            new Color[]{new Color(0,0,0,0), new Color(185,15,15,(int)(alpha*255))});
        g.setPaint(vg); g.fillRect(0,0,W,H);
    }

    private void drawMatchOver(Graphics2D g, int W, int H) {
        g.setColor(new Color(0,0,0,165)); g.fillRect(0,0,W,H);

        Player winner = session.getWinner();
        Player human  = session.getHumanPlayer();
        boolean humanWon = (winner != null && winner == human);

        String title = humanWon ? "VICTORY!" : "DEFEATED!";
        Color  tc    = humanWon ? GameConstants.C_GOLD2 : GameConstants.C_RED;

        g.setFont(GameConstants.F_TITLE);
        FontMetrics fm = g.getFontMetrics();
        for (int i=4;i>=1;i--) {
            g.setColor(new Color(tc.getRed(),tc.getGreen(),tc.getBlue(),(int)(40.0/i)));
            g.drawString(title, W/2-fm.stringWidth(title)/2-i, H/2-70+i);
        }
        g.setColor(tc); g.drawString(title, W/2-fm.stringWidth(title)/2, H/2-70);

        // Reason
        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_WHITE);
        String reason;
        if (humanWon) {
            reason = "You reached " + session.getKillsToWin() + " kills first!";
        } else if (winner != null) {
            reason = human.getDeaths() >= session.getDeathsToLose()
                ? "You ran out of lives!  " + winner.getName() + " wins."
                : winner.getName() + " reached " + session.getKillsToWin() + " kills!";
        } else {
            Player top = session.getPlayers().stream().max((a,b)->a.getKills()-b.getKills()).orElse(human);
            reason = top==human ? "Time up — you had the most kills!" : "Time up — "+top.getName()+" wins!";
        }
        fm = g.getFontMetrics();
        g.drawString(reason, W/2-fm.stringWidth(reason)/2, H/2-28);

        // Stats
        int bx=W/2-165, by2=H/2-8;
        g.setColor(new Color(0,0,0,125)); g.fillRoundRect(bx,by2,330,64,8,8);
        g.setFont(GameConstants.F_HUD);
        g.setColor(GameConstants.C_GREEN);
        g.drawString("Kills:   " + human.getKills(), bx+20, by2+22);
        g.setColor(GameConstants.C_RED);
        g.drawString("Deaths:  " + human.getDeaths(), bx+20, by2+44);
        g.setColor(GameConstants.C_GOLD2);
        g.drawString("K/D:  " + (human.getDeaths()==0?"Perfect":String.format("%.1f",(float)human.getKills()/Math.max(1,human.getDeaths()))), bx+185, by2+33);

        g.setFont(GameConstants.F_BODY); g.setColor(GameConstants.C_DIM);
        String cont = "Press ESC to continue";
        fm = g.getFontMetrics();
        g.drawString(cont, W/2-fm.stringWidth(cont)/2, H/2+78);
    }

    public float getCamX() { return camX; }
    public float getCamY() { return camY; }
}

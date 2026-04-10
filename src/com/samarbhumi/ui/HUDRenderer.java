package com.samarbhumi.ui;

import com.samarbhumi.core.*;
import com.samarbhumi.core.GameSession.*;
import com.samarbhumi.entity.Player;
import com.samarbhumi.weapon.Weapon;

import java.awt.*;
import java.util.*;

/**
 * Draws the in-game HUD: HP/fuel bars, ammo, timer, kill feed, scoreboard,
 * goal tracker (WIN bar + LIVES bar), controls tooltip, minimap, respawn overlay.
 */
public class HUDRenderer {

    private static final int SW = GameConstants.WIN_W;
    private static final int SH = GameConstants.WIN_H;

    // ── Main draw entry point ─────────────────────────────────────────────

    public static void draw(Graphics2D g, Player human,
                            LinkedList<KillFeedEntry> killFeed,
                            LinkedList<FloatText> floats,
                            float matchTimer,
                            java.util.List<Player> allPlayers,
                            int killsToWin, int deathsToLose,
                            boolean teamMode, int blueKills, int redKills) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawPlayerPanel(g, human);
        drawTimer(g, matchTimer);
        drawKillFeed(g, killFeed);
        drawFloats(g, floats);
        drawScoreboard(g, allPlayers, teamMode);
        if (teamMode) drawTeamBars(g, blueKills, redKills, killsToWin);
        else          drawGoalBars(g, human, allPlayers, killsToWin, deathsToLose);
        
        long humanCount = allPlayers.stream().filter(p -> p.isHuman()).count();
        drawControlsHint(g, humanCount == 1);
        
        drawRespawnOverlay(g, human);
    }

    // ── Player panel (top-left) ───────────────────────────────────────────

    private static void drawPlayerPanel(Graphics2D g, Player p) {
        int x = 10, y = 10;

        // Background
        g.setColor(new Color(0, 0, 0, 148));
        g.fillRoundRect(x - 4, y - 4, 242, 96, 10, 10);
        g.setColor(new Color(80, 120, 40, 80));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(x - 4, y - 4, 242, 96, 10, 10);

        // HP bar
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_WHITE);
        g.drawString("HP", x, y + 14);
        float hf = p.getHpFraction();
        Color hc = hf > 0.6f ? GameConstants.C_HP_GREEN : hf > 0.3f ? GameConstants.C_HP_YELLOW : GameConstants.C_HP_RED;
        barBg(g, x + 28, y + 4, 162, 14);
        g.setColor(hc);
        g.fillRoundRect(x + 29, y + 5, (int)(160 * hf), 12, 4, 4);
        barFg(g, x + 28, y + 4, 162, 14);
        g.setFont(GameConstants.F_HUD); g.setColor(hc);
        g.drawString("" + p.getHp(), x + 194, y + 14);

        // Fuel bar
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_WHITE);
        g.drawString("JP", x, y + 32);
        float ff = p.getFuelFraction();
        barBg(g, x + 28, y + 22, 162, 10);
        g.setColor(GameConstants.C_FUEL);
        g.fillRoundRect(x + 29, y + 23, (int)(160 * ff), 8, 3, 3);
        barFg(g, x + 28, y + 22, 162, 10);

        // Jump indicator dots
        int j = p.getJumpsAvailable();
        for (int i = 0; i < 2; i++) {
            g.setColor(i < j ? GameConstants.C_ACCENT : new Color(40, 40, 40));
            g.fillOval(x + 194 + i * 14, y + 22, 10, 10);
        }

        // Weapon / ammo
        // Grenade count
        g.setFont(GameConstants.F_SMALL);
        g.setColor(new Color(255, 200, 60, 200));
        g.drawString("GRENADES: " + p.getGrenadeCount(), x, y + 82);

        Weapon w = p.getActiveWeapon();
        if (w != null) {
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRoundRect(x, y + 38, 56, 50, 6, 6);
            g.setColor(new Color(255, 255, 255, 30));
            g.setStroke(new BasicStroke(0.8f));
            g.drawRoundRect(x, y + 38, 56, 50, 6, 6);

            g.setFont(new Font("SansSerif", Font.BOLD, 8));
            g.setColor(GameConstants.C_DIM);
            g.drawString(w.getType().displayName, x + 2, y + 50);

            g.setFont(GameConstants.F_HUD_BIG);
            g.setColor(w.getClipAmmo() == 0 ? GameConstants.C_RED : GameConstants.C_WHITE);
            g.drawString("" + w.getClipAmmo(), x + 4, y + 80);
            g.setFont(GameConstants.F_HUD);
            g.setColor(GameConstants.C_DIM);
            g.drawString("/" + w.getReserveAmmo(), x + 32, y + 80);

            if (w.isReloading()) {
                long ms = System.currentTimeMillis();
                float fl = (ms / 250) % 2 == 0 ? 1f : 0.4f;
                g.setFont(GameConstants.F_HUD);
                g.setColor(new Color(255, 200, 50, (int)(230 * fl)));
                g.drawString("RELOADING", x + 62, y + 70);
                barBg(g, x + 62, y + 74, 120, 6);
                g.setColor(GameConstants.C_GOLD);
                g.fillRoundRect(x + 63, y + 75, (int)(118 * w.getReloadProgress()), 4, 2, 2);
            }
        }
    }

    // ── Match timer (top-centre) ──────────────────────────────────────────

    private static void drawTimer(Graphics2D g, float timeLeft) {
        int cx = SW / 2;
        String ts = String.format("%d:%02d", (int)(timeLeft/60), (int)(timeLeft%60));
        g.setFont(GameConstants.F_HEAD);
        FontMetrics fm = g.getFontMetrics();
        int tw = fm.stringWidth(ts);
        g.setColor(new Color(0, 0, 0, 148));
        g.fillRoundRect(cx - tw/2 - 16, 5, tw + 32, 32, 8, 8);
        g.setColor(new Color(80, 120, 40, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(cx - tw/2 - 16, 5, tw + 32, 32, 8, 8);
        Color tc = timeLeft < 30 ? GameConstants.C_RED : GameConstants.C_WHITE;
        if (timeLeft < 30 && (System.currentTimeMillis()/500)%2==0) tc = GameConstants.C_GOLD2;
        g.setColor(tc);
        g.drawString(ts, cx - tw/2, 28);
    }

    // ── Kill feed (top-right) ─────────────────────────────────────────────

    private static void drawKillFeed(Graphics2D g, LinkedList<KillFeedEntry> feed) {
        if (feed.isEmpty()) return;
        // Measure widest entry to centre the feed
        g.setFont(GameConstants.F_HUD);
        int lineH = 24, padding = 8;
        // Draw from top-centre
        int feedY = 10;
        for (KillFeedEntry kf : feed) {
            float alpha = Math.min(1f, kf.life / 1.5f);
            int a = (int)(alpha * 220);
            if (a <= 0) { feedY += lineH; continue; }

            g.setFont(GameConstants.F_HUD);
            FontMetrics fm = g.getFontMetrics();
            int killerW  = fm.stringWidth(kf.killer);
            g.setFont(GameConstants.F_SMALL);
            FontMetrics fmS = g.getFontMetrics();
            int weapW    = fmS.stringWidth(" [" + kf.weapon + "] ");
            g.setFont(GameConstants.F_HUD);
            int victimW  = fm.stringWidth(kf.victim);
            int totalW   = killerW + weapW + victimW + padding * 2;
            int startX   = SW/2 - totalW/2;

            // Background pill
            g.setColor(new Color(0, 0, 0, Math.min(200, a)));
            g.fillRoundRect(startX - padding, feedY, totalW + padding, lineH, 6, 6);
            g.setColor(new Color(80, 130, 40, Math.min(120, a)));
            g.setStroke(new BasicStroke(0.8f));
            g.drawRoundRect(startX - padding, feedY, totalW + padding, lineH, 6, 6);

            // Killer — green
            g.setFont(GameConstants.F_HUD);
            g.setColor(new Color(100, 230, 100, Math.min(255, a)));
            g.drawString(kf.killer, startX, feedY + 16);

            // Weapon — gold
            g.setFont(GameConstants.F_SMALL);
            g.setColor(new Color(255, 210, 60, Math.min(255, a)));
            g.drawString(" [" + kf.weapon + "] ", startX + killerW, feedY + 16);

            // Victim — red
            g.setFont(GameConstants.F_HUD);
            g.setColor(new Color(230, 80, 80, Math.min(255, a)));
            g.drawString(kf.victim, startX + killerW + weapW, feedY + 16);

            feedY += lineH + 2;
        }
    }

    // ── Floating damage texts ─────────────────────────────────────────────

    private static void drawFloats(Graphics2D g, LinkedList<FloatText> texts) {
        for (FloatText ft : texts) {
            float alpha = ft.life / ft.maxLife;
            int a = (int)(alpha * 230);
            if (a <= 0) continue;
            Color c = new Color(ft.color.getRed(), ft.color.getGreen(), ft.color.getBlue(), Math.min(255, a));
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(new Color(0, 0, 0, Math.min(255, (int)(a * 0.7f))));
            g.drawString(ft.text, (int)ft.x + 1, (int)ft.y + 1);
            g.setColor(c);
            g.drawString(ft.text, (int)ft.x, (int)ft.y);
        }
    }

    // ── Scoreboard (top-right, below kill feed) ───────────────────────────

    private static void drawScoreboard(Graphics2D g, java.util.List<Player> players, boolean teamMode) {
        int panelW = 200, rowH = 20;
        int x = SW - panelW - 8, y = 8;

        if (teamMode) {
            // Group by team: Vajra (Blue) then Pralay (Red)
            java.util.List<Player> blue = players.stream()
                .filter(p->p.getTeam()==com.samarbhumi.core.Enums.Team.BLUE)
                .sorted((a,b)->b.getKills()-a.getKills())
                .collect(java.util.stream.Collectors.toList());
            java.util.List<Player> red = players.stream()
                .filter(p->p.getTeam()==com.samarbhumi.core.Enums.Team.RED)
                .sorted((a,b)->b.getKills()-a.getKills())
                .collect(java.util.stream.Collectors.toList());
            int blueKills = blue.stream().mapToInt(Player::getKills).sum();
            int redKills  = red.stream().mapToInt(Player::getKills).sum();

            int panelH = 14 + (2 + blue.size() + red.size()) * rowH + 6;
            g.setColor(new Color(0,0,0,145));
            g.fillRoundRect(x-4, y, panelW+8, panelH, 8, 8);
            g.setColor(new Color(80,130,40,80));
            g.setStroke(new BasicStroke(0.8f));
            g.drawRoundRect(x-4, y, panelW+8, panelH, 8, 8);

            // Column headers
            g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_GOLD);
            g.drawString("PLAYER", x+14, y+13);
            g.drawString("K", x+panelW-38, y+13);
            g.drawString("D", x+panelW-16, y+13);
            g.setColor(new Color(80,130,40,80)); g.setStroke(new BasicStroke(0.8f));
            g.drawLine(x, y+16, x+panelW, y+16);

            int rowY = y + 16 + rowH - 4;
            // Vajra header
            g.setFont(GameConstants.F_SMALL);
            g.setColor(GameConstants.C_TEAM_BLUE);
            g.drawString("⚡ VAJRA  " + blueKills, x+2, rowY);
            rowY += rowH;
            for (Player p : blue) { drawScoreRow(g, p, x, rowY, panelW); rowY += rowH; }
            // Pralay header
            g.setFont(GameConstants.F_SMALL);
            g.setColor(GameConstants.C_TEAM_RED);
            g.drawString("☄ PRALAY  " + redKills, x+2, rowY);
            rowY += rowH;
            for (Player p : red) { drawScoreRow(g, p, x, rowY, panelW); rowY += rowH; }

        } else {
            int panelH = 26 + players.size() * rowH;
            g.setColor(new Color(0,0,0,145));
            g.fillRoundRect(x-4, y, panelW+8, panelH, 8, 8);
            g.setColor(new Color(80,130,40,80));
            g.setStroke(new BasicStroke(0.8f));
            g.drawRoundRect(x-4, y, panelW+8, panelH, 8, 8);

            g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_GOLD);
            g.drawString("PLAYER",  x+14, y+14);
            g.drawString("K",  x+panelW-38, y+14);
            g.drawString("D",  x+panelW-16, y+14);
            g.setColor(new Color(80,130,40,80)); g.setStroke(new BasicStroke(0.8f));
            g.drawLine(x, y+17, x+panelW, y+17);

            java.util.List<Player> sorted = players.stream()
                .sorted((a,b) -> b.getKills()-a.getKills())
                .collect(java.util.stream.Collectors.toList());
            int rowY = y + 18 + rowH - 4;
            for (Player p : sorted) { drawScoreRow(g, p, x, rowY, panelW); rowY += rowH; }
        }
    }

    private static void drawScoreRow(Graphics2D g, Player p, int x, int rowY, int panelW) {
        Color dot = p.getTeam()==com.samarbhumi.core.Enums.Team.BLUE
            ? GameConstants.C_TEAM_BLUE : GameConstants.C_TEAM_RED;
        if (p.isHuman()) dot = GameConstants.C_GOLD2;
        if (p.isHuman()) {
            g.setColor(new Color(80,130,40,38));
            g.fillRoundRect(x-2, rowY-16, panelW+4, 20, 4, 4);
        }
        g.setColor(dot); g.fillOval(x, rowY-10, 8, 8);
        g.setFont(GameConstants.F_SMALL);
        g.setColor(p.isHuman() ? GameConstants.C_GOLD2 : GameConstants.C_WHITE);
        String nm = p.getName().length()>9 ? p.getName().substring(0,8)+"." : p.getName();
        g.drawString(nm, x+13, rowY);
        g.setFont(GameConstants.F_HUD);
        FontMetrics fm2 = g.getFontMetrics();
        g.setColor(GameConstants.C_GREEN);
        String ks = ""+p.getKills();
        g.drawString(ks, x+panelW-38+(14-fm2.stringWidth(ks))/2, rowY);
        g.setColor(GameConstants.C_RED);
        String ds = ""+p.getDeaths();
        g.drawString(ds, x+panelW-16+(14-fm2.stringWidth(ds))/2, rowY);
    }

    // ── Goal bars (bottom-left) ───────────────────────────────────────────

    private static void drawGoalBars(Graphics2D g, Player human,
                                      java.util.List<Player> players,
                                      int killsToWin, int deathsToLose) {
        int x = 8, y = SH - 98;
        int panW = 230;
        g.setColor(new Color(0, 0, 0, 145));
        g.fillRoundRect(x - 2, y - 2, panW + 4, 92, 8, 8);
        g.setColor(new Color(80, 120, 40, 85));
        g.setStroke(new BasicStroke(0.8f));
        g.drawRoundRect(x - 2, y - 2, panW + 4, 92, 8, 8);

        // Win bar
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_GOLD);
        g.drawString("KILLS: " + human.getKills() + " / " + killsToWin, x + 4, y + 15);
        barBg(g, x + 4, y + 19, panW - 8, 8);
        float wp = Math.min(1f, (float)human.getKills() / Math.max(1, killsToWin));
        Color wc = wp > 0.8f ? new Color(100, 255, 80) : wp > 0.5f ? GameConstants.C_ACCENT : GameConstants.C_GREEN;
        g.setColor(wc);
        g.fillRoundRect(x + 5, y + 20, (int)((panW - 10) * wp), 6, 2, 2);

        // Lives bar
        int livesLeft = Math.max(0, deathsToLose - human.getDeaths());
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_RED2);
        g.drawString("LIVES: " + livesLeft + " / " + deathsToLose, x + 4, y + 44);
        barBg(g, x + 4, y + 48, panW - 8, 8);
        float lp = Math.min(1f, (float)livesLeft / Math.max(1, deathsToLose));
        Color lc = lp > 0.5f ? GameConstants.C_HP_GREEN : lp > 0.25f ? GameConstants.C_HP_YELLOW : GameConstants.C_HP_RED;
        g.setColor(lc);
        g.fillRoundRect(x + 5, y + 49, (int)((panW - 10) * lp), 6, 2, 2);

        // Leader
        Player leader = players.stream().max((a,b)->a.getKills()-b.getKills()).orElse(human);
        g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_DIM);
        String ls = leader == human ? "You lead! Keep going." : leader.getName()+" leads with "+leader.getKills();
        g.drawString(ls, x + 4, y + 69);

        g.setFont(GameConstants.F_SMALL); g.setColor(new Color(130, 130, 110, 110));
        g.drawString("Goal: enemies x3 kills  |  ESC = Pause", x + 4, y + 82);
    }

    // ── Controls hint (bottom-centre) ────────────────────────────────────

    private static void drawControlsHint(Graphics2D g, boolean singlePlayer) {
        int cx = SW / 2, y = SH - 4;
        g.setFont(GameConstants.F_SMALL);
        
        String p1 = singlePlayer ? "WASD / ARROWS = Move | SPACE=Jump | SHIFT=Grenade | Q=Melee | R=Reload" : "P1: ARROWS=Move | SHIFT=Grenade | Num5=Melee | Num0=Reload";
        String p2 = singlePlayer ? "LMB / ENTER = Fire | F=Pickup | TAB=Swap | ESC=Pause" : "P2: WASD=Move | CTRL=Grenade | Q=Melee | R=Reload | LMB=Fire";
        
        FontMetrics fm = g.getFontMetrics();
        int tw = Math.max(fm.stringWidth(p1), fm.stringWidth(p2));
        g.setColor(new Color(0, 0, 0, 100));
        g.fillRoundRect(cx - tw/2 - 6, y - 30, tw + 12, 32, 5, 5);
        g.setColor(new Color(120, 180, 60, 138));
        g.drawString(p1, cx - fm.stringWidth(p1)/2, y - 17);
        g.setColor(new Color(100, 160, 50, 118));
        g.drawString(p2, cx - fm.stringWidth(p2)/2, y - 3);
    }

    // ── Respawn overlay ───────────────────────────────────────────────────

    private static void drawRespawnOverlay(Graphics2D g, Player p) {
        if (p.isAlive()) return;
        float rt = p.getRespawnTimer();
        if (rt <= 0) return;

        int cx = SW/2, cy = SH/2;

        // Dark red-tinted overlay
        g.setColor(new Color(60, 0, 0, 180));
        g.fillRect(0, 0, SW, SH);

        // Central card
        g.setColor(new Color(8, 4, 4, 220));
        g.fillRoundRect(cx - 280, cy - 90, 560, 180, 18, 18);
        g.setColor(new Color(200, 50, 30, 180));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(cx - 280, cy - 90, 560, 180, 18, 18);

        // "ELIMINATED" — bold orange-red with black shadow for readability
        g.setFont(GameConstants.F_TITLE);
        FontMetrics fm = g.getFontMetrics();
        String elim = "ELIMINATED";
        int ex = cx - fm.stringWidth(elim)/2;
        // Shadow
        g.setColor(new Color(0, 0, 0, 255));
        g.drawString(elim, ex+3, cy - 36+3);
        // Main text — bright red-orange
        g.setColor(new Color(255, 90, 40));
        g.drawString(elim, ex, cy - 36);

        // Killer name — bright white with shadow
        g.setFont(GameConstants.F_HEAD);
        fm = g.getFontMetrics();
        String killedBy = "Killed by  " + p.getKillerName();
        int kx = cx - fm.stringWidth(killedBy)/2;
        g.setColor(new Color(0,0,0,200)); g.drawString(killedBy, kx+2, cy+2);
        g.setColor(new Color(255, 220, 100)); g.drawString(killedBy, kx, cy);

        // Respawn countdown — bright cyan
        g.setFont(GameConstants.F_HUD_BIG);
        fm = g.getFontMetrics();
        String resp = "Respawning in  " + (int)Math.ceil(rt) + "...";
        int rx2 = cx - fm.stringWidth(resp)/2;
        g.setColor(new Color(0,0,0,200)); g.drawString(resp, rx2+2, cy+32);
        g.setColor(new Color(100, 220, 255)); g.drawString(resp, rx2, cy+30);

        // Countdown ring
        g.setColor(new Color(255,255,255, 40));
        g.setStroke(new BasicStroke(5f));
        g.drawArc(cx - 34, cy + 45, 68, 68, 90, -(int)(360*(rt/GameConstants.RESPAWN_TIME)));
        g.setColor(new Color(100, 220, 255));
        g.setStroke(new BasicStroke(3.5f));
        g.drawArc(cx - 34, cy + 45, 68, 68, 90,  (int)(360*(1f-rt/GameConstants.RESPAWN_TIME)));
    }

    // ── Team kill bars (team mode only) ─────────────────────────────────

    private static void drawTeamBars(Graphics2D g, int blueKills, int redKills, int target) {
        int x = 8, y = SH - 98, panW = 230;
        g.setColor(new Color(0,0,0,145));
        g.fillRoundRect(x-2, y-2, panW+4, 92, 8, 8);
        g.setColor(new Color(80,120,40,85)); g.setStroke(new BasicStroke(0.8f));
        g.drawRoundRect(x-2, y-2, panW+4, 92, 8, 8);

        // Blue team bar
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_TEAM_BLUE);
        g.drawString("BLUE: " + blueKills + " / " + target, x+4, y+16);
        barBg(g, x+4, y+20, panW-8, 9);
        float bp = Math.min(1f, (float)blueKills/Math.max(1,target));
        g.setColor(GameConstants.C_TEAM_BLUE);
        g.fillRoundRect(x+5, y+21, (int)((panW-10)*bp), 7, 2, 2);

        // Red team bar
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_TEAM_RED);
        g.drawString("RED:  " + redKills + " / " + target, x+4, y+46);
        barBg(g, x+4, y+50, panW-8, 9);
        float rp = Math.min(1f, (float)redKills/Math.max(1,target));
        g.setColor(GameConstants.C_TEAM_RED);
        g.fillRoundRect(x+5, y+51, (int)((panW-10)*rp), 7, 2, 2);

        g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_DIM);
        String leader = blueKills > redKills ? "BLUE leads!" : redKills > blueKills ? "RED leads!" : "Tied!";
        g.drawString(leader + "  |  ESC = Pause", x+4, y+74);
    }

        // ── Minimap ───────────────────────────────────────────────────────────

    public static void drawMinimap(Graphics2D g, com.samarbhumi.map.GameMap map,
                                    java.util.List<Player> players, float camX, float camY) {
        int mw=188, mh=85, mx=SW-mw-8, my=SH-mh-8;
        float scX=(float)mw/(map.getW()*GameConstants.TILE_SIZE);
        float scY=(float)mh/(map.getH()*GameConstants.TILE_SIZE);
        g.setColor(new Color(0,0,0,165)); g.fillRoundRect(mx,my,mw,mh,6,6);
        g.setColor(new Color(80,120,40,118)); g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(mx,my,mw,mh,6,6);
        g.setColor(new Color(58,88,40,158));
        for (int r=0;r<map.getH();r++) for (int c=0;c<map.getW();c++) {
            if (map.getTile(c,r)==com.samarbhumi.core.Enums.TileType.SOLID) {
                int tx=(int)(mx+c*GameConstants.TILE_SIZE*scX);
                int ty=(int)(my+r*GameConstants.TILE_SIZE*scY);
                g.fillRect(tx,ty,Math.max(1,(int)(GameConstants.TILE_SIZE*scX)),Math.max(1,(int)(GameConstants.TILE_SIZE*scY)));
            }
        }
        for (Player p : players) {
            if (!p.isAlive()) continue;
            Color dot = p.isHuman() ? GameConstants.C_GOLD2 :
                p.getTeam()==com.samarbhumi.core.Enums.Team.BLUE ? GameConstants.C_TEAM_BLUE : GameConstants.C_TEAM_RED;
            int dx=(int)(mx+p.cx()*scX), dy=(int)(my+p.cy()*scY);
            g.setColor(new Color(0,0,0,150)); g.fillOval(dx-2,dy-2,8,8);
            g.setColor(dot); g.fillOval(dx-1,dy-1,6,6);
        }
        int vx=(int)(mx+camX*scX), vy=(int)(my+camY*scY);
        int vw=(int)(SW*scX), vh=(int)(SH*scY);
        g.setColor(new Color(255,255,255,38));
        g.fillRect(Math.max(mx,vx),Math.max(my,vy),Math.min(vw,mw),Math.min(vh,mh));
        g.setColor(new Color(255,255,255,98)); g.setStroke(new BasicStroke(0.8f));
        g.drawRect(Math.max(mx,vx),Math.max(my,vy),Math.min(vw,mw-2),Math.min(vh,mh-2));
    }

    // ── Crosshair ─────────────────────────────────────────────────────────

    public static void drawCrosshairAt(Graphics2D g, int mx, int my, boolean firing) {
        int sz = firing ? 14 : 18;
        g.setColor(new Color(0,0,0,100)); g.setStroke(new BasicStroke(2.5f));
        g.drawLine(mx-sz-1,my+1,mx-5+1,my+1); g.drawLine(mx+5+1,my+1,mx+sz+1,my+1);
        g.drawLine(mx+1,my-sz+1,mx+1,my-5+1); g.drawLine(mx+1,my+5+1,mx+1,my+sz+1);
        g.setColor(new Color(255,255,255,225)); g.setStroke(new BasicStroke(1.5f));
        g.drawLine(mx-sz,my,mx-5,my); g.drawLine(mx+5,my,mx+sz,my);
        g.drawLine(mx,my-sz,mx,my-5); g.drawLine(mx,my+5,mx,my+sz);
        g.setColor(new Color(255,80,80,210)); g.fillOval(mx-2,my-2,4,4);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void barBg(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(15,15,15,205)); g.fillRoundRect(x,y,w,h,4,4);
        g.setColor(new Color(0,0,0,100)); g.setStroke(new BasicStroke(0.8f)); g.drawRoundRect(x,y,w,h,4,4);
    }

    private static void barFg(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(255,255,255,28)); g.fillRoundRect(x+1,y+1,w-2,h/2,3,3);
        g.setColor(new Color(255,255,255,58)); g.drawRoundRect(x,y,w,h,4,4);
    }
}
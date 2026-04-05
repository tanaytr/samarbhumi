package com.samarbhumi.ui;

import com.samarbhumi.entity.Player;
import com.samarbhumi.core.*;
import com.samarbhumi.net.NetManager;
import com.samarbhumi.core.Enums.*;
import com.samarbhumi.progression.PlayerProfile;
import com.samarbhumi.audio.AudioEngine;

import java.awt.*;
import java.awt.geom.AffineTransform;

// ============================================================================
//  MAIN MENU SCREEN
// ============================================================================
class MainMenuScreen {

    public enum Action { NONE, PLAY, PROFILE, STORE, SETTINGS, LEADERBOARD, SWITCH_PROFILE, QUIT }
    private float time = 0f;
    private boolean showLeaderboard = false;
    private final int W = UIRenderer.W, H = UIRenderer.H, CX = W / 2;
    private float[] solX  = {-50, W + 50, 180, W - 180};
    private float[] solDir = {1, -1, 1, -1};
    private java.util.List<PlayerProfile.LeaderboardEntry> lbCache = null;
    private float lbTimer = 0f;

    public void update(float dt) {
        time += dt;
        for (int i = 0; i < solX.length; i++) {
            solX[i] += solDir[i] * 18f * dt;
            if (solX[i] > W + 80) solX[i] = -80;
            if (solX[i] < -80)   solX[i] = W + 80;
        }
        lbTimer -= dt;
        if (lbCache == null || lbTimer <= 0) {
            lbCache = PlayerProfile.getLeaderboard();
            lbTimer = 10f; // Refresh every 10s
        }
    }

    public void render(Graphics2D g, int mx, int my, PlayerProfile profile) {
        UIRenderer.drawMenuBG(g, time);
        for (int i = 0; i < solX.length; i++)
            drawSilhouette(g, (int) solX[i], H - 95, solDir[i] > 0);

        // Buttons — right column
        int bw = 260, bh = 48, bx = CX + 80, gap = 44;
        int by = (H - (7 * bh + 6 * gap)) / 2;

        UIRenderer.button(g, "[ PLAY ]",           bx, by,             bw, bh, mx, my, false, new Color(35, 100, 15));
        UIRenderer.button(g, "[ PROFILE ]",        bx, by + (bh+gap),  bw, bh, mx, my, false, new Color(30, 65, 20));
        UIRenderer.button(g, "[ STORE ]",           bx, by+2*(bh+gap),  bw, bh, mx, my, false, new Color(100, 70, 10));
        UIRenderer.button(g, "[ SETTINGS ]",        bx, by+3*(bh+gap),  bw, bh, mx, my, false, new Color(30, 52, 30));
        UIRenderer.button(g, "[ LEADERBOARD ]",     bx, by+4*(bh+gap),  bw, bh, mx, my, false, new Color(80, 50, 120));
        UIRenderer.button(g, "[ SWITCH PROFILE ]",  bx, by+5*(bh+gap),  bw, bh, mx, my, false, new Color(50, 50, 10));
        UIRenderer.button(g, "[ QUIT ]",            bx, by+6*(bh+gap),  bw, bh, mx, my, false, new Color(110, 20, 15));

        // Logo — left side, vertically centred with the whole button block
        int logoY = by + (7*bh + 6*gap)/2 - 18;  // centre of button stack
        UIRenderer.drawLogo(g, (int)(W * 0.30f), logoY, time);

        // Conditional Leaderboard Modal
        if (showLeaderboard) drawLeaderboardModal(g, mx, my);

        // Profile badge top-left — profile may be null (guest mode)
        if (profile != null) drawProfileBadge(g, profile);
        else                 drawGuestBadge(g);

        g.setFont(GameConstants.F_SMALL);
        g.setColor(new Color(80, 120, 40, 150));
        g.drawString("v1.2  Built with Java", 10, H - 8);

        g.setFont(GameConstants.F_SMALL);
        g.setColor(new Color(100, 150, 50, 120));
        String c1 = "P1: Arrow Keys | Up x2=Jump | Num8=Jet | Num5=Melee | Enter=Shoot | LMB=Fire";
        String c2 = "P2: WASD | W x2=Jump | E=Jet | Q=Melee | R=Reload | LMB=Fire";
        FontMetrics fm = g.getFontMetrics();
        g.drawString(c1, CX - fm.stringWidth(c1) / 2, H - 20);
        g.drawString(c2, CX - fm.stringWidth(c2) / 2, H - 8);
    }

    public Action handleClick(int mx, int my) {
        if (showLeaderboard) {
            int pw = 450, ph = 350;
            int px = (W - pw) / 2, py = (H - ph) / 2;
            if (new Rectangle(px + pw / 2 - 60, py + ph - 60, 120, 36).contains(mx, my)) {
                showLeaderboard = false;
            }
            return Action.NONE;
        }

        int bw = 260, bh = 48, bx = CX + 80, gap = 44;
        int by = (H - (7 * bh + 6 * gap)) / 2;
        if (mx >= bx && mx <= bx + bw) {
            Action[] order = { Action.PLAY, Action.PROFILE, Action.STORE,
                               Action.SETTINGS, Action.LEADERBOARD, Action.SWITCH_PROFILE, Action.QUIT };
            for (int i = 0; i < order.length; i++) {
                if (my >= by + i*(bh+gap) && my <= by + i*(bh+gap) + bh) {
                    if (order[i] == Action.LEADERBOARD) {
                        showLeaderboard = true;
                        lbCache = null; // force immediate fetch
                        return Action.NONE;
                    }
                    return order[i];
                }
            }
        }
        return Action.NONE;
    }

    private void drawProfileBadge(Graphics2D g, PlayerProfile p) {
        int px = 10, py = 10;
        g.setColor(new Color(0, 0, 0, 145));
        g.fillRoundRect(px, py, 210, 60, 10, 10);
        g.setColor(new Color(80, 130, 40, 100));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(px, py, 210, 60, 10, 10);
        g.setFont(GameConstants.F_HUD);
        g.setColor(GameConstants.C_GOLD2);
        g.drawString(p.getPlayerName(), px + 10, py + 20);
        g.setFont(GameConstants.F_SMALL);
        g.setColor(GameConstants.C_DIM);
        g.drawString("Lv " + p.getLevel() + "  " + p.getRankTitle(), px + 10, py + 35);
        g.setColor(GameConstants.C_GOLD);
        g.drawString(p.getCoins() + " coins", px + 10, py + 50);
        UIRenderer.drawHBar(g, px + 100, py + 44, 100, 8, p.levelProgress(), GameConstants.C_ACCENT);
    }

    /** Badge shown when no profile is loaded (guest mode). */
    private void drawGuestBadge(Graphics2D g) {
        int px = 10, py = 10;
        g.setColor(new Color(0, 0, 0, 130));
        g.fillRoundRect(px, py, 210, 60, 10, 10);
        g.setColor(new Color(80, 80, 40, 90));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(px, py, 210, 60, 10, 10);
        g.setFont(GameConstants.F_HUD);
        g.setColor(new Color(180, 180, 100));
        g.drawString("GUEST", px + 10, py + 20);
        g.setFont(GameConstants.F_SMALL);
        g.setColor(GameConstants.C_DIM);
        g.drawString("No profile loaded", px + 10, py + 35);
        g.setColor(new Color(140, 120, 50, 180));
        g.drawString("Switch Profile to sign in", px + 10, py + 50);
    }

    private void drawSilhouette(Graphics2D g, int x, int y, boolean right) {
        AffineTransform saved = g.getTransform();
        g.setColor(new Color(12, 25, 6, 65));
        if (!right) { g.translate(x * 2 + 40, 0); g.scale(-1, 1); }
        g.fillRoundRect(x + 14, y - 40, 12, 28, 4, 4);
        g.fillOval(x + 13, y - 56, 14, 16);
        g.fillRect(x + 24, y - 38, 20, 5);
        g.fillRect(x + 14, y - 14, 5, 20);
        g.fillRect(x + 21, y - 14, 5, 20);
        g.setTransform(saved);
    }

    private void drawLeaderboardModal(Graphics2D g, int mx, int my) {
        int pw = 530, ph = 350;
        int px = (W - pw) / 2, py = (H - ph) / 2;
        
        // Dim background
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, W, H);
        
        UIRenderer.panel(g, px, py, pw, ph, "Local Leaderboard");
        
        int ry = py + 85;
        g.setFont(GameConstants.F_SMALL);
        g.setColor(GameConstants.C_DIM);
        // Draw headers individually for precise alignment
        g.drawString("RANK",   px + 30,  ry - 20);
        g.drawString("PLAYER", px + 95,  ry - 20);
        g.drawString("LEVEL",  px + 320, ry - 20);
        g.drawString("TOTAL XP", px + 415, ry - 20);

        if (lbCache == null || lbCache.isEmpty()) {
            g.setColor(Color.WHITE);
            g.drawString("No profiles found.", px + pw/2 - 70, ry + 50);
        } else {
            for (int i = 0; i < lbCache.size(); i++) {
                PlayerProfile.LeaderboardEntry e = lbCache.get(i);
                int rowY = ry + i * 40;
                
                // Rank
                g.setFont(GameConstants.F_SUBHEAD);
                g.setColor(i == 0 ? GameConstants.C_GOLD : Color.WHITE);
                g.drawString("#" + (i + 1), px + 30, rowY);

                // Name
                g.setFont(GameConstants.F_HUD);
                g.setColor(Color.WHITE);
                String name = e.name();
                if (name.length() > 14) name = name.substring(0, 14);
                g.drawString(name, px + 95, rowY);

                // Level
                g.setColor(GameConstants.C_GOLD2);
                g.drawString("Lv " + e.level(), px + 320, rowY);

                // XP (Right-aligned under the header)
                g.setFont(GameConstants.F_SMALL);
                g.setColor(GameConstants.C_DIM);
                String xpStr = String.format("%,d", e.xp());
                g.drawString(xpStr, px + 415, rowY);

                g.setColor(new Color(80, 120, 40, 60));
                g.drawLine(px + 30, rowY + 12, px + pw - 30, rowY + 12);
            }
        }
        
        UIRenderer.button(g, "BACK", px + pw / 2 - 60, py + ph - 60, 120, 36, mx, my);
    }
}

// ============================================================================
//  LOBBY / MODE SELECTION SCREEN
// ============================================================================
class LobbyScreen {

    public enum Action   { NONE, START, BACK }
    public enum GameMode { VS_BOTS, TWO_PLAYER, ONLINE }

    private GameMode mode        = GameMode.VS_BOTS;
    private boolean  teamMode    = false;   // false=personal, true=team
    private int      selectedMap = 0;
    private int      numBots     = 3;
    private int      diffIdx     = 1;
    private float    time        = 0f;

    // Name editing
    private boolean editingName = false;
    private String  nameInput   = "";

    // Online lobby
    private String  lobbyCode     = "";
    private String  joinCodeInput = "";
    private boolean editingJoinCode = false;
    private String  onlineStatus  = "";
    private int     onlineTeamSub = 0;

    private final int W = UIRenderer.W, CX = W / 2;

    private static final String[] MAP_NAMES = {"Warzone Alpha", "Jungle Ruins", "Steel Fortress", "City Ruins"};
    private static final String[] MAP_DESC  = {
        "Open field, grass platforms, water trenches",
        "Vertical stone ruins, dense ladder networks",
        "Symmetric metal fortress, tight corridors",
        "Urban rooftops, fire escapes, flooded alleys"
    };
    private static final String[] DIFF_NAMES = {"EASY", "MEDIUM", "HARD"};
    private static final Color[]  DIFF_COLS  = {
        new Color(50, 180, 50), new Color(220, 180, 30), new Color(220, 50, 40)
    };

    public void update(float dt) {
        time += dt;
        // Animate online status dots
    }

    public void render(Graphics2D g, int mx, int my) {
        UIRenderer.drawMenuBG(g, time);
        UIRenderer.panel(g, CX - 390, 35, 780, 620, "BATTLE SETUP");
        int L = CX - 368;

        // ── Mode tabs ────────────────────────────────────────────────────
        int modeY = 72;
        g.setFont(GameConstants.F_HUD);
        g.setColor(GameConstants.C_DIM);
        g.drawString("MODE:", L, modeY + 20);

        boolean vsB = mode == GameMode.VS_BOTS, vs2 = mode == GameMode.TWO_PLAYER, vsO = mode == GameMode.ONLINE;
        int tabX = L + 80;
        UIRenderer.button(g, "VS BOTS",       tabX,       modeY, 155, 34, mx, my, vsB, vsB ? new Color(40,100,15) : new Color(22,45,10));
        UIRenderer.button(g, "2 PLAYER LOCAL", tabX + 165, modeY, 170, 34, mx, my, vs2, vs2 ? new Color(40,100,15) : new Color(22,45,10));
        UIRenderer.button(g, "ONLINE",         tabX + 345, modeY, 130, 34, mx, my, vsO, vsO ? new Color(30,80,100) : new Color(15,40,55));

        // ── Personal / Team mode toggle ──────────────────────────────────
        int toggleY = modeY + 36;
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_DIM);
        g.drawString("PLAY MODE:", L, toggleY + 20);
        int tgX = L + 120;
        UIRenderer.button(g, "PERSONAL", tgX,       toggleY, 140, 28, mx, my, !teamMode,
            !teamMode ? new Color(35,90,15) : new Color(18,40,8));
        UIRenderer.button(g, "TEAM",     tgX + 150, toggleY, 100, 28, mx, my,  teamMode,
             teamMode ? new Color(20,60,110) : new Color(10,30,55));

        // ── Player name ──────────────────────────────────────────────────
        int nameY = toggleY + 44;
        g.setFont(GameConstants.F_HUD);
        g.setColor(GameConstants.C_DIM);
        g.drawString("P1 NAME:", L, nameY + 20);
        int nbx = L + 105, nbw = 200, nbh = 28;
        g.setColor(editingName ? new Color(35,70,18,220) : new Color(15,30,8,200));
        g.fillRoundRect(nbx, nameY, nbw, nbh, 6, 6);
        g.setColor(editingName ? GameConstants.C_ACCENT : new Color(55,95,28,150));
        g.setStroke(new BasicStroke(editingName ? 2f : 1f));
        g.drawRoundRect(nbx, nameY, nbw, nbh, 6, 6);
        g.setFont(GameConstants.F_HUD);
        g.setColor(GameConstants.C_WHITE);
        String dispName = editingName ? nameInput + (((int)(time*2))%2==0?"|":"") : nameInput;
        g.drawString(dispName.isEmpty() ? "(click to edit)" : dispName, nbx + 8, nameY + 20);
        if (!editingName) UIRenderer.button(g, "EDIT", nbx + nbw + 8, nameY, 48, nbh, mx, my, false, new Color(28,58,14));

        if (mode == GameMode.ONLINE) {
            renderOnlinePanel(g, mx, my, L, nameY + 46);
        } else {
            renderLocalPanel(g, mx, my, L, nameY + 46);
        }

        // Buttons positioned relative to panel bottom (not H), so they always show
        int panelBottom = 35 + 620;  // panel y + panel height
        int btnY = panelBottom - 72;
        
        if (mode == GameMode.ONLINE && NetManager.currentLobby != null && NetManager.localPlayerIdx != 0) {
            g.setFont(GameConstants.F_HEAD);
            g.setColor(GameConstants.C_GOLD2);
            g.drawString("Waiting for Host to start...", CX - 130, btnY + 34);
        } else {
            UIRenderer.button(g, "[ START BATTLE ]", CX - 130, btnY, 290, 58, mx, my, false, new Color(38, 105, 14));
        }
        UIRenderer.button(g, "[ BACK ]",          L,        btnY, 145, 58, mx, my, false, new Color(110, 28, 15));
    }

    private void renderLocalPanel(Graphics2D g, int mx, int my, int L, int startY) {
        // Map selection
        g.setFont(GameConstants.F_SUBHEAD);
        g.setColor(GameConstants.C_DIM);
        g.drawString("SELECT MAP", L, startY);
        int mapY = startY + 8;
        for (int i = 0; i < 4; i++) {
            int cx = L + i * 185, cy = mapY, cw = 178, ch = 90;
            boolean sel  = (i == selectedMap);
            boolean over = new Rectangle(cx, cy, cw, ch).contains(mx, my);
            g.setColor(sel || over ? new Color(40,80,18,220) : new Color(15,28,8,190));
            g.fillRoundRect(cx, cy, cw, ch, 10, 10);
            g.setColor(sel ? GameConstants.C_ACCENT : over ? new Color(75,135,32,180) : new Color(48,85,22,120));
            g.setStroke(new BasicStroke(sel ? 2.5f : 1f));
            g.drawRoundRect(cx, cy, cw, ch, 10, 10);
            drawMapThumb(g, i, cx + 7, cy + 7, 60, 52);
            g.setFont(GameConstants.F_HUD);
            g.setColor(sel ? GameConstants.C_GOLD2 : GameConstants.C_WHITE);
            g.drawString(MAP_NAMES[i], cx + 73, cy + 22);
            g.setFont(GameConstants.F_SMALL);
            g.setColor(GameConstants.C_DIM);
            drawWrapped(g, MAP_DESC[i], cx + 73, cy + 36, cw - 80, 13);
        }

        int botY = mapY + 102;
        if (mode == GameMode.VS_BOTS) {
            g.setFont(GameConstants.F_SUBHEAD);
            g.setColor(GameConstants.C_WHITE);
            g.drawString("ENEMIES:", L, botY + 18);
            UIRenderer.button(g, " - ", L + 115, botY, 36, 26, mx, my, false, new Color(100,28,14));
            g.setFont(GameConstants.F_HEAD);
            g.setColor(GameConstants.C_GOLD2);
            g.drawString("" + numBots, L + 162, botY + 22);
            UIRenderer.button(g, " + ", L + 185, botY, 36, 26, mx, my, false, new Color(28,88,14));
            for (int i = 0; i < 5; i++) {
                g.setColor(i < numBots ? GameConstants.C_TEAM_RED : new Color(28,28,28,180));
                g.fillOval(L + 236 + i * 26, botY + 4, 18, 18);
            }

            int diffY = botY + 40;
            g.setFont(GameConstants.F_SUBHEAD);
            g.setColor(GameConstants.C_WHITE);
            g.drawString("DIFFICULTY:", L, diffY + 22);
            for (int i = 0; i < 3; i++) {
                boolean dsel = (i == diffIdx);
                UIRenderer.button(g, DIFF_NAMES[i], L + 148 + i*142, diffY, 132, 36, mx, my, dsel,
                    dsel ? DIFF_COLS[i] : new Color(22,45,12));
            }

            // Team preview (only in team mode)
            if (teamMode && mode==GameMode.VS_BOTS) {
                int blueAllies = numBots / 2;
                int redEnemies = numBots - blueAllies;
                int prevY = diffY + 44;
                g.setColor(new Color(8,18,38,200)); g.fillRoundRect(L, prevY, 740, 38, 6, 6);
                g.setColor(new Color(40,80,160,120)); g.setStroke(new BasicStroke(1f));
                g.drawRoundRect(L, prevY, 740, 38, 6, 6);
                g.setFont(GameConstants.F_SMALL);
                g.setColor(GameConstants.C_TEAM_BLUE);
                g.drawString("BLUE TEAM: You + " + blueAllies + " bot" + (blueAllies!=1?"s":""), L+10, prevY+16);
                g.setColor(GameConstants.C_DIM); g.drawString("  vs  ", L+220, prevY+16);
                g.setColor(GameConstants.C_TEAM_RED);
                g.drawString("RED TEAM: " + redEnemies + " bot" + (redEnemies!=1?"s":""), L+270, prevY+16);
                g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_DIM);
                g.drawString("First team to " + Math.max(1,numBots*3) + " combined kills wins", L+10, prevY+30);
                renderInfoBox(g, L, prevY + 46, 740);
            } else {
                renderInfoBox(g, L, botY + 88, 740);
            }
        } else {
            // 2-player info
            g.setColor(new Color(18,45,12,185));
            g.fillRoundRect(L, botY, 740, 70, 8, 8);
            g.setColor(new Color(75,125,38,115));
            g.setStroke(new BasicStroke(1f));
            g.drawRoundRect(L, botY, 740, 70, 8, 8);
            g.setFont(GameConstants.F_HUD);
            g.setColor(GameConstants.C_GOLD);
            g.drawString("2 PLAYER LOCAL  -  Both play on same keyboard", L + 12, botY + 22);
            g.setFont(GameConstants.F_SMALL);
            g.setColor(GameConstants.C_WHITE);
            g.drawString("P1: Arrow Keys | Up x2=Jump | Num8=Jet | Num5=Melee | Enter=Shoot", L + 12, botY + 42);
            g.drawString("P2: WASD | W x2=Jump | E=Jet | Q=Melee | R=Reload | LMB=Fire", L + 12, botY + 58);
            // Co-op label
            if (teamMode) {
                g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_TEAM_BLUE);
                g.drawString("CO-OP: P1 + P2 = BLUE team  vs  all bots = RED team", L+12, botY+72);
            }
            renderInfoBox(g, L, botY + 82, 740);
        }
    }

    private void renderOnlinePanel(Graphics2D g, int mx, int my, int L, int startY) {
        int panelH = 320;
        g.setColor(new Color(8, 20, 35, 210));
        g.fillRoundRect(L, startY, 740, panelH, 10, 10);
        g.setColor(new Color(30, 80, 120, 150));
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(L, startY, 740, panelH, 10, 10);

        g.setFont(GameConstants.F_HEAD);
        g.setColor(new Color(80, 180, 255));
        g.drawString("ONLINE MULTIPLAYER", L + 20, startY + 32);

        g.setFont(GameConstants.F_BODY);
        g.setColor(GameConstants.C_WHITE);
        g.drawString("Play with friends over the internet.", L + 20, startY + 55);

        // Create lobby
        int cy = startY + 82;
        g.setFont(GameConstants.F_SUBHEAD);
        g.setColor(GameConstants.C_GOLD);
        g.drawString("HOST A GAME", L + 20, cy);
        cy += 4;
        UIRenderer.button(g, "CREATE LOBBY", L + 20, cy, 200, 38, mx, my, false, new Color(30, 90, 140));
        if (!lobbyCode.isEmpty()) {
            g.setFont(GameConstants.F_HUD);
            g.setColor(GameConstants.C_WHITE);
            g.drawString("Your code:", L + 230, cy + 14);
            g.setFont(new Font("Monospaced", Font.BOLD, 22));
            g.setColor(GameConstants.C_GOLD2);
            g.drawString(lobbyCode, L + 325, cy + 26);
            g.setFont(GameConstants.F_SMALL);
            g.setColor(GameConstants.C_DIM);
            g.drawString("Share this code with friends", L + 230, cy + 42);
        }

        // Join lobby
        cy += 60;
        g.setFont(GameConstants.F_SUBHEAD);
        g.setColor(GameConstants.C_GOLD);
        g.drawString("JOIN A GAME", L + 20, cy);
        cy += 4;
        int jbx = L + 20, jby = cy, jbw = 160, jbh = 32;
        g.setColor(editingJoinCode ? new Color(20,50,80,220) : new Color(10,25,45,200));
        g.fillRoundRect(jbx, jby, jbw, jbh, 6, 6);
        g.setColor(editingJoinCode ? new Color(80,180,255) : new Color(40,100,160,150));
        g.setStroke(new BasicStroke(editingJoinCode ? 2f : 1f));
        g.drawRoundRect(jbx, jby, jbw, jbh, 6, 6);
        g.setFont(new Font("Monospaced", Font.BOLD, 16));
        g.setColor(GameConstants.C_WHITE);
        String jDisp = editingJoinCode ? joinCodeInput + (((int)(time*2))%2==0?"|":"") : (joinCodeInput.isEmpty()?"Enter code...":joinCodeInput);
        g.drawString(jDisp, jbx + 10, jby + 22);
        UIRenderer.button(g, "JOIN", jbx + jbw + 10, jby, 80, jbh, mx, my, false, new Color(30,90,140));

        // Team mode options for online
        if (teamMode) {
            cy += 48;
            g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_GOLD);
            g.drawString("TEAM MODE:", L+20, cy+14);
            UIRenderer.button(g, "ALL vs BOTS", L+140, cy, 150, 28, mx, my, onlineTeamSub==0, onlineTeamSub==0?new Color(20,80,140):new Color(10,40,70));
            UIRenderer.button(g, "HUMANS vs HUMANS", L+300, cy, 200, 28, mx, my, onlineTeamSub==1, onlineTeamSub==1?new Color(20,80,140):new Color(10,40,70));
            g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_DIM);
            String desc = onlineTeamSub==0 ? "All lobby players on BLUE, bots on RED (co-op)" : "Lobby splits evenly: half BLUE, half RED (PvP teams, no bots)";
            g.drawString(desc, L+20, cy+38);
        }

        // Status
        if (!onlineStatus.isEmpty()) {
            cy += 50;
            g.setFont(GameConstants.F_HUD);
            g.setColor(onlineStatus.startsWith("Error") ? GameConstants.C_RED : new Color(80,200,80));
            g.drawString(onlineStatus, L + 20, cy);
            
            if (NetManager.currentLobby != null) {
                cy += 20;
                g.setColor(GameConstants.C_GOLD2);
                g.drawString("Players in Lobby: " + NetManager.totalPlayers + " / 10", L + 20, cy);
                cy += 18;
                g.setFont(GameConstants.F_SMALL);
                g.setColor(GameConstants.C_WHITE);
                for (int i=0; i<NetManager.totalPlayers; i++) {
                    String name = NetManager.onlineNames.getOrDefault(i, "Player " + (i+1));
                    g.drawString((i+1) + ". " + name, L + 20, cy);
                    cy += 14;
                }
            }
        }


    }

    private void renderInfoBox(Graphics2D g, int L, int y, int width) {
        g.setColor(new Color(8,18,5,210));
        g.fillRoundRect(L, y, width, 130, 8, 8);
        g.setColor(new Color(68,118,32,110));
        g.setStroke(new BasicStroke(1f));
        g.drawRoundRect(L, y, width, 130, 8, 8);

        int c1x = L + 10, c2x = CX + 8;
        g.setFont(GameConstants.F_HUD);
        g.setColor(GameConstants.C_GOLD);
        g.drawString("OBJECTIVE", c1x, y + 18);
        g.setFont(GameConstants.F_SMALL);
        g.setColor(GameConstants.C_WHITE);
        String[] goals = {
            "First to ENEMIES x 3 kills wins  (1 bot = 3 kills, 2 bots = 6, etc.)",
            "Run out of lives (ENEMIES x 3 deaths) = you lose",
            "Pick up health packs to heal",
            "Collect weapon drops for firepower",
            "Respawn in " + (int)GameConstants.RESPAWN_TIME + "s - keep fighting!"
        };
        int gy = y + 34;
        for (String s : goals) { g.drawString("* " + s, c1x, gy); gy += 18; }

        g.setFont(GameConstants.F_HUD);
        g.setColor(GameConstants.C_GOLD);
        g.drawString("P1 CONTROLS  (Arrow Keys)", c2x, y + 18);
        g.setFont(GameConstants.F_SMALL);
        g.setColor(GameConstants.C_WHITE);
        String[] ctrl = {
            "Left/Right=Move  Up x2=Jump  Down=Crouch  Num8=Jetpack",
            "Num5=Melee  Num0=Reload  Shift=Grenade  End=Pickup",
            "PgDn=Swap Weapon  Arrow keys=Aim gun",
            "Enter=Shoot  |  Mouse LMB=Fire (mouse aim)",
        };
        int cy2 = y + 34;
        for (String s : ctrl) { g.drawString(s, c2x, cy2); cy2 += 22; }
    }

    private void drawMapThumb(Graphics2D g, int idx, int x, int y, int w, int h) {
        switch (idx) {
            case 0 -> { // Warzone — grass + water
                g.setColor(new Color(20,45,75)); g.fillRect(x,y,w,h);
                g.setColor(new Color(55,88,38)); g.fillRect(x,y+h*2/3,w,h/3);
                g.setColor(new Color(80,125,52));
                g.fillRect(x+w/3,y+h/2-4,w/3,5);
                g.fillRect(x+4,y+h/3,w/4,5);
                g.fillRect(x+w-w/4-4,y+h/3,w/4,5);
                g.setColor(new Color(30,85,160,120));
                g.fillRect(x+4,y+h-h/5,w-8,h/5-2);
            }
            case 1 -> { // Jungle — dark stone + ladders
                g.setColor(new Color(15,35,18)); g.fillRect(x,y,w,h);
                g.setColor(new Color(45,72,35)); g.fillRect(x,y+h*2/3,w,h/3);
                g.setColor(new Color(62,95,45));
                for (int i=0;i<4;i++) g.fillRect(x+5+i*w/4,y+h-h/4-i*8,5,h/4+i*8);
                g.setColor(new Color(140,105,45));
                g.fillRect(x+w/4-1,y+4,3,h-8); g.fillRect(x+3*w/4-1,y+4,3,h-8);
            }
            case 2 -> { // Steel Fortress — metal grey
                g.setColor(new Color(22,28,38)); g.fillRect(x,y,w,h);
                g.setColor(new Color(68,78,88)); g.fillRect(x,y+h*2/3,w,h/3);
                g.setColor(new Color(88,98,108));
                g.fillRect(x+4,y+5,w/3,h*2/3);
                g.fillRect(x+w-w/3-4,y+5,w/3,h*2/3);
                g.fillRect(x+w/3,y+h/2-3,w/3,5);
                g.setColor(new Color(108,122,135));
                g.fillRect(x+4,y+5,w/3,5);
                g.fillRect(x+w-w/3-4,y+5,w/3,5);
            }
            case 3 -> { // City Ruins — orange glow, buildings
                g.setColor(new Color(20,12,8)); g.fillRect(x,y,w,h);
                // City skyline
                g.setColor(new Color(35,22,12));
                for (int i=0;i<6;i++) {
                    int bw=6+i*2, bh=15+i*5;
                    g.fillRect(x+2+i*(w/6), y+h-h/3-bh, bw, bh);
                }
                // Ground
                g.setColor(new Color(55,50,45)); g.fillRect(x,y+h*2/3,w,h/3);
                // Orange glow
                g.setColor(new Color(200,80,10,60)); g.fillRect(x,y+h/2,w,h/3);
                // Windows
                g.setColor(new Color(255,200,80,180));
                for (int i=0;i<5;i++) g.fillRect(x+4+i*w/5,y+h/2-5,3,4);
            }
        }
        g.setColor(new Color(80,130,40,85)); g.setStroke(new BasicStroke(0.8f));
        g.drawRect(x,y,w,h);
    }

    private void drawWrapped(Graphics2D g, String text, int x, int y, int maxW, int lh) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int cy = y;
        for (String word : words) {
            String test = line.length()==0 ? word : line+" "+word;
            if (fm.stringWidth(test) > maxW && line.length() > 0) {
                g.drawString(line.toString(), x, cy); cy += lh; line = new StringBuilder(word);
            } else line = new StringBuilder(test);
        }
        if (line.length() > 0) g.drawString(line.toString(), x, cy);
    }

    public boolean handleKey(java.awt.event.KeyEvent e) {
        if (editingName) {
            int code = e.getKeyCode();
            if (code==java.awt.event.KeyEvent.VK_ENTER||code==java.awt.event.KeyEvent.VK_ESCAPE) { editingName=false; return true; }
            if (code==java.awt.event.KeyEvent.VK_BACK_SPACE) { if(nameInput.length()>0) nameInput=nameInput.substring(0,nameInput.length()-1); return true; }
            char c=e.getKeyChar();
            if (c!=java.awt.event.KeyEvent.CHAR_UNDEFINED&&nameInput.length()<16&&!Character.isISOControl(c)) nameInput+=c;
            return true;
        }
        if (editingJoinCode) {
            int code = e.getKeyCode();
            if (code==java.awt.event.KeyEvent.VK_ENTER) { editingJoinCode=false; return true; }
            if (code==java.awt.event.KeyEvent.VK_ESCAPE) { editingJoinCode=false; joinCodeInput=""; return true; }
            if (code==java.awt.event.KeyEvent.VK_BACK_SPACE) { if(joinCodeInput.length()>0) joinCodeInput=joinCodeInput.substring(0,joinCodeInput.length()-1); return true; }
            char c=e.getKeyChar();
            if (c!=java.awt.event.KeyEvent.CHAR_UNDEFINED&&joinCodeInput.length()<6&&Character.isLetterOrDigit(c)) joinCodeInput+=(""+c).toUpperCase();
            return true;
        }
        return false;
    }

    public Action handleClick(int mx, int my) {
        int L = CX - 368;
        int modeY = 72, tabX = L + 80;
        // Mode tabs
        if (new Rectangle(tabX,       modeY, 155, 34).contains(mx,my)) { mode=GameMode.VS_BOTS;    return Action.NONE; }
        if (new Rectangle(tabX + 165, modeY, 170, 34).contains(mx,my)) { mode=GameMode.TWO_PLAYER; return Action.NONE; }
        if (new Rectangle(tabX + 345, modeY, 130, 34).contains(mx,my)) { mode=GameMode.ONLINE;     return Action.NONE; }

        // Personal / Team toggle
        int toggleY2 = modeY + 42;
        int tgX2 = L + 120;
        if (new Rectangle(tgX2,       toggleY2, 140, 28).contains(mx,my)) { teamMode=false; return Action.NONE; }
        if (new Rectangle(tgX2 + 150, toggleY2, 100, 28).contains(mx,my)) { teamMode=true;  return Action.NONE; }

        // Online team sub-mode buttons
        if (mode==GameMode.ONLINE && teamMode) {
            // cy starts at startY + 82 + 4 + 60 = startY + 146
            // and then cy += 48 for the team mode buttons
            int startY2 = nameY + 42; // handleClick uses +42 for startY
            int cy2 = startY2 + 146 + 48; 
            if (new Rectangle(L+140, cy2, 150, 28).contains(mx,my)) { onlineTeamSub=0; return Action.NONE; }
            if (new Rectangle(L+300, cy2, 200, 28).contains(mx,my)) { onlineTeamSub=1; return Action.NONE; }
        }

        // Name edit
        int toggleY = modeY + 42;
        int nameY = toggleY + 36;
        int nbx = L + 105;
        if (new Rectangle(nbx,nameY,200,28).contains(mx,my)||new Rectangle(nbx+208,nameY,48,28).contains(mx,my)) { editingName=true; return Action.NONE; }

        int startY = nameY + 42;
        if (mode == GameMode.ONLINE) {
            // Create lobby
            if (new Rectangle(L+20, startY+86, 200, 38).contains(mx,my)) {
                onlineStatus = "Connecting to server...";
                lobbyCode = generateLobbyCode();
                String hostName = nameInput.isEmpty() ? "Host" : nameInput;
                new Thread(() -> {
                    if (NetManager.connectAndCreate(lobbyCode, hostName)) {
                        onlineStatus = "Lobby created! Share code: " + lobbyCode;
                    } else {
                        onlineStatus = "Error: Failed to connect to server.";
                        lobbyCode = "";
                    }
                }).start();
                return Action.NONE;
            }
            // Join code field
            if (new Rectangle(L+20, startY+150, 160, 32).contains(mx,my)) { editingJoinCode=true; return Action.NONE; }
            // Join button
            if (new Rectangle(L+190, startY+150, 80, 32).contains(mx,my)) {
                if (joinCodeInput.length()>=4) {
                    onlineStatus="Connecting to lobby "+joinCodeInput+"...";
                    String guestName = nameInput.isEmpty() ? "Guest" : nameInput;
                    new Thread(() -> {
                        NetManager.connectAndJoin(joinCodeInput, guestName);
                        if ("JOINED".equals(NetManager.lastResponse)) {
                            lobbyCode = joinCodeInput;
                            onlineStatus = "Joined Lobby " + lobbyCode + ". Waiting for Host...";
                        } else {
                            onlineStatus = "Error: Lobby not found or server offline.";
                        }
                    }).start();
                } else onlineStatus="Error: Enter a valid 4-6 character code";
                return Action.NONE;
            }
            // Start button (Host only)
            if (new Rectangle(CX-130, 583, 290, 58).contains(mx,my)) {
                if (NetManager.currentLobby != null) {
                    if (NetManager.localPlayerIdx == 0) { // Host
                        NetManager.startMatchBroadcast();
                        return Action.START;
                    } else {
                        return Action.NONE; // Guest just falls through and ignores click
                    }
                }
            }
        } else {
            // Map cards
            int mapY = startY + 8;
            for (int i=0;i<4;i++) if (new Rectangle(L+i*185,mapY,178,90).contains(mx,my)) { selectedMap=i; return Action.NONE; }
            // Bots
            int botY = mapY + 102;
            if (mode==GameMode.VS_BOTS) {
                if (new Rectangle(L+115,botY,36,26).contains(mx,my)&&numBots>0) { numBots--; return Action.NONE; }
                if (new Rectangle(L+185,botY,36,26).contains(mx,my)&&numBots<5) { numBots++; return Action.NONE; }
                int diffY = botY+40;
                for (int i=0;i<3;i++) if (new Rectangle(L+148+i*142,diffY,132,36).contains(mx,my)) diffIdx=i;
            }
        }

        if (new Rectangle(CX-130, 583, 290, 58).contains(mx,my)) return Action.START;
        if (new Rectangle(L,      583, 145, 58).contains(mx,my)) return Action.BACK;
        return Action.NONE;
    }

    private String generateLobbyCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        for (int i=0;i<4;i++) sb.append(chars.charAt((int)(Math.random()*chars.length())));
        return sb.toString();
    }

    public MapId     getSelectedMap()  { return MapId.values()[selectedMap]; }
    public int       getNumBots()      { return mode==GameMode.TWO_PLAYER||mode==GameMode.ONLINE ? 0 : numBots; }
    public Difficulty getDifficulty()  { return Difficulty.values()[diffIdx]; }
    public GameMode  getMode()        { return mode; }
    public boolean   isTwoPlayer()     { return mode==GameMode.TWO_PLAYER; }
    public boolean   isOnlineMode()    { return mode==GameMode.ONLINE; }
    public boolean   isTeamMode()      { return teamMode; }
    public int       getOnlineTeamSub(){ return onlineTeamSub; }
    public String    getNameInput()    { return nameInput; }
    public boolean   isEditingName()   { return editingName||editingJoinCode; }
    public void      setNameDefault(String n) { if (nameInput.isEmpty()) nameInput=n; }
    public void      setOnlineStatus(String status) { this.onlineStatus = status; }
}

// ============================================================================
//  PROFILE SCREEN
// ============================================================================
class ProfileScreen {
    public enum Action { NONE, BACK }
    private float time=0f;
    private final int W=UIRenderer.W, H=UIRenderer.H, CX=W/2;
    public void update(float dt) { time+=dt; }

    public void render(Graphics2D g, int mx, int my, PlayerProfile p) {
        UIRenderer.drawMenuBG(g, time);
        UIRenderer.panel(g, CX-380, 50, 760, 580, "PROFILE");
        int lx=CX-350, ry=80;

        // Avatar box
        g.setColor(new Color(15,30,8,200)); g.fillRoundRect(lx,ry,180,240,10,10);
        g.setColor(new Color(60,100,30,150)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(lx,ry,180,240,10,10);
        drawChar(g, lx+90, ry+160, p.getEquippedSkin(), p.getEquippedTrail());
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_GOLD2);
        UIRenderer.centerText(g, p.getEquippedSkin().name(), lx+90, ry+228);

        int rx=lx+200;
        g.setFont(GameConstants.F_HEAD); g.setColor(GameConstants.C_GOLD2); g.drawString(p.getPlayerName(), rx, ry+28);
        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_ACCENT); g.drawString(p.getRankTitle()+"  LV "+p.getLevel(), rx, ry+52);
        g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_DIM); g.drawString("XP "+p.xpForCurrentLevel()+" / "+p.xpNeededForNextLevel(), rx, ry+72);
        UIRenderer.drawHBar(g, rx, ry+78, 340, 14, p.levelProgress(), GameConstants.C_ACCENT);
        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_GOLD); g.drawString(p.getCoins()+" COINS", rx, ry+108);

        int sy=ry+130, sw=160;
        UIRenderer.drawStatRow(g,rx,sy,sw,"Total Kills",""+p.getTotalKills(),GameConstants.C_GREEN);
        UIRenderer.drawStatRow(g,rx,sy+24,sw,"Deaths",""+p.getTotalDeaths(),GameConstants.C_RED);
        UIRenderer.drawStatRow(g,rx,sy+48,sw,"K/D",String.format("%.2f",p.getKD()),GameConstants.C_GOLD2);
        UIRenderer.drawStatRow(g,rx,sy+72,sw,"Matches",""+p.getTotalMatches(),GameConstants.C_WHITE);
        UIRenderer.drawStatRow(g,rx,sy+96,sw,"Wins",""+p.getTotalWins(),GameConstants.C_ACCENT);

        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_DIM); g.drawString("UNLOCK PROGRESS", lx, ry+280);
        drawUnlocks(g, p, lx, ry+300);
        UIRenderer.button(g, "[ BACK ]", CX-70, H-100, 140, 44, mx, my, false, new Color(110,30,20));
    }

    private void drawChar(Graphics2D g, int cx, int cy, SkinId skin, SkinId trail) {
        Color body=new Color(60,100,40), sk=new Color(200,155,100);
        Color eq=switch(skin){case WARRIOR->new Color(55,80,40);case COMMANDO->new Color(40,60,90);case RENEGADE->new Color(80,50,30);default->new Color(200,200,210);};
        g.setColor(sk); g.fillOval(cx-12,cy-70,24,22);
        g.setColor(eq); g.fillRoundRect(cx-13,cy-74,26,18,4,4);
        g.setColor(body); g.fillRoundRect(cx-10,cy-48,20,30,4,4);
        g.setColor(sk); g.fillRoundRect(cx-18,cy-46,8,22,4,4); g.fillRoundRect(cx+10,cy-46,8,22,4,4);
        g.setColor(eq); g.fillRoundRect(cx-10,cy-18,8,26,4,4); g.fillRoundRect(cx+2,cy-18,8,26,4,4);
        Color jc=switch(trail){case JET_FIRE->new Color(180,80,20);case JET_ICE->new Color(80,160,220);case JET_RAINBOW->new Color(180,80,180);default->new Color(80,90,70);};
        g.setColor(jc); g.fillRoundRect(cx-20,cy-46,8,24,3,3);
        g.setColor(new Color(55,58,60)); g.fillRect(cx+18,cy-42,22,6);
    }

    private void drawUnlocks(Graphics2D g, PlayerProfile p, int x, int y) {
        for (PlayerProfile.UnlockEntry e : PlayerProfile.UNLOCK_TABLE) {
            boolean locked=e.level()>p.getLevel();
            g.setColor(locked?new Color(28,28,28,180):new Color(38,78,18,180));
            g.fillRoundRect(x,y,40,36,5,5);
            g.setColor(locked?new Color(55,55,55,150):GameConstants.C_ACCENT);
            g.setStroke(new BasicStroke(1f)); g.drawRoundRect(x,y,40,36,5,5);
            g.setFont(new Font("SansSerif",Font.BOLD,8)); g.setColor(locked?new Color(55,55,55):GameConstants.C_WHITE);
            UIRenderer.centerText(g,"Lv"+e.level(),x+20,y+14);
            g.setFont(new Font("SansSerif",Font.PLAIN,7)); g.setColor(locked?new Color(48,48,48):GameConstants.C_DIM);
            String lbl=e.label().length()>7?e.label().substring(0,6):e.label();
            UIRenderer.centerText(g,lbl,x+20,y+28);
            x+=42;
        }
    }

    public Action handleClick(int mx, int my) {
        if (new Rectangle(CX-70,H-100,140,44).contains(mx,my)) return Action.BACK;
        return Action.NONE;
    }
}

// ============================================================================
//  STORE SCREEN
// ============================================================================
class StoreScreen {
    public enum Action { NONE, BACK, BOUGHT }
    private int tab=0; private float time=0f;
    private final int W=UIRenderer.W, H=UIRenderer.H, CX=W/2;
    private static final String[] TABS={"CHARACTERS","WEAPON SKINS","DEATH FX","JET TRAILS","ARSENAL"};

    public void update(float dt) { time+=dt; }

    public void render(Graphics2D g, int mx, int my, PlayerProfile profile) {
        UIRenderer.drawMenuBG(g, time);
        UIRenderer.panel(g, CX-420, 45, 840, 590, "STORE");

        g.setFont(GameConstants.F_HEAD); g.setColor(GameConstants.C_GOLD2);
        g.drawString(profile.getCoins()+" coins", CX-400, 88);
        g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_DIM);
        g.drawString("Earn coins by playing matches!", CX-400, 102);

        int tx=CX-405, ty=110;
        for (int i=0;i<TABS.length;i++) {
            boolean sel=(i==tab), over=new Rectangle(tx+i*162,ty,158,30).contains(mx,my);
            g.setColor(sel?new Color(40,80,15):over?new Color(25,50,10):new Color(15,30,6));
            g.fillRoundRect(tx+i*162,ty,158,30,6,6);
            g.setColor(sel?GameConstants.C_ACCENT:new Color(60,100,30,150));
            g.setStroke(new BasicStroke(sel?2f:1f)); g.drawRoundRect(tx+i*162,ty,158,30,6,6);
            g.setFont(new Font("SansSerif", Font.BOLD, 10)); g.setColor(sel?GameConstants.C_GOLD2:GameConstants.C_DIM);
            UIRenderer.centerText(g,TABS[i],tx+i*162+79,ty+20);
        }

        java.util.List<PlayerProfile.StoreItem> items=getTabItems(tab);
        int cols=4,iw=190,ih=130,sx=CX-400,sy=150,gx=10,gy=10;
        for (int i=0;i<items.size();i++) {
            PlayerProfile.StoreItem item=items.get(i);
            int ix=sx+(i%cols)*(iw+gx), iy=sy+(i/cols)*(ih+gy);
            boolean over=new Rectangle(ix,iy,iw,ih).contains(mx,my);
            boolean owned=profile.isUnlocked(item.skin(),item.cat());
            drawItem(g,item,ix,iy,iw,ih,over,owned,profile.getCoins()>=item.cost());
        }
        UIRenderer.button(g,"[ BACK ]",CX-70,H-78,140,44,mx,my,false,new Color(110,30,20));
    }

    private void drawItem(Graphics2D g, PlayerProfile.StoreItem item, int x, int y, int w, int h, boolean hov, boolean owned, boolean can) {
        g.setColor(owned?new Color(20,50,12,200):hov?new Color(25,45,12,200):new Color(12,22,6,200));
        g.fillRoundRect(x,y,w,h,8,8);
        g.setColor(owned?GameConstants.C_ACCENT:hov?new Color(80,140,30):new Color(40,70,20,120));
        g.setStroke(new BasicStroke(owned?2f:1f)); g.drawRoundRect(x,y,w,h,8,8);
        g.setColor(new Color(0,0,0,100)); g.fillRoundRect(x+6,y+6,w-12,70,6,6);
        drawIcon(g,item,x+w/2,y+42);
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_WHITE); UIRenderer.centerText(g,item.label(),x+w/2,y+86);
        if (owned) { g.setColor(GameConstants.C_GREEN); UIRenderer.centerText(g,"OWNED",x+w/2,y+106); }
        else { g.setColor(can?GameConstants.C_GOLD2:GameConstants.C_DIM); UIRenderer.centerText(g,item.cost()+" coins",x+w/2,y+106); }
        if (hov&&!owned) { g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_WHITE); UIRenderer.centerText(g,"Click to Buy",x+w/2,y+h-4); }
    }

    private void drawIcon(Graphics2D g, PlayerProfile.StoreItem item, int cx, int cy) {
        Color c=switch(item.skin()){
            case WARRIOR->new Color(60,100,40);case COMMANDO->new Color(40,70,110);case RENEGADE->new Color(100,60,30);case GHOST->new Color(200,210,220);
            case DESERT_CAMO->new Color(160,130,70);case ARCTIC->new Color(190,220,235);case URBAN_CAMO->new Color(90,100,85);case CHROME->new Color(185,195,205);case GOLD_PLATED->new Color(210,175,40);
            case JET_FIRE->new Color(255,100,20);case JET_ICE->new Color(80,180,255);case JET_RAINBOW->new Color(200,80,200);
            case DEATH_EXPLODE->new Color(255,140,20);case DEATH_STAR->new Color(80,160,255);case DEATH_COINS->new Color(220,190,40);
            default->new Color(100,140,60);
        };
        g.setColor(c);
        switch(item.cat()){
            case CHARACTER->{ g.fillOval(cx-12,cy-20,24,22); g.setColor(c.darker()); g.fillRoundRect(cx-8,cy+2,16,20,4,4); }
            case WEAPON_SKIN->{ g.fillRect(cx-18,cy-4,36,8); g.fillRect(cx-8,cy+4,8,10); }
            case DEATH_EFFECT->{ for(int i=0;i<6;i++){ double a=i*Math.PI/3; g.fillOval((int)(cx+12*Math.cos(a))-5,(int)(cy+12*Math.sin(a))-5,10,10); } }
            case JET_TRAIL->{ g.setStroke(new BasicStroke(4f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); for(int i=0;i<3;i++) g.drawLine(cx,cy-16+i*8,cx+14,cy+16-i*8); }
            default->g.fillOval(cx-14,cy-14,28,28);
        }
    }

    private java.util.List<PlayerProfile.StoreItem> getTabItems(int t) {
        return PlayerProfile.STORE.stream().filter(i->i.cat()==switch(t){
            case 0->UnlockCategory.CHARACTER;case 1->UnlockCategory.WEAPON_SKIN;case 2->UnlockCategory.DEATH_EFFECT;case 3->UnlockCategory.JET_TRAIL;
            case 4->UnlockCategory.DUAL_WIELD; default->UnlockCategory.CONSUMABLE;
        }).toList();
    }

    private java.util.List<PlayerProfile.StoreItem> getTabItemsRaw(int t) {
        return PlayerProfile.STORE.stream().filter(i->i.cat()==switch(t){
            case 0->UnlockCategory.CHARACTER;case 1->UnlockCategory.WEAPON_SKIN;case 2->UnlockCategory.DEATH_EFFECT;case 3->UnlockCategory.JET_TRAIL;
            default->UnlockCategory.CONSUMABLE;
        }).toList();
    }

    public Action handleClick(int mx, int my, PlayerProfile profile) {
        int tx=CX-405,ty=110;
        for (int i=0;i<TABS.length;i++) if(new Rectangle(tx+i*162,ty,158,30).contains(mx,my)){tab=i;return Action.NONE;}
        java.util.List<PlayerProfile.StoreItem> items=getTabItems(tab);
        int cols=4,iw=190,ih=130,sx=CX-400,sy=150,gx=10,gy=10;
        for (int i=0;i<items.size();i++){
            int ix=sx+(i%cols)*(iw+gx),iy=sy+(i/cols)*(ih+gy);
            if(new Rectangle(ix,iy,iw,ih).contains(mx,my)){profile.buy(items.get(i));return Action.BOUGHT;}
        }
        if(new Rectangle(CX-70,H-78,140,44).contains(mx,my)) return Action.BACK;
        return Action.NONE;
    }
}

// ============================================================================
//  SETTINGS SCREEN
// ============================================================================
class SettingsScreen {
    public enum Action { NONE, BACK }
    private float time=0f;
    private final int W=UIRenderer.W, H=UIRenderer.H, CX=W/2;
    public void update(float dt) { time+=dt; }

    public void render(Graphics2D g, int mx, int my, AudioEngine audio) {
        float bgmVol=audio.getBgmVolume(), sfxVol=audio.getSfxVolume();
        UIRenderer.drawMenuBG(g, time);
        UIRenderer.panel(g, CX-310, 65, 620, 540, "SETTINGS");
        int lx=CX-280, sx=lx+160, sw=360, ry=115;

        drawSlider(g, lx, ry,     sw, "BGM Volume", bgmVol, sx);
        drawSlider(g, lx, ry+52,  sw, "SFX Volume", sfxVol, sx);

        int bY=ry+114;
        g.setColor(new Color(10,20,6,185)); g.fillRoundRect(lx,bY,580,304,8,8);
        g.setColor(new Color(58,98,28,110)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(lx,bY,580,304,8,8);

        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_GOLD); g.drawString("CONTROLS", lx+10, bY+22);
        g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_WHITE);
        Object[][] rows = {
            {true,"PLAYER 1  (Arrow Keys + Mouse)"},
            {false,"Left/Right","Move  |  Up x2 = Double Jump"},
            {false,"Down","Crouch  |  Arrow keys also AIM gun"},
            {false,"Num8 / Shift","Jetpack / Grenade"},
            {false,"Num5 / Num0","Melee / Reload"},
            {false,"End / PgDn","Pickup / Swap Weapon"},
            {false,"Enter / LMB","Shoot (Key / Mouse Aim)"},
            {false,"ESC","Pause"},
            {true,""},
            {true,"PLAYER 2  (WASD + Mouse)"},
            {false,"A / D","Move  |  W x2 = Double Jump"},
            {false,"S / E","Crouch / Jetpack  |  Q = Melee"},
            {false,"Ctrl / R","Grenade / Reload"},
            {false,"F / G","Pickup / Swap Weapon"},
            {false,"Tab / Mouse","Shoot / Fire (Mouse Aim)"},
        };
        int ly=bY+40;
        for (Object[] row : rows) {
            if ((boolean)row[0]) {
                g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_ACCENT);
                if (!((String)row[1]).isEmpty()) g.drawString((String)row[1], lx+10, ly);
            } else {
                g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_WHITE);
                g.drawString((String)row[1], lx+18, ly);
                g.setColor(GameConstants.C_DIM);
                g.drawString((String)row[2], lx+175, ly);
            }
            ly+=16;
        }
        UIRenderer.button(g,"[ BACK ]",CX-70,H-95,140,44,mx,my,false,new Color(110,30,20));
    }

    private void drawSlider(Graphics2D g, int x, int y, int sw, String label, float val, int sx) {
        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_WHITE); g.drawString(label, x, y+20);
        g.setColor(new Color(15,30,8,200)); g.fillRoundRect(sx,y+8,sw,16,8,8);
        g.setColor(new Color(40,80,20,200)); g.fillRoundRect(sx,y+8,(int)(sw*val),16,8,8);
        g.setColor(new Color(80,130,40,150)); g.setStroke(new BasicStroke(1f)); g.drawRoundRect(sx,y+8,sw,16,8,8);
        int kx=sx+(int)(sw*val)-8;
        g.setColor(GameConstants.C_ACCENT); g.fillOval(kx,y+4,18,22);
        g.setColor(GameConstants.C_GOLD2); g.setStroke(new BasicStroke(1.5f)); g.drawOval(kx,y+4,18,22);
        g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_DIM);
        g.drawString((int)(val*100)+"%", sx+sw+8, y+22);
    }

    public Action handleClick(int mx, int my) {
        if (new Rectangle(CX-70,H-95,140,44).contains(mx,my)) return Action.BACK;
        return Action.NONE;
    }

    public void handleDrag(int mx, int my, AudioEngine audio) {
        int lx=CX-280, ry=115, sx=lx+160, sw=360;
        if (my>=ry+8&&my<=ry+24&&mx>=sx&&mx<=sx+sw) audio.setBgmVolume(Math.max(0,Math.min(1,(float)(mx-sx)/sw)));
        if (my>=ry+60&&my<=ry+76&&mx>=sx&&mx<=sx+sw) audio.setSfxVolume(Math.max(0,Math.min(1,(float)(mx-sx)/sw)));
    }
}

// ============================================================================
//  PAUSE SCREEN
// ============================================================================
class PauseScreen {
    public enum Action { NONE, RESUME, MAIN_MENU }
    private final int W=UIRenderer.W, H=UIRenderer.H, CX=W/2;

    public void render(Graphics2D g, int mx, int my) {
        g.setColor(new Color(0,0,0,158)); g.fillRect(0,0,W,H);
        UIRenderer.panel(g, CX-175, H/2-145, 350, 290, "PAUSED");
        UIRenderer.button(g,"[ RESUME ]",   CX-135, H/2-70, 270, 52, mx, my, false, new Color(35,100,15));
        UIRenderer.button(g,"[ MAIN MENU ]",CX-135, H/2+0,  270, 52, mx, my, false, new Color(100,25,15));
        g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_DIM);
        UIRenderer.centerText(g,"ESC = Resume",CX,H/2+92);
    }

    public Action handleClick(int mx, int my) {
        if (new Rectangle(CX-135,H/2-70,270,52).contains(mx,my)) return Action.RESUME;
        if (new Rectangle(CX-135,H/2+0,270,52).contains(mx,my))  return Action.MAIN_MENU;
        return Action.NONE;
    }
}

// ============================================================================
//  POST MATCH SCREEN
// ============================================================================
class PostMatchScreen {
    public enum Action { NONE, PLAY_AGAIN, MAIN_MENU }
    private final int W=UIRenderer.W, H=UIRenderer.H, CX=W/2;
    private java.util.List<String> unlocks;
    private float time=0f;
    private boolean teamMode=false;
    public void setUnlocks(java.util.List<String> u) { unlocks=u; }
    public void setTeamMode(boolean tm) { teamMode=tm; }
    public void update(float dt) { time+=dt; }

    public void render(Graphics2D g, int mx, int my, java.util.List<Player> players,
                       int xpG, int coinsG, PlayerProfile profile) {
        UIRenderer.drawMenuBG(g, time);
        UIRenderer.panel(g, CX-400, 40, 800, 580, "MATCH RESULTS");

        int tx=CX-360, ty=90;
        int nextY;

        if (teamMode) {
            nextY = renderTeamResults(g, players, tx, ty);
        } else {
            nextY = renderPersonalResults(g, players, tx, ty);
        }

        int by2 = Math.max(nextY + 14, 380); // Ensure rewards don't push too far or overlap
        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_ACCENT2);
        g.drawString("+"+xpG+" XP",tx,by2+20); g.setColor(GameConstants.C_GOLD2);
        g.drawString("+"+coinsG+" coins",tx+130,by2+20);
        if (profile != null) {
            UIRenderer.drawHBar(g,tx+260,by2+8,200,14,profile.levelProgress(),GameConstants.C_ACCENT);
            g.setFont(GameConstants.F_SMALL); g.setColor(GameConstants.C_DIM); g.drawString("Lv "+profile.getLevel(),tx+465,by2+20);
        }

        if (unlocks!=null&&!unlocks.isEmpty()) {
            int uy=by2+40;
            g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_GOLD2);
            g.drawString("UNLOCKED:",tx,uy); uy+=20;
            for (String u:unlocks){ g.setFont(GameConstants.F_BODY); g.setColor(GameConstants.C_ACCENT); g.drawString("  > "+u,tx,uy); uy+=18; }
        }

        UIRenderer.button(g,"[ PLAY AGAIN ]",CX-280,H-88,240,52,mx,my,false,new Color(35,100,15));
        UIRenderer.button(g,"[ MAIN MENU ]", CX+40, H-88,240,52,mx,my,false,new Color(100,25,15));
    }

    private int renderPersonalResults(Graphics2D g, java.util.List<Player> players, int tx, int ty) {
        g.drawString("PLAYER",tx+16,ty); 
        g.drawString("KILLS",tx+330,ty);
        g.drawString("DEATHS",tx+425,ty); 
        g.drawString("K/D",tx+540,ty);
        g.setColor(new Color(60,100,30,120)); g.setStroke(new BasicStroke(1f));
        g.drawLine(tx,ty+5,tx+680,ty+5);
        int[] iy={ty+12};
        players.stream().sorted((a,b)->b.getKills()-a.getKills()).forEach(p->{
            drawPlayerRow(g, p, tx, iy[0]);
            iy[0]+=26;
        });
        return iy[0];
    }

    private int renderTeamResults(Graphics2D g, java.util.List<Player> players, int tx, int ty) {
        // Compute team kills
        int blueKills=0, redKills=0;
        for (Player p : players) {
            if (p.getTeam()==com.samarbhumi.core.Enums.Team.BLUE) blueKills+=p.getKills();
            else redKills+=p.getKills();
        }
        boolean blueWins = blueKills >= redKills;

        int iy = ty;

        // Team score banner
        g.setFont(GameConstants.F_SUBHEAD);
        FontMetrics bfm = g.getFontMetrics();
        // Vajra part
        int bannerX = tx;
        g.setColor(blueWins ? GameConstants.C_GOLD2 : GameConstants.C_DIM);
        g.drawString("VAJRA", bannerX, iy+16);
        bannerX += bfm.stringWidth("VAJRA") + 8;
        g.setColor(GameConstants.C_WHITE);
        g.drawString("" + blueKills, bannerX, iy+16);
        bannerX += bfm.stringWidth("" + blueKills) + 8;
        g.setColor(GameConstants.C_DIM);
        g.drawString("–", bannerX, iy+16);
        bannerX += bfm.stringWidth("–") + 8;
        g.setColor(GameConstants.C_WHITE);
        g.drawString("" + redKills, bannerX, iy+16);
        bannerX += bfm.stringWidth("" + redKills) + 8;
        g.setColor(!blueWins ? GameConstants.C_GOLD2 : GameConstants.C_DIM);
        g.drawString("PRALAY", bannerX, iy+16);
        iy += 26;

        g.setColor(new Color(60,100,30,100)); g.setStroke(new BasicStroke(1f));
        g.drawLine(tx, iy, tx+680, iy);
        iy += 10;

        // Column headers
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_GOLD);
        g.drawString("PLAYER",tx+16,iy); 
        g.drawString("KILLS",tx+330,iy);
        g.drawString("DEATHS",tx+425,iy); 
        g.drawString("K/D",tx+540,iy);
        iy += 12;

        // VAJRA (Blue) section
        iy = drawTeamSection(g, players, com.samarbhumi.core.Enums.Team.BLUE,
                             "⚡ VAJRA", GameConstants.C_TEAM_BLUE, tx, iy, blueWins);
        iy += 6;
        // PRALAY (Red) section
        iy = drawTeamSection(g, players, com.samarbhumi.core.Enums.Team.RED,
                             "☄ PRALAY", GameConstants.C_TEAM_RED, tx, iy, !blueWins);
        return iy;
    }

    private int drawTeamSection(Graphics2D g, java.util.List<Player> players,
                                 com.samarbhumi.core.Enums.Team team, String teamLabel,
                                 Color teamColor, int tx, int iy, boolean won) {
        // Team header row
        g.setColor(new Color(teamColor.getRed(), teamColor.getGreen(), teamColor.getBlue(), 60));
        g.fillRoundRect(tx-4, iy, 690, 20, 4, 4);
        g.setFont(GameConstants.F_HUD);
        g.setColor(teamColor);
        g.drawString(teamLabel + (won ? "  ★ WINNER" : ""), tx+8, iy+15);
        iy += 36;

        // Players on this team, sorted by kills
        players.stream()
            .filter(p -> p.getTeam()==team)
            .sorted((a,b) -> b.getKills()-a.getKills())
            .forEach(p -> { /* handled below with external iy */ });

        // Need mutable iy inside lambda — use array
        int[] row = {iy};
        players.stream()
            .filter(p -> p.getTeam()==team)
            .sorted((a,b) -> b.getKills()-a.getKills())
            .forEach(p -> { drawPlayerRow(g, p, tx, row[0]); row[0]+=26; });
        return row[0];
    }

    private void drawPlayerRow(Graphics2D g, Player p, int tx, int rowY) {
        boolean isH=p.isHuman();
        g.setColor(isH?new Color(50,100,20,80):new Color(20,20,20,60));
        g.fillRoundRect(tx-4,rowY-14,690,24,5,5);
        g.setFont(GameConstants.F_BODY); g.setColor(isH?GameConstants.C_GOLD2:GameConstants.C_WHITE);
        g.drawString(p.getName()+(isH?" (You)":""),tx+16,rowY+3);
        
        Color tc=p.getTeam()==com.samarbhumi.core.Enums.Team.BLUE?GameConstants.C_TEAM_BLUE:GameConstants.C_TEAM_RED;
        g.setColor(tc); g.fillOval(tx+285,rowY-9,12,12);
        
        g.setColor(GameConstants.C_WHITE);
        FontMetrics fm = g.getFontMetrics();
        // Stats right-aligned to their labels
        String kStr = ""+p.getKills();
        g.drawString(kStr, tx+330 + 35 - fm.stringWidth(kStr), rowY+3);
        String dStr = ""+p.getDeaths();
        g.drawString(dStr, tx+425 + 45 - fm.stringWidth(dStr), rowY+3);
        String kdStr = p.getDeaths()==0?"Perfect":String.format("%.1f",(float)p.getKills()/p.getDeaths());
        g.drawString(kdStr, tx+540 + 20 - fm.stringWidth(kdStr), rowY+3);
    }

    public Action handleClick(int mx, int my) {
        if (new Rectangle(CX-280,H-88,240,52).contains(mx,my)) return Action.PLAY_AGAIN;
        if (new Rectangle(CX+40, H-88,240,52).contains(mx,my)) return Action.MAIN_MENU;
        return Action.NONE;
    }
}

// ============================================================================
//  PROFILE SELECT SCREEN  (shown on first launch and from main menu)
// ============================================================================
// ============================================================================
//  SWITCH PROFILE OVERLAY  (shown over main menu — no app restart)
// ============================================================================
class SwitchProfileOverlay {
    public enum Action { NONE, SWITCHED, SIGN_OUT, CANCEL }

    private java.util.List<String> profiles = new java.util.ArrayList<>();
    private String currentName = "";
    private String newNameInput = "";
    private boolean editingNew  = false;
    private float   time        = 0f;
    private String  feedbackMsg = "";
    private float   feedbackTimer = 0f;
    // Only-one-profile sign-out confirmation
    private boolean confirmSignOut = false;

    private final int W = UIRenderer.W, H = UIRenderer.H, CX = W/2;

    public void open(String activeName) {
        currentName   = activeName;
        profiles      = com.samarbhumi.progression.PlayerProfile.listProfiles();
        newNameInput  = "";
        editingNew    = false;
        feedbackMsg   = "";
        feedbackTimer = 0f;
        confirmSignOut = false;
    }

    public void update(float dt) {
        time += dt;
        if (feedbackTimer > 0) feedbackTimer -= dt;
    }

    public void render(Graphics2D g, int mx, int my) {
        // Dim the background
        g.setColor(new Color(0,0,0,170));
        g.fillRect(0,0,W,H);

        int pw = 640, ph = confirmSignOut ? 220 : Math.min(540, 240 + Math.max(profiles.size(),1)*44);
        int px = CX - pw/2, py = H/2 - ph/2;
        UIRenderer.panel(g, px, py, pw, ph, confirmSignOut ? "SIGN OUT?" : "SWITCH PROFILE");

        int lx = px + 24;

        if (confirmSignOut) {
            // Only one profile — ask sign out
            g.setFont(GameConstants.F_BODY); g.setColor(GameConstants.C_WHITE);
            UIRenderer.centerText(g, "Only one profile exists.", CX, py+80);
            g.setColor(GameConstants.C_DIM);
            UIRenderer.centerText(g, "Sign out to return to guest / profile select?", CX, py+104);
            UIRenderer.button(g, "[ YES — SIGN OUT ]", CX-220, py+ph-76, 200, 42, mx, my, false, new Color(130,30,15));
            UIRenderer.button(g, "[ CANCEL ]",          CX+20,  py+ph-76, 160, 42, mx, my, false, new Color(28,58,14));
            return;
        }

        // Profile list
        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_GOLD);
        g.drawString("SAVED PROFILES", lx, py+72);

        int listY = py + 80;
        for (int i = 0; i < profiles.size(); i++) {
            boolean isCurrent = profiles.get(i).equals(currentName);
            boolean over = new java.awt.Rectangle(lx, listY+i*44, pw-80, 38).contains(mx,my);
            g.setColor(isCurrent ? new Color(40,90,18,230) : over ? new Color(25,55,12,180) : new Color(12,24,6,160));
            g.fillRoundRect(lx, listY+i*44, pw-80, 38, 8,8);
            g.setColor(isCurrent ? GameConstants.C_ACCENT : new Color(55,90,25,120));
            g.setStroke(new BasicStroke(isCurrent ? 2f : 0.8f));
            g.drawRoundRect(lx, listY+i*44, pw-80, 38, 8,8);
            g.setFont(GameConstants.F_HUD); g.setColor(isCurrent ? GameConstants.C_GOLD2 : GameConstants.C_WHITE);
            g.drawString(profiles.get(i) + (isCurrent ? "  ← active" : ""), lx+14, listY+i*44+25);
        }

        // Create new
        int createY = listY + Math.max(profiles.size(),1)*44 + 18;
        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_GOLD);
        g.drawString("CREATE & SWITCH", lx, createY);
        createY += 6;
        int nbw = pw-250, nbh = 38;
        g.setColor(editingNew ? new Color(30,65,15,230) : new Color(12,24,6,200));
        g.fillRoundRect(lx, createY, nbw, nbh, 8,8);
        g.setColor(editingNew ? GameConstants.C_ACCENT : new Color(50,85,22,150));
        g.setStroke(new BasicStroke(editingNew ? 2f : 1f));
        g.drawRoundRect(lx, createY, nbw, nbh, 8,8);
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_WHITE);
        String disp = editingNew ? newNameInput+(((int)(time*2))%2==0?"|":"") : (newNameInput.isEmpty()?"Click to type name..":newNameInput);
        g.drawString(disp, lx+10, createY+24);
        UIRenderer.button(g, "CREATE", lx+nbw+15, createY, 120, nbh, mx, my, false, new Color(35,95,14));

        if (feedbackTimer > 0) {
            g.setFont(GameConstants.F_BODY);
            g.setColor(feedbackMsg.startsWith("Error") ? GameConstants.C_RED : GameConstants.C_GREEN);
            UIRenderer.centerText(g, feedbackMsg, CX, createY+nbh+20);
        }

        // Sign out + Cancel buttons at bottom
        UIRenderer.button(g, "SIGN OUT", px+pw-240, py+ph-60, 140, 38, mx, my, false, new Color(120,25,12));
        UIRenderer.button(g, "CANCEL",   px+pw-90,  py+ph-60, 80,  38, mx, my, false, new Color(35,58,20));
    }

    /** Returns the profile name to switch to, or "" */
    private String pendingSwitch = "";
    public String getPendingSwitch() { return pendingSwitch; }

    public Action handleClick(int mx, int my) {
        int pw = 640, ph = confirmSignOut ? 220 : Math.min(540, 200 + Math.max(profiles.size(),1)*44);
        int px = CX - pw/2, py = H/2 - ph/2;
        int lx = px + 24;

        if (confirmSignOut) {
            if (new java.awt.Rectangle(CX-220, py+ph-76, 200, 42).contains(mx,my)) return Action.SIGN_OUT;
            if (new java.awt.Rectangle(CX+20,  py+ph-76, 160, 42).contains(mx,my)) { confirmSignOut=false; return Action.NONE; }
            return Action.NONE;
        }

        int listY = py + 80;
        for (int i = 0; i < profiles.size(); i++) {
            if (new java.awt.Rectangle(lx, listY+i*44, pw-80, 38).contains(mx,my)) {
                String chosen = profiles.get(i);
                if (chosen.equals(currentName)) {
                    feedbackMsg = "Already playing as " + chosen; feedbackTimer = 1.5f;
                    return Action.NONE;
                }
                pendingSwitch = chosen;
                return Action.SWITCHED;
            }
        }

        int createY = listY + Math.max(profiles.size(),1)*44 + 24;
        int nbw = pw-250, nbh = 38;
        if (new java.awt.Rectangle(lx, createY, nbw, nbh).contains(mx,my)) { editingNew=true; return Action.NONE; }
        if (new java.awt.Rectangle(lx+nbw+15, createY, 120, nbh).contains(mx,my)) {
            if (newNameInput.trim().isEmpty()) { feedbackMsg="Error: enter a name first."; feedbackTimer=2f; }
            else { 
                pendingSwitch=newNameInput.trim(); 
                try { com.samarbhumi.progression.PlayerProfile.loadOrCreate(pendingSwitch).save(); } catch (Exception ignored) {}
                newNameInput=""; editingNew=false; return Action.SWITCHED; 
            }
            return Action.NONE;
        }
        // Sign out
        if (new java.awt.Rectangle(px+pw-240, py+ph-60, 140, 38).contains(mx,my)) {
            if (profiles.size() <= 1) { confirmSignOut=true; return Action.NONE; }
            return Action.SIGN_OUT;
        }
        // Cancel
        if (new java.awt.Rectangle(px+pw-90, py+ph-60, 80, 38).contains(mx,my)) return Action.CANCEL;
        editingNew = false;
        return Action.NONE;
    }

    public boolean handleKey(java.awt.event.KeyEvent e) {
        if (!editingNew) {
            if (e.getKeyCode()==java.awt.event.KeyEvent.VK_ESCAPE) return false; // let caller close
            return false;
        }
        int code = e.getKeyCode();
        if (code==java.awt.event.KeyEvent.VK_ENTER)   { editingNew=false; return true; }
        if (code==java.awt.event.KeyEvent.VK_ESCAPE)   { editingNew=false; newNameInput=""; return true; }
        if (code==java.awt.event.KeyEvent.VK_BACK_SPACE) {
            if (!newNameInput.isEmpty()) newNameInput=newNameInput.substring(0,newNameInput.length()-1);
            return true;
        }
        char ch = e.getKeyChar();
        if (ch!=java.awt.event.KeyEvent.CHAR_UNDEFINED && newNameInput.length()<16 && !Character.isISOControl(ch))
            newNameInput+=ch;
        return true;
    }

    public boolean isEditing() { return editingNew; }
}

// ============================================================================
//  PROFILE SELECT SCREEN  (shown on first launch and from main menu)
// ============================================================================
class ProfileSelectScreen {
    public enum Action { NONE, LOAD, CREATE, DELETE }

    private java.util.List<String> profiles = new java.util.ArrayList<>();
    private int    selected       = -1;
    private String newNameInput   = "";
    private boolean editingNew    = false;
    private float   time          = 0f;
    private String  feedbackMsg   = "";
    private float   feedbackTimer = 0f;

    private final int W = UIRenderer.W, H = UIRenderer.H, CX = W/2;

    public void refresh() {
        profiles = com.samarbhumi.progression.PlayerProfile.listProfiles();
        if (selected >= profiles.size()) selected = profiles.size()-1;
    }

    public void update(float dt) {
        time += dt;
        if (feedbackTimer > 0) feedbackTimer -= dt;
    }

    public void render(Graphics2D g, int mx, int my) {
        UIRenderer.drawMenuBG(g, time);
        UIRenderer.panel(g, CX-350, 60, 700, 560, "SELECT PROFILE");

        int lx = CX - 320;

        // Profile list
        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_GOLD);
        g.drawString("SAVED PROFILES", lx, 112);

        int listY = 120;
        if (profiles.isEmpty()) {
            g.setFont(GameConstants.F_BODY); g.setColor(GameConstants.C_DIM);
            g.drawString("No saved profiles yet — create one below.", lx, listY + 24);
        } else {
            for (int i = 0; i < profiles.size(); i++) {
                boolean sel = (i == selected);
                boolean over = new java.awt.Rectangle(lx, listY+i*44, 500, 40).contains(mx, my);
                g.setColor(sel ? new Color(40,90,18,220) : over ? new Color(25,55,12,180) : new Color(12,24,6,160));
                g.fillRoundRect(lx, listY+i*44, 500, 40, 8, 8);
                g.setColor(sel ? GameConstants.C_ACCENT : new Color(55,90,25,120));
                g.setStroke(new BasicStroke(sel ? 2f : 0.8f));
                g.drawRoundRect(lx, listY+i*44, 500, 40, 8, 8);
                g.setFont(GameConstants.F_HUD);
                g.setColor(sel ? GameConstants.C_GOLD2 : GameConstants.C_WHITE);
                g.drawString(profiles.get(i), lx+16, listY+i*44+26);
                // Delete button
                UIRenderer.button(g, "✕", lx+510, listY+i*44+4, 32, 32, mx, my, false, new Color(100,22,14));
            }
        }

        // Create new profile section
        int createY = listY + Math.max(profiles.size(), 1)*44 + 24;
        g.setFont(GameConstants.F_SUBHEAD); g.setColor(GameConstants.C_GOLD);
        g.drawString("CREATE NEW PROFILE", lx, createY);
        createY += 8;

        // Name input box
        int nbx = lx, nbw = 320, nbh = 36;
        g.setColor(editingNew ? new Color(30,65,15,230) : new Color(12,24,6,200));
        g.fillRoundRect(nbx, createY, nbw, nbh, 8, 8);
        g.setColor(editingNew ? GameConstants.C_ACCENT : new Color(50,85,22,150));
        g.setStroke(new BasicStroke(editingNew ? 2f : 1f));
        g.drawRoundRect(nbx, createY, nbw, nbh, 8, 8);
        g.setFont(GameConstants.F_HUD); g.setColor(GameConstants.C_WHITE);
        String disp = editingNew ? newNameInput+(((int)(time*2))%2==0?"|":"") : (newNameInput.isEmpty()?"Click to enter name...":newNameInput);
        g.drawString(disp, nbx+10, createY+24);
        UIRenderer.button(g, "CREATE", nbx+nbw+10, createY, 130, nbh, mx, my, false, new Color(35,95,14));

        // Feedback message
        if (feedbackTimer > 0) {
            g.setFont(GameConstants.F_HUD);
            g.setColor(feedbackMsg.startsWith("Error") ? GameConstants.C_RED : GameConstants.C_GREEN);
            UIRenderer.centerText(g, feedbackMsg, CX, createY+60);
        }

        // Load / Play button
        if (selected >= 0) {
            UIRenderer.button(g, "[ PLAY AS: " + profiles.get(selected) + " ]", CX-180, H-100, 360, 52, mx, my, false, new Color(35,100,14));
        } else {
            g.setFont(GameConstants.F_BODY); g.setColor(GameConstants.C_DIM);
            UIRenderer.centerText(g, "Select a profile above to play, or create a new one.", CX, H-80);
        }
    }

    public Action handleClick(int mx, int my) {
        int lx = CX-320, listY = 120;

        // Profile list rows
        for (int i = 0; i < profiles.size(); i++) {
            if (new java.awt.Rectangle(lx, listY+i*44, 500, 40).contains(mx,my)) {
                selected = i; return Action.NONE;
            }
            // Delete button
            if (new java.awt.Rectangle(lx+510, listY+i*44+4, 32, 32).contains(mx,my)) {
                com.samarbhumi.progression.PlayerProfile.loadOrCreate(profiles.get(i)).deleteSave();
                if (selected == i) selected = -1;
                refresh();
                feedbackMsg = "Profile deleted."; feedbackTimer = 2.5f;
                return Action.DELETE;
            }
        }

        int createY = listY + Math.max(profiles.size(),1)*44 + 32;
        int nbx = lx, nbw = 320, nbh = 36;

        // Name input box
        if (new java.awt.Rectangle(nbx, createY, nbw, nbh).contains(mx,my)) {
            editingNew = true; return Action.NONE;
        }
        // Create button
        if (new java.awt.Rectangle(nbx+nbw+10, createY, 130, nbh).contains(mx,my)) {
            if (newNameInput.trim().isEmpty()) {
                feedbackMsg = "Error: Enter a name first."; feedbackTimer = 2f;
            } else {
                String createdName = newNameInput.trim();
                try { com.samarbhumi.progression.PlayerProfile.loadOrCreate(createdName).save(); } catch (Exception ignored) {}
                profiles.add(createdName);
                selected = profiles.size()-1;
                feedbackMsg = "Profile '" + createdName + "' ready!"; feedbackTimer = 2f;
                newNameInput = ""; editingNew = false;
                return Action.CREATE;
            }
            return Action.NONE;
        }

        // Play button
        if (selected >= 0 && new java.awt.Rectangle(CX-180, H-100, 360, 52).contains(mx,my)) {
            return Action.LOAD;
        }

        editingNew = false;
        return Action.NONE;
    }

    public boolean handleKey(java.awt.event.KeyEvent e) {
        if (!editingNew) return false;
        int code = e.getKeyCode();
        if (code == java.awt.event.KeyEvent.VK_ENTER) { editingNew = false; return true; }
        if (code == java.awt.event.KeyEvent.VK_ESCAPE) { editingNew = false; newNameInput = ""; return true; }
        if (code == java.awt.event.KeyEvent.VK_BACK_SPACE) {
            if (!newNameInput.isEmpty()) newNameInput = newNameInput.substring(0, newNameInput.length()-1);
            return true;
        }
        char ch = e.getKeyChar();
        if (ch != java.awt.event.KeyEvent.CHAR_UNDEFINED && newNameInput.length() < 16 && !Character.isISOControl(ch)) {
            newNameInput += ch;
        }
        return true;
    }

    public String getSelectedName() {
        return (selected >= 0 && selected < profiles.size()) ? profiles.get(selected) : "";
    }
    public boolean isEditing() { return editingNew; }
}
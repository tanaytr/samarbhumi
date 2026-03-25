package com.samarbhumi.ui;

import com.samarbhumi.core.GameConstants;
import java.awt.*;

/**
 * Shared drawing utilities — modern dark UI with sharp typography,
 * glowing accents, smooth gradients, and cinematic menu backgrounds.
 */
public final class UIRenderer {
    private UIRenderer() {}

    public static final int W = GameConstants.WIN_W;
    public static final int H = GameConstants.WIN_H;

    // ── Accent palette ────────────────────────────────────────────────────
    private static final Color PANEL_BG_TOP   = new Color(12, 18, 10, 248);
    private static final Color PANEL_BG_BOT   = new Color( 6, 10,  4, 252);

    // ── Button — modern flat design with glow on hover ─────────────────
    public static boolean button(Graphics2D g, String label,
                                  int x, int y, int bw, int bh,
                                  int mx, int my, boolean selected, Color col) {
        boolean over = mx>=x && mx<=x+bw && my>=y && my<=y+bh;
        boolean lit  = over || selected;

        // Outer drop shadow
        g.setColor(new Color(0, 0, 0, lit ? 110 : 70));
        g.fillRoundRect(x+2, y+4, bw, bh, 12, 12);

        // Button body — two-stop gradient
        Color top = lit ? col.brighter().brighter() : col.brighter();
        Color bot = lit ? col : col.darker();
        GradientPaint body = new GradientPaint(x, y, top, x, y+bh, bot);
        g.setPaint(body);
        g.fillRoundRect(x, y, bw, bh, 12, 12);

        // Highlight sheen on top third
        g.setColor(new Color(255,255,255, lit ? 55 : 30));
        g.fillRoundRect(x+2, y+2, bw-4, bh/3, 10, 10);

        // Glow border on hover/selected
        if (lit) {
            g.setColor(new Color(col.getRed()/2+128, col.getGreen()/2+128, col.getBlue()/2+128, 160));
            g.setStroke(new BasicStroke(1.8f));
            g.drawRoundRect(x, y, bw, bh, 12, 12);
            // Outer glow ring
            g.setColor(new Color(col.getRed()/2+100, col.getGreen()/2+100, col.getBlue()/2+100, 55));
            g.setStroke(new BasicStroke(3f));
            g.drawRoundRect(x-1, y-1, bw+2, bh+2, 13, 13);
        } else {
            g.setColor(new Color(0, 0, 0, 60));
            g.setStroke(new BasicStroke(1.2f));
            g.drawRoundRect(x, y, bw, bh, 12, 12);
        }

        // Label — centred, with crisp text shadow
        g.setFont(GameConstants.F_SUBHEAD);
        FontMetrics fm = g.getFontMetrics();
        int tx = x + bw/2 - fm.stringWidth(label)/2;
        int ty = y + bh/2 + fm.getAscent()/2 - 2;
        g.setColor(new Color(0, 0, 0, 140));
        g.drawString(label, tx+1, ty+1);
        g.setColor(lit ? Color.WHITE : new Color(235, 235, 220));
        g.drawString(label, tx, ty);

        return over;
    }

    public static boolean button(Graphics2D g, String label, int x, int y, int bw, int bh, int mx, int my) {
        return button(g, label, x, y, bw, bh, mx, my, false, new Color(35, 65, 18));
    }

    // ── Panel — frosted glass dark card ──────────────────────────────────
    public static void panel(Graphics2D g, int x, int y, int pw, int ph, String title) {
        // Layered shadow
        for (int i = 6; i >= 1; i--) {
            g.setColor(new Color(0, 0, 0, 12 * i));
            g.fillRoundRect(x + i, y + i*2, pw, ph, 18, 18);
        }

        // Glass body
        GradientPaint body = new GradientPaint(x, y, PANEL_BG_TOP, x, y+ph, PANEL_BG_BOT);
        g.setPaint(body);
        g.fillRoundRect(x, y, pw, ph, 18, 18);

        // Subtle inner light at top
        GradientPaint inner = new GradientPaint(x, y, new Color(255,255,255,12), x, y+60, new Color(255,255,255,0));
        g.setPaint(inner);
        g.fillRoundRect(x, y, pw, 60, 18, 18);

        // Border — thin bright line
        GradientPaint border = new GradientPaint(x, y, new Color(100,180,50,200), x, y+ph, new Color(50,90,20,80));
        g.setPaint(border);
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(x, y, pw, ph, 18, 18);

        if (!title.isEmpty()) {
            // Title bar gradient
            GradientPaint titleBar = new GradientPaint(x, y, new Color(40,90,18,220), x, y+42, new Color(15,30,8,200));
            g.setPaint(titleBar);
            g.fillRoundRect(x+1, y+1, pw-2, 42, 17, 17);
            g.fillRect(x+1, y+28, pw-2, 15);

            // Title text
            g.setFont(GameConstants.F_HEAD);
            FontMetrics fm = g.getFontMetrics();
            int tx = x + pw/2 - fm.stringWidth(title)/2;
            // Text shadow
            g.setColor(new Color(0, 0, 0, 140));
            g.drawString(title, tx+1, y+30);
            // Gold gradient text
            GradientPaint textGrad = new GradientPaint(tx, y+10, new Color(255,230,80), tx, y+32, new Color(200,150,20));
            g.setPaint(textGrad);
            g.drawString(title, tx, y+29);

            // Divider line with glow
            g.setColor(new Color(80,160,40,60));
            g.setStroke(new BasicStroke(2f));
            g.drawLine(x+8, y+42, x+pw-8, y+42);
            g.setColor(new Color(120,220,60,120));
            g.setStroke(new BasicStroke(0.8f));
            g.drawLine(x+8, y+42, x+pw-8, y+42);
        }
    }

    // ── Background — vivid warzone panorama ─────────────────────────────
    public static void drawMenuBG(Graphics2D g, float time) {
        // Sky — deep teal-to-indigo gradient (not murky black)
        GradientPaint sky = new GradientPaint(0, 0, new Color(5, 18, 42),
                                               0, H * 0.6f, new Color(12, 38, 22));
        g.setPaint(sky);
        g.fillRect(0, 0, W, H);

        // Aurora borealis bands — sweeping across upper sky
        drawAurora(g, time);

        // Stars
        long seed = 0xDEADBEEFL;
        for (int i = 0; i < 100; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sx = (int)(Math.abs(seed >> 32) % W);
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int sy = (int)(Math.abs(seed >> 32) % (H * 42 / 100));
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float phase = (seed >> 24) * 0.0001f;
            float twinkle = 0.3f + 0.7f * (float)Math.abs(Math.sin(time * 0.9f + phase));
            int bright = (int)(twinkle * 220);
            int sz = ((seed >> 20) & 7) == 0 ? 2 : 1;
            g.setColor(new Color(bright, bright, Math.min(255, bright + 30), bright));
            g.fillOval(sx, sy, sz, sz);
        }

        // Moon with warm glow
        int moonX = W - 130, moonY = 18, moonR = 58;
        for (int i = 8; i >= 1; i--) {
            g.setColor(new Color(220, 200, 120, 6 * i));
            g.fillOval(moonX - i*5, moonY - i*5, moonR + i*10, moonR + i*10);
        }
        GradientPaint moonGrad = new GradientPaint(moonX, moonY, new Color(245, 240, 200),
                                                    moonX + moonR, moonY + moonR, new Color(210, 195, 140));
        g.setPaint(moonGrad);
        g.fillOval(moonX, moonY, moonR, moonR);
        g.setColor(new Color(5, 18, 42, 200));
        g.fillOval(moonX + 12, moonY - 8, moonR, moonR);

        // Distant mountain range — blue-purple silhouette
        drawMountainRange(g, time, 0.06f, H - 280, new Color(18, 30, 62, 160), 6, 210, 0L);
        drawMountainRange(g, time, 0.12f, H - 220, new Color(22, 45, 35, 195), 8, 160, 12345L);
        drawMountainRange(g, time, 0.18f, H - 165, new Color(15, 35, 18, 230), 10, 120, 99887L);

        // City ruins with glowing windows
        drawCityWithLights(g, time, H);

        // Ground — dark earth with subtle colour
        GradientPaint ground = new GradientPaint(0, H - 90, new Color(20, 48, 15),
                                                  0, H,       new Color(5, 12, 3));
        g.setPaint(ground);
        g.fillRect(0, H - 90, W, 90);
        // Ground edge highlight
        g.setColor(new Color(45, 95, 25, 120));
        g.fillRect(0, H - 90, W, 4);

        // Horizontal atmospheric glow at horizon
        GradientPaint horizon = new GradientPaint(0, H - 200, new Color(0, 0, 0, 0),
                                                   0, H - 80,  new Color(30, 80, 20, 55));
        g.setPaint(horizon);
        g.fillRect(0, H - 200, W, 120);

        // Scan-line micro-texture
        g.setColor(new Color(0, 0, 0, 10));
        for (int scanY = 0; scanY < H; scanY += 3) g.drawLine(0, scanY, W, scanY);

        // Radial vignette
        RadialGradientPaint vig = new RadialGradientPaint(W / 2f, H / 2f, W * 0.68f,
            new float[]{0.45f, 1.0f},
            new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 140)});
        g.setPaint(vig);
        g.fillRect(0, 0, W, H);
    }

    private static void drawAurora(Graphics2D g, float time) {
        // Two sweeping bands of colour across the upper 40% of sky
        int bands = 3;
        for (int b = 0; b < bands; b++) {
            float phase  = time * 0.18f + b * 2.1f;
            float offset = (float)Math.sin(phase) * 80;
            int   baseY  = 60 + b * 55 + (int)offset;
            int   alpha  = 28 + b * 8;
            // Colour cycles: teal → green → purple
            float cycle  = (float)((Math.sin(time * 0.12f + b * 1.5) + 1.0) * 0.5);
            int   r = (int)(cycle * 80);
            int   gv= (int)(120 + cycle * 80);
            int   bv= (int)(140 + (1-cycle) * 80);
            for (int x = 0; x < W; x += 2) {
                float wavY  = baseY + (float)Math.sin(x * 0.008f + phase) * 30
                                    + (float)Math.sin(x * 0.015f + phase * 0.7f) * 20;
                int   bandH = 45 + (int)(Math.abs(Math.sin(x * 0.005f + phase)) * 30);
                GradientPaint ap = new GradientPaint(x, wavY, new Color(r, gv, bv, 0),
                                                      x, wavY + bandH, new Color(r, gv, bv, alpha));
                g.setPaint(ap);
                g.fillRect(x, (int)wavY, 2, bandH);
            }
        }
    }

    private static void drawMountainRange(Graphics2D g, float time, float parallax,
                                           int baseY, Color col, int peaks, int maxH, long seed) {
        g.setColor(col);
        float offX = -(time * parallax * 12f) % W;
        for (int pass = 0; pass < 2; pass++) {
            int sx = (int)(offX + pass * W);
            int[] xp = new int[peaks * 2 + 4];
            int[] yp = new int[peaks * 2 + 4];
            int n = 0;
            xp[n] = sx; yp[n++] = H;
            long s = seed + pass * 54321L;
            for (int i = 0; i < peaks; i++) {
                s = s * 1664525L + 1013904223L;
                int bx = sx + i * W / peaks;
                int px = sx + i * W / peaks + W / peaks / 2;
                int ph = 40 + (int)(Math.abs(s) % maxH);
                xp[n] = bx; yp[n++] = baseY + 25;
                xp[n] = px; yp[n++] = baseY - ph;
            }
            xp[n] = sx + W; yp[n++] = baseY + 25;
            xp[n] = sx + W; yp[n++] = H;
            g.fillPolygon(xp, yp, n);
        }
    }

    private static void drawCityWithLights(Graphics2D g, float time, int H) {
        long seed = 0xBEEFC0DEL;
        int x = -30;
        while (x < W + 80) {
            seed = seed * 6364136223846793005L + 1L;
            int bw = 28 + (int)(Math.abs(seed >> 40) % 88);
            seed = seed * 6364136223846793005L + 1L;
            int bh = 55 + (int)(Math.abs(seed >> 40) % 185);
            seed = seed * 6364136223846793005L + 1L;
            int gap = 3 + (int)(Math.abs(seed >> 40) % 18);

            // Building silhouette — dark but with subtle colour variation
            int dark = 6 + (int)(Math.abs(seed >> 48) % 8);
            g.setColor(new Color(dark, dark + 4, dark + 2, 240));
            g.fillRect(x, H - 88 - bh, bw, bh);

            // Rooftop detail
            seed = seed * 6364136223846793005L + 1L;
            for (int chunk = 0; chunk < bw; chunk += 10) {
                int crag = (int)(Math.abs((seed >> (chunk % 40)) * 7) % 18);
                g.fillRect(x + chunk, H - 88 - bh - crag, Math.min(9, bw - chunk), crag + 2);
            }

            // Glowing windows — warm amber and cool blue
            for (int wy = H - 88 - bh + 8; wy < H - 95; wy += 18) {
                for (int wx = x + 5; wx < x + bw - 10; wx += 14) {
                    seed = seed * 6364136223846793005L + 1L;
                    boolean lit = (Math.abs(seed >> 32) % 100) < 55; // 55% chance lit
                    if (lit) {
                        // Flicker occasionally
                        float flicker = 1.0f;
                        if ((Math.abs(seed >> 48) % 80) < 5)
                            flicker = 0.4f + 0.6f * (float)Math.abs(Math.sin(time * 8f + wx));
                        boolean warm = (Math.abs(seed >> 24) % 3) != 0;
                        Color wc = warm
                            ? new Color(255, 210, 90, (int)(160 * flicker))
                            : new Color(120, 190, 255, (int)(140 * flicker));
                        // Window glow
                        g.setColor(new Color(wc.getRed(), wc.getGreen(), wc.getBlue(), (int)(40 * flicker)));
                        g.fillRect(wx - 2, wy - 2, 12, 16);
                        // Window itself
                        g.setColor(wc);
                        g.fillRect(wx, wy, 8, 11);
                    } else {
                        g.setColor(new Color(2, 4, 2, 180));
                        g.fillRect(wx, wy, 8, 11);
                    }
                }
            }
            x += bw + gap;
        }
    }


    // ── Logo — cinematic gold title ───────────────────────────────────────
    public static void drawLogo(Graphics2D g, int cx, int y, float time) {
        String title = "SAMARBHUMI";
        g.setFont(GameConstants.F_TITLE);
        FontMetrics fm = g.getFontMetrics();
        int tx = cx - fm.stringWidth(title)/2;

        // Bloom glow — multiple layers
        float pulse = 0.5f + 0.5f * (float)Math.sin(time * 1.1f);
        for (int i = 8; i >= 1; i--) {
            int alpha = (int)(18 * pulse / i);
            g.setColor(new Color(120, 220, 40, alpha));
            g.drawString(title, tx - i, y + i/2);
        }

        // Shadow
        g.setColor(new Color(0, 0, 0, 200));
        g.drawString(title, tx+3, y+3);

        // Main — shimmer effect
        float shimmer = (float)Math.sin(time * 0.6f) * 0.5f + 0.5f;
        Color c1 = lerp(new Color(255,215,60), new Color(255,240,130), shimmer);
        Color c2 = lerp(new Color(180,110,10), new Color(210,150,20), shimmer);
        GradientPaint grd = new GradientPaint(tx, y-36, c1, tx, y+4, c2);
        g.setPaint(grd);
        g.drawString(title, tx, y);

        // Tagline
        String sub = "WAR  NEVER  ENDS";
        g.setFont(GameConstants.F_SUBHEAD);
        fm = g.getFontMetrics();
        int subX = cx - fm.stringWidth(sub)/2;
        // Letter-spacing effect — draw letter by letter
        g.setColor(new Color(140, 200, 70, 200));
        g.drawString(sub, subX, y + 26);
    }

    private static Color lerp(Color a, Color b, float t) {
        return new Color(
            (int)(a.getRed()   + (b.getRed()   - a.getRed()  ) * t),
            (int)(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int)(a.getBlue()  + (b.getBlue()  - a.getBlue() ) * t)
        );
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    public static void centerText(Graphics2D g, String s, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s)/2, y);
    }

    public static void drawStatRow(Graphics2D g, int x, int y, int rowW, String label, String value, Color valCol) {
        g.setFont(GameConstants.F_BODY);
        g.setColor(new Color(140, 140, 110, 180));
        g.drawString(label, x, y);
        g.setFont(GameConstants.F_HUD);
        g.setColor(valCol);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(value, x + rowW - fm.stringWidth(value), y);
        g.setColor(new Color(60, 100, 30, 50));
        g.setStroke(new BasicStroke(0.8f));
        g.drawLine(x, y + 4, x + rowW, y + 4);
    }

    public static void drawHBar(Graphics2D g, int x, int y, int bw, int bh, float frac, Color fill) {
        // Track
        g.setColor(new Color(5, 8, 3, 220));
        g.fillRoundRect(x, y, bw, bh, 6, 6);
        g.setColor(new Color(0, 0, 0, 80));
        g.setStroke(new BasicStroke(0.8f));
        g.drawRoundRect(x, y, bw, bh, 6, 6);

        if (frac > 0) {
            int fw = Math.max(1, (int)((bw-2)*frac));
            // Fill
            GradientPaint fp = new GradientPaint(x, y, fill.brighter(), x, y+bh, fill.darker());
            g.setPaint(fp);
            g.fillRoundRect(x+1, y+1, fw, bh-2, 5, 5);
            // Shine
            g.setColor(new Color(255,255,255,38));
            g.fillRoundRect(x+1, y+1, fw, (bh-2)/2, 4, 4);
        }
    }
}

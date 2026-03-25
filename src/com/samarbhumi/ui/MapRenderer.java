package com.samarbhumi.ui;

import com.samarbhumi.core.*;
import com.samarbhumi.core.Enums.*;
import com.samarbhumi.entity.PickupItem;
import com.samarbhumi.map.GameMap;

import java.awt.*;
import java.util.List;
import java.util.Random;

/**
 * Draws the game map: sky gradient, parallax background layers, tiles, pickups.
 * Tile drawing uses per-face shading for a 2.5D illusion.
 * Demonstrates: layered rendering, spatial culling (only draw visible tiles).
 */
public class MapRenderer {

    private static final int T = GameConstants.TILE_SIZE;

    public static void drawBackground(Graphics2D g, float camX, float camY, int screenW, int screenH) {
        drawBackground(g, camX, camY, screenW, screenH, 0);
    }

    public static void drawBackground(Graphics2D g, float camX, float camY, int screenW, int screenH, int mapStyle) {
        // Sky — varies by map
        Color skyTop, skyBot;
        switch (mapStyle) {
            case 1 -> { skyTop = new Color(15,30,15); skyBot = new Color(30,55,25); } // jungle — dark green
            case 2 -> { skyTop = new Color(18,22,30); skyBot = new Color(30,35,45); } // fortress — dark grey-blue
            case 3 -> { skyTop = new Color(20,12,8);  skyBot = new Color(35,20,10); } // city — orange smoke haze
            default-> { skyTop = GameConstants.T_SKY_TOP; skyBot = GameConstants.T_SKY_BOT; } // warzone — blue night
        }
        GradientPaint sky = new GradientPaint(0,0, skyTop, 0,screenH*0.7f, skyBot);
        g.setPaint(sky); g.fillRect(0,0,screenW,screenH);

        // Moon / sun
        g.setColor(new Color(255,240,200,mapStyle==1?40:90));
        g.fillOval(screenW-160, 30, 70, 70);
        g.setColor(new Color(255,250,220,40));
        g.fillOval(screenW-165, 25, 80, 80);

        // Far mountain silhouette
        float px1 = -(camX * 0.1f) % screenW;
        Color mtnColor = mapStyle==2 ? new Color(35,40,50,120) : mapStyle==1 ? new Color(20,40,18,120) : new Color(30,48,38,120);
        g.setColor(mtnColor);
        drawMountainLayer(g,(int)px1, screenH, screenW, 180, 6);
        drawMountainLayer(g,(int)(px1+screenW), screenH, screenW, 180, 6);

        // Mid trees / structures
        float px2 = -(camX * 0.2f) % screenW;
        if (mapStyle == 2) {
            // Metal fortress — distant building silhouettes
            g.setColor(new Color(25,30,38,160));
            drawBuildingLayer(g,(int)px2, screenH, screenW);
            drawBuildingLayer(g,(int)(px2+screenW), screenH, screenW);
        } else if (mapStyle == 3) {
            // City — dense lit skyscraper skyline
            g.setColor(new Color(30,18,10,180));
            drawBuildingLayer(g,(int)px2, screenH, screenW);
            drawBuildingLayer(g,(int)(px2+screenW), screenH, screenW);
            // Orange smog glow at horizon
            GradientPaint smog = new GradientPaint(0, screenH-120, new Color(180,80,10,60), 0, screenH-20, new Color(0,0,0,0));
            g.setPaint(smog); g.fillRect(0, screenH-120, screenW, 120);
        } else {
            Color treeColor = mapStyle==1 ? new Color(18,38,14,160) : new Color(22,42,18,160);
            g.setColor(treeColor);
            drawTreeLine(g,(int)px2, screenH-80, screenW, 12345);
            drawTreeLine(g,(int)(px2+screenW), screenH-80, screenW, 12345);
        }

        // Near trees
        float px3 = -(camX * 0.3f) % screenW;
        Color nearColor = mapStyle==1 ? new Color(14,32,10,200) : mapStyle==2 ? new Color(20,25,32,200) : new Color(18,35,14,200);
        g.setColor(nearColor);
        drawTreeLine(g,(int)px3, screenH-40, screenW, 54321);
        drawTreeLine(g,(int)(px3+screenW), screenH-40, screenW, 54321);
    }

    private static void drawBuildingLayer(Graphics2D g, int offX, int screenH, int sw) {
        java.util.Random r = new java.util.Random(99887L);
        int x = offX;
        while (x < offX + sw) {
            int bw = 30 + r.nextInt(50);
            int bh = 60 + r.nextInt(100);
            g.fillRect(x, screenH - bh, bw, bh);
            // antenna
            if (r.nextBoolean()) g.fillRect(x + bw/2 - 1, screenH - bh - 15, 2, 15);
            x += bw + r.nextInt(20);
        }
    }

    private static void drawMountainLayer(Graphics2D g, int offX, int screenH, int sw, int maxH, int peaks) {
        int[] xp = new int[peaks*2+4], yp = new int[peaks*2+4];
        int n=0;
        xp[n]=offX; yp[n++]=screenH;
        for (int i=0; i<peaks; i++) {
            int bx = offX + (i*2)*sw/peaks/2;
            int px2= offX + (i*2+1)*sw/peaks/2;
            xp[n]=bx; yp[n++]=screenH;
            xp[n]=px2; yp[n++]=screenH-50-new Random(i*1337).nextInt(maxH);
        }
        xp[n]=offX+sw; yp[n++]=screenH;
        xp[n]=offX+sw; yp[n++]=screenH;
        g.fillPolygon(xp, yp, n);
    }

    private static void drawTreeLine(Graphics2D g, int offX, int baseY, int sw, long seed) {
        Random r = new Random(seed);
        for (int x=offX; x<offX+sw; x+=18+r.nextInt(12)) {
            int h = 30+r.nextInt(35);
            // Triangle tree
            g.fillPolygon(new int[]{x,x+10,x+20}, new int[]{baseY,baseY-h,baseY},3);
        }
    }

    public static void drawTiles(Graphics2D g, GameMap map, float camX, float camY,
                                  int screenW, int screenH) {
        int minC = Math.max(0, (int)(camX/T)-1);
        int maxC = Math.min(map.getW()-1, minC+screenW/T+2);
        int minR = Math.max(0, (int)(camY/T)-1);
        int maxR = Math.min(map.getH()-1, minR+screenH/T+2);

        for (int r=minR; r<=maxR; r++) {
            for (int c=minC; c<=maxC; c++) {
                TileType tile = map.getTile(c,r);
                if (tile == TileType.AIR || tile == TileType.SPAWN_BLUE || tile == TileType.SPAWN_RED) continue;
                int px = (int)(c*T - camX);
                int py = (int)(r*T - camY);
                drawTile(g, tile, px, py, c, r, map);
            }
        }
    }

    private static void drawTile(Graphics2D g, TileType tile, int px, int py, int c, int r, GameMap map) {
        switch (tile) {
            case SOLID    -> drawSolidTile(g, px, py, c, r, map);
            case PLATFORM -> drawPlatformTile(g, px, py);
            case LADDER   -> drawLadderTile(g, px, py);
            case WATER    -> drawWaterTile(g, px, py);
            default -> {}
        }
    }

    private static void drawSolidTile(Graphics2D g, int px, int py, int c, int r, GameMap map) {
        int style = map.getMapStyle(); // 0=grass, 1=jungle stone, 2=metal

        // Tile variant from position for texture variation
        long seed = c * 7919L + r * 3571L;
        int variant = (int)(Math.abs(seed) % 3);

        Color base, dark, top;
        switch (style) {
            case 1 -> { // Jungle Ruins — mossy dark stone
                base = variant==0 ? new Color(55,75,45)  : variant==1 ? new Color(70,85,55)  : new Color(45,65,38);
                dark = variant==0 ? new Color(35,50,28)  : variant==1 ? new Color(45,60,35)  : new Color(28,42,20);
                top  = new Color(85,120,55);
            }
            case 2 -> { // Steel Fortress — industrial metal plates
                base = variant==0 ? new Color(72,82,90)  : variant==1 ? new Color(60,70,78)  : new Color(82,90,98);
                dark = variant==0 ? new Color(45,52,58)  : variant==1 ? new Color(38,44,50)  : new Color(52,58,65);
                top  = new Color(110,125,135);
            }
            case 3 -> { // City Ruins — cracked concrete, asphalt, brick
                base = variant==0 ? new Color(75,72,68)  : variant==1 ? new Color(88,80,72)  : new Color(62,60,58);
                dark = variant==0 ? new Color(50,48,45)  : variant==1 ? new Color(58,52,46)  : new Color(40,38,36);
                top  = new Color(105,100,92);
            }
            default -> { // Warzone Alpha — grass / dirt
                base = variant==0 ? GameConstants.T_GROUND : variant==1 ? GameConstants.T_ROCK : new Color(75,90,55);
                dark = variant==0 ? GameConstants.T_GROUND_DARK : variant==1 ? GameConstants.T_ROCK_DARK : new Color(50,62,35);
                top  = GameConstants.T_GROUND_TOP;
            }
        }

        g.setColor(base);
        g.fillRect(px, py, T, T);

        // Top face highlight if tile above is open
        TileType above = map.getTile(c, r-1);
        if (above==TileType.AIR||above==TileType.SPAWN_BLUE||above==TileType.SPAWN_RED||above==TileType.PLATFORM) {
            g.setColor(top);
            g.fillRect(px, py, T, 6);
            if (style == 0 && variant==0) { // grass tufts on warzone
                g.setColor(new Color(55,110,35));
                for (int gx=px+2; gx<px+T-2; gx+=4) {
                    int gh = 2+((gx*31+py*17)%4);
                    g.fillRect(gx, py-gh, 2, gh);
                }
            }
            if (style == 1) { // moss on jungle
                g.setColor(new Color(60,130,40,120));
                for (int gx=px+1; gx<px+T-1; gx+=5) g.fillRect(gx, py, 3, 4);
            }
        }

        // Style-specific surface details
        g.setColor(dark);
        g.setStroke(new BasicStroke(0.5f));
        switch (style) {
            case 0 -> { if (variant==1) { g.drawLine(px+4,py+6,px+10,py+14); g.drawLine(px+T-8,py+4,px+T-12,py+T-6); } }
            case 1 -> { // stone cracks
                g.drawLine(px+3, py+8, px+12, py+16);
                g.drawLine(px+T-5, py+5, px+T-10, py+T-4);
                if (variant==2) g.drawLine(px+T/2, py+3, px+T/2+4, py+T-3);
            }
            case 2 -> { // metal rivets + panel lines
                g.fillOval(px+2, py+2, 4, 4); g.fillOval(px+T-6, py+2, 4, 4);
                g.fillOval(px+2, py+T-6, 4, 4); g.fillOval(px+T-6, py+T-6, 4, 4);
                if (variant==0) g.drawLine(px+T/2, py, px+T/2, py+T); // panel seam
            }
            case 3 -> { // City Ruins — cracked concrete
                if (variant==0) { g.drawLine(px+5,py+4,px+14,py+18); g.drawLine(px+T-6,py+8,px+T-10,py+T-5); }
                else if (variant==1) { g.drawLine(px+3,py+12,px+T-4,py+18); g.drawLine(px+8,py+3,px+12,py+T-4); }
                else { g.fillRect(px+4,py+4,3,3); g.fillRect(px+T-8,py+T-8,3,3); }
            }
        }

        g.setColor(new Color(0,0,0,25)); g.fillRect(px,py,3,T); g.fillRect(px,py,T,3);
        g.setColor(new Color(255,255,255,15)); g.drawLine(px,py,px+T,py); g.drawLine(px,py,px,py+T);
        g.setColor(new Color(0,0,0,50)); g.setStroke(new BasicStroke(0.8f)); g.drawRect(px,py,T,T);
    }

    private static void drawPlatformTile(Graphics2D g, int px, int py) {
        // Wooden plank platform
        g.setColor(GameConstants.T_LADDER);
        g.fillRect(px, py+T-10, T, 10);
        // Plank lines
        g.setColor(new Color(120,90,40));
        g.setStroke(new BasicStroke(0.8f));
        for (int dx=0; dx<T; dx+=12) g.drawLine(px+dx, py+T-10, px+dx, py+T);
        // Top highlight
        g.setColor(new Color(200,165,80));
        g.fillRect(px, py+T-10, T, 3);
        // Underside dark
        g.setColor(new Color(80,55,20));
        g.fillRect(px, py+T-3, T, 3);
    }

    private static void drawLadderTile(Graphics2D g, int px, int py) {
        // Side rails
        g.setColor(GameConstants.T_LADDER);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(px+8,  py, px+8,  py+T);
        g.drawLine(px+T-8,py, px+T-8,py+T);
        // Rungs
        g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int ry=py+4; ry<py+T; ry+=10)
            g.drawLine(px+8, ry, px+T-8, ry);
        // Highlight on rails
        g.setColor(new Color(200,165,80,120));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(px+8,py,px+8,py+T);
    }

    private static void drawWaterTile(Graphics2D g, int px, int py) {
        g.setColor(GameConstants.T_WATER);
        g.fillRect(px, py, T, T);
        // Wave lines
        g.setColor(new Color(80,140,220,80));
        long t2 = System.currentTimeMillis();
        for (int wy=py+4; wy<py+T; wy+=6) {
            int wave = (int)(Math.sin((px+t2*0.002f)*0.3f)*3);
            g.drawLine(px, wy+wave, px+T, wy-wave);
        }
    }

    public static void drawPickups(Graphics2D g, List<PickupItem> pickups, float camX, float camY) {
        for (PickupItem item : pickups) {
            if (!item.isAlive()) continue;
            int px = (int)(item.getPos().x - camX);
            int py = (int)(item.getPos().y - camY + item.getBobOffset());
            drawPickup(g, item, px, py);
        }
    }

    private static void drawPickup(Graphics2D g, PickupItem item, int px, int py) {
        // Glow ring
        int ga = (int)(item.getGlowAlpha() * 80);
        Color gc = switch(item.getPickupType()) {
            case WEAPON -> new Color(255,200,50,ga);
            case HEALTH -> new Color(50,220,50,ga);
            case AMMO   -> new Color(50,150,255,ga);
            default     -> new Color(200,200,200,ga);
        };
        g.setColor(gc);
        g.fillOval(px-4, py-4, 32, 32);

        // Background bubble
        g.setColor(new Color(20,20,20,180));
        g.fillRoundRect(px, py, 24, 24, 6, 6);
        g.setColor(new Color(80,80,80,180));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(px, py, 24, 24, 6, 6);

        // Icon
        switch (item.getPickupType()) {
            case WEAPON -> drawWeaponPickupIcon(g, item.getWeaponType(), px+12, py+12);
            case HEALTH -> {
                g.setColor(new Color(50,220,50));
                g.setFont(new Font("Arial Black", Font.BOLD, 14));
                g.drawString("+", px+7, py+18);
            }
            case AMMO -> {
                g.setColor(new Color(80,160,255));
                g.setFont(new Font("Arial Black", Font.BOLD, 10));
                g.drawString("AMM", px+2, py+16);
            }
            default -> {}
        }
    }

    private static void drawWeaponPickupIcon(Graphics2D g, WeaponType wt, int cx, int cy) {
        g.setColor(new Color(180,180,180));
        switch (wt) {
            case ASSAULT_RIFLE -> { g.fillRect(cx-8,cy-2,16,4); g.fillRect(cx-2,cy+2,5,5); }
            case SHOTGUN       -> { g.fillRect(cx-10,cy-3,18,6); g.fillRect(cx-2,cy+3,5,5); }
            case SNIPER        -> { g.fillRect(cx-10,cy-2,20,3); g.fillRect(cx+2,cy-5,3,3); }
            case ROCKET_LAUNCHER->{ g.setColor(new Color(120,80,40)); g.fillRoundRect(cx-10,cy-4,20,8,4,4); }
            default            -> { g.fillRect(cx-6,cy-2,12,4); g.fillRect(cx-2,cy+2,4,5); }
        }
    }
}

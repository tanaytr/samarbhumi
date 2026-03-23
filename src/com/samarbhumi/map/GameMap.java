package com.samarbhumi.map;

import com.samarbhumi.core.*;
import com.samarbhumi.core.Enums.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tile-based game map.
 * Handles collision queries, spawn points, tile rendering data.
 * Demonstrates 2D arrays, collision mesh, spatial queries.
 */
public class GameMap {

    private final TileType[][] tiles;
    private final int W, H;
    private final List<Vec2> spawnBlue = new ArrayList<>();
    private final List<Vec2> spawnRed  = new ArrayList<>();
    private final String name;
    /** 0 = grass/open (Warzone Alpha), 1 = jungle/stone (Jungle Ruins), 2 = metal/fortress (Steel Fortress) */
    private int mapStyle = 0;

    public GameMap(String name, TileType[][] tiles) {
        this.name  = name;
        this.tiles = tiles;
        this.H     = tiles.length;
        this.W     = tiles[0].length;
        scanSpawns();
    }

    public void setMapStyle(int s) { mapStyle = s; }
    public int  getMapStyle()      { return mapStyle; }

    private void scanSpawns() {
        for (int r=0;r<H;r++) for (int c=0;c<W;c++) {
            if (tiles[r][c]==TileType.SPAWN_BLUE) spawnBlue.add(new Vec2(c*GameConstants.TILE_SIZE, r*GameConstants.TILE_SIZE));
            if (tiles[r][c]==TileType.SPAWN_RED)  spawnRed .add(new Vec2(c*GameConstants.TILE_SIZE, r*GameConstants.TILE_SIZE));
        }
        // Fallback
        if (spawnBlue.isEmpty()) spawnBlue.add(new Vec2(100,200));
        if (spawnRed.isEmpty())  spawnRed .add(new Vec2(GameConstants.MAP_W_PX-200,200));
    }

    // ── Collision ──────────────────────────────────────────────────────────

    /** Resolve AABB against map tiles. Modifies pos and vel in place; sets onGround. */
    public void resolve(com.samarbhumi.physics.PhysicsBody body, float dt) {
        AABB box = body.getBounds();

        // Determine tile range to check
        int minC = Math.max(0, (int)(box.left()/GameConstants.TILE_SIZE) - 1);
        int maxC = Math.min(W-1, (int)(box.right()/GameConstants.TILE_SIZE) + 1);
        int minR = Math.max(0, (int)(box.top()/GameConstants.TILE_SIZE) - 1);
        int maxR = Math.min(H-1, (int)(box.bottom()/GameConstants.TILE_SIZE) + 1);

        boolean inWater = false, onLadder = false;

        for (int r=minR; r<=maxR; r++) {
            for (int c=minC; c<=maxC; c++) {
                TileType t = tiles[r][c];
                float tx = c*GameConstants.TILE_SIZE, ty = r*GameConstants.TILE_SIZE;
                float ts = GameConstants.TILE_SIZE;
                AABB tile = new AABB(tx, ty, ts, ts);

                if (t == TileType.WATER)  { if(box.overlaps(tile)) inWater=true; continue; }
                if (t == TileType.LADDER) { if(box.overlaps(tile)) onLadder=true; continue; }
                if (t == TileType.AIR || t == TileType.SPAWN_BLUE || t == TileType.SPAWN_RED) continue;

                if (t == TileType.PLATFORM) {
                    // Only collide from above
                    if (body.getVel().y >= 0
                     && box.bottom() - body.getVel().y*dt*1.5f <= tile.top()
                     && box.overlaps(tile)) {
                        body.getPos().y = tile.top() - body.getH();
                        body.getVel().y = 0;
                        body.setOnGround(true);
                    }
                    continue;
                }

                if (t == TileType.SOLID && box.overlaps(tile)) {
                    Vec2 sep = box.overlap(tile);
                    body.getPos().x += sep.x;
                    body.getPos().y += sep.y;
                    if (sep.y < 0) { body.getVel().y = Math.min(0, body.getVel().y); body.setOnGround(true); }
                    if (sep.y > 0)   body.getVel().y = Math.max(0, body.getVel().y);
                    if (sep.x != 0)  body.getVel().x = 0;
                    box = body.getBounds(); // refresh after push
                }
            }
        }

        body.setInWater(inWater);
        body.setOnLadder(onLadder);

        // World boundary clamp
        if (body.getPos().x < 0) { body.getPos().x=0; body.getVel().x=0; }
        if (body.getPos().x+body.getW() > W*GameConstants.TILE_SIZE) {
            body.getPos().x = W*GameConstants.TILE_SIZE - body.getW(); body.getVel().x=0;
        }
        if (body.getPos().y+body.getH() > H*GameConstants.TILE_SIZE) {
            body.getPos().y = H*GameConstants.TILE_SIZE - body.getH();
            body.getVel().y = 0; body.setOnGround(true);
        }
    }

    /** Ray-cast from (x1,y1) to (x2,y2). Returns true if solid tile hit. */
    public boolean lineOfSight(float x1, float y1, float x2, float y2) {
        int ts = GameConstants.TILE_SIZE;
        float dx = x2-x1, dy = y2-y1;
        float len = (float)Math.sqrt(dx*dx+dy*dy);
        if (len == 0) return true;
        float sx = dx/len, sy = dy/len;
        float cx = x1, cy = y1;
        for (float d=0; d<len; d+=ts/2f) {
            int tc=(int)(cx/ts), tr=(int)(cy/ts);
            if (tc>=0&&tc<W&&tr>=0&&tr<H && tiles[tr][tc]==TileType.SOLID) return false;
            cx+=sx*ts/2f; cy+=sy*ts/2f;
        }
        return true;
    }

    /** Returns true if the tile at pixel position is solid. */
    public boolean isSolid(float px, float py) {
        int c=(int)(px/GameConstants.TILE_SIZE), r=(int)(py/GameConstants.TILE_SIZE);
        if (c<0||c>=W||r<0||r>=H) return true;
        return tiles[r][c] == TileType.SOLID;
    }

    public TileType getTile(int c, int r){ return (c>=0&&c<W&&r>=0&&r<H)?tiles[r][c]:TileType.AIR; }
    public int getW()                    { return W; }
    public int getH()                    { return H; }
    public String getName()              { return name; }
    public Vec2 getSpawnBlue(int idx)    { return spawnBlue.get(idx % spawnBlue.size()); }
    public Vec2 getSpawnRed(int idx)     { return spawnRed.get(idx  % spawnRed.size()); }
    public int  spawnBlueCount()         { return spawnBlue.size(); }
    public int  spawnRedCount()          { return spawnRed.size(); }
}

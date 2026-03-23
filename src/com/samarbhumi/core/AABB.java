package com.samarbhumi.core;

/** Axis-Aligned Bounding Box. Used for all collision detection. */
public final class AABB {
    public float x, y, w, h;

    public AABB(float x, float y, float w, float h) { this.x=x;this.y=y;this.w=w;this.h=h; }
    public AABB(AABB o) { x=o.x;y=o.y;w=o.w;h=o.h; }

    public float left()   { return x; }
    public float right()  { return x+w; }
    public float top()    { return y; }
    public float bottom() { return y+h; }
    public float cx()     { return x+w/2; }
    public float cy()     { return y+h/2; }

    public boolean overlaps(AABB o) {
        return x < o.x+o.w && x+w > o.x && y < o.y+o.h && y+h > o.y;
    }
    public boolean contains(float px, float py) {
        return px>=x && px<=x+w && py>=y && py<=y+h;
    }
    /** Returns overlap vector: how much `this` must move to separate from `o`. */
    public Vec2 overlap(AABB o) {
        float ox = Math.min(right()-o.left(), o.right()-left());
        float oy = Math.min(bottom()-o.top(), o.bottom()-top());
        if (ox < oy) return new Vec2( (cx()<o.cx()) ? -ox : ox , 0);
        else         return new Vec2(0, (cy()<o.cy()) ? -oy : oy);
    }
    public void set(float x, float y, float w, float h){ this.x=x;this.y=y;this.w=w;this.h=h; }
}

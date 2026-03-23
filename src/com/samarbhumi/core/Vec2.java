package com.samarbhumi.core;

import java.io.Serializable;

/** Mutable 2D float vector. Used everywhere for positions, velocities, forces. */
public final class Vec2 implements Serializable {
    private static final long serialVersionUID = 1L;
    public float x, y;

    public Vec2()                     { x=0; y=0; }
    public Vec2(float x, float y)    { this.x=x; this.y=y; }
    public Vec2(Vec2 o)              { this.x=o.x; this.y=o.y; }

    public Vec2 set(float x, float y){ this.x=x; this.y=y; return this; }
    public Vec2 set(Vec2 o)          { this.x=o.x; this.y=o.y; return this; }
    public Vec2 add(Vec2 o)          { x+=o.x; y+=o.y; return this; }
    public Vec2 add(float x, float y){ this.x+=x; this.y+=y; return this; }
    public Vec2 sub(Vec2 o)          { x-=o.x; y-=o.y; return this; }
    public Vec2 scale(float s)       { x*=s; y*=s; return this; }
    public Vec2 copy()               { return new Vec2(x,y); }
    public float len()               { return (float)Math.sqrt(x*x+y*y); }
    public float len2()              { return x*x+y*y; }
    public float dot(Vec2 o)         { return x*o.x+y*o.y; }
    public float dst(Vec2 o)         { float dx=x-o.x,dy=y-o.y; return (float)Math.sqrt(dx*dx+dy*dy); }
    public float dst2(Vec2 o)        { float dx=x-o.x,dy=y-o.y; return dx*dx+dy*dy; }
    public Vec2 nor()                { float l=len(); if(l>0){x/=l;y/=l;} return this; }
    public boolean isZero()          { return x==0&&y==0; }
    public static Vec2 fromAngle(float angleRad) {
        return new Vec2((float)Math.cos(angleRad),(float)Math.sin(angleRad));
    }
    @Override public String toString(){ return "("+x+","+y+")"; }
}

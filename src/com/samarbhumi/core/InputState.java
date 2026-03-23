package com.samarbhumi.core;

import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Keyboard + mouse state for 2 local players.
 *
 * P1 — ARROW KEYS:
 *   Left/Right = Move       Up = Jump (press again mid-air = double jump)
 *   Down = Crouch           Arrow keys also AIM the gun while held
 *   Num8 = Jetpack          Num5 / Del = Melee
 *   Num0 = Reload           End = Pickup       PgDn = Swap weapon
 *   Enter / Numpad Enter = Shoot (keyboard aim)
 *   Mouse LMB = Fire (mouse aim)
 *
 * P2 — WASD:
 *   A/D = Move              W = Jump (double-tap = double jump)
 *   S = Crouch              E = Jetpack         Q = Melee
 *   R = Reload              F = Pickup          Tab = Swap
 *   LMB = Fire (mouse aim)
 */
public class InputState implements KeyListener, MouseListener, MouseMotionListener {

    private final Set<Integer> held          = new HashSet<>();
    private final Set<Integer> justPressed   = new HashSet<>();
    private final Set<Integer> pressBuffer   = new HashSet<>();
    private final Set<Integer> releaseBuffer = new HashSet<>();

    public volatile float   mouseX, mouseY;
    public volatile boolean mouseLeft, mouseRight;
    public volatile boolean mouseMoved;

    public synchronized void pollFrame() {
        justPressed.clear();
        justPressed.addAll(pressBuffer);
        pressBuffer.clear();
        releaseBuffer.clear();
        mouseMoved = false;
    }

    public boolean held(int k)    { return held.contains(k); }
    public boolean pressed(int k) { return justPressed.contains(k); }

    // ── P1 — Arrow Keys + Numpad (with number-row fallbacks) ────────────
    public boolean p1Left()      { return held(KeyEvent.VK_LEFT); }
    public boolean p1Right()     { return held(KeyEvent.VK_RIGHT); }
    // Up arrow for jump - also Space as alternative
    public boolean p1Jump()      { return pressed(KeyEvent.VK_UP) || pressed(KeyEvent.VK_SPACE); }
    public boolean p1JumpHeld()  { return held(KeyEvent.VK_UP)    || held(KeyEvent.VK_SPACE); }
    public boolean p1Down()      { return held(KeyEvent.VK_DOWN); }
    // Numpad 8 OR keyboard 8 for jetpack
    public boolean p1Jetpack()   { return held(KeyEvent.VK_NUMPAD8) || held(KeyEvent.VK_8); }
    // Left Mouse Button fires (mouse aim). Enter fires (arrow key aim).
    public boolean p1FireMouse() { return mouseLeft; }
    public boolean p1FireKey()   { return held(KeyEvent.VK_ENTER); }
    // SHIFT = Throw Grenade (P1)
    public boolean p1Grenade()   { return pressed(KeyEvent.VK_SHIFT); }
    // Numpad 0 OR keyboard 0 for reload
    public boolean p1Reload()    { return pressed(KeyEvent.VK_NUMPAD0) || pressed(KeyEvent.VK_0); }
    // Numpad 5 OR keyboard 5 for melee
    public boolean p1Melee()     { return pressed(KeyEvent.VK_NUMPAD5) || pressed(KeyEvent.VK_5) || pressed(KeyEvent.VK_DECIMAL); }
    public boolean p1Pickup()    { return pressed(KeyEvent.VK_END) || pressed(KeyEvent.VK_INSERT) || pressed(KeyEvent.VK_9); }
    public boolean p1Swap()      { return pressed(KeyEvent.VK_PAGE_DOWN) || pressed(KeyEvent.VK_7); }

    /** Returns aim angle from held arrow keys, NaN if none held */
    public float p1KeyAimAngle() {
        boolean u = held(KeyEvent.VK_UP), d = held(KeyEvent.VK_DOWN);
        boolean l = held(KeyEvent.VK_LEFT), r = held(KeyEvent.VK_RIGHT);
        if (r && u)  return -(float)Math.PI / 4;
        if (r && d)  return  (float)Math.PI / 4;
        if (l && u)  return -(float)Math.PI * 3/4;
        if (l && d)  return  (float)Math.PI * 3/4;
        if (r)       return  0f;
        if (l)       return  (float)Math.PI;
        if (u)       return -(float)Math.PI / 2;
        if (d)       return  (float)Math.PI / 2;
        return Float.NaN;
    }

    // ── P2 — WASD ─────────────────────────────────────────────────────────
    public boolean p2Left()      { return held(KeyEvent.VK_A); }
    public boolean p2Right()     { return held(KeyEvent.VK_D); }
    public boolean p2Jump()      { return pressed(KeyEvent.VK_W); }
    public boolean p2JumpHeld()  { return held(KeyEvent.VK_W); }
    public boolean p2Down()      { return held(KeyEvent.VK_S); }
    public boolean p2Jetpack()   { return held(KeyEvent.VK_E); }
    // TAB = Shoot  (LMB also fires)
    public boolean p2FireMouse() { return mouseLeft; }
    public boolean p2FireKey()   { return held(KeyEvent.VK_TAB); }
    // CTRL = Throw Grenade
    public boolean p2Grenade()   { return pressed(KeyEvent.VK_CONTROL); }
    public boolean p2Reload()    { return pressed(KeyEvent.VK_R); }
    public boolean p2Melee()     { return pressed(KeyEvent.VK_Q); }
    public boolean p2Pickup()    { return pressed(KeyEvent.VK_F); }
    public boolean p2Swap()      { return pressed(KeyEvent.VK_G); }

    // ── Global ────────────────────────────────────────────────────────────
    public boolean pause() { return pressed(KeyEvent.VK_ESCAPE); }

    // ── Key wiring ────────────────────────────────────────────────────────
    @Override public synchronized void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (!held.contains(k)) pressBuffer.add(k);
        held.add(k);
    }
    @Override public synchronized void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        held.remove(k);
        releaseBuffer.add(k);
    }
    @Override public void keyTyped(KeyEvent e) {}

    @Override public void mouseMoved(MouseEvent e)    { mouseX=e.getX(); mouseY=e.getY(); mouseMoved=true; }
    @Override public void mouseDragged(MouseEvent e)  { mouseX=e.getX(); mouseY=e.getY(); mouseMoved=true; }
    @Override public void mousePressed(MouseEvent e)  {
        if (e.getButton()==MouseEvent.BUTTON1) mouseLeft =true;
        if (e.getButton()==MouseEvent.BUTTON3) mouseRight=true;
    }
    @Override public void mouseReleased(MouseEvent e) {
        if (e.getButton()==MouseEvent.BUTTON1) mouseLeft =false;
        if (e.getButton()==MouseEvent.BUTTON3) mouseRight=false;
    }
    @Override public void mouseClicked(MouseEvent e)  {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}

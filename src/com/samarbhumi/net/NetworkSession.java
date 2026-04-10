package com.samarbhumi.net;

import com.samarbhumi.core.*;
import com.samarbhumi.entity.Player;
import com.samarbhumi.map.GameMap;
import com.samarbhumi.progression.PlayerProfile;
import com.samarbhumi.weapon.Weapon;

import java.util.List;

public class NetworkSession extends GameSession {

    // Track previous masks to detect JUST_PRESSED events for actions
    private int[] prevMasks;
    private int   stuckFrames = 0;
    private static final int MAX_STUCK_FRAMES = 180; // 3 seconds at 60fps

    public NetworkSession(GameMap map, PlayerProfile profile, boolean teamMode) {
        super(map, profile, 0, com.samarbhumi.core.Enums.Difficulty.MEDIUM, false, teamMode);
        this.isOnlineMatch = true;
        
        // Rebuild players list for online play according to lobby size
        List<Player> players = getPlayers();
        players.clear();
        
        int total = NetManager.totalPlayers;
        this.prevMasks = new int[total];

        for (int i=0; i<total; i++) {
            boolean isLocal = (i == NetManager.localPlayerIdx);
            Enums.Team team;
            if (teamMode) {
                // Team Mode: P1/P2 Blue vs P3/P4 Red (even split)
                team = (i < total/2) ? Enums.Team.BLUE : Enums.Team.RED;
            } else {
                // Free for all: only local player is 'Blue' for HUD purposes, everyone else Red
                team = isLocal ? Enums.Team.BLUE : Enums.Team.RED;
            }
            
            com.samarbhumi.core.Vec2 sp = team == Enums.Team.BLUE ? map.getSpawnBlue(i) : map.getSpawnRed(i);
            String cachedName = NetManager.onlineNames.getOrDefault(i, "Player " + (i+1));
            String pName = isLocal ? profile.getPlayerName() : cachedName;
            Player p = new Player(i, pName, team, sp.x, sp.y, isLocal);
            
            if (isLocal) {
                p.setCharacterSkin(profile.getEquippedSkin());
                p.setJetTrail(profile.getEquippedTrail());
                p.setDeathEffect(profile.getEquippedDeath());
                this.humanPlayerIdx = i;
            }
            players.add(p);
        }
    }

    @Override
    public void update(float dt, InputState localInput) {
        int frame = NetManager.currentFrame;
        
        // Ensure connection is still alive
        if (!NetManager.inMatch) {
            matchOver = true;
            return;
        }

        // Only send input once per frame lock. If we are blocked waiting for peers, don't resend!
        // We can just rely on NetManager putting it in local buffer.
        NetManager.NetInput[] localCheck = NetManager.isFrameReady(frame) ? NetManager.consumeFrame(frame) : null;

        if (localCheck == null && NetManager.localPlayerIdx != -1) {
            // We haven't consumed it yet, so meaning frame is not completely ready.
            // But did WE send our input yet? 
            if (com.samarbhumi.net.NetManager.isFrameReady(frame)) {
               // nothing, handled below
            } else {
                // Send our local input for this frame
                Player localP = getPlayers().get(NetManager.localPlayerIdx);
                float keyAim = localInput.p1KeyAimAngle();
                float aimAngle;
                if (localInput.mouseMoved || Float.isNaN(keyAim)) {
                    float camX = getCamX(localP), camY = getCamY(localP);
                    aimAngle = (float) Math.atan2((localInput.mouseY + camY) - localP.cy(), (localInput.mouseX + camX) - localP.cx());
                } else {
                    aimAngle = keyAim;
                }
                
                // Determine if we already sent the input for this frame (NetManager handles caching internally but let's be safe)
                // Actually NetManager.sendLocalInput overwrites the same frame slot if called repeatedly.
                NetManager.sendLocalInput(frame, localInput, aimAngle);
            }
        }

        // Lockstep wait
        if (!NetManager.isFrameReady(frame)) {
            stuckFrames++;
            if (stuckFrames > MAX_STUCK_FRAMES) {
                System.err.println("[NET] Desync/Timeout on frame " + frame + ". Forcing disconnect.");
                NetManager.disconnect();
                matchOver = true;
            }
            return;
        }
        stuckFrames = 0;
        
        NetManager.NetInput[] inputs = localCheck != null ? localCheck : NetManager.consumeFrame(frame);
        if (inputs == null) return; // Should never happen
        
        // Apply inputs to specific players
        for (int i=0; i<inputs.length; i++) {
            if (i >= getPlayers().size()) continue;
            Player p = getPlayers().get(i);
            if (!p.isAlive()) continue;
            
            NetManager.NetInput ni = inputs[i];
            if (ni == null) continue;

            int mask = ni.bitmask;
            int prevMask = prevMasks[i];
            int pressed = mask & ~prevMask;
            
            if ((mask & 1) != 0) p.moveLeft();
            if ((mask & 2) != 0) p.moveRight();
            p.updateJump((mask & 4) != 0);
            p.setCrouch((mask & 8) != 0);
            if ((mask & 16) != 0) p.activateJetpack(dt);
            if ((pressed & 32) != 0) p.triggerReload();
            if ((pressed & 64) != 0) p.swapWeapon();
            if ((pressed & 128) != 0) tryPickup(p);
            if ((pressed & 256) != 0) doMeleeHit(p);
            if ((pressed & 512) != 0) throwGrenade(p);
            
            p.setAimAngle(ni.aimAngle);
            
            if ((mask & 1024) != 0) {
                if (p.tryFire()) {
                    Weapon w = p.getActiveWeapon();
                    if (w != null) fireWeapon(p, w);
                }
            }
            
            prevMasks[i] = mask;
        }
        
        // Advance physics locally for everyone
        super.update(dt, null);
        
        // Advance frame counter
        NetManager.currentFrame++;
    }
}

package com.samarbhumi.net;

import com.samarbhumi.core.GameConstants;
import com.samarbhumi.core.InputState;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.*;

public class NetManager {

    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static Thread listenThread;

    public static String currentLobby = null;
    public static int localPlayerIdx = -1;
    public static int totalPlayers = 1;

    public static volatile boolean inMatch = false;
    public static volatile int currentFrame = 0;

    // Online name syncing via INPUT protocol
    public static final Map<Integer, String> onlineNames = new ConcurrentHashMap<>();

    // Buffer of received inputs: frame -> playerIdx -> NetInput
    // ConcurrentHashMap since listener thread writes, main loop reads
    private static final Map<Integer, NetInput[]> frameInputs = new ConcurrentHashMap<>();

    // Wait mechanism for UI
    public static volatile String lastResponse = null;

    public static class NetInput {
        public int bitmask;
        public float aimAngle;
        public NetInput(int b, float a) { this.bitmask = b; this.aimAngle = a; }
    }

    // Connect to server if not connected
    private static boolean ensureConnected() {
        if (socket != null && !socket.isClosed()) return true;
        try {
            socket = new Socket(GameConstants.NET_HOST, GameConstants.NET_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            listenThread = new Thread(NetManager::listenLoop);
            listenThread.setDaemon(true);
            listenThread.start();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void listenLoop() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("CREATED:")) {
                    currentLobby = line.substring(8).trim();
                    localPlayerIdx = 0; 
                    totalPlayers = 1;
                    lastResponse = "CREATED";
                } else if (line.startsWith("JOINED:")) {
                    String[] parts = line.split(":");
                    currentLobby = parts[1];
                    try { totalPlayers = Integer.parseInt(parts[2]); } catch(Exception ignored){}
                    localPlayerIdx = totalPlayers - 1; 
                    lastResponse = "JOINED";
                } else if (line.startsWith("PLAYER_JOINED:")) {
                    try { totalPlayers = Integer.parseInt(line.split(":")[1]); } catch(Exception ignored){}
                } else if (line.startsWith("START:")) {
                    inMatch = true;
                    currentFrame = 0;
                    frameInputs.clear();
                } else if (line.startsWith("INPUT:NAME:")) {
                    // INPUT:NAME:playerIdx:name
                    String[] p = line.split(":", 4);
                    if (p.length == 4) {
                        try {
                            int pIdx = Integer.parseInt(p[2]);
                            onlineNames.put(pIdx, p[3]);
                        } catch (Exception ignored) {}
                    }
                } else if (line.startsWith("INPUT:")) {
                    // INPUT:code:frame:playerIdx:bitmask:aim
                    String[] p = line.split(":");
                    if (p.length == 6) {
                        try {
                            int frame = Integer.parseInt(p[2]);
                            int pIdx = Integer.parseInt(p[3]);
                            int mask = Integer.parseInt(p[4]);
                            float aim = Float.parseFloat(p[5]);
                            frameInputs.computeIfAbsent(frame, k -> new NetInput[10])[pIdx] = new NetInput(mask, aim);
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            disconnect();
        }
    }

    public static boolean connectAndCreate(String code, String hostName) {
        if (!ensureConnected()) return false;
        lastResponse = null;
        onlineNames.clear();
        out.println("CREATE:" + code);
        long start = System.currentTimeMillis();
        // block briefly for reply
        while (lastResponse == null && System.currentTimeMillis() - start < 3000) {
            try { Thread.sleep(50); } catch (Exception ignored) {}
        }
        if ("CREATED".equals(lastResponse)) {
            onlineNames.put(localPlayerIdx, hostName);
            broadcastName(hostName);
            return true;
        }
        return false;
    }

    public static boolean connectAndJoin(String code, String guestName) {
        if (!ensureConnected()) return false;
        lastResponse = null;
        onlineNames.clear();
        out.println("JOIN:" + code);
        long start = System.currentTimeMillis();
        while (lastResponse == null && System.currentTimeMillis() - start < 3000) {
            try { Thread.sleep(50); } catch (Exception ignored) {}
        }
        if ("JOINED".equals(lastResponse)) {
            onlineNames.put(localPlayerIdx, guestName);
            broadcastName(guestName);
            return true;
        }
        return false;
    }

    public static void broadcastName(String name) {
        if (out != null && currentLobby != null) {
            out.println("INPUT:NAME:" + localPlayerIdx + ":" + name);
        }
    }

    public static void startMatchBroadcast() {
        if (out != null && currentLobby != null) {
            out.println("START:" + currentLobby);
            inMatch = true;
            currentFrame = 0;
            frameInputs.clear();
        }
    }

    public static void sendLocalInput(int frame, InputState input, float mappedAimAngle) {
        if (out == null || currentLobby == null) return;
        int mask = 0;
        if (input.p1Left()) mask |= 1;
        if (input.p1Right()) mask |= 2;
        if (input.p1JumpHeld()) mask |= 4;
        if (input.p1Down()) mask |= 8;
        if (input.p1Jetpack()) mask |= 16;
        if (input.p1Reload()) mask |= 32;
        if (input.p1Swap()) mask |= 64;
        if (input.p1Pickup()) mask |= 128;
        if (input.p1Melee()) mask |= 256;
        if (input.p1Grenade()) mask |= 512;
        if (input.p1FireMouse() || input.p1FireKey()) mask |= 1024;

        // Immediately put local input in our own frame buffer so we don't bounce it to server and back
        frameInputs.computeIfAbsent(frame, k -> new NetInput[10])[localPlayerIdx] = new NetInput(mask, mappedAimAngle);

        // Send to server
        out.println("INPUT:" + frame + ":" + localPlayerIdx + ":" + mask + ":" + mappedAimAngle);
    }

    public static boolean isFrameReady(int frame) {
        NetInput[] arr = frameInputs.get(frame);
        if (arr == null) return false;
        for (int i = 0; i < totalPlayers; i++) {
            if (arr[i] == null) return false;
        }
        return true;
    }

    public static NetInput[] consumeFrame(int frame) {
        return frameInputs.remove(frame);
    }

    public static void disconnect() {
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
        socket = null; out = null; in = null;
        inMatch = false;
        currentLobby = null;
    }
}

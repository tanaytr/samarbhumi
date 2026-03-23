# Samarbhumi — Online Multiplayer Setup
> **v1.1** — Local 2-player and team modes are fully working out of the box.
> Online multiplayer (internet play) requires deploying the relay server described below.

Online multiplayer uses a lightweight **relay server** hosted in the cloud.
Players at different locations join the same lobby by entering a 4-letter code.
No port forwarding or static IP is needed.

---

## How It Works

```
Player A (host) ──┐
Player B        ──┤──► Relay Server (cloud) ──► broadcasts inputs to all
Player C        ──┤                               each client runs same game
Player D        ──┘
```

- The server **does not run the game** — it only forwards player inputs
- Every client runs an identical physics simulation (lockstep model)
- Supports **2–4 players** per lobby
- Playable at up to ~100ms round-trip time (fine for same-country play)

---

## Step 1 — Get a Free Server

### Option A: Railway.app (Recommended — easiest)
1. Go to https://railway.app and sign up (free)
2. Click **New Project → Deploy from GitHub repo**
3. Upload just the `server/` folder (or the whole project)
4. Railway auto-detects Java and deploys it
5. Copy the **public URL** Railway gives you (e.g. `samarbhumi-relay.up.railway.app`)

### Option B: Fly.io
1. Install the Fly CLI: https://fly.io/docs/hands-on/install-flyctl/
2. Run `fly launch` in the `server/` folder
3. Follow prompts — choose the free tier
4. Your URL will be `your-app-name.fly.dev`

### Option C: Any VPS (DigitalOcean, AWS, etc.)
1. Get a server with Java 17+
2. Copy `server/RelayServer.java` to it
3. Compile: `javac RelayServer.java`
4. Run: `java RelayServer &`  (listens on port 7777 by default)
5. Use the server's public IP as the host

---

## Step 2 — Build the Relay Server

The relay server is a single Java file. Create `server/RelayServer.java`:

```java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RelayServer {
    static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "7777"));
    static final Map<String, List<PrintWriter>> lobbies = new ConcurrentHashMap<>();
    static final Map<String, String> clientLobby = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        System.out.println("Samarbhumi Relay Server on port " + PORT);
        ServerSocket ss = new ServerSocket(PORT);
        while (true) {
            Socket client = ss.accept();
            new Thread(() -> handle(client)).start();
        }
    }

    static void handle(Socket sock) {
        try (BufferedReader in  = new BufferedReader(new InputStreamReader(sock.getInputStream()));
             PrintWriter   out = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()), true)) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("CREATE:")) {
                    String code = line.substring(7).trim().toUpperCase();
                    lobbies.computeIfAbsent(code, k -> new CopyOnWriteArrayList<>()).add(out);
                    clientLobby.put(sock.toString(), code);
                    out.println("CREATED:" + code);
                } else if (line.startsWith("JOIN:")) {
                    String code = line.substring(5).trim().toUpperCase();
                    List<PrintWriter> lobby = lobbies.get(code);
                    if (lobby == null) { out.println("ERROR:Lobby not found"); continue; }
                    lobby.add(out);
                    clientLobby.put(sock.toString(), code);
                    out.println("JOINED:" + code + ":" + lobby.size());
                    broadcast(code, "PLAYER_JOINED:" + lobby.size(), out);
                } else if (line.startsWith("INPUT:")) {
                    String code = clientLobby.get(sock.toString());
                    if (code != null) broadcast(code, line, out);
                } else if (line.equals("PING")) {
                    out.println("PONG");
                }
            }
        } catch (IOException ignored) {
        } finally {
            String code = clientLobby.remove(sock.toString());
            if (code != null) {
                List<PrintWriter> lobby = lobbies.get(code);
                if (lobby != null) lobby.removeIf(w -> {
                    try { w.checkError(); return w.checkError(); }
                    catch (Exception e) { return true; }
                });
            }
        }
    }

    static void broadcast(String code, String msg, PrintWriter sender) {
        List<PrintWriter> lobby = lobbies.getOrDefault(code, List.of());
        for (PrintWriter w : lobby) if (w != sender) w.println(msg);
    }
}
```

---

## Step 3 — Configure the Game Client

Open `src/com/samarbhumi/core/GameConstants.java` and update:

```java
// ── Online Multiplayer ──────────────────────────────────────────────────
public static final String NET_HOST = "your-relay-server.up.railway.app";
public static final int    NET_PORT = 7777;
```

Replace `your-relay-server.up.railway.app` with your actual server URL from Step 1.

---

## Step 4 — Playing Online

1. **Host** starts the game → Battle Setup → clicks **ONLINE** tab
2. Host clicks **CREATE LOBBY** → gets a 4-letter code like `KXZR`
3. Host shares the code with friends (Discord, WhatsApp, etc.)
4. **Guests** start the game → Battle Setup → **ONLINE** tab → type the code → click **JOIN**
5. Once all players have joined, host clicks **START BATTLE**

---

## Latency Guide

| Distance | Expected Ping | Experience |
|----------|--------------|------------|
| Same city | < 10ms | Perfect |
| Same country (India) | 20–50ms | Excellent |
| India ↔ SEA | 50–100ms | Good |
| India ↔ Europe | 100–180ms | Playable |
| India ↔ US | 180–280ms | Laggy |

**Tip:** Choose a server region closest to all players.
- India/SEA → Railway Singapore region
- Europe → Railway EU region
- Americas → Railway US region

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| "Lobby not found" | Make sure host created lobby before you tried to join |
| Connection timeout | Check `NET_HOST` in GameConstants.java is correct |
| Rubber-banding | High latency — try a closer server region |
| Players see different things | Rare desync — everyone ESC and start a new match |

---

## Free Tier Limits

- **Railway free tier:** 500 hours/month (enough for ~16 hrs/day of play)
- **Fly.io free tier:** 3 shared-CPU VMs, always free
- The relay server uses almost no CPU or memory — handles dozens of simultaneous lobbies

---

*Full architecture details in `docs/PROJECT_COMPLETE.md`*
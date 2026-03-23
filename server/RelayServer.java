package server;
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
package com.samarbhumi.ui;

import com.samarbhumi.audio.AudioEngine;
import com.samarbhumi.core.GameConstants;
import com.samarbhumi.core.GameSession;
import com.samarbhumi.core.InputState;
import com.samarbhumi.entity.Player;
import com.samarbhumi.map.GameMap;
import com.samarbhumi.map.MapFactory;
import com.samarbhumi.progression.PlayerProfile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferStrategy;
import java.util.List;

/**
 * Main application window and fixed-timestep game loop.
 *
 * Changes v1.1:
 *  - App always starts on MAIN_MENU (home), never on PROFILE_SELECT.
 *  - profile==null means guest mode; PLAY requires a profile (shows popup if none).
 *  - Switch Profile: single-profile case shows sign-out confirmation; never kills app.
 *  - App icon generated programmatically (no missing resource, no red-box).
 *  - Team leaderboard: PostMatchScreen & HUDRenderer show Vajra/Pralay team splits.
 */
public class GameWindow extends JFrame {

    private enum AppState {
        MAIN_MENU, LOBBY, PLAYING, PAUSED, POST_MATCH,
        PROFILE, STORE, SETTINGS,
        PROFILE_SELECT   // only entered when user explicitly signs out
    }
    private AppState state = AppState.MAIN_MENU;

    private final AudioEngine   audio;
    private final InputState    input;
    private PlayerProfile       profile;   // null = guest

    // "No profile loaded" popup
    private boolean showNoProfilePopup  = false;
    private float   noProfilePopupTimer = 0f;

    private final MainMenuScreen  mainMenu   = new MainMenuScreen();
    private final LobbyScreen     lobby      = new LobbyScreen();
    private final ProfileScreen   profileSc  = new ProfileScreen();
    private final StoreScreen     storeSc    = new StoreScreen();
    private final SettingsScreen  settingsSc = new SettingsScreen();
    private final PauseScreen     pauseSc    = new PauseScreen();
    private final PostMatchScreen postMatch  = new PostMatchScreen();

    private final ProfileSelectScreen  profileSelect    = new ProfileSelectScreen();
    private final SwitchProfileOverlay switchProfileOvl = new SwitchProfileOverlay();
    private boolean showSwitchOverlay = false;

    private GameSession  session;
    private GameScreen   gameScreen;
    private int          pendingXP, pendingCoins;
    private List<String> pendingUnlocks;

    private final Canvas  canvas;
    private volatile boolean running = false;

    private volatile int     mouseX = GameConstants.WIN_W / 2;
    private volatile int     mouseY = GameConstants.WIN_H / 2;
    private volatile boolean mouseClicked  = false;
    private volatile boolean mouseDragging = false;

    private volatile int   renderW = GameConstants.WIN_W;
    private volatile int   renderH = GameConstants.WIN_H;
    private volatile float scaleCache   = 1f;
    private volatile int   offsetXCache = 0;
    private volatile int   offsetYCache = 0;

    public GameWindow() {
        super("Samarbhumi \u2014 War Never Ends");
        audio   = new AudioEngine();
        input   = new InputState();
        profile = null;   // start as guest — home page always loads
        profileSelect.refresh();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(true);
        setMinimumSize(new Dimension(800, 500));
        setIconImage(buildAppIcon());

        canvas = new Canvas();
        canvas.setBackground(Color.BLACK);
        canvas.setIgnoreRepaint(true);
        canvas.setFocusable(true);
        getContentPane().add(canvas, BorderLayout.CENTER);

        canvas.addKeyListener(input);
        canvas.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (showSwitchOverlay && switchProfileOvl.isEditing()) {
                    switchProfileOvl.handleKey(e);
                } else if (state == AppState.PROFILE_SELECT && profileSelect.isEditing()) {
                    profileSelect.handleKey(e);
                } else if (state == AppState.LOBBY && lobby.isEditingName()) {
                    lobby.handleKey(e);
                } else if (showSwitchOverlay && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    showSwitchOverlay = false;
                } else if (showNoProfilePopup && e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    showNoProfilePopup = false;
                }
            }
        });
        canvas.addMouseMotionListener(input);
        canvas.addMouseListener(input);

        MouseAdapter uiMouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e)   { mouseX=e.getX(); mouseY=e.getY(); }
            @Override public void mouseDragged(MouseEvent e) { mouseX=e.getX(); mouseY=e.getY(); mouseDragging=true; }
            @Override public void mousePressed(MouseEvent e) {
                mouseX = e.getX(); mouseY = e.getY();
                if (e.getButton() == MouseEvent.BUTTON1) mouseClicked = true;
                canvas.requestFocusInWindow();
            }
        };
        canvas.addMouseListener(uiMouse);
        canvas.addMouseMotionListener(uiMouse);
        addMouseListener(uiMouse);
        addMouseMotionListener(uiMouse);

        canvas.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                renderW = Math.max(1, canvas.getWidth());
                renderH = Math.max(1, canvas.getHeight());
                recalcScale();
                SwingUtilities.invokeLater(() -> {
                    try { canvas.createBufferStrategy(2); }
                    catch (Exception ignored) {}
                });
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                running = false;
                if (profile != null) try { profile.save(); } catch (Exception ignored) {}
                audio.close();
            }
        });

        pack();
        setSize(GameConstants.WIN_W, GameConstants.WIN_H);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Start in full screen
        setVisible(true);
        canvas.createBufferStrategy(2);
        recalcScale();

        SwingUtilities.invokeLater(() ->
            SwingUtilities.invokeLater(() -> canvas.requestFocusInWindow())
        );
    }

    /** Programmatically-drawn app icon — no external file needed. */
    private Image buildAppIcon() {
        int sz = 64;
        BufferedImage img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(12, 22, 6));
        g.fillRoundRect(0, 0, sz, sz, 14, 14);
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        FontMetrics fm = g.getFontMetrics();
        int tx = (sz - fm.stringWidth("S")) / 2;
        int ty = (sz + fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(new Color(220, 175, 30));
        g.drawString("S", tx, ty);
        g.setColor(new Color(70, 130, 35));
        g.setStroke(new BasicStroke(3f));
        g.drawRoundRect(1, 1, sz-3, sz-3, 14, 14);
        g.dispose();
        return img;
    }

    private void recalcScale() {
        float sx = (float) renderW / GameConstants.WIN_W;
        float sy = (float) renderH / GameConstants.WIN_H;
        float s  = Math.min(sx, sy);
        scaleCache   = s;
        offsetXCache = (int)((renderW - GameConstants.WIN_W * s) / 2);
        offsetYCache = (int)((renderH - GameConstants.WIN_H * s) / 2);
    }

    private int toLogicalX(int x) { return (int)((x - offsetXCache) / scaleCache); }
    private int toLogicalY(int y) { return (int)((y - offsetYCache) / scaleCache); }

    public void run() {
        running = true;
        audio.playTheme(AudioEngine.THEME_MENU);

        long  prevTime    = System.nanoTime();
        final float DT    = 1f / GameConstants.TARGET_FPS;
        float accumulator = 0f;

        while (running) {
            long  now   = System.nanoTime();
            float frame = Math.min((now - prevTime) / 1_000_000_000f, 0.05f);
            prevTime    = now;
            accumulator += frame;

            int lmx = toLogicalX(mouseX);
            int lmy = toLogicalY(mouseY);
            input.mouseX = lmx;
            input.mouseY = lmy;

            input.pollFrame();

            boolean clicked  = mouseClicked;  mouseClicked  = false;
            boolean dragged  = mouseDragging; mouseDragging = false;

            while (accumulator >= DT) { update(DT); accumulator -= DT; }

            if (clicked) handleClick(lmx, lmy);
            if (dragged)  handleDrag(lmx, lmy);

            render();

            long sleepNs = GameConstants.FRAME_NS - (System.nanoTime() - now);
            if (sleepNs > 500_000L) {
                try { Thread.sleep(sleepNs / 1_000_000L, (int)(sleepNs % 1_000_000L)); }
                catch (InterruptedException ignored) {}
            }
        }
    }

    private void update(float dt) {
        if (showNoProfilePopup) {
            noProfilePopupTimer -= dt;
            if (noProfilePopupTimer <= 0) showNoProfilePopup = false;
            return;
        }
        if (showSwitchOverlay) { switchProfileOvl.update(dt); return; }
        switch (state) {
            case PROFILE_SELECT -> profileSelect.update(dt);
            case MAIN_MENU  -> mainMenu.update(dt);
            case LOBBY      -> {
                lobby.update(dt);
                // Guest auto-start when host starts
                if (lobby.getMode()==LobbyScreen.GameMode.ONLINE && com.samarbhumi.net.NetManager.inMatch && com.samarbhumi.net.NetManager.localPlayerIdx > 0) {
                    startMatch();
                }
            }
            case PROFILE    -> profileSc.update(dt);
            case STORE      -> storeSc.update(dt);
            case SETTINGS   -> settingsSc.update(dt);
            case POST_MATCH -> postMatch.update(dt);
            case PLAYING -> {
                if (input.pause()) { state = AppState.PAUSED; return; }
                session.update(dt, input);
                if (session.isMatchOver()) endMatch();
            }
            case PAUSED -> { if (input.pause()) state = AppState.PLAYING; }
        }
    }

    private void render() {
        BufferStrategy bs = canvas.getBufferStrategy();
        if (bs == null) return;

        Graphics g = null;
        try { g = bs.getDrawGraphics(); }
        catch (Exception e) { return; }

        try {
            Graphics2D g2 = (Graphics2D) g;
            int cw = renderW, ch = renderH;

            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, cw, ch);

            g2.setColor(new Color(5, 8, 3));
            if (offsetXCache > 0) {
                g2.fillRect(0, 0, offsetXCache, ch);
                g2.fillRect(cw - offsetXCache, 0, offsetXCache + 1, ch);
            }
            if (offsetYCache > 0) {
                g2.fillRect(0, 0, cw, offsetYCache);
                g2.fillRect(0, ch - offsetYCache, cw, offsetYCache + 1);
            }

            g2.translate(offsetXCache, offsetYCache);
            g2.scale(scaleCache, scaleCache);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,     RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            int lmx = toLogicalX(mouseX);
            int lmy = toLogicalY(mouseY);

            switch (state) {
                case PROFILE_SELECT -> profileSelect.render(g2, lmx, lmy);
                case MAIN_MENU  -> mainMenu.render(g2, lmx, lmy, profile);  // profile may be null (guest)
                case LOBBY      -> lobby.render(g2, lmx, lmy);
                case PROFILE    -> { if (profile!=null) profileSc.render(g2, lmx, lmy, profile); }
                case STORE      -> { if (profile!=null) storeSc.render(g2, lmx, lmy, profile); }
                case SETTINGS   -> settingsSc.render(g2, lmx, lmy, audio);
                case POST_MATCH -> { if (session!=null) postMatch.render(g2, lmx, lmy, session.getPlayers(), pendingXP, pendingCoins, profile); }
                case PLAYING    -> { if (gameScreen!=null) gameScreen.render(g2, lmx, lmy); }
                case PAUSED     -> {
                    if (gameScreen!=null) gameScreen.render(g2, lmx, lmy);
                    pauseSc.render(g2, lmx, lmy);
                }
            }

            if (showSwitchOverlay) switchProfileOvl.render(g2, lmx, lmy);
            if (showNoProfilePopup) renderNoProfilePopup(g2, lmx, lmy);

        } catch (Exception ex) {
            System.err.println("[Render] " + ex.getMessage());
        } finally {
            g.dispose();
        }
        if (!bs.contentsLost()) bs.show();
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Overlay popup: user clicked PLAY with no profile loaded.
     * Tells them to use Switch Profile / Settings to load or create one.
     */
    private void renderNoProfilePopup(Graphics2D g, int mx, int my) {
        int W = GameConstants.WIN_W, H = GameConstants.WIN_H, CX = W/2;
        g.setColor(new Color(0,0,0,175));
        g.fillRect(0, 0, W, H);

        int pw = 600, ph = 230, px = CX - pw/2, py = H/2 - ph/2;
        g.setColor(new Color(20, 10, 8, 245));
        g.fillRoundRect(px, py, pw, ph, 14, 14);
        g.setColor(new Color(190, 60, 20, 220));
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(px, py, pw, ph, 14, 14);

        g.setFont(GameConstants.F_SUBHEAD);
        g.setColor(new Color(255, 160, 50));
        UIRenderer.centerText(g, "NO PROFILE LOADED", CX, py + 46);

        g.setFont(GameConstants.F_BODY);
        g.setColor(GameConstants.C_WHITE);
        UIRenderer.centerText(g, "A profile is required to play.", CX, py + 80);
        g.setColor(GameConstants.C_DIM);
        UIRenderer.centerText(g, "Use  [ SWITCH PROFILE ]  on the home screen to load", CX, py + 104);
        UIRenderer.centerText(g, "an existing profile, or create a new one.", CX, py + 122);

        UIRenderer.button(g, "[ OK ]", CX-70, py + ph - 60, 140, 40, mx, my, false, new Color(110, 35, 15));
    }

    private void handleClick(int mx, int my) {
        audio.sfxMenuClick();

        // Dismiss no-profile popup
        if (showNoProfilePopup) {
            int W = GameConstants.WIN_W, H = GameConstants.WIN_H, CX = W/2;
            int pw = 600, ph = 230, px = CX - pw/2, py = H/2 - ph/2;
            if (new Rectangle(CX-70, py+ph-60, 140, 40).contains(mx,my) ||
                mx < px || mx > px+pw || my < py || my > py+ph) {
                showNoProfilePopup = false;
            }
            return;
        }

        // Switch-profile overlay
        if (showSwitchOverlay) {
            switch (switchProfileOvl.handleClick(mx, my)) {
                case SWITCHED -> {
                    String chosen = switchProfileOvl.getPendingSwitch();
                    if (!chosen.isEmpty()) {
                        if (profile != null) try { profile.save(); } catch (Exception ignored) {}
                        profile = PlayerProfile.loadOrCreate(chosen);
                        try { profile.save(); } catch (Exception ignored) {}
                    }
                    showSwitchOverlay = false;
                }
                case SIGN_OUT -> {
                    if (profile != null) try { profile.save(); } catch (Exception ignored) {}
                    profile = null;
                    showSwitchOverlay = false;
                    // Stay on main menu as guest — no forced PROFILE_SELECT screen
                }
                case CANCEL -> showSwitchOverlay = false;
                default -> {}
            }
            return;
        }

        switch (state) {
            case PROFILE_SELECT -> {
                switch (profileSelect.handleClick(mx, my)) {
                    case LOAD, CREATE -> {
                        String name = profileSelect.getSelectedName();
                        if (!name.isEmpty()) {
                            profile = PlayerProfile.loadOrCreate(name);
                            try { profile.save(); } catch (Exception ignored) {}
                            state = AppState.MAIN_MENU;
                            audio.playTheme(AudioEngine.THEME_MENU);
                        }
                    }
                    default -> {}
                }
            }
            case MAIN_MENU -> {
                switch (mainMenu.handleClick(mx, my)) {
                    case PLAY -> {
                        if (profile == null) {
                            showNoProfilePopup = true;
                            noProfilePopupTimer = 15f;
                        } else {
                            lobby.setNameDefault(profile.getPlayerName());
                            state = AppState.LOBBY;
                        }
                    }
                    case PROFILE  -> { if (profile != null) state = AppState.PROFILE; }
                    case STORE    -> { if (profile != null) state = AppState.STORE;   }
                    case SETTINGS -> state = AppState.SETTINGS;
                    case QUIT     -> {
                        if (profile != null) try { profile.save(); } catch (Exception ignored) {}
                        System.exit(0);
                    }
                    case SWITCH_PROFILE -> {
                        switchProfileOvl.open(profile != null ? profile.getPlayerName() : "");
                        showSwitchOverlay = true;
                    }
                    default -> {}
                }
            }
            case LOBBY -> {
                switch (lobby.handleClick(mx, my)) {
                    case START -> startMatch();
                    case BACK  -> { 
                        state = AppState.MAIN_MENU; 
                        audio.playTheme(AudioEngine.THEME_MENU); 
                        com.samarbhumi.net.NetManager.disconnect();
                    }
                    default -> {}
                }
            }
            case PROFILE    -> { if (profileSc.handleClick(mx,my)==ProfileScreen.Action.BACK) state=AppState.MAIN_MENU; }
            case STORE -> {
                StoreScreen.Action a = storeSc.handleClick(mx,my,profile);
                if (a==StoreScreen.Action.BACK) state=AppState.MAIN_MENU;
                if (a==StoreScreen.Action.BOUGHT){ audio.sfxPickup(); try{profile.save();}catch(Exception ignored){} }
            }
            case SETTINGS   -> { if (settingsSc.handleClick(mx,my)==SettingsScreen.Action.BACK) state=AppState.MAIN_MENU; }
            case PAUSED -> {
                switch (pauseSc.handleClick(mx,my)) {
                    case RESUME    -> state = AppState.PLAYING;
                    case MAIN_MENU -> { 
                        state = AppState.MAIN_MENU; 
                        session = null; 
                        audio.playTheme(AudioEngine.THEME_MENU); 
                        com.samarbhumi.net.NetManager.disconnect();
                    }
                    default -> {}
                }
            }
            case POST_MATCH -> {
                switch (postMatch.handleClick(mx,my)) {
                    case PLAY_AGAIN -> { state=AppState.LOBBY; audio.playTheme(AudioEngine.THEME_BATTLE); }
                    case MAIN_MENU  -> { 
                        state = AppState.MAIN_MENU; 
                        audio.playTheme(AudioEngine.THEME_MENU); 
                        com.samarbhumi.net.NetManager.disconnect();
                    }
                    default -> {}
                }
            }
            default -> {}
        }
    }

    private void handleDrag(int mx, int my) {
        if (state == AppState.SETTINGS) settingsSc.handleDrag(mx, my, audio);
    }

    private void startMatch() {
        String nameInput = lobby.getNameInput();
        if (profile != null && !nameInput.isEmpty()) profile.setPlayerName(nameInput);

        GameMap map = MapFactory.create(lobby.getSelectedMap());
        if (lobby.getMode() == LobbyScreen.GameMode.ONLINE) {
            session = new com.samarbhumi.net.NetworkSession(map, profile, lobby.isTeamMode());
        } else {
            session = new GameSession(map, profile, lobby.getNumBots(), lobby.getDifficulty(),
                                     lobby.isTwoPlayer(), lobby.isTeamMode());
        }
        gameScreen  = new GameScreen(session);
        state       = AppState.PLAYING;
        audio.playTheme(AudioEngine.THEME_BATTLE);
    }

    private void endMatch() {
        Player  human = session.getHumanPlayer();
        boolean won   = session.getWinner() == human;
        pendingXP     = human.getXpEarned() + (won ? GameConstants.XP_PER_WIN : 0);
        pendingCoins  = human.getCoinsEarned();
        if (profile != null) {
            pendingUnlocks = profile.addMatchResult(human.getKills(), human.getDeaths(), won, pendingCoins);
        } else {
            pendingUnlocks = java.util.Collections.emptyList();
        }
        postMatch.setUnlocks(pendingUnlocks);
        postMatch.setTeamMode(lobby.isTeamMode());
        if (!pendingUnlocks.isEmpty()) audio.sfxLevelUp();
        if (profile != null) try { profile.save(); } catch (Exception ignored) {}
        audio.playTheme(won ? AudioEngine.THEME_VICTORY : AudioEngine.THEME_GAMEOVER);
        state = AppState.POST_MATCH;
    }
}
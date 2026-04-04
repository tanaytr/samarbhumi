import com.samarbhumi.ui.GameWindow;
import javax.swing.*;

/**
 * Samarbhumi - War Never Ends
 * Entry point.
 */
public class Main {
    public static void main(String[] args) {
        // Performance: start hardware acceleration immediately
        System.setProperty("sun.java2d.accthreshold", "0");

        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            System.setProperty("sun.java2d.opengl", "true");
        }

        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext",               "true");

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}
            GameWindow window = new GameWindow();
            new Thread(() -> window.run(), "GameLoop").start();
        });
    }
}

import com.samarbhumi.ui.GameWindow;
import javax.swing.*;

/**
 * Samarbhumi - War Never Ends
 * Entry point.
 */
public class Main {
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext",               "true");
        System.setProperty("sun.java2d.accthreshold",    "0");

        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) {}
            GameWindow window = new GameWindow();
            new Thread(() -> window.run(), "GameLoop").start();
        });
    }
}

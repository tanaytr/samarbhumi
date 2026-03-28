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
        if (os.contains("win")) {
            // Windows: Direct3D is the most stable high-performance pipeline
            System.setProperty("sun.java2d.d3d", "true");
            System.setProperty("sun.java2d.noddraw", "true");
        } else if (os.contains("mac")) {
            System.setProperty("sun.java2d.opengl", "true");
        } else {
            // Linux: Use OpenGL but with the common pmoffscreen stability fix
            System.setProperty("sun.java2d.opengl", "true");
            System.setProperty("sun.java2d.pmoffscreen", "false");
            System.setProperty("sun.java2d.xrender", "true");
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

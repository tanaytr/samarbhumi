import com.samarbhumi.progression.PlayerProfile;
import java.io.File;

public class TestSave {
    public static void main(String[] args) {
        System.out.println("Starting test...");
        PlayerProfile p = new PlayerProfile("TestPlayer");
        try {
            p.save();
            System.out.println("Saved successfully to " + p.getSavePath());
            System.out.println("Files in saves/ : ");
            File dir = new File("saves/");
            if (dir.exists() && dir.listFiles() != null) {
                for (File f : dir.listFiles()) {
                    System.out.println(" - " + f.getName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

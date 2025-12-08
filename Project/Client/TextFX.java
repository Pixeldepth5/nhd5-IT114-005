// UCID: nhd5
// Date: December 8, 2025
// Description: TextFX – helper for custom fonts (Behind The Nineties + Visby CF)
// References:
//  - https://www.w3schools.com/java/java_methods.asp
//  - https://www.w3schools.com/java/java_files_read.asp (reading from file)

package Client;

import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.io.FileInputStream;

public class TextFX {

    // NOTE: these are the exact font file locations on your Mac
    private static final String NINETIES_PATH =
            "/Users/nilka/Library/Fonts/Behind The Nineties Bold.ttf";
    private static final String VISBY_PATH =
            "/Users/nilka/Library/Fonts/VisbyCF-Regular.otf";

    private static Font titleFont;
    private static Font subtitleFont;

    static {
        loadFonts();
    }

    // Load fonts from files, fall back to default if anything fails
    private static void loadFonts() {
        try {
            // Behind The Nineties for title
            File nFile = new File(NINETIES_PATH);
            if (nFile.exists()) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(nFile));
                titleFont = f.deriveFont(28f);
            }
        } catch (Exception ignored) { }

        try {
            // Visby CF for subtitles / body
            File vFile = new File(VISBY_PATH);
            if (vFile.exists()) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, new FileInputStream(vFile));
                subtitleFont = f.deriveFont(14f);
            }
        } catch (Exception ignored) { }

        // Fallbacks if fonts didn’t load
        if (titleFont == null) {
            titleFont = new Font("SansSerif", Font.BOLD, 28);
        }
        if (subtitleFont == null) {
            subtitleFont = new Font("SansSerif", Font.PLAIN, 14);
        }
    }

    // ===== Public helpers =====

    // Apply Behind The Nineties title font
    public static void setTitleFont(Component comp) {
        if (comp != null) {
            comp.setFont(titleFont);
        }
    }

    // Apply Visby CF to any component (subtitle / body)
    public static void setSubtitleFont(Component comp) {
        if (comp != null) {
            comp.setFont(subtitleFont);
        }
    }

    // Used by TitledBorder so all panels share the same subtitle font
    public static Font getSubtitleFont() {
        return subtitleFont;
    }
}

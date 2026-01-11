// UCID: nhd5
// Date: December 8, 2025
// Description: TextFX – safely loads custom fonts (Behind The Nineties + Visby CF).
//              Falls back to standard system fonts if custom fonts are unavailable.
//              Searches system fonts first, then local fonts folder.
// References:
//   - W3Schools: https://www.w3schools.com/java/java_files.asp

package Client;

import java.awt.*;
import java.io.File;

public class TextFX {

  // Static fonts loaded once at startup
  private static Font titleFont = null;
  private static Font subtitleFont = null;

  // Static block runs when class is first loaded - attempts to load custom fonts
  static {
      titleFont = tryLoadFont("Behind the Nineties", 36);
      subtitleFont = tryLoadFont("Visby CF", 16);

      // Fallback to standard fonts if custom fonts not found
      if (titleFont == null)
          titleFont = new Font("Serif", Font.BOLD, 36);

      if (subtitleFont == null)
          subtitleFont = new Font("SansSerif", Font.PLAIN, 16);
  }

  // Applies the title font to a component
  public static void setTitleFont(Component c) {
      c.setFont(titleFont);
  }

  // Returns the subtitle font (for use in creating derived fonts)
  public static Font getSubtitleFont() {
      return subtitleFont;
  }

  // Applies the subtitle font to a component
  public static void setSubtitleFont(Component c) {
      c.setFont(subtitleFont);
  }

  /**
   * Attempts to load a font by name from system fonts or local fonts folder.
   * Searches system fonts first, then checks ./fonts/ directory for .ttf/.otf files.
   * @param name Font name to search for (e.g., "Behind the Nineties", "Visby CF")
   * @param size Font size to apply
   * @return Loaded Font object, or null if not found
   */
  private static Font tryLoadFont(String name, int size) {
      try {
          GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

          // Step 1: Try to find in installed system fonts
          for (Font f : ge.getAllFonts()) {
              if (f.getName().equalsIgnoreCase(name) ||
                  f.getFontName().equalsIgnoreCase(name)) {
                  return f.deriveFont(Font.PLAIN, size);
              }
          }

          // Step 2: Try loading from fonts folder (project root)
          File dir = new File("fonts");
          if (!dir.exists()) {
              // Try relative to project root
              dir = new File("./fonts");
          }
          if (dir.exists() && dir.isDirectory()) {
              // Filter for font files (.ttf or .otf)
              File[] files = dir.listFiles((d, fileName) -> {
                  String lower = fileName.toLowerCase();
                  return lower.endsWith(".ttf") || lower.endsWith(".otf");
              });
              if (files != null) {
                  // Search through font files for matching name
                  for (File f : files) {
                      String fileName = f.getName().toLowerCase();
                      // Match "Behind the Nineties" font
                      if (name.toLowerCase().contains("nineties") && 
                          (fileName.contains("nineties") || fileName.contains("behind"))) {
                          Font loaded = Font.createFont(Font.TRUETYPE_FONT, f);
                          ge.registerFont(loaded);
                          return loaded.deriveFont(Font.PLAIN, size);
                      }
                      // Match "Visby CF" font
                      if (name.toLowerCase().contains("visby") && 
                          fileName.contains("visby")) {
                          Font loaded = Font.createFont(Font.TRUETYPE_FONT, f);
                          ge.registerFont(loaded);
                          return loaded.deriveFont(Font.PLAIN, size);
                      }
                  }
              }
          }
      } catch (Exception ignored) {
          // If font loading fails, return null (fallback fonts will be used)
      }
      return null;
  }
}

package Client;

import java.awt.*;
import java.io.File;

/**

* Safely loads fonts (Behind The Nineties + Visby CF).
* Falls back to standard fonts if unavailable.
  */
  public class TextFX {

  private static Font titleFont = null;
  private static Font subtitleFont = null;

  static {
  titleFont = tryLoadFont("Behind the Nineties", 36);
  subtitleFont = tryLoadFont("Visby CF", 16);

  if (titleFont == null)
       titleFont = new Font("Serif", Font.BOLD, 36);

  if (subtitleFont == null)
      subtitleFont = new Font("SansSerif", Font.PLAIN, 16);
  }

  public static void setTitleFont(Component c) {
  c.setFont(titleFont);
  }

  public static Font getSubtitleFont() {
  return subtitleFont;
  }

  public static void setSubtitleFont(Component c) {
  c.setFont(subtitleFont);
  }

  // Attempts to load font by name from system or ./fonts/
  private static Font tryLoadFont(String name, int size) {
  try {
  GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

  // 1. Try to find in installed system fonts
       for (Font f : ge.getAllFonts()) {
           if (f.getName().equalsIgnoreCase(name) ||
               f.getFontName().equalsIgnoreCase(name)) {
               return f.deriveFont(Font.PLAIN, size);
           }
       }

       // 2. Try loading from fonts folder (project root)
       File dir = new File("fonts");
       if (!dir.exists()) {
           // Try relative to project root
           dir = new File("./fonts");
       }
       if (dir.exists() && dir.isDirectory()) {
           File[] files = dir.listFiles((d, fileName) -> {
               String lower = fileName.toLowerCase();
               return lower.endsWith(".ttf") || lower.endsWith(".otf");
           });
           if (files != null) {
               for (File f : files) {
                   String fileName = f.getName().toLowerCase();
                   if (name.toLowerCase().contains("nineties") && 
                       (fileName.contains("nineties") || fileName.contains("behind"))) {
                       Font loaded = Font.createFont(Font.TRUETYPE_FONT, f);
                       ge.registerFont(loaded);
                       return loaded.deriveFont(Font.PLAIN, size);
                   }
                   if (name.toLowerCase().contains("visby") && 
                       fileName.contains("visby")) {
                       Font loaded = Font.createFont(Font.TRUETYPE_FONT, f);
                       ge.registerFont(loaded);
                       return loaded.deriveFont(Font.PLAIN, size);
                   }
               }
           }
       }
  } catch (Exception ignored) {}
  return null;
  }
  }

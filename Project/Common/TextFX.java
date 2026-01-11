package Common;

/**
 * Placeholder for text coloring utilities.
 * Colorize simply returns the original string.
 */
public class TextFX {
    public enum Color {
        RED, YELLOW, PURPLE, CYAN, BLUE, GREEN
    }

    public static String colorize(String message, Color color) {
        return message;
    }
}

// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame TextFX – adds simple colored text output
// Reference: https://www.w3schools.com/java/

package Client;

public abstract class TextFX {
    public enum Color {
        RESET("\033[0m"),
        RED("\033[0;31m"),
        GREEN("\033[0;32m"),
        YELLOW("\033[0;33m"),
        BLUE("\033[0;34m"),
        PURPLE("\033[0;35m"),
        CYAN("\033[0;36m"),
        WHITE("\033[0;37m");

        private final String code;

        Color(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }

    public static String colorize(String text, Color color) {
        return color.getCode() + text + Color.RESET.getCode();
    }
}

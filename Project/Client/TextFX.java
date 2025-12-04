// UCID: nhd5
// Date: December 3, 2025
// Description: Custom TextFX class for managing fonts and text effects in UI.

package Client;

import java.awt.*;
import javax.swing.*;

public class TextFX {
    // Reference: W3Schools - Java Text Effects

    public static void setVisbyBold(JLabel label) {
        label.setFont(new Font("Visby Bold", Font.PLAIN, 24));  // Set font for title
    }

    public static void setNineities(JLabel label) {
        label.setFont(new Font("Nineities", Font.PLAIN, 18));  // Set font for subtitles
    }
}

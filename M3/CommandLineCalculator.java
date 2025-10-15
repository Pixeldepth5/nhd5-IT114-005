package M3;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/*
Challenge 1: Command-Line Calculator
------------------------------------
- Accept two numbers and an operator as command-line arguments
- Supports addition (+) and subtraction (-)
- Allow integer and floating-point numbers
- Ensures correct decimal places in output based on input (e.g., 0.1 + 0.2 → 1 decimal place)
- Display an error for invalid inputs or unsupported operators
- Capture 5 variations of tests
*/

// sources I used  - / - Java methods & main basics: https://www.w3schools.com/java/java_methods.asp
// - try/catch exceptions:       https://www.w3schools.com/java/java_try_catch.asp
// - BigDecimal intro:           https://www.w3schools.com/java/java_bigdecimal.asp
// - Strings (parsing text):     https://www.w3schools.com/java/java_strings.asp
// - Formatting numbers:         https://www.w3schools.com/java/java_format_numbers.asp

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "nhd5"; // <-- change to your ucid

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

        try {
            System.out.println("Calculating result...");
            // extract the equation (format is <num1> <operator> <num2>)
            String leftText  = args[0]; // first number as text
            String opText    = args[1]; // + or -
            String rightText = args[2]; // second number as text
            // check if operator is addition or subtraction
              if (!(opText.equals("+") || opText.equals("-"))) {
                System.out.println("Invalid operator. Use + or - only.");
                printFooter(ucid, 1);
                return;
            }
            // check the type of each number and choose appropriate parsing
            BigDecimal left  = new BigDecimal(leftText);
            BigDecimal right = new BigDecimal(rightText);


            // generate the equation result (Important: ensure decimals display as the
            int leftDecimals  = countDecimals(leftText);  
            int rightDecimals = countDecimals(rightText);  
            int maxDecimals   = Math.max(leftDecimals, rightDecimals);
            // longest decimal passed)
             BigDecimal ans;
            if (opText.equals("+")) {
                ans = left.add(right);
            } else {
                ans = left.subtract(right);
            }
            // i.e., 0.1 + 0.2 would show as one decimal place (0.3), 0.11 + 0.2 would shows
            String pattern = (maxDecimals == 0) ? "0" : ("0." + "0".repeat(maxDecimals));
            DecimalFormat df = new DecimalFormat(pattern);
            df.setRoundingMode(RoundingMode.HALF_UP);

            String formatted = df.format(ans);

            // as two (0.31), etc
            System.out.println("The answer is " + formatted);



        } catch (Exception e) {
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }
}

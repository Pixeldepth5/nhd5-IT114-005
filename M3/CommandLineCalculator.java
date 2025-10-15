package M3;

import java.math.BigDecimal;      // nhd5 / Nilkanth Dhariya / 10/12/25
import java.text.DecimalFormat;   // nhd5 / Nilkanth Dhariya / 10/12/25

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

/*
  SOURCES I looked at (W3Schools – URLs my professor can check):
  - Java methods & main:  https://www.w3schools.com/java/java_methods.asp
  - try/catch basics:     https://www.w3schools.com/java/java_try_catch.asp
  - BigDecimal intro:     https://www.w3schools.com/java/java_bigdecimal.asp
  - Strings (indexOf…):   https://www.w3schools.com/java/java_strings.asp
  - Number formatting:    https://www.w3schools.com/java/java_format_numbers.asp
*/

public class CommandLineCalculator extends BaseClass {
    private static String ucid = "nhd5"; // my UCID (nhd5)

    public static void main(String[] args) {
        printHeader(ucid, 1, "Objective: Implement a calculator using command-line arguments.");

        // I need exactly 3 things: number, operator, number
        if (args.length != 3) {
            System.out.println("Usage: java M3.CommandLineCalculator <num1> <operator> <num2>");
            printFooter(ucid, 1);
            return;
        }

        try {
            System.out.println("Calculating result...");

            // get the three parts from command line
            String leftText = args[0];
            String opText = args[1];
            String rightText = args[2];

            // only + or - are allowed
            if (!opText.equals("+") && !opText.equals("-")) {
                System.out.println("Invalid operator. Use + or - only.");
                printFooter(ucid, 1);
                return;
            }

            // make numbers from the text (BigDecimal keeps decimals accurate)
            BigDecimal left = new BigDecimal(leftText);
            BigDecimal right = new BigDecimal(rightText);

            // figure out how many decimals each input has (beginner way — just count after '.')
            int leftDecimals = 0;
            int dotL = leftText.indexOf('.');
            if (dotL != -1) {
                int end = leftText.length();
                int e1 = leftText.indexOf('e');
                int e2 = leftText.indexOf('E');
                int ePos = -1;
                if (e1 != -1 && e2 != -1) {
                    ePos = (e1 < e2) ? e1 : e2;
                } else if (e1 != -1) {
                    ePos = e1;
                } else if (e2 != -1) {
                    ePos = e2;
                }
                if (ePos != -1) {
                    end = ePos;
                }
                leftDecimals = end - dotL - 1;
                if (leftDecimals < 0) leftDecimals = 0;
            }

            int rightDecimals = 0;
            int dotR = rightText.indexOf('.');
            if (dotR != -1) {
                int end = rightText.length();
                int e1 = rightText.indexOf('e');
                int e2 = rightText.indexOf('E');
                int ePos = -1;
                if (e1 != -1 && e2 != -1) {
                    ePos = (e1 < e2) ? e1 : e2;
                } else if (e1 != -1) {
                    ePos = e1;
                } else if (e2 != -1) {
                    ePos = e2;
                }
                if (ePos != -1) {
                    end = ePos;
                }
                rightDecimals = end - dotR - 1;
                if (rightDecimals < 0) rightDecimals = 0;
            }

            int maxDecimals = leftDecimals;
            if (rightDecimals > maxDecimals) {
                maxDecimals = rightDecimals;
            }

            // do the math
            BigDecimal answer;
            if (opText.equals("+")) {
                answer = left.add(right);
            } else {
                answer = left.subtract(right);
            }

            // build a simple pattern like "0" or "0.0" or "0.00" (beginner loop, no fancy repeat)
            String pattern;
            if (maxDecimals == 0) {
                pattern = "0";
            } else {
                String dots = "0.";
                int i = 0;
                while (i < maxDecimals) {
                    dots = dots + "0";
                    i = i + 1;
                }
                pattern = dots;
            }

            // format the answer with that many decimals
            DecimalFormat df = new DecimalFormat(pattern);
            String formatted = df.format(answer);

            // match the screenshot style
            System.out.println("The answer is " + formatted);

        } catch (Exception e) {
            // any error: wrong numbers, wrong format, etc.
            System.out.println("Invalid input. Please ensure correct format and valid numbers.");
        }

        printFooter(ucid, 1);
    }
}

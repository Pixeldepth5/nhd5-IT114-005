package M3;

/*
Challenge 2: Simple Slash Command Handler
-----------------------------------------
- Accept user input as slash commands
  - "/greet <name>" → Prints "Hello, <name>!"
  - "/roll <num>d<sides>" → Roll <num> dice with <sides> and returns a single outcome as "Rolled <num>d<sides> and got <result>!"
  - "/echo <message>" → Prints the message back
  - "/quit" → Exits the program
- Commands are case-insensitive
- Print an error for unrecognized commands
- Print errors for invalid command formats (when applicable)
- Capture 3 variations of each command except "/quit"
*/

/* SOURCES I looked at (W3Schools – short + beginner friendly)
   - Scanner (user input): https://www.w3schools.com/java/java_user_input.asp
   - Strings (split/trim): https://www.w3schools.com/java/java_strings.asp
   - Math.random():        https://www.w3schools.com/java/java_math.asp
*/

import java.util.Scanner;

public class SlashCommandHandler extends BaseClass {
    private static String ucid = "nhd5"; // nhd5 / Nilkanth Dhariya / 10/12/25

    public static void main(String[] args) {
        printHeader(ucid, 2, "Objective: Implement a simple slash command parser.");

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Enter command: ");
            String input = scanner.nextLine().trim();  // read line from user

            if (input.length() == 0) {
                System.out.println("Error: empty command. Try /greet, /roll, /echo, or /quit.");
                continue;
            }

            // split into command and the rest (case-insensitive command)
            String[] parts = input.split("\\s+", 2);
            String cmd = parts[0].toLowerCase();
            String rest = (parts.length > 1) ? parts[1].trim() : "";

            // ---------------- /quit ----------------
            if (cmd.equals("/quit")) {
                System.out.println("Goodbye!");
                break;
            }

            // ---------------- /greet <name> ----------------
            else if (cmd.equals("/greet")) {
                if (rest.length() == 0) {
                    System.out.println("Error: Usage is /greet <name>");
                } else {
                    System.out.println("Hello, " + rest + "!");
                }
            }

            // ---------------- /echo <message> ----------------
            else if (cmd.equals("/echo")) {
                if (rest.length() == 0) {
                    System.out.println("Error: Usage is /echo <message>");
                } else {
                    System.out.println(rest);
                }
            }

            // ---------------- /roll <num>d<sides> ----------------
            else if (cmd.equals("/roll")) {
                // format must look like: 2d6 (or 3D20)
                if (rest.length() == 0) {
                    System.out.println("Error: Usage is /roll <num>d<sides>  (e.g., /roll 2d6)");
                } else {
                    // find 'd' (or 'D')
                    int dPos = rest.indexOf('d');
                    if (dPos == -1) dPos = rest.indexOf('D');

                    if (dPos == -1) {
                        System.out.println("Error: bad format. Try /roll 2d6");
                    } else {
                        String numStr = rest.substring(0, dPos).trim();
                        String sidesStr = rest.substring(dPos + 1).trim();

                        // try to parse integers
                        int num = -1;
                        int sides = -1;
                        try {
                            num = Integer.parseInt(numStr);
                            sides = Integer.parseInt(sidesStr);
                        } catch (Exception e) {
                            // leave as -1
                        }

                        if (num < 1 || sides < 1) {
                            System.out.println("Error: both <num> and <sides> must be positive integers.");
                        } else {
                            // roll once per die and sum them (beginner loop)
                            int total = 0;
                            int i = 0;
                            while (i < num) {
                                // Math.random() gives 0.0 to <1.0, so convert to 1..sides
                                int one = (int)(Math.random() * sides) + 1;
                                total = total + one;
                                i = i + 1;
                            }
                            System.out.println("Rolled " + num + "d" + sides + " and got " + total + "!");
                        }
                    }
                }
            }

            // ---------------- unknown command ----------------
            else {
                System.out.println("Unhandled command");
            }
        }

        printFooter(ucid, 2);
        scanner.close();
    }
}


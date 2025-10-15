package M3;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/*
Challenge 3: Mad Libs Generator (Randomized Stories)
-----------------------------------------------------
- Load a **random** story from the "stories" folder
- Extract **each line** into a collection (i.e., ArrayList)
- Prompts user for each placeholder (i.e., <adjective>) 
    - Any word the user types is acceptable, no need to verify if it matches the placeholder type
    - Any placeholder with underscores should display with spaces instead
- Replace placeholders with user input (assign back to original slot in collection)
*/

/* SOURCES I looked at (W3Schools – URLs my professor can check):
   - Scanner (user input):  https://www.w3schools.com/java/java_user_input.asp
   - File basics:           https://www.w3schools.com/java/java_files.asp
   - Strings (indexOf...):  https://www.w3schools.com/java/java_strings.asp
*/

public class MadLibsGenerator extends BaseClass {
    private static final String STORIES_FOLDER = "M3/stories";
    private static String ucid = "nhd5"; // nhd5 / Nilkanth Dhariya / 10/12/25

    public static void main(String[] args) {
        printHeader(ucid, 3,
            "Objective: Implement a Mad Libs generator that replaces placeholders dynamically.");

        Scanner scanner = new Scanner(System.in);
        File folder = new File(STORIES_FOLDER);

        List<String> lines = new ArrayList<>();

        // --- Step 1: Build a small pool of built-in stories from the text I was given (fallback) ---
        List<List<String>> builtInStories = new ArrayList<>();

        // Story 1
        List<String> s1 = new ArrayList<>();
        s1.add("Today, I went to the zoo and saw a <adjective> <animal>.");
        s1.add("It was <verb_ending_in_ing> near the <place>.");
        s1.add("The zookeeper said it loved eating <food> and sleeping under a <object>.");
        s1.add("Before I left, I bought a <adjective> <souvenir> from the gift shop!");
        builtInStories.add(s1);

        // Story 2
        List<String> s2 = new ArrayList<>();
        s2.add("While hiking in the <adjective> mountains, I discovered a <adjective> cave.");
        s2.add("Inside, I found a <adjective> <creature> guarding a <object>.");
        s2.add("It looked at me and <verb_past_tense> loudly.");
        s2.add("I quickly grabbed my <object> and <verb_past_tense> out of there!");
        s2.add("That was the most <adjective> adventure of my life.");
        builtInStories.add(s2);

        // Story 3
        List<String> s3 = new ArrayList<>();
        s3.add("My best friend invented a <adjective> machine that can <verb>.");
        s3.add("All you need is a <object>, and it will turn it into a <adjective> <object>.");
        s3.add("Yesterday, I put a <food> in it, and out came a <adjective> <animal>!");
        s3.add("Now, everyone calls me the <adjective> scientist!");
        builtInStories.add(s3);

        // Story 4
        List<String> s4 = new ArrayList<>();
        s4.add("I was traveling through space in my <adjective> spaceship when I landed on <planet>.");
        s4.add("The aliens there were <adjective> and <verb_ending_in_ing> around a <object>.");
        s4.add("One of them handed me a <adjective> <object> and said, \"<gibberish_phrase>!\"");
        s4.add("I had no idea what it meant, but I took it and <verb_past_tense> back to my spaceship.");
        builtInStories.add(s4);

        // Story 5
        List<String> s5 = new ArrayList<>();
        s5.add("A <adjective> witch gave me a potion that would make me <adjective>.");
        s5.add("She told me to drink it while standing on a <object> under the <adjective> moon.");
        s5.add("As soon as I drank it, I started <verb_ending_in_ing> uncontrollably.");
        s5.add("From that day forward, I became the most <adjective> person in town.");
        builtInStories.add(s5);

        // --- Step 2: Try to load a random .txt story from the folder; if none found, use built-in ---
        boolean pickedFromFolder = false;
        if (folder.exists() && folder.isDirectory()) {
            File[] all = folder.listFiles();
            if (all != null && all.length > 0) {
                // prefer .txt files
                List<File> txtOnly = new ArrayList<>();
                for (File f : all) {
                    if (f.isFile() && f.getName().toLowerCase().endsWith(".txt")) {
                        txtOnly.add(f);
                    }
                }
                File[] pool = (txtOnly.size() > 0) ? txtOnly.toArray(new File[0]) : all;

                if (pool.length > 0) {
                    java.util.Random rng = new java.util.Random();
                    File chosen = pool[rng.nextInt(pool.length)];

                    try (Scanner fileIn = new Scanner(chosen, "UTF-8")) {
                        while (fileIn.hasNextLine()) {
                            lines.add(fileIn.nextLine());
                        }
                        pickedFromFolder = true; // success
                    } catch (Exception e) {
                        // if reading fails, we'll fall back to built-in pool below
                        pickedFromFolder = false;
                    }
                }
            }
        }

        if (!pickedFromFolder) {
            // Randomly pick one of the built-in stories every run
            java.util.Random rng = new java.util.Random();
            List<String> chosenBuiltIn = builtInStories.get(rng.nextInt(builtInStories.size()));
            lines.addAll(chosenBuiltIn);
        }

        // --- Step 3: For each line, find <placeholder> pieces and prompt the user to fill them ---
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            while (true) {
                int start = line.indexOf('<');
                if (start == -1) {
                    break; // no token
                }
                int end = line.indexOf('>', start + 1);
                if (end == -1) {
                    break; // bad token, just stop
                }

                String token = line.substring(start + 1, end); // e.g., adjective or verb_ending_in_ing
                String label = token.replace('_', ' ');        // show nicer prompt

                System.out.print("Enter " + label + ": ");
                String word = scanner.nextLine();              // any text is fine

                // replace only this occurrence by rebuilding the string
                String before = line.substring(0, start);
                String after = line.substring(end + 1);
                line = before + word + after;
            }

            // put the updated line back in the same slot
            lines.set(i, line);
        }

        // --- Step 4: Print the final story ---
        System.out.println("\nYour Completed Mad Libs Story:\n");
        StringBuilder finalStory = new StringBuilder();
        for (String line : lines) {
            finalStory.append(line).append("\n");
        }
        System.out.println(finalStory.toString());

        printFooter(ucid, 3);
        scanner.close();
    }
}

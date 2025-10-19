package M2;

public class Problem4 extends BaseClass {
    private static String[] array1 = { "hello world!", "java programming", "special@#$%^&characters", "numbers 123 456",
            "mIxEd CaSe InPut!" };
    private static String[] array2 = { "hello world", "java programming", "this is a title case test",
            "capitalize every word", "mixEd CASE input" };
    private static String[] array3 = { "  hello   world  ", "java    programming  ",
            "  extra    spaces  between   words   ",
            "      leading and trailing spaces      ", "multiple      spaces" };
    private static String[] array4 = { "hello world", "java programming", "short", "a", "even" };

    private static void transformText(String[] arr, int arrayNumber) {
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfoBasic(arr, arrayNumber);

        // Challenge 1: Remove non-alphanumeric characters except spaces
        // Challenge 2: Convert text to Title Case
        // Challenge 3: Trim leading/trailing spaces and remove duplicate spaces
        // Result 1-3: Assign final phrase to `placeholderForModifiedPhrase`
        // Challenge 4 (extra credit): Extract up to middle 3 characters when possible (beginning starts at middle of phrase excluding the first and last characters),
        // assign to 'placeholderForMiddleCharacters'
        
        // if not enough characters assign "Not enough characters"
 
        // Step 1: sketch out plan using comments (include ucid and date)
        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)

        // nhd5 10-01-2025
        // step 1: using String.replaceAll("[^A-Za-z0-9 ]", "") to remove non-alphanumeric characters, using the space in the class to preserve spaces
        //         source: W3Schools — String replaceAll(): https://www.w3schools.com/java/ref_string_replaceall.asp
        //         (regex overview): https://www.w3schools.com/java/java_regex.asp
        // step 2: using split(" ") and a simple loop with substring/toUpperCase/toLowerCase to make text title case
        //         sources: W3Schools — String split(): https://www.w3schools.com/java/ref_string_split.asp
        //                  String toUpperCase(): https://www.w3schools.com/java/ref_string_touppercase.asp
        //                  String toLowerCase(): https://www.w3schools.com/java/ref_string_tolowercase.asp
        //                  String substring(): https://www.w3schools.com/java/ref_string_substring.asp
        // step 3: using trim() to remove spaces at beginning and end, using replaceAll(" +", " ") to remove duplicate spaces
        //         sources: W3Schools — String trim(): https://www.w3schools.com/java/ref_string_trim.asp
        //                  String replaceAll(): https://www.w3schools.com/java/ref_string_replaceall.asp
        // step 4: assigning result to placeholderForModifiedPhrase
        // step 5: using length() and a middle index (len/2) to determine the middle
        //         sources: W3Schools — String length(): https://www.w3schools.com/java/ref_string_length.asp
        // step 6: using Math.max and Math.min to get up to the middle 3 characters
        //         sources: W3Schools — Math.max(): https://www.w3schools.com/java/ref_math_max.asp
        //                  Math.min(): https://www.w3schools.com/java/ref_math_min.asp
        // step 7: using substring(start, end) to ensure the middle characters exclude the first and last characters of the phrase
        //         source: W3Schools — String substring(): https://www.w3schools.com/java/ref_string_substring.asp
        // step 8: assigning to placeholderForMiddleCharacters

        
        String placeholderForModifiedPhrase = "";
        String placeholderForMiddleCharacters = "";
        
        for(int i = 0; i <arr.length; i++){
            // Start Solution Edits

            String s = arr[i];

            // Challenge 1: keep letters, digits, and spaces
            s = s.replaceAll("[^A-Za-z0-9 ]", "");

            // Challenge 2: Title Case (beginner approach)
            String[] parts = s.split(" ");
            String result = "";
            for (int w = 0; w < parts.length; w++) {
                String word = parts[w];
                if (word.length() > 0) {
                    String first = word.substring(0, 1).toUpperCase();
                    String rest  = (word.length() > 1) ? word.substring(1).toLowerCase() : "";
                    if (!result.isEmpty()) result += " ";
                    result += first + rest;
                }
            }
            s = result;

            // Challenge 3: trim ends and collapse multiple spaces
            s = s.trim();
            s = s.replaceAll(" +", " ");

            // Result 1–3
            placeholderForModifiedPhrase = s;

            // Challenge 4: up to 3 middle chars, excluding very first and very last char
            String middle = "Not enough characters";
            int len = s.length();
            if (len > 2) {
                int mid   = len / 2;
                int start = Math.max(1, mid - 1);          // never before index 1
                int end   = Math.min(len - 1, start + 3);  // never include last char
                if (start < end) {
                    middle = s.substring(start, end);
                }
            }
            placeholderForMiddleCharacters = middle;
            
             // End Solution Edits
            System.out.println(String.format("Index[%d] \"%s\" | Middle: \"%s\"",i, placeholderForModifiedPhrase, placeholderForMiddleCharacters));
        }

       

        
        System.out.println("\n______________________________________");
    }

    public static void main(String[] args) {
        final String ucid = "nhd5"; // <-- change to your UCID
        // No edits below this line
        printHeader(ucid, 4);

        transformText(array1, 1);
        transformText(array2, 2);
        transformText(array3, 3);
        transformText(array4, 4);
        printFooter(ucid, 4);
    }

}
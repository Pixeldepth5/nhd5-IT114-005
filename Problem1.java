package M2;

public class Problem1 extends BaseClass {
    private static int[] array1 = {0,1,2,3,4,5,6,7,8,9};   
    private static int[] array2 = {9,8,7,6,5,4,3,2,1,0};
    private static int[] array3 = {0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9};
    private static int[] array4 = {9,9,8,8,7,7,6,6,5,5,4,4,3,3,2,2,1,1,0,0}; 
    private static void printOdds(int[] arr, int arrayNumber){
        // Only make edits between the designated "Start" and "End" comments
        printArrayInfo(arr, arrayNumber);

        // Challenge: Print odd values only in a single line separated by commas
        //   nhd5, Sep 29, 2025.
        // Step 1: Iterate through the array by using a for loop which goes through each value 
        // Step 2; using modules to detrmine if odd (Value %2!=0).
        // Step 3: Print the odd values on one line; use a flag so we add commas between numbers
        //         but not before the first or after the last value.

        // Step 2: Add/commit your outline of comments (required for full credit)
        // Step 3: Add code to solve the problem (add/commit as needed)
        System.out.print("Output Array: ");
        // Start Solution Edits
        
     

        // Track to see if the odd number is already printed 
        // if we do , then print a comma before the next number 

         boolean printedOne = false;

        for (int value : arr) {                 // Step 1: iterate through array
            if (value % 2 != 0) {               // Step 2: check if odd
                if (printedOne) {               // Step 3: handle commas between numbers
                    System.out.print(",");
                }
                System.out.print(value);        // print the odd number itself
                printedOne = true;              // mark that we've printed at least one
            }
        }

        // End Solution Edits
        System.out.println("");
        System.out.println("______________________________________");
    }
    public static void main(String[] args) {
        final String ucid = "nhd5"; // <-- change to your UCID
        // no edits below this line
        printHeader(ucid, 1);
        printOdds(array1,1);
        printOdds(array2,2);
        printOdds(array3,3);
        printOdds(array4,4);
        printFooter(ucid, 1);
        
    }
}
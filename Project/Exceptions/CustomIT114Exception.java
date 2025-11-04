// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame Custom base exception class for IT114 Milestone 1
// Reference: https://www.w3schools.com/java/

package Exceptions;

public abstract class CustomIT114Exception extends Exception {
    public CustomIT114Exception(String message) {
        super(message);
    }

    public CustomIT114Exception(String message, Throwable cause) {
        super(message, cause);
    }
}

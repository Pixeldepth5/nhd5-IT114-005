// UCID: nhd5
// Date: November 3, 2025
// Description: CustomIT114Exception – base exception class for all custom exceptions in this project.
//              Provides a common exception hierarchy for room-related errors.
// Reference: https://www.w3schools.com/java/java_try_catch.asp

package Exceptions;

public abstract class CustomIT114Exception extends Exception {
    public CustomIT114Exception(String message) {
        super(message);
    }

    public CustomIT114Exception(String message, Throwable cause) {
        super(message, cause);
    }
}

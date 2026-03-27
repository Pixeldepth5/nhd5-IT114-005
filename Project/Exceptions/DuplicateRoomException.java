// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame DuplicateRoomException – thrown if a room already exists
// Reference: https://www.w3schools.com/java/

package Exceptions;

public class DuplicateRoomException extends CustomIT114Exception {
    public DuplicateRoomException(String message) {
        super(message);
    }

    public DuplicateRoomException(String message, Throwable cause) {
        super(message, cause);
    }
}

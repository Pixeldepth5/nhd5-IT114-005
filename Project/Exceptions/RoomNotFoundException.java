// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame RoomNotFoundException – thrown if a room cannot be found
// Reference: https://www.w3schools.com/java/

package Exceptions;

public class RoomNotFoundException extends CustomIT114Exception {
    public RoomNotFoundException(String message) {
        super(message);
    }

    public RoomNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

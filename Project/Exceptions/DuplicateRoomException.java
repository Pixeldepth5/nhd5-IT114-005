// UCID: nhd5
// Date: November 3, 2025
// Description: DuplicateRoomException – thrown when trying to create a room that already exists.
//              Thrown by Server.createRoom() if room name is already in use.
// Reference: https://www.w3schools.com/java/java_try_catch.asp

package Exceptions;

public class DuplicateRoomException extends CustomIT114Exception {
    public DuplicateRoomException(String message) {
        super(message);
    }

    public DuplicateRoomException(String message, Throwable cause) {
        super(message, cause);
    }
}

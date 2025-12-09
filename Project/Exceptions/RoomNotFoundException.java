// UCID: nhd5
// Date: November 3, 2025
// Description: RoomNotFoundException – thrown when trying to join a room that doesn't exist.
//              Thrown by Server.joinRoom() if the requested room name is not found.
// Reference: https://www.w3schools.com/java/java_try_catch.asp

package Exceptions;

public class RoomNotFoundException extends CustomIT114Exception {
    public RoomNotFoundException(String message) {
        super(message);
    }

    public RoomNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

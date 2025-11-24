package Common;

// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame PayloadType – defines message categories shared by client and server
// Reference: https://www.w3schools.com/java/java_enums.asp (enum syntax & usage)

public enum PayloadType {
    CLIENT_CONNECT,
    CLIENT_ID,
    MESSAGE,
    ROOM_CREATE,
    ROOM_JOIN,
    ROOM_LEAVE,
    DISCONNECT,
    POINTS_UPDATE     // Milestone 2 – new enum value to sync points (enum idea from W3Schools)
}

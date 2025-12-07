// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame PayloadType – defines message categories shared by client and server
// Reference: https://www.w3schools.com/java/java_enums.asp
package Common;

public enum PayloadType {
    CLIENT_CONNECT,
    CLIENT_ID,
    MESSAGE,
    ROOM_CREATE,
    ROOM_JOIN,
    ROOM_LEAVE,
    DISCONNECT,

    // Milestone 2+
    POINTS_UPDATE,

    // Milestone 3 – trivia game specific
    QA_UPDATE,          // sends question + answers
    USERLIST_UPDATE,    // syncs user list / points / lock-in / away / spectator
    TIMER               // round countdown timer
}

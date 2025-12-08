// UCID: nhd5
// Date: December 8, 2025
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

    // Points
    POINTS_UPDATE,

    // Original milestone names (keep for server compatibility)
    QUESTION,
    USER_LIST,

    // New names used in the updated client (aliases)
    QA_UPDATE,
    USERLIST_UPDATE,

    // Round timer
    TIMER
}

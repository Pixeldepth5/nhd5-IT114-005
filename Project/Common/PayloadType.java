// UCID: nhd5
// Date: December 8, 2025
// Description: PayloadType for Trivia Game (Milestone 3)
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

    // Game-specific payloads
    QUESTION,       // QAPayload: category + question + answers
    USER_LIST,      // UserListPayload: players, scores, locked/away/spectator
    POINTS_UPDATE,  // PointsPayload: sync player points
    TIMER           // send countdown timer each second
}

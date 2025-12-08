package Common;

public enum PayloadType {
    CLIENT_CONNECT,
    CLIENT_ID,
    MESSAGE,
    ROOM_CREATE,
    ROOM_JOIN,
    ROOM_LEAVE,
    DISCONNECT,

    // Milestone 2
    POINTS_UPDATE,

    // Milestone 3
    QA_UPDATE,          // Server sends question+answers
    USERLIST_UPDATE,    // Server syncs user list with client
    TIMER,              // Countdown timer

    // *** LEGACY NAMES USED IN GAMEROOM (needed to fix compile errors) ***
    QUESTION,
    USER_LIST
}

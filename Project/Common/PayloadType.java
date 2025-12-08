package Common;

public enum PayloadType {

    // Connection + handshake
    CONNECT,
    CLIENT_ID,

    // Chat & server messages
    MESSAGE,
    SERVER_MESSAGE,
    COMMAND,

    // Gameplay
    QUESTION,
    USER_LIST,
    POINTS_UPDATE,
    TIMER,

    // Numeric value (timer seconds, round number, etc.)
    NUMBER
}

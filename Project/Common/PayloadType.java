// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame PayloadType – defines message categories shared by client and server
// Reference: https://www.w3schools.com/java/

package Common;

public enum PayloadType {
    CLIENT_CONNECT,   // when client connects to server
    CLIENT_ID,        // when server assigns client an ID
    MESSAGE,          // regular chat or trivia messages
    ROOM_CREATE,      // room creation request
    ROOM_JOIN,        // room joining
    ROOM_LEAVE,       // room leaving
    DISCONNECT        // disconnect event
}

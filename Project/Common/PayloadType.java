// UCID: nhd5
// Date: December 8, 2025
// Description: Shared enum for all payload types used by Client + Server.
//              Determines what kind of data is in a Payload object.
// References:
//   - W3Schools: https://www.w3schools.com/java/java_enums.asp

package Common;

public enum PayloadType {

  // Connection & rooms
  CLIENT_CONNECT,   // used with ConnectionPayload from client
  CONNECT,          // legacy alias (safe but not required)
  CLIENT_ID,
  ROOM_CREATE,
  ROOM_JOIN,
  ROOM_LEAVE,
  DISCONNECT,

  // Chat & messages
  MESSAGE,          // normal chat + game events (with/without [CHAT] prefix)
  SERVER_MESSAGE,   // reserved / legacy
  COMMAND,          // reserved / legacy

  // Gameplay
  QUESTION,         // QAPayload: question + answers
  QA_UPDATE,        // alias for QUESTION (not required, kept for compatibility)
  USER_LIST,        // UserListPayload: users + points + flags
  USERLIST_UPDATE,  // alias for USER_LIST
  POINTS_UPDATE,    // PointsPayload: scoring updates
  TIMER,            // Payload: message = seconds remaining

  // Generic numeric payload (not currently used but safe)
  NUMBER
}

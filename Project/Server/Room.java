// UCID: nhd5
// Date: November 23, 2025
// Description: WordGuesserGame Room – base class for chat/game rooms
// Reference:
//   https://www.w3schools.com/java/java_arraylist.asp (looping through collections)
//   https://www.w3schools.com/java/java_try_catch.asp (exception handling)

package Server;

import Common.Constants;
import Exceptions.DuplicateRoomException;
import Exceptions.RoomNotFoundException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class Room {
    public static final String LOBBY = "lobby";

    protected final String name;
    protected final ConcurrentHashMap<Long, ServerThread> clients = new ConcurrentHashMap<>();

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Returns a collection of all clients in this room.
     * GameRoom uses this with a simple for-each loop, like the
     * W3Schools ArrayList/collection examples.
     */
    protected synchronized Collection<ServerThread> getClients() {
        return clients.values();
    }

    /**
     * Adds a client to this room and broadcasts a join message.
     */
    protected synchronized void addClient(ServerThread client) {
        // This assumes BaseServerThread provides setCurrentRoom(Room)
        client.setCurrentRoom(this);
        clients.put(client.getClientId(), client);
        broadcast(null, client.getDisplayName() + " joined " + name);
    }

    /**
     * Handles when a client disconnects from the server or room.
     */
    protected synchronized void handleDisconnect(ServerThread client) {
        clients.remove(client.getClientId());
        broadcast(null, client.getDisplayName() + " left " + name);
    }

    /**
     * Basic chat message handler.
     */
    protected synchronized void handleMessage(ServerThread sender, String msg) {
        broadcast(sender, sender.getDisplayName() + ": " + msg);
    }

    /**
     * Creates a new room and moves the sender into that room.
     */
    protected synchronized void handleCreateRoom(ServerThread sender, String roomName) {
        try {
            Server server = Server.getInstance();
            if (server != null) {
                // Leave current room first
                handleDisconnect(sender);
                server.createRoom(roomName);
                server.joinRoom(roomName, sender);
            } else {
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Server not initialized.");
            }
        } catch (DuplicateRoomException | RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, e.getMessage());
        }
    }

    /**
     * Joins an existing room. Used for commands like:
     *   /joinroom someRoom
     *   /leaveroom  (internally uses joinroom lobby)
     */
    protected synchronized void handleJoinRoom(ServerThread sender, String roomName) {
        try {
            Server server = Server.getInstance();
            if (server != null) {
                // Leave the current room
                handleDisconnect(sender);
                // Join the new room
                server.joinRoom(roomName, sender);
            } else {
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Server not initialized.");
            }
        } catch (RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, e.getMessage());
        }
    }

    /**
     * Broadcast a message to all clients in this room.
     */
    protected synchronized void broadcast(ServerThread sender, String message) {
        for (ServerThread client : clients.values()) {
            if (sender == null || client != sender) {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, message);
            }
        }
    }
}

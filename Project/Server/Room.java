// UCID: nhd5
// Date: December 8, 2025
// Description: Room – base class for chat/game rooms (Lobby + GameRoom)
//  - Holds list of clients in the room
//  - For non-command messages, broadcasts as chat using [CHAT] prefix
// References:
//   https://www.w3schools.com/java/java_arraylist.asp
//   https://www.w3schools.com/java/java_try_catch.asp

package Server;

import Common.Constants;
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
     */
    protected synchronized Collection<ServerThread> getClients() {
        return clients.values();
    }

    /**
     * Adds a client to this room and broadcasts a join message.
     */
    protected synchronized void addClient(ServerThread client) {
        client.setCurrentRoom(this);
        clients.put(client.getClientId(), client);
        broadcast(null, "user \"" + client.getDisplayName() + "\" has joined the " + name);
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
     * Non-command messages hit this from GameRoom or subclass.
     * This method prefixes with [CHAT] so clients can separate chat vs events.
     */
    protected synchronized void handleMessage(ServerThread sender, String msg) {
        if (msg == null) return;
        String trimmed = msg.trim();
        if (trimmed.isEmpty()) return;

        String display = sender.getDisplayName() + ": " + trimmed;
        for (ServerThread client : clients.values()) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, "[CHAT] " + display);
        }
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
        } catch (Exception e) {
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
                handleDisconnect(sender);
                server.joinRoom(roomName, sender);
            } else {
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Server not initialized.");
            }
        } catch (Exception e) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID, e.getMessage());
        }
    }

    /**
     * Broadcast a game/event message to all clients in this room.
     * NOTE: This is used for GAME EVENTS, not chat.
     */
    protected synchronized void broadcast(ServerThread sender, String message) {
        for (ServerThread client : clients.values()) {
            if (sender == null || client != sender) {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, message);
            }
        }
    }
}

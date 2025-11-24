// UCID: nhd5
// Date: November 23, 2025
// Description: WordGuesserGame Room – base class for chat/game rooms
// Reference: 
//   https://www.w3schools.com/java/java_arraylist.asp (looping through collections)
//   https://www.w3schools.com/java/java_try_catch.asp (exception handling)

package Server;

import Common.Constants;
import Exceptions.*;
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

    // Allows GameRoom to loop through all clients
    protected synchronized Collection<ServerThread> getClients() {
        return clients.values();
    }

    // Adds a client to the room
    protected synchronized void addClient(ServerThread client) {
        clients.put(client.getClientId(), client);
        broadcast(null, client.getDisplayName() + " joined " + name);
    }

    // Handles when a client disconnects
    protected synchronized void handleDisconnect(ServerThread client) {
        clients.remove(client.getClientId());
        broadcast(null, client.getDisplayName() + " left " + name);
    }

    // Basic chat message
    protected synchronized void handleMessage(ServerThread sender, String msg) {
        broadcast(sender, sender.getDisplayName() + ": " + msg);
    }

    // Room creation
    protected synchronized void handleCreateRoom(ServerThread sender, String roomName) {
        try {
            Server server = Server.getInstance();
            if (server != null) {
                server.createRoom(roomName);
                server.joinRoom(roomName, sender);
            } else {
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Server not initialized.");
            }
        } catch (DuplicateRoomException | RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, e.getMessage());
        }
    }

    // Join a new room
    protected synchronized void handleJoinRoom(ServerThread sender, String roomName) {
        try {
            Server server = Server.getInstance();
            if (server != null) {
                server.joinRoom(roomName, sender);
            } else {
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Server not initialized.");
            }
        } catch (RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, e.getMessage());
        }
    }

    // Broadcast message to all clients in room
    protected synchronized void broadcast(ServerThread sender, String message) {
        for (ServerThread client : clients.values()) {
            if (sender == null || client != sender) {
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, message);
            }
        }
    }
}

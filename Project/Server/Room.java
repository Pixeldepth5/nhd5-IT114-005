// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame Room – manages groups of connected clients and room actions
// Reference: https://www.w3schools.com/java/

package Server;

import java.util.concurrent.ConcurrentHashMap;
import Exceptions.*;
import Common.Constants;

public class Room {
    public static final String LOBBY = "lobby";
    private final String name;
    private final ConcurrentHashMap<Long, ServerThread> clients = new ConcurrentHashMap<>();
    private boolean isRunning = true;

    public Room(String name) {
        this.name = name;
        System.out.println("Created Room: " + name);
    }

    public String getName() { return name; }

    protected synchronized void addClient(ServerThread client) {
        clients.put(client.getClientId(), client);
        client.setCurrentRoom(this);
        broadcast(null, client.getDisplayName() + " joined " + name);
    }

    protected synchronized void removeClient(ServerThread client) {
        clients.remove(client.getClientId());
        broadcast(null, client.getDisplayName() + " left " + name);
        if (!name.equalsIgnoreCase(LOBBY) && clients.isEmpty()) close();
    }

    protected synchronized void broadcast(ServerThread sender, String msg) {
        long senderId = sender == null ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        clients.values().removeIf(c -> !c.sendMessage(senderId, msg));
    }

    protected synchronized void handleCreateRoom(ServerThread sender, String roomName) {
        try {
            Server server = new Server();
            server.createRoom(roomName);
            server.joinRoom(roomName, sender);
        } catch (DuplicateRoomException | RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, e.getMessage());
        }
    }

    protected synchronized void handleJoinRoom(ServerThread sender, String roomName) {
        try {
            Server server = new Server();
            server.joinRoom(roomName, sender);
        } catch (RoomNotFoundException e) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, e.getMessage());
        }
    }

    protected synchronized void handleMessage(ServerThread sender, String text) {
        broadcast(sender, sender.getDisplayName() + ": " + text);
    }

    protected synchronized void handleDisconnect(ServerThread sender) {
        removeClient(sender);
    }

    private void close() {
        isRunning = false;
        clients.clear();
        System.out.println("Closed Room: " + name);
    }
}

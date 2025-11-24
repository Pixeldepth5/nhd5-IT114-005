// UCID: nhd5
// Date: November 23, 2025
// Description: WordGuesserGame Server – singleton that creates and manages Rooms for Milestone 2
// Reference: 
//   https://www.w3schools.com/java/java_modifier_static.asp (static fields & methods)
//   https://www.w3schools.com/java/java_hashmap.asp (using HashMap / ConcurrentHashMap)

package Server;

import Exceptions.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Server instance;

    private boolean isRunning = true;
    private int port = 3000;
    private long nextClientId = 0;
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public Server() {
        instance = this;
        try {
            createRoom(Room.LOBBY);
        } catch (DuplicateRoomException e) {
            System.out.println("Lobby already exists.");
        }
    }

    // Singleton access method (based on W3Schools static examples)
    public static Server getInstance() {
        return instance;
    }

    // Creates a new Room or GameRoom
    public synchronized void createRoom(String name) throws DuplicateRoomException {
        String lower = name.toLowerCase();
        if (rooms.containsKey(lower)) {
            throw new DuplicateRoomException("Room " + name + " already exists");
        }

        Room newRoom;
        if (Room.LOBBY.equalsIgnoreCase(name)) {
            newRoom = new Room(name);
        } else {
            newRoom = new GameRoom(name);
        }

        rooms.put(lower, newRoom);
        System.out.println("Created Room: " + name);
    }

    // Adds a client to a room
    public synchronized void joinRoom(String roomName, ServerThread client) throws RoomNotFoundException {
        String lower = roomName.toLowerCase();
        Room targetRoom = rooms.get(lower);
        if (targetRoom == null) {
            throw new RoomNotFoundException("Room " + roomName + " not found");
        }

        targetRoom.addClient(client);
        System.out.println(client.getDisplayName() + " joined room " + roomName);
    }

    // Starts the server socket and accepts new clients
    public void start() {
        System.out.println("Server starting on port " + port + "...");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                ServerThread clientThread = new ServerThread(clientSocket, this::onClientInitialized);
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    private void onClientInitialized(ServerThread client) {
        nextClientId++;
        client.setClientId(nextClientId);
        try {
            joinRoom(Room.LOBBY, client);
        } catch (RoomNotFoundException e) {
            System.out.println("Error adding client to lobby.");
        }
    }

    public static void main(String[] args) {
        new Server().start();
    }
}

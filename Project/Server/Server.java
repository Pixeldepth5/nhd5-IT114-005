// UCID: nhd5
// Date: December 8, 2025
// Description: Server – creates Rooms, accepts connections, assigns client IDs,
//              and places all new clients into Lobby.
// References:
//   - W3Schools: https://www.w3schools.com/java/java_hashmap.asp
//   - W3Schools: https://www.w3schools.com/java/java_files_create.asp
//   - W3Schools: ServerSocket example structure

package Server;

import Exceptions.DuplicateRoomException;
import Exceptions.RoomNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {

    // Singleton instance (rubric expects static-based pattern)
    private static Server instance;

    private boolean isRunning = true;
    private int port = 3000;
    private long nextClientId = 0;

    // Room registry, key = lowercase room name
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public Server() {
        instance = this; // Set singleton instance

        // Ensure Lobby exists on startup (required by rubric)
        try {
            createRoom(Room.LOBBY);
        } catch (DuplicateRoomException e) {
            System.out.println("Lobby already exists.");
        }
    }

    /**
     * Global getter for this Server instance.
     */
    public static Server getInstance() {
        return instance;
    }

    /**
     * Creates a Lobby or GameRoom depending on the name.
     */
    public synchronized void createRoom(String name) throws DuplicateRoomException {
        String key = name.toLowerCase();
        if (rooms.containsKey(key)) {
            throw new DuplicateRoomException("Room '" + name + "' already exists.");
        }

        Room newRoom;

        // Lobby is now a GameRoom so ready commands work
        newRoom = new GameRoom(name);

        rooms.put(key, newRoom);
        System.out.println("Created Room: " + name);
    }

    /**
     * Adds a client to a room by name (called on /join commands + first login).
     */
    public synchronized void joinRoom(String roomName, ServerThread client)
            throws RoomNotFoundException {

        String key = roomName.toLowerCase();
        Room target = rooms.get(key);

        if (target == null) {
            throw new RoomNotFoundException("Room '" + roomName + "' not found.");
        }

        target.addClient(client);
        System.out.println(client.getDisplayName() + " joined room " + roomName);
    }

    /**
     * Start a TCP server on port 3000 and constantly accept new clients.
     */
    /**
     * Starts the server and listens for incoming client connections.
     * Each new connection gets its own ServerThread to handle communication.
     */
    public void start() {
        System.out.println("Server starting on port " + port + "...");

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Continuously accept new client connections
            while (isRunning) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Create a thread for this client - callback runs when client is initialized
                ServerThread clientThread = new ServerThread(clientSocket, this::onClientInitialized);
                clientThread.start();
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Called once ServerThread streams are initialized.
     * Assign unique client ID → put them into Lobby.
     */
    private void onClientInitialized(ServerThread client) {
        nextClientId++;
        client.setClientId(nextClientId);

        try {
            joinRoom(Room.LOBBY, client);
        } catch (RoomNotFoundException e) {
            System.out.println("Error placing client into Lobby.");
        }
    }

    public static void main(String[] args) {
        new Server().start();
    }
}

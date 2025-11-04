// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame Server – handles client connections, room creation, and message relay
// Reference: https://www.w3schools.com/java/

package Server;

import Exceptions.DuplicateRoomException;
import Exceptions.RoomNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private boolean isRunning = true;
    private int port = 3000;
    private long nextClientId = 0;
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    public Server() {}

    public void start(int port) {
        this.port = port;
        System.out.println("TriviaGuessGame Server listening on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            createRoom(Room.LOBBY);

            while (isRunning) {
                System.out.println("Waiting for next Trivia client...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected!");
                ServerThread thread = new ServerThread(clientSocket, this::onClientInitialized);
                thread.start();
            }
        } catch (IOException | DuplicateRoomException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void onClientInitialized(ServerThread serverThread) {
        nextClientId++;
        serverThread.setClientId(nextClientId);
        serverThread.sendClientId();

        try {
            joinRoom(Room.LOBBY, serverThread);
        } catch (RoomNotFoundException e) {
            e.printStackTrace();
        }

        System.out.println("Initialized: " + serverThread.getDisplayName());
    }

    public synchronized void createRoom(String name) throws DuplicateRoomException {
        String lower = name.toLowerCase();
        if (rooms.containsKey(lower)) {
            throw new DuplicateRoomException("Room " + name + " already exists");
        }
        Room newRoom = new Room(name);
        rooms.put(lower, newRoom);
        System.out.println("Created Room: " + name);
    }

    public synchronized void joinRoom(String name, ServerThread client) throws RoomNotFoundException {
        String lower = name.toLowerCase();
        if (!rooms.containsKey(lower)) {
            throw new RoomNotFoundException("Room " + name + " not found");
        }
        Room currentRoom = client.getCurrentRoom();
        if (currentRoom != null) {
            currentRoom.removeClient(client);
        }
        Room nextRoom = rooms.get(lower);
        nextRoom.addClient(client);
    }

    public synchronized void removeRoom(Room room) {
        rooms.remove(room.getName().toLowerCase());
        System.out.println("Removed Room: " + room.getName());
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start(3000);
    }
}

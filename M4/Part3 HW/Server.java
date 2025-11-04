package M4.Part3HW;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * nhd5
 * 
 * Part 3 Server
 * Manages multiple clients (via ServerThread) and exposes shared logic for:
 *  - broadcasting normal messages
 *  - private messaging (/pm)
 *  - listing connected users (/who)
 * 
 * Sources:
 *  - Threads overview: https://www.w3schools.com/java/java_threads.asp
 *  - ArrayList usage:  https://www.w3schools.com/java/java_arraylist.asp
 *  - try/catch:        https://www.w3schools.com/java/java_try_catch.asp
 */
public class Server {
    private int port = 3000;

    // Shared list of connected clients
    private final List<ServerThread> clients = new ArrayList<>();

    // Sequential ID counter for new connections
    private int nextClientId = 1;

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception ignore) {}
        server.start(port);
        System.out.println("Server Stopped");
    }

    /**
     * Starts the server socket and continuously accepts new client connections.
     * Each accepted client is wrapped in a ServerThread and assigned a unique ID.
     *
     * @param port port to listen on
     */
    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket client = serverSocket.accept(); // blocking call
                ServerThread clientThread = new ServerThread(client, this, st -> {
                    // Assign sequential ID and register in list
                    st.setClientId(nextClientId++);
                    synchronized (clients) {
                        clients.add(st);
                    }
                });
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a normal chat message to all connected clients except the sender.
     *
     * @param sender  origin of the message
     * @param message text to forward
     */
    protected void handleMessage(ServerThread sender, String message) {
        synchronized (clients) {
            for (ServerThread st : clients) {
                if (st != sender) {
                    st.sendToClient("Client " + sender.getClientId() + ": " + message);
                }
            }
        }
    }

    /**
     * Handles private messaging: /pm <targetId> <message>
     * Sends the formatted result ONLY to the sender and to the receiver.
     * If target not found, notifies the sender.
     *
     * @param sender   who initiated the PM
     * @param targetId target client id
     * @param message  PM text
     */
    protected void handlePrivateMessage(ServerThread sender, int targetId, String message) {
        ServerThread target = null;

        synchronized (clients) {
            for (ServerThread st : clients) {
                if (st.getClientId() == targetId) {
                    target = st;
                    break;
                }
            }
        }

        if (target == null) {
            sender.sendToClient("User " + targetId + " not found");
            return;
        }

        // Required format per rubric
        String formatted = "PM from " + sender.getClientId() + ": " + message;

        // Send to receiver and echo to sender
        target.sendToClient(formatted);
        sender.sendToClient(formatted);
    }

    /**
     * Responds to a /who request by sending the requester a comma-separated list
     * of currently connected sequential IDs.
     *
     * @param requester the client who asked
     */
    protected void handleWho(ServerThread requester) {
        StringBuilder sb = new StringBuilder("Connected users: ");
        synchronized (clients) {
            for (int i = 0; i < clients.size(); i++) {
                sb.append(clients.get(i).getClientId());
                if (i < clients.size() - 1) sb.append(", ");
            }
        }
        requester.sendToClient(sb.toString());
    }

    /**
     * Removes a disconnected client from the registry.
     *
     * @param st client thread to remove
     */
    protected void handleDisconnect(ServerThread st) {
        synchronized (clients) {
            clients.remove(st);
        }
    }
}

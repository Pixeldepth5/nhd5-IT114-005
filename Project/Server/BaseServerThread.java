// UCID: nhd5
// Date: November 4, 2025
// Description: TriviaGuessGame BaseServerThread – manages socket I/O and client threading safely
// Reference: https://www.w3schools.com/java/

package Server;

import Common.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket; // includes Payload, PayloadType, ConnectionPayload, and User

/**
 * This class is the parent thread logic for each connected client.
 * It handles reading incoming payloads, forwarding messages, and
 * notifying rooms when clients disconnect.
 */
public abstract class BaseServerThread extends Thread {
    protected Socket client;
    protected ObjectOutputStream out;
    protected boolean isRunning = false;
    protected Room currentRoom;
    protected User user = new User(); // Common.User (shared between server & client)

    // Basic user management
    public void setClientId(long id) { user.setClientId(id); }
    public long getClientId() { return user.getClientId(); }
    public void setClientName(String name) { user.setClientName(name); onInitialized(); }
    public String getClientName() { return user.getClientName(); }
    public String getDisplayName() { return user.getDisplayName(); }
    public Room getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(Room room) { currentRoom = room; }

    @Override
    public void run() {
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {

            this.out = out;
            isRunning = true;
            Payload fromClient;

            while (isRunning) {
                fromClient = (Payload) in.readObject();
                if (fromClient != null) {
                    processPayload(fromClient);
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected.");
        } finally {
            // ✅ FIX: safely cast only if this thread is an instance of ServerThread
            if (currentRoom != null && this instanceof ServerThread) {
                currentRoom.handleDisconnect((ServerThread) this);
            }
            cleanup();
        }
    }

    // Abstract methods implemented by subclasses
    protected abstract void processPayload(Payload payload);
    protected abstract void onInitialized();

    // Sends payload to the client
    protected boolean sendToClient(Payload payload) {
        try {
            out.writeObject(payload);
            out.flush();
            return true;
        } catch (IOException e) {
            cleanup();
            return false;
        }
    }

    // Cleanup and safely close connections
    protected void cleanup() {
        try {
            if (client != null) client.close();
        } catch (IOException ignored) {}
        isRunning = false;
    }

    // Sends the client its assigned ID
    protected boolean sendClientId() {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setPayloadType(PayloadType.CLIENT_ID);
        payload.setClientId(getClientId());
        payload.setClientName(getClientName());
        return sendToClient(payload);
    }

    // Sends chat messages or system messages back to the client
    protected boolean sendMessage(long id, String msg) {
        Payload payload = new Payload();
        payload.setPayloadType(PayloadType.MESSAGE);
        payload.setClientId(id);
        payload.setMessage(msg);
        return sendToClient(payload);
    }
}

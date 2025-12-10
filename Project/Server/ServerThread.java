// UCID: nhd5
// Date: November 23, 2025
// Description: WordGuesserGame ServerThread – manages communication with individual clients
// Reference: https://www.w3schools.com/java/java_methods.asp (defining methods with parameters & return types)

package Server;

import Common.*;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

public class ServerThread extends BaseServerThread {
    private Consumer<ServerThread> onInitializationComplete;

    public ServerThread(Socket myClient, Consumer<ServerThread> onInit) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInit, "Callback cannot be null");
        this.client = myClient;
        this.onInitializationComplete = onInit;
    }

    @Override
    protected void processPayload(Payload incoming) {
        switch (incoming.getPayloadType()) {
            case CLIENT_CONNECT -> setClientName(((ConnectionPayload) incoming).getClientName().trim());
            case DISCONNECT -> currentRoom.handleDisconnect(this);
            case MESSAGE -> currentRoom.handleMessage(this, incoming.getMessage());
            case ROOM_CREATE -> currentRoom.handleCreateRoom(this, incoming.getMessage());
            case ROOM_JOIN -> currentRoom.handleJoinRoom(this, incoming.getMessage());
            case ROOM_LEAVE -> currentRoom.handleJoinRoom(this, Room.LOBBY);
            default -> System.out.println("Unknown payload type.");
        }
    }

    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this);
    }

    // ===== NEW for WordGuesserGame (Milestone 2) =====
    // Reference: https://www.w3schools.com/java/java_methods.asp (methods with parameters & return types)

    /**
     * Helper method so a Room (like GameRoom) can send any kind of Payload
     * directly to this client.
     */
    public boolean sendPayload(Payload payload) {
        return sendToClient(payload);
    }
}

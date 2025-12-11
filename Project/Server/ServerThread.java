// UCID: nhd5
// Date: December 8, 2025
// Description: ServerThread – per-client bridge. Unpacks incoming payloads and
//              forwards raw data to Room/GameRoom handle* methods, and exposes
//              a sendPayload helper for server→client sync.
// Reference: https://www.w3schools.com/java/java_methods.asp

package Server;

import Common.*;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

public class ServerThread extends BaseServerThread {
    private final Consumer<ServerThread> onInitializationComplete;

    public ServerThread(Socket myClient, Consumer<ServerThread> onInit) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(onInit, "Callback cannot be null");
        this.client = myClient;
        this.onInitializationComplete = onInit;
    }

    /**
     * Routes incoming payloads to appropriate Room handler methods.
     * Extracts data from payload and passes raw data (not payload) to Room.
     */
    @Override
    protected void processPayload(Payload incoming) {
        switch (incoming.getPayloadType()) {
            case CLIENT_CONNECT -> setClientName(((ConnectionPayload) incoming).getClientName().trim());
            case DISCONNECT     -> currentRoom.handleDisconnect(this);
            case MESSAGE        -> currentRoom.handleMessage(this, incoming.getMessage());
            case ROOM_CREATE    -> currentRoom.handleCreateRoom(this, incoming.getMessage());
            case ROOM_JOIN      -> currentRoom.handleJoinRoom(this, incoming.getMessage());
            case ROOM_LEAVE     -> currentRoom.handleJoinRoom(this, Room.LOBBY);
            case ROOM_LIST      -> sendRoomList();
            default             -> System.out.println("Unknown payload type from client: " + incoming.getPayloadType());
        }
    }

    @Override
    protected void onInitialized() {
        onInitializationComplete.accept(this);
    }

    /** Helper so rooms can push any payload type to this client. */
    public boolean sendPayload(Payload payload) {
        return sendToClient(payload);
    }

    private void sendRoomList() {
        RoomResultPayload payload = new RoomResultPayload();
        payload.setPayloadType(PayloadType.ROOM_LIST);
        payload.setRooms(Server.getInstance().listRooms());
        sendToClient(payload);
    }
}

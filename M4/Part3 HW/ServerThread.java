package M4.Part3HW;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import M4.Part3HW.TextFX.Color;

/**
 * nhd5
 *
 * This thread represents a single connected client.
 * It listens for incoming client messages, determines whether it is
 * a normal chat message or a command (/pm, /who, reverse, quit, etc.),
 * and then delegates the logic to the Server.
 *
 * Sources:
 * - String splitting: https://www.w3schools.com/java/ref_string_split.asp
 * - Threading:         https://www.w3schools.com/java/java_threads.asp
 */
public class ServerThread extends Thread {
    private Socket client;
    private boolean isRunning = false;
    private ObjectOutputStream out;
    private Server server;
    private long clientId;
    private Consumer<ServerThread> onInitializationComplete;

    private void info(String message) {
        System.out.println(String.format("Thread[%s]: %s", this.getClientId(), message));
    }

    protected ServerThread(Socket myClient, Server server, Consumer<ServerThread> onInitializationComplete) {
        Objects.requireNonNull(myClient, "Client socket cannot be null");
        Objects.requireNonNull(server, "Server cannot be null");
        Objects.requireNonNull(onInitializationComplete, "callback cannot be null");
        this.client = myClient;
        this.server = server;
        this.onInitializationComplete = onInitializationComplete;
        info("ServerThread created");
    }

    // Used by Server to assign sequential IDs
    protected void setClientId(long newId) {
        this.clientId = newId;
    }

    public long getClientId() {
        return this.clientId;
    }

    public boolean isRunning() {
        return isRunning;
    }

    protected void disconnect() {
        if (!isRunning) return;
        info("Thread being disconnected by server");
        isRunning = false;
        this.interrupt();
        cleanup();
    }

    protected boolean sendToClient(String message) {
        if (!isRunning) return false;
        try {
            out.writeObject(message);
            out.flush();
            return true;
        } catch (IOException e) {
            info("Error sending message to client");
            cleanup();
            return false;
        }
    }

    @Override
    public void run() {
        info("Thread starting");
        try (ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(client.getInputStream())) {

            this.out = out;
            isRunning = true;
            onInitializationComplete.accept(this);

            String fromClient;
            while (isRunning) {
                try {
                    fromClient = (String) in.readObject();
                    if (fromClient == null) break;
                    info(TextFX.colorize("Received: " + fromClient, Color.CYAN));
                    processPayload(fromClient);
                } catch (Exception e) {
                    break;
                }
            }
        } catch (Exception e) {
            info("General Exception");
        } finally {
            isRunning = false;
            cleanup();
        }
    }

    private void processPayload(String incoming) {
        if (!processCommand(incoming)) {
            // If not a command -> broadcast normal message
            server.handleMessage(this, incoming);
        }
    }

    /**
     * Checks whether incoming text is a command and executes it.
     *
     * @param message full text from client
     * @return true if this was a command (so skip broadcast)
     */
    private boolean processCommand(String message) {
        // ✅ NEW: /pm logic (human format)
        if (message.startsWith("/pm")) {
            String[] parts = message.split("\\s+", 3);
            if (parts.length < 3) {
                sendToClient("Usage: /pm <targetId> <message>");
                return true;
            }
            try {
                int targetId = Integer.parseInt(parts[1]);
                server.handlePrivateMessage(this, targetId, parts[2]);
            } catch (NumberFormatException e) {
                sendToClient("Invalid target id");
            }
            return true;
        }

        // ✅ NEW: /who command
        if (message.equalsIgnoreCase("/who")) {
            server.handleWho(this);
            return true;
        }

        // ✅ ORIGINAL COMMAND SYSTEM (CSV-based)
        if (message.startsWith(Constants.COMMAND_TRIGGER)) {
            String[] cmdData = message.split(",");
            if (cmdData.length >= 2) {
                String command = cmdData[1].trim();
                switch (command) {
                    case "quit":
                    case "disconnect":
                    case "logout":
                    case "logoff":
                        server.handleDisconnect(this);
                        return true;
                    case "reverse":
                        String text =
                                String.join(" ", Arrays.copyOfRange(cmdData, 2, cmdData.length));
                        server.handleReverseText(this, text);
                        return true;
                }
            }
        }
        return false;
    }

    private void cleanup() {
        info("Cleanup() start");
        try { client.close(); } catch (IOException ignored) {}
        info("Cleanup() end");
    }
}

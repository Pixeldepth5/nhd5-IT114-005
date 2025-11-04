// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame Client – handles user input, server communication, and room actions
// Reference: https://www.w3schools.com/java/

package Client;


import Common.*;
import Server.RoomAction;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client {
    private Socket server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = true;
    private User myUser = new User();


    final Pattern ipPattern = Pattern.compile("/connect\\s+(\\d+\\.\\d+\\.\\d+\\.\\d+):(\\d+)");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost):(\\d+)");

    public Client() {
        System.out.println(TextFX.colorize("TriviaGuessGame Client started.", TextFX.Color.CYAN));
    }

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            CompletableFuture.runAsync(this::listenToServer);
            System.out.println(TextFX.colorize("Connected to TriviaGuessGame Server.", TextFX.Color.GREEN));
            return true;
        } catch (IOException e) {
            System.out.println(TextFX.colorize("Failed to connect to server.", TextFX.Color.RED));
            return false;
        }
    }

    private boolean isConnectionCommand(String text) {
        return ipPattern.matcher(text).matches() || localhostPattern.matcher(text).matches();
    }

    private boolean processCommand(String text) throws IOException {
        if (!text.startsWith("/")) return false;
        String cmd = text.trim().substring(1);

        if (isConnectionCommand(text)) {
            Matcher m = ipPattern.matcher(text);
            Matcher m2 = localhostPattern.matcher(text);
            String host = "localhost";
            int port = 3000;
            if (m.matches()) {
                host = m.group(1);
                port = Integer.parseInt(m.group(2));
            } else if (m2.matches()) {
                port = Integer.parseInt(m2.group(2));
            }
            connect(host, port);
            sendClientName(myUser.getClientName());
            return true;
        } else if (cmd.startsWith("name")) {
            myUser.setClientName(cmd.replace("name", "").trim());
            System.out.println(TextFX.colorize("Set name to " + myUser.getClientName(), TextFX.Color.YELLOW));
            return true;
        } else if (cmd.startsWith("createroom")) {
            sendRoomAction(cmd.replace("createroom", "").trim(), RoomAction.CREATE);
            return true;
        } else if (cmd.startsWith("joinroom")) {
            sendRoomAction(cmd.replace("joinroom", "").trim(), RoomAction.JOIN);
            return true;
        } else if (cmd.equalsIgnoreCase("quit")) {
            close();
            return true;
        }
        return false;
    }

    private void sendRoomAction(String roomName, RoomAction action) throws IOException {
        Payload payload = new Payload();
        payload.setMessage(roomName);
        switch (action) {
            case CREATE -> payload.setPayloadType(PayloadType.ROOM_CREATE);
            case JOIN -> payload.setPayloadType(PayloadType.ROOM_JOIN);
            case LEAVE -> payload.setPayloadType(PayloadType.ROOM_LEAVE);
        }
        sendToServer(payload);
    }

    private void sendClientName(String name) throws IOException {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setClientName(name);
        payload.setPayloadType(PayloadType.CLIENT_CONNECT);
        sendToServer(payload);
    }

    private void sendToServer(Payload payload) throws IOException {
        if (server != null && !server.isClosed()) {
            out.writeObject(payload);
            out.flush();
        }
    }

    private void listenToServer() {
        try {
            while (isRunning) {
                Payload p = (Payload) in.readObject();
                if (p == null) break;
                System.out.println(TextFX.colorize(p.getMessage(), TextFX.Color.PURPLE));
            }
        } catch (Exception e) {
            System.out.println(TextFX.colorize("Server connection closed.", TextFX.Color.RED));
        } finally {
            close();
        }
    }

    private void listenToInput() {
        try (Scanner si = new Scanner(System.in)) {
            while (isRunning) {
                String input = si.nextLine();
                if (!processCommand(input)) {
                    Payload p = new Payload();
                    p.setMessage(input);
                    p.setPayloadType(PayloadType.MESSAGE);
                    sendToServer(p);
                }
            }
        } catch (IOException e) {
            System.out.println(TextFX.colorize("Error reading input.", TextFX.Color.RED));
        }
    }

    public void start() throws IOException {
        CompletableFuture.runAsync(this::listenToInput).join();
    }

    private void close() {
        isRunning = false;
        try {
            if (server != null) server.close();
        } catch (IOException ignored) {}
        System.out.println(TextFX.colorize("Client closed.", TextFX.Color.RED));
    }

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.start();
    }
}

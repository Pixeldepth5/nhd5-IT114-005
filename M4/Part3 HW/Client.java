package M4.Part3HW;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

/**
 * nhd5
 *
 * Part 3 Client
 * Connects to the server and sends raw user input,
 * including /pm <id> <message> and /who commands.
 *
 * Source (client socket usage):
 * https://www.w3schools.com/java/java_networking.asp
 */
public class Client {

    private Socket server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean isRunning = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.start("localhost", 3000);
    }

    /**
     * Establishes socket connection to server and starts
     * simultaneous input + read loops.
     *
     * @param address hostname (localhost)
     * @param port    port to connect to
     */
    public void start(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            isRunning = true;

            // Thread to read messages coming *from* server
            Thread reader = new Thread(this::readLoop);
            reader.start();

            // Main thread reads user input and forwards to server
            try (Scanner scanner = new Scanner(System.in)) {
                System.out.println("Connected. Use /pm <id> <message> or /who");
                while (isRunning) {
                    String line = scanner.nextLine();
                    send(line);
                }
            }

        } catch (IOException e) {
            System.out.println("Unable to connect to server");
            e.printStackTrace();
        }
    }

    /**
     * Background loop that receives messages from the server.
     * Uses ObjectInputStream.readObject() which blocks until data arrives.
     */
    private void readLoop() {
        try {
            while (isRunning) {
                Object obj = in.readObject();
                if (obj instanceof String msg) {
                    System.out.println(msg);
                }
            }
        } catch (Exception e) {
            System.out.println("Disconnected from server");
            isRunning = false;
        }
    }

    /**
     * Sends user-entered text to the server.
     *
     * @param message text or command (/pm, /who, etc.)
     */
    private void send(String message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.out.println("Error sending to server");
            isRunning = false;
        }
    }
}

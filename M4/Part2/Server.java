package M4.Part2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * nhd5
 * 
 * Part 2 Server - Single connection version
 * Adds support for `/pm <targetId> <message>` per assignment challenge.
 * 
 * Source (basic server socket example):
 * https://www.w3schools.com/java/java_networking.asp
 */
public class Server {
    private int port = 3000;
    // In Part 2 we simulate a single user with ID=1
    private final int VALID_TARGET_ID = 1;

    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);

        try (ServerSocket serverSocket = new ServerSocket(port);
             Socket client = serverSocket.accept();  // blocking - one client only
             PrintWriter out = new PrintWriter(client.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {

            System.out.println("Client connected, waiting for message");
            String fromClient;

            while ((fromClient = in.readLine()) != null) {
                System.out.println("From client: " + fromClient);

                if ("/kill server".equalsIgnoreCase(fromClient)) {
                    System.out.println("Client killed server");
                    break;
                }
                else if (fromClient.startsWith("/reverse")) {
                    StringBuilder sb = new StringBuilder(fromClient.replace("/reverse ", ""));
                    sb.reverse();
                    String rev = sb.toString();
                    System.out.println("To client: " + rev);
                    out.println(rev);
                }
                else if (fromClient.startsWith("/pm")) {
                    handlePrivateMessage(fromClient, out);
                }
                else {
                    out.println(fromClient);
                }
            }

        } catch (IOException e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            System.out.println("closing server socket");
        }
    }

    /**
     * Handles the `/pm <id> <message>` private message command.
     * This simulates only a single user (ID=1).
     *
     * @param pmMessage the raw `/pm ...` string from client
     * @param out       PrintWriter to send result back
     *
     * Source for string split:
     * https://www.w3schools.com/java/ref_string_split.asp
     */
    private void handlePrivateMessage(String pmMessage, PrintWriter out) {
        String[] parts = pmMessage.split("\\s+", 3);

        if (parts.length < 3) {
            out.println("Usage: /pm <targetId> <message>");
            return;
        }

        try {
            int targetId = Integer.parseInt(parts[1]);
            String message = parts[2];

            if (targetId == VALID_TARGET_ID) {
                // Format required per rubric: PM from <who>: <message>
                out.println("PM from 1: " + message);
            } else {
                out.println("User " + targetId + " not found");
            }

        } catch (NumberFormatException e) {
            out.println("Invalid target id");
        }
    }

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // ignore invalid arg
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}

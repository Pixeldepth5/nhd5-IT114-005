package M4.Part2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * nhd5
 * 
 * Part 2 Client - Single connection version
 * Adds support for `/pm <targetId> <message>` command which
 * is forwarded directly to the server for processing.
 * 
 * Source (regex usage):
 * https://www.w3schools.com/java/java_regex.asp
 */
public class Client {

    private Socket server = null;
    private PrintWriter out = null;
    private BufferedReader in = null;

    // Matches /connect <ip:port> or /connect localhost:port
    final Pattern ipAddressPattern = Pattern.compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");

    // Matches PM command: /pm <id> <message>
    final Pattern pmPattern = Pattern.compile("^/pm\\s+\\d+\\s+.+$");

    private boolean isRunning = false;

    public Client() {
        System.out.println("Client Created");
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();
    }

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new PrintWriter(server.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(server.getInputStream()));
            System.out.println("Client connected");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    private boolean isConnection(String text) {
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * Processes commands typed by the user, including /pm
     * 
     * @param text user input
     * @return true if the command was handled
     */
    private boolean processClientCommand(String text) {
        if (isConnection(text)) {
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            return true;
        }
        else if (pmPattern.matcher(text).matches()) {
            // PM command detected -> forward as-is to server
            if (isConnected()) {
                out.println(text);
            } else {
                System.out.println("Not connected to server");
            }
            return true;
        }
        else if ("/quit".equalsIgnoreCase(text)) {
            isRunning = false;
            return true;
        }
        return false;
    }

    public void start() throws IOException {
        System.out.println("Client starting");
        try (Scanner si = new Scanner(System.in)) {
            String line = "";
            isRunning = true;
            while (isRunning) {
                try {
                    System.out.println("Waiting for input");
                    line = si.nextLine();
                    if (!processClientCommand(line)) {
                        if (isConnected()) {
                            out.println(line);
                            if (out.checkError()) {
                                System.out.println("Connection to server may have been lost");
                            }
                            String fromServer = in.readLine();
                            if (fromServer != null) {
                                System.out.println("Reply from server: " + fromServer);
                            } else {
                                System.out.println("Server disconnected");
                                break;
                            }
                        } else {
                            System.out.println("Not connected to server");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Connection dropped");
                    break;
                }
            }
            System.out.println("Exited loop");
        } finally {
            close();
        }
    }

    private void close() {
        try {
            out.close();
        } catch (Exception ignored) {}
        try {
            in.close();
        } catch (Exception ignored) {}
        try {
            server.close();
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

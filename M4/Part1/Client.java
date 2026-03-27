package M4.Part1;

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
 * Client that sends coin flip commands to a server
 * 
 * HOW THIS CODE WAS BUILT:
 * 1. Started with base client code that could connect to server
 * 2. Added BufferedReader to receive messages FROM server (needed to get flip results back)
 * 3. Created /flip command pattern using regex to detect when user types /flip
 * 4. Added flip handling logic in processClientCommand() to send /flip and wait for response
 * 5. Updated close() method to properly close the input stream
 */
public class Client {

    private Socket server = null;
    private PrintWriter out = null;
    
    // CHANGE #1: Added BufferedReader to RECEIVE messages from server
    // We need this because when we flip a coin, the server sends back the result
    // Without this, we can only SEND to server, not RECEIVE from server
    // Source: https://www.w3schools.com/java/java_files_read.asp (BufferedReader basics)
    private BufferedReader in = null;
    
    // Regex patterns to recognize commands
    // Source: https://www.w3schools.com/java/java_regex.asp
    final Pattern ipAddressPattern = Pattern
            .compile("/connect\\s+(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{3,5})");
    final Pattern localhostPattern = Pattern.compile("/connect\\s+(localhost:\\d{3,5})");
    
    // CHANGE #2: Added pattern to detect /flip command
    // This pattern looks for exactly "/flip" - no extra spaces or characters
    // Source: https://www.w3schools.com/java/java_regex.asp
    final Pattern flipPattern = Pattern.compile("/flip");
    
    private boolean isRunning = false;

    public Client() {
        System.out.println("Client Created");
    }

    public boolean isConnected() {
        if (server == null) {
            return false;
        }
        // https://stackoverflow.com/a/10241044
        // Note: these check the client's end of the socket connect; therefore they
        // don't really help determine
        // if the server had a problem and is just for lesson's sake
        return server.isConnected() && !server.isClosed() && !server.isInputShutdown() && !server.isOutputShutdown();

    }

    /**
     * Takes an ip address and a port to attempt a socket connection to a server.
     * 
     * @param address
     * @param port
     * @return true if connection was successful
     */
    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            // channel to send to server
            out = new PrintWriter(server.getOutputStream(), true);
            
            // CHANGE #3: Initialize BufferedReader to receive messages FROM server
            // getInputStream() gets data coming FROM the server TO us
            // InputStreamReader converts the raw bytes to characters
            // BufferedReader makes it easy to read line by line
            // Source: https://www.w3schools.com/java/java_files_read.asp
            in = new BufferedReader(new InputStreamReader(server.getInputStream()));
            
            System.out.println("Client connected");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    /**
     * <p>
     * Check if the string contains the <i>connect</i> command
     * followed by an ip address and port or localhost and port.
     * </p>
     * <p>
     * Example format: 123.123.123:3000
     * </p>
     * <p>
     * Example format: localhost:3000
     * </p>
     * https://www.w3schools.com/java/java_regex.asp
     * 
     * @param text
     * @return
     */
    private boolean isConnection(String text) {
        // https://www.w3schools.com/java/java_regex.asp
        Matcher ipMatcher = ipAddressPattern.matcher(text);
        Matcher localhostMatcher = localhostPattern.matcher(text);
        return ipMatcher.matches() || localhostMatcher.matches();
    }

    /**
     * CHANGE #4: New method to check if user typed /flip command
     * Uses regex pattern matching to detect the exact string "/flip"
     * 
     * @param text - the input from user
     * @return true if text matches /flip pattern
     * Source: https://www.w3schools.com/java/java_regex.asp
     */
    private boolean isFlip(String text) {
        Matcher flipMatcher = flipPattern.matcher(text);
        return flipMatcher.matches();
    }

    /**
     * Controller for handling various text commands.
     * <p>
     * Add more here as needed
     * </p>
     * 
     * @param text
     * @return true if a text was a command or triggered a command
     */
    private boolean processClientCommand(String text) {
        if (isConnection(text)) {
            // replaces multiple spaces with single space
            // splits on the space after connect (gives us host and port)
            // splits on : to get host as index 0 and port as index 1
            String[] parts = text.trim().replaceAll(" +", " ").split(" ")[1].split(":");
            connect(parts[0].trim(), Integer.parseInt(parts[1].trim()));
            return true;
        } 
        // CHANGE #5: Handle /flip command
        // This is where the magic happens for the coin flip feature
        else if (isFlip(text)) {
            // First, make sure we're connected to a server
            if (isConnected()) {
                // Step 1: Send the /flip command to the server
                // The server is waiting for this message
                // Source: https://www.w3schools.com/java/java_files_create.asp (PrintWriter usage)
                out.println(text);
                
                try {
                    // Step 2: WAIT for the server to send back the result
                    // readLine() is a BLOCKING call - it waits until server sends something
                    // The server will send back something like "Server flipped a coin and got heads"
                    // Source: https://www.w3schools.com/java/java_files_read.asp
                    String response = in.readLine();
                    
                    // Step 3: Display the result to the user
                    // Check for null in case connection was lost
                    if (response != null) {
                        System.out.println(response);
                    }
                } catch (IOException e) {
                    // If something goes wrong reading from server, show error
                    System.out.println("Error reading flip result from server");
                    e.printStackTrace();
                }
            } else {
                // Can't flip if we're not connected!
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
        try (Scanner si = new Scanner(System.in);) {
            String line = "";
            isRunning = true;
            while (isRunning) {
                try {
                    System.out.println("Waiting for input");
                    line = si.nextLine();
                    if (!processClientCommand(line)) {
                        if (isConnected()) {
                            out.println(line);
                            // https://stackoverflow.com/a/8190411
                            // you'll notice it triggers on the second request after server socket closes
                            if (out.checkError()) {
                                System.out.println("Connection to server may have been lost");
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
        } catch (Exception e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            close();
        }
    }

    /**
     * CHANGE #6: Updated close() to also close the input stream
     * Important to close ALL resources to prevent memory leaks
     * Source: https://www.w3schools.com/java/java_files_read.asp
     */
    private void close() {
        // Close input stream (BufferedReader)
        try {
            System.out.println("Closing input stream");
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Close output stream (PrintWriter)
        try {
            System.out.println("Closing output stream");
            out.close();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Close socket connection
        try {
            System.out.println("Closing connection");
            server.close();
            System.out.println("Closed socket");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException ne) {
            System.out.println("Server was never opened so this exception is ok");
        }
    }

    public static void main(String[] args) {
        Client client = new Client();

        try {
            // if start is private, it's valid here since this main is part of the class
            client.start();
        } catch (IOException e) {
            System.out.println("Exception from main()");
            e.printStackTrace();
        }
    }
}
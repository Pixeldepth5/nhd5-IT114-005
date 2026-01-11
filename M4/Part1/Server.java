package M4.Part1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

/**
 * nhd5
 * Server that handles client connections and processes coin flip commands
 * 
 * HOW THIS CODE WAS BUILT:
 * 1. Started with base server code that could receive messages from client
 * 2. Added Random object to generate random coin flip results (0 or 1)
 * 3. Created flipCoin() method that generates random number and converts to heads/tails
 * 4. Added PrintWriter to SEND messages back TO client (needed to return flip results)
 * 5. Added /flip command detection in the message loop
 * 6. When /flip is received, call flipCoin() and send result back to client
 */
public class Server {
    private int port = 3000;
    
    // CHANGE #1: Added Random object to generate random numbers for coin flips
    // Random is a built-in Java class that generates random numbers
    // We'll use nextInt(2) to get either 0 or 1 (representing heads or tails)
    // Source: https://www.w3schools.com/java/java_howto_random_number.asp
    private Random random = new Random();

    /**
     * CHANGE #2: Created flipCoin() method to handle the coin flip logic
     * This method does three things:
     * 1. Generates a random number (0 or 1)
     * 2. Converts the number to "heads" or "tails"
     * 3. Returns a formatted message as required by the assignment
     * 
     * @return String message in format "<who> flipped a coin and got <result>"
     * Sources:
     * - Random numbers: https://www.w3schools.com/java/java_howto_random_number.asp
     * - If-else: https://www.w3schools.com/java/java_conditions.asp
     * - String concatenation: https://www.w3schools.com/java/java_strings_concat.asp
     */
    private String flipCoin() {
        // Step 1: Generate random number - either 0 or 1
        // nextInt(2) generates a random integer from 0 (inclusive) to 2 (exclusive)
        // So we get either 0 or 1
        // Source: https://www.w3schools.com/java/java_howto_random_number.asp
        int result = random.nextInt(2);
        
        // Step 2: Convert the number to a word
        // If we got 0, the coin landed on "heads"
        // If we got 1, the coin landed on "tails"
        // Source: https://www.w3schools.com/java/java_conditions.asp
        String outcome;
        if (result == 0) {
            outcome = "heads";
        } else {
            outcome = "tails";
        }
        
        // Step 3: Build the message string
        // Format required by assignment: "<who> flipped a coin and got <result>"
        // We use string concatenation with the + operator
        // Source: https://www.w3schools.com/java/java_strings_concat.asp
        String message = "Server flipped a coin and got " + outcome;
        
        // Return the complete message
        return message;
    }

    private void start(int port) {
        this.port = port;
        System.out.println("Listening on port " + this.port);
        // server listening
        try (ServerSocket serverSocket = new ServerSocket(port);
                // client wait
                Socket client = serverSocket.accept(); // blocking;

                // read from client
                BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                
                // CHANGE #3: Added PrintWriter to SEND messages back TO the client
                // Before this change, server could only RECEIVE messages, not SEND them
                // We need this to send the flip result back to the client
                // PrintWriter writes text to the output stream
                // The 'true' parameter enables auto-flush (sends immediately)
                // Source: https://www.w3schools.com/java/java_files_create.asp (PrintWriter basics)
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);) {

            System.out.println("Client connected, waiting for message");
            String fromClient = "";
            
            // Main loop - keep reading messages from client
            // Source: https://www.w3schools.com/java/java_while_loop.asp
            while ((fromClient = in.readLine()) != null) {
                if ("/kill server".equalsIgnoreCase(fromClient)) {
                    // normally you wouldn't have a remote kill command, this is just for example
                    // sake
                    System.out.println("Client killed server");
                    break;
                } 
                // CHANGE #4: Added handling for /flip command
                // When client sends "/flip", we need to:
                // 1. Call flipCoin() to get the result
                // 2. Print it to server console (so we can see it)
                // 3. Send it back to the client using PrintWriter
                // Source: https://www.w3schools.com/java/java_conditions.asp (if-else)
                else if ("/flip".equalsIgnoreCase(fromClient)) {
                    // Step 1: Call flipCoin() method to get the flip result
                    // This returns a string like "Server flipped a coin and got heads"
                    String flipResult = flipCoin();
                    
                    // Step 2: Display the result on the server's console
                    // This lets us (the server operator) see what happened
                    // Source: https://www.w3schools.com/java/java_output.asp
                    System.out.println(flipResult);
                    
                    // Step 3: Send the result back to the client
                    // This is the KEY line - the client is waiting for this message!
                    // out.println() sends the message through the socket to the client
                    // Source: https://www.w3schools.com/java/java_files_create.asp
                    out.println(flipResult);
                } 
                else {
                    // For any other message, just print it to server console
                    System.out.println("From client: " + fromClient);
                }
            }
        } catch (IOException e) {
            System.out.println("Exception from start()");
            e.printStackTrace();
        } finally {
            System.out.println("closing server socket");
        }
    }

    public static void main(String[] args) {
        System.out.println("Server Starting");
        Server server = new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // can ignore, will either be index out of bounds or type mismatch
            // will default to the defined value prior to the try/catch
        }
        server.start(port);
        System.out.println("Server Stopped");
    }
}
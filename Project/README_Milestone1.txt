==================================================
IT114 – Milestone 1 Submission | TriviaGuessGame
==================================================

Student: Nilkanth Dhariya (UCID: nhd5)
Date: November 3, 2025
Project Title: Trivia Guess Game
Branch: Milestone1
Port: 3000 (default)
Server/Client Architecture: Multi-threaded with Rooms

--------------------------------------------------
📘 Purpose
--------------------------------------------------
This project implements the Milestone 1 requirements based on the Part 5 baseline provided in class. 
The server handles multiple clients simultaneously and supports the creation and joining of rooms, 
starting with a default “lobby.” Each client connects through the command line using /name and /connect 
commands. The implementation is adapted for the Trivia Guess Game theme while maintaining all 
required network functionality for Milestone 1.

--------------------------------------------------
💡 Commands
--------------------------------------------------
/name <yourname>    – Sets the client name
/connect localhost:3000  – Connects to the server
/createroom <roomname>  – Creates a new room
/joinroom <roomname>   – Joins an existing room
/quit              – Disconnects the client

--------------------------------------------------
🧠 Testing Steps
--------------------------------------------------
1. Run `Server.java` from the Server package.
2. Open multiple terminal windows and run `Client.java` from the Client package in each.
3. Set different names with `/name Alice`, `/name Bob`, `/name Charlie`.
4. Connect each to `localhost:3000`.
5. Use `/createroom Trivia1` and `/joinroom Trivia1` to verify room management.
6. Send messages between clients to see multi-room broadcasts.

--------------------------------------------------
🔗 References
--------------------------------------------------
- Java Basics: https://www.w3schools.com/java/
- Sockets and Networking: https://www.w3schools.com/java/java_networking.asp
- Threads in Java: https://www.w3schools.com/java/java_threads.asp

--------------------------------------------------
✅ Submission Checklist
--------------------------------------------------
☑ All required packages (Client, Server, Common, Exceptions)
☑ Proper UCID and Date in headers
☑ Server listens on port 3000 and accepts multiple clients
☑ Clients can /name, /connect, /createroom, /joinroom
☑ PDF worksheet with screenshots ready for upload
☑ Git branch Milestone1 created and pushed
☑ Pull Request linked to main

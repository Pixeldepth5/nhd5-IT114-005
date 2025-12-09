# Assignment Code Snippets - Trivia Game

This document contains code snippets organized by assignment requirement. Each section shows the relevant code with file paths and line numbers.

---

## 1. Connection/Details Panels

### Client-Side: Connection Panel UI
**File:** `Client/Client.java`  
**Lines:** 138-194

```java
/**
 * Creates the connection panel with username, host, port fields and connect button.
 */
private JPanel buildConnectionPanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new GridBagLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Username"));
  
  // Username field
  txtUser = createTextField("Player", 15);
  
  // Host field
  txtHost = createTextField("localhost", 15);
  
  // Port field
  txtPort = createTextField("3000", 15);
  
  // Connect button
  btnConnect = createConnectButton();
  
  return panel;
}
```

### Client-Side: Connection Handler
**File:** `Client/Client.java`  
**Lines:** 470-501

```java
/**
 * Handles when user clicks the Connect button.
 * Creates socket connection and sends connection payload to server.
 */
private void connectClicked(ActionEvent e) {
  if (connected) return;
  
  try {
      String host = txtHost.getText().trim();
      int port = Integer.parseInt(txtPort.getText().trim());
      
      socket = new Socket(host, port);
      out = new ObjectOutputStream(socket.getOutputStream());
      in = new ObjectInputStream(socket.getInputStream());
      connected = true;
      
      ConnectionPayload cp = new ConnectionPayload();
      cp.setPayloadType(PayloadType.CLIENT_CONNECT);
      cp.setClientName(currentName);
      send(cp);
      
      startReaderThread();
  } catch (Exception ex) {
      appendEvent("Connect failed: " + ex.getMessage());
  }
}
```

### Server-Side: Connection Processing
**File:** `Server/ServerThread.java`  
**Lines:** 26-36

```java
@Override
protected void processPayload(Payload incoming) {
    switch (incoming.getPayloadType()) {
        case CLIENT_CONNECT -> setClientName(((ConnectionPayload) incoming).getClientName().trim());
        case DISCONNECT     -> currentRoom.handleDisconnect(this);
        case MESSAGE        -> currentRoom.handleMessage(this, incoming.getMessage());
        // ... other cases
    }
}
```

---

## 2. Ready Panel

### Client-Side: Ready Checkbox Creation
**File:** `Client/Client.java`  
**Lines:** 245-256

```java
/**
 * Creates the Ready checkbox.
 * When checked, marks the player as ready to start the game.
 */
private JCheckBox createReadyCheckbox() {
  JCheckBox chkReady = new JCheckBox("Ready");
  chkReady.setOpaque(false);
  chkReady.setForeground(new Color(30, 60, 120));
  TextFX.setSubtitleFont(chkReady);
  chkReady.addActionListener(e -> {
      if (chkReady.isSelected() && !isReady) {
          readyClicked();
      }
  });
  return chkReady;
}
```

### Client-Side: Ready Click Handler
**File:** `Client/Client.java`  
**Lines:** 636-644

```java
/**
 * Handles when user clicks Ready checkbox.
 * Sends /ready command to server.
 */
private void readyClicked() {
  if (!connected) return;
  if (isReady) {
      appendEvent("You are already ready!");
      return;
  }
  sendCommand("/ready");
  isReady = true;
}
```

### Server-Side: Ready Handler
**File:** `Server/GameRoom.java`  
**Lines:** 203-225

```java
/**
 * Handles /ready command from client.
 * Marks player as ready and starts game if all players are ready.
 */
private void handleReady(ServerThread sender) {
    long id = sender.getClientId();
    
    if (spectator.getOrDefault(id, false)) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Spectators cannot ready up.");
        return;
    }
    
    ready.put(id, true);
    
    if (hostClientId == Constants.DEFAULT_CLIENT_ID) {
        hostClientId = id;
        broadcast(null, sender.getDisplayName() + " is the session host.");
    }
    
    broadcast(null, sender.getDisplayName() + " is ready.");
    sendUserListToAll();
    
    if (!sessionActive && allActivePlayersReady()) {
        startSession();
    }
}
```

### Server-Side: Ready Check Logic
**File:** `Server/GameRoom.java`  
**Lines:** 227-245

```java
/**
 * Checks if all active (non-away, non-spectator) players are ready.
 * Requires at least 2 active players.
 */
private boolean allActivePlayersReady() {
    int activeCount = 0;
    int readyCount = 0;
    
    for (ServerThread st : getClients()) {
        long id = st.getClientId();
        if (spectator.getOrDefault(id, false)) continue;
        if (away.getOrDefault(id, false)) continue;
        
        activeCount++;
        if (ready.getOrDefault(id, false)) {
            readyCount++;
        }
    }
    
    // Need at least 2 active players and all must be ready
    return activeCount >= 2 && activeCount == readyCount;
}
```

---

## 3. User List Panel

### Client-Side: User List Panel Creation
**File:** `Client/Client.java`  
**Lines:** 258-293

```java
/**
 * Creates the players list panel that shows all connected players.
 */
private JPanel buildUserPanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Players"));
  
  createUserList();
  setupUserListRenderer();
  
  panel.add(new JScrollPane(lstUsers), BorderLayout.CENTER);
  return panel;
}

/**
 * Creates the user list component.
 */
private void createUserList() {
  userModel = new DefaultListModel<>();
  lstUsers = new JList<>(userModel);
  lstUsers.setBackground(Color.WHITE);
  lstUsers.setForeground(new Color(30, 60, 120));
}
```

### Client-Side: User List Update Handler
**File:** `Client/Client.java`  
**Lines:** 584-600

```java
/**
 * Handles USER_LIST payload from server.
 * Updates the displayed list of players with their points and status.
 */
private void handleUserList(UserListPayload up) {
  userModel.clear();
  users.clear();
  
  for (int i = 0; i < up.getClientIds().size(); i++) {
      long id = up.getClientIds().get(i);
      String name = up.getDisplayNames().get(i);
      int pts = up.getPoints().get(i);
      boolean locked = up.getLockedIn().get(i);
      boolean away = up.getAway().get(i);
      boolean spec = up.getSpectator().get(i);
      
      User u = new User(id, name, pts, locked, away, spec);
      users.put(id, u);
      userModel.addElement(u);
  }
}
```

### Server-Side: User List Sync
**File:** `Server/GameRoom.java`  
**Lines:** 641-660

```java
/**
 * Sends updated user list to all clients.
 * Includes points, locked status, away status, and spectator status.
 */
private void sendUserListToAll() {
    UserListPayload up = new UserListPayload();
    up.setPayloadType(PayloadType.USER_LIST);
    up.setClientId(Constants.DEFAULT_CLIENT_ID);
    
    // Collect data for each client
    for (ServerThread st : getClients()) {
        long id = st.getClientId();
        String name = st.getDisplayName();
        int pts = points.getOrDefault(id, 0);
        boolean locked = lockedThisRound.getOrDefault(id, false);
        boolean isAway = away.getOrDefault(id, false);
        boolean isSpec = spectator.getOrDefault(id, false);
        
        up.addUser(id, name, pts, locked, isAway, isSpec);
    }
    
    // Send to all clients
    for (ServerThread st : getClients()) {
        st.sendPayload(up);
    }
}
```

### User Display Format
**File:** `Client/User.java`  
**Lines:** 26-34

```java
/**
 * Formats user information for display in the list.
 * Shows: name (id) | pts: X [LOCKED] [AWAY] [SPECTATOR]
 */
public String toDisplayString() {
    StringBuilder sb = new StringBuilder();
    sb.append(name).append(" (").append(id).append(") ");
    sb.append("| pts: ").append(points);
    if (locked) sb.append(" [LOCKED]");
    if (away) sb.append(" [AWAY]");
    if (spectator) sb.append(" [SPECTATOR]");
    return sb.toString();
}
```

---

## 4. Game Events Panel

### Client-Side: Events Panel Creation
**File:** `Client/Client.java`  
**Lines:** 450-465

```java
/**
 * Creates the game events panel that shows game-related messages.
 */
private JPanel buildEventsPanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Game Events"));
  
  txtEvents = new JTextArea();
  txtEvents.setEditable(false);
  txtEvents.setLineWrap(true);
  txtEvents.setWrapStyleWord(true);
  txtEvents.setBackground(Color.WHITE);
  txtEvents.setForeground(new Color(30, 30, 30));
  
  panel.add(new JScrollPane(txtEvents), BorderLayout.CENTER);
  return panel;
}
```

### Client-Side: Append Event Method
**File:** `Client/Client.java`  
**Lines:** 743-746

```java
/**
 * Adds a message to the game events panel.
 * @param msg The message to display
 */
private void appendEvent(String msg) {
  txtEvents.append(msg + "\n");
  txtEvents.setCaretPosition(txtEvents.getDocument().getLength());
}
```

### Server-Side: Broadcast Messages
**File:** `Server/Room.java`  
**Lines:** 112-118

```java
/**
 * Broadcasts a game/event message to all clients in this room.
 * Used for game events, not chat.
 */
protected synchronized void broadcast(ServerThread sender, String message) {
    for (ServerThread client : clients.values()) {
        if (sender == null || client != sender) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, message);
        }
    }
}
```

### Server-Side: Timer Updates
**File:** `Server/GameRoom.java`  
**Lines:** 729-738

```java
/**
 * Sends timer update to all clients.
 * @param seconds The number of seconds remaining
 */
private void sendTimerUpdate(int seconds) {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.TIMER);
    p.setClientId(Constants.DEFAULT_CLIENT_ID);
    p.setMessage(Integer.toString(seconds));
    
    for (ServerThread st : getClients()) {
        st.sendPayload(p);
    }
}
```

### Client-Side: Timer Display
**File:** `Client/Client.java`  
**Lines:** 518-537

```java
private void handlePayload(Payload p) {
  switch (p.getPayloadType()) {
      case TIMER -> lblTimer.setText("Timer: " + p.getMessage());
      // ... other cases
  }
}
```

---

## 5. Game Area

### Client-Side: Game Panel Creation
**File:** `Client/Client.java`  
**Lines:** 375-445

```java
/**
 * Creates the main game panel with question, answers, category, and timer.
 */
private JPanel buildGamePanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout(10, 10));
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Question"));
  
  // Category and timer at top
  JPanel topPanel = createCategoryTimerPanel();
  panel.add(topPanel, BorderLayout.NORTH);
  
  // Question display in center
  JScrollPane questionDisplay = createQuestionDisplay();
  panel.add(questionDisplay, BorderLayout.CENTER);
  
  // Answer buttons at bottom
  JPanel bottomContainer = createAnswerButtonsContainer();
  panel.add(bottomContainer, BorderLayout.SOUTH);
  
  return panel;
}
```

### Client-Side: Question Display Handler
**File:** `Client/Client.java`  
**Lines:** 561-582

```java
/**
 * Handles QUESTION payload from server.
 * Updates the UI to show the current question and answer options.
 */
private void handleQA(QAPayload q) {
  lockedThisRound = false;
  lblCategory.setText("Category: " + q.getCategory());
  txtQuestion.setText(q.getQuestionText());
  txtQuestion.setCaretPosition(0);
  
  ArrayList<String> ans = q.getAnswers();
  for (int i = 0; i < 4; i++) {
      if (ans != null && i < ans.size()) {
          answerButtons[i].setText(ans.get(i));
          answerButtons[i].setEnabled(true);
          answerButtons[i].setBackground(Color.WHITE);
          answerButtons[i].setForeground(new Color(30, 60, 120));
      } else {
          answerButtons[i].setText("-");
          answerButtons[i].setEnabled(false);
      }
  }
}
```

### Client-Side: Answer Click Handler
**File:** `Client/Client.java`  
**Lines:** 605-615

```java
/**
 * Handles when user clicks an answer button.
 * Sends answer to server and locks in the choice.
 */
private void answerClicked(int idx) {
  if (!connected || lockedThisRound || isSpectator || isAway) return;
  
  sendCommand("/answer " + idx);
  lockedThisRound = true;
  
  // Disable all buttons and highlight selected
  for (JButton btn : answerButtons) btn.setEnabled(false);
  answerButtons[idx].setBackground(new Color(200, 220, 255));
  answerButtons[idx].setForeground(new Color(30, 60, 120));
}
```

### Server-Side: Question Sending
**File:** `Server/GameRoom.java`  
**Lines:** 662-676

```java
/**
 * Sends the current question to all clients.
 */
private void sendQuestionToAll() {
    if (currentQuestion == null) return;
    
    QAPayload qa = new QAPayload();
    qa.setPayloadType(PayloadType.QUESTION);
    qa.setClientId(Constants.DEFAULT_CLIENT_ID);
    qa.setCategory(currentQuestion.category);
    qa.setQuestionText(currentQuestion.text);
    qa.getAnswers().clear();
    qa.getAnswers().addAll(currentQuestion.answers);
    
    for (ServerThread st : getClients()) {
        st.sendPayload(qa);
    }
}
```

### Server-Side: Answer Handler
**File:** `Server/GameRoom.java`  
**Lines:** 493-540

```java
/**
 * Handles /answer command from client.
 * Records the answer and checks if all players have locked in.
 */
private void handleAnswer(ServerThread sender, String arg) {
    if (!sessionActive || currentQuestion == null) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "No active round – wait for the next question.");
        return;
    }
    
    long id = sender.getClientId();
    if (spectator.getOrDefault(id, false)) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Spectators cannot answer.");
        return;
    }
    if (away.getOrDefault(id, false)) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "You are marked AWAY. Use /back to participate again.");
        return;
    }
    if (lockedThisRound.getOrDefault(id, false)) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "You already locked in this round.");
        return;
    }
    
    int index = mapChoiceToIndex(arg);
    boolean correct = (index == currentQuestion.correctIndex);
    lockedThisRound.put(id, true);
    
    broadcast(null, sender.getDisplayName() +
            (correct ? " locked in the CORRECT answer!" :
                    " locked in the WRONG answer."));
    
    if (correct) {
        correctOrder.add(id);
    }
    
    sendUserListToAll();
    
    if (allActivePlayersLocked()) {
        endRound("All active players locked in.");
    }
}
```

---

## 6. Category Selection

### Client-Side: Category Checkboxes
**File:** `Client/Client.java`  
**Lines:** 245-253

```java
/**
 * Creates a category checkbox.
 * @param text The category name (e.g., "Geography", "Science")
 */
private JCheckBox makeCategory(String text) {
  JCheckBox box = new JCheckBox(text);
  box.setOpaque(false);
  box.setSelected(true);
  box.setForeground(new Color(30, 60, 120));
  TextFX.setSubtitleFont(box);
  box.addActionListener(e -> sendCategories());
  return box;
}
```

### Client-Side: Send Categories
**File:** `Client/Client.java`  
**Lines:** 657-673

```java
/**
 * Sends selected categories to server.
 * Only the host can change categories during ready check.
 */
private void sendCategories() {
  if (!connected) return;
  
  String cats = "";
  if (chkGeo.isSelected()) cats += "geography,";
  if (chkSci.isSelected()) cats += "science,";
  if (chkMath.isSelected()) cats += "math,";
  if (chkHist.isSelected()) cats += "history,";
  
  if (!cats.isEmpty()) {
      cats = cats.substring(0, cats.length() - 1);
      sendCommand("/categories " + cats);
  }
}
```

### Server-Side: Category Handler
**File:** `Server/GameRoom.java`  
**Lines:** 313-341

```java
/**
 * Handles /categories command from host.
 * Updates which categories are enabled for the session.
 */
private void handleCategories(ServerThread sender, String csv) {
    if (sessionActive) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Categories can be changed only before the game starts.");
        return;
    }
    if (sender.getClientId() != hostClientId) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Only the session host can change categories.");
        return;
    }
    
    enabledCategories.clear();
    if (csv == null || csv.trim().isEmpty()) {
        return;
    }
    
    String[] parts = csv.split(",");
    for (String p : parts) {
        String cat = p.trim().toLowerCase();
        if (!cat.isEmpty()) {
            enabledCategories.add(cat);
        }
    }
    
    broadcast(null, "Host set categories to: " + enabledCategories);
}
```

### Server-Side: Category Filtering in Question Selection
**File:** `Server/GameRoom.java`  
**Lines:** 468-487

```java
/**
 * Draws a random question from the pool, respecting enabled categories.
 */
private Question drawRandomQuestion() {
    if (questionPool.isEmpty()) return null;
    
    ArrayList<Integer> eligible = new ArrayList<>();
    for (int i = 0; i < questionPool.size(); i++) {
        Question q = questionPool.get(i);
        if (enabledCategories.isEmpty() || enabledCategories.contains(q.category)) {
            eligible.add(i);
        }
    }
    
    if (eligible.isEmpty()) {
        int idx = rng.nextInt(questionPool.size());
        return questionPool.remove(idx);
    } else {
        int poolIndex = eligible.get(rng.nextInt(eligible.size()));
        return questionPool.remove(poolIndex);
    }
}
```

---

## 7. Add New Questions

### Client-Side: Add Question Dialog
**File:** `Client/Client.java`  
**Lines:** 675-705

```java
/**
 * Shows a dialog for adding a new question.
 * Can only be done outside of an active session.
 */
private void showAddQDialog() {
  if (!connected) {
      appendEvent("Not connected.");
      return;
  }
  
  String cat = JOptionPane.showInputDialog(frame,
          "Category (geography, science, math, history):");
  if (cat == null || cat.trim().isEmpty()) return;
  
  String q = JOptionPane.showInputDialog(frame, "Question:");
  if (q == null || q.trim().isEmpty()) return;
  
  String a1 = JOptionPane.showInputDialog(frame, "Answer A:");
  String a2 = JOptionPane.showInputDialog(frame, "Answer B:");
  String a3 = JOptionPane.showInputDialog(frame, "Answer C:");
  String a4 = JOptionPane.showInputDialog(frame, "Answer D:");
  String correct = JOptionPane.showInputDialog(frame,
          "Correct index (0=A, 1=B, 2=C, 3=D):");
  
  if (a1 == null || a2 == null || a3 == null || a4 == null || correct == null) return;
  
  String line = cat + "|" + q + "|" + a1 + "|" + a2 + "|" + a3 + "|" + a4 + "|" + correct;
  sendCommand("/addq " + line);
}
```

### Server-Side: Add Question Handler
**File:** `Server/GameRoom.java`  
**Lines:** 744-760

```java
/**
 * Handles /addq command from client.
 * Adds a new question to the question pool.
 */
private void handleAddQuestion(ServerThread sender, String args) {
    if (sessionActive) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "You can only add questions while no game is running.");
        return;
    }
    
    Question q = parseQuestionLine(args);
    if (q == null) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Invalid /addq format. Use category|question|a1|a2|a3|a4|correctIndex");
        return;
    }
    
    questionPool.add(q);
    broadcast(null, "New question added in '" + q.category +
            "' by " + sender.getDisplayName() + ".");
}
```

---

## 8. Away Status

### Client-Side: Away Checkbox
**File:** `Client/Client.java`  
**Lines:** 280-289

```java
/**
 * Creates the Away checkbox.
 * When checked, marks the player as away (skipped in rounds but still in game).
 */
private JCheckBox createAwayCheckbox() {
  JCheckBox box = new JCheckBox("Away");
  box.setOpaque(false);
  box.setForeground(new Color(30, 60, 120));
  box.addActionListener(e -> toggleAway());
  TextFX.setSubtitleFont(box);
  return box;
}
```

### Client-Side: Toggle Away
**File:** `Client/Client.java`  
**Lines:** 655-659

```java
/**
 * Toggles away status and sends command to server.
 */
private void toggleAway() {
  isAway = chkAway.isSelected();
  if (isAway) sendCommand("/away");
  else sendCommand("/back");
}
```

### Server-Side: Away Handler
**File:** `Server/GameRoom.java`  
**Lines:** 346-357

```java
/**
 * Handles /away and /back commands.
 * Updates player's away status and broadcasts to all clients.
 */
private void handleAway(ServerThread sender, boolean isAway) {
    long id = sender.getClientId();
    away.put(id, isAway);
    
    if (isAway) {
        broadcast(null, sender.getDisplayName() + " is now AWAY.");
    } else {
        broadcast(null, sender.getDisplayName() + " is back.");
    }
    
    sendUserListToAll();
}
```

### Server-Side: Away Status in User List
**File:** `Server/GameRoom.java`  
**Lines:** 641-660

```java
// In sendUserListToAll(), away status is included:
boolean isAway = away.getOrDefault(id, false);
up.addUser(id, name, pts, locked, isAway, isSpec);
```

### Server-Side: Ignoring Away Users in Logic
**File:** `Server/GameRoom.java`  
**Lines:** 228-245

```java
// In allActivePlayersReady(), away users are skipped:
if (away.getOrDefault(id, false)) continue;

// In handleAnswer(), away users cannot answer:
if (away.getOrDefault(id, false)) {
    sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
            "You are marked AWAY. Use /back to participate again.");
    return;
}
```

---

## 9. Spectators

### Client-Side: Spectator Checkbox
**File:** `Client/Client.java`  
**Lines:** 270-279

```java
/**
 * Creates the Spectator checkbox.
 * When checked, marks the player as a spectator (can watch but not play).
 */
private JCheckBox createSpectatorCheckbox() {
  JCheckBox box = new JCheckBox("Spectator");
  box.setOpaque(false);
  box.setForeground(new Color(30, 60, 120));
  box.addActionListener(e -> toggleSpectator());
  TextFX.setSubtitleFont(box);
  return box;
}
```

### Client-Side: Toggle Spectator
**File:** `Client/Client.java`  
**Lines:** 661-666

```java
/**
 * Toggles spectator status and sends command to server.
 */
private void toggleSpectator() {
  boolean newState = chkSpectator.isSelected();
  if (newState && !isSpectator) {
      isSpectator = true;
      sendCommand("/spectate");
  }
}
```

### Server-Side: Spectator Handler
**File:** `Server/GameRoom.java`  
**Lines:** 359-365

```java
/**
 * Handles /spectate command.
 * Marks player as spectator and prevents them from participating.
 */
private void handleSpectate(ServerThread sender) {
    long id = sender.getClientId();
    spectator.put(id, true);
    ready.put(id, false); // spectators do not participate
    broadcast(null, sender.getDisplayName() + " joined as a spectator.");
    sendUserListToAll();
}
```

### Server-Side: Spectator Auto-Assignment
**File:** `Server/GameRoom.java`  
**Lines:** 134-144

```java
// In addClient(), if game is in progress, new players become spectators:
if (sessionActive) {
    spectator.put(id, true);
    client.sendMessage(Constants.DEFAULT_CLIENT_ID,
            "Game already in progress – you joined as a spectator.");
    
    // Send current question so they see the board
    if (currentQuestion != null) {
        sendQuestionToSingleClient(client);
    }
}
```

### Server-Side: Spectator Filtering in Game Logic
**File:** `Server/GameRoom.java`  
**Lines:** 228-245, 493-540

```java
// In allActivePlayersReady(), spectators are skipped:
if (spectator.getOrDefault(id, false)) continue;

// In handleAnswer(), spectators cannot answer:
if (spectator.getOrDefault(id, false)) {
    sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
            "Spectators cannot answer.");
    return;
}

// In handleReady(), spectators cannot ready up:
if (spectator.getOrDefault(id, false)) {
    sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
            "Spectators cannot ready up.");
    return;
}
```

### Server-Side: Prevent Spectators from Chatting
**File:** `Server/Room.java`  
**Lines:** 59-68

```java
// Chat is handled in base Room class, but GameRoom can override if needed.
// Spectators are prevented from sending messages in Client.java:
private void sendChat() {
  if (isSpectator) {
      appendEvent("Spectators cannot chat.");
      return;
  }
  // ... send chat message
}
```

### Server-Side: Spectator Seeing Correct Answer
**File:** `Server/GameRoom.java`  
**Lines:** 579-587

```java
/**
 * Shows the correct answer to all clients.
 * Spectators can see this along with regular players.
 */
private void showCorrectAnswer() {
    if (currentQuestion == null) return;
    int idx = currentQuestion.correctIndex;
    if (idx < 0 || idx >= currentQuestion.answers.size()) return;
    
    char letter = (char) ('A' + idx);
    String answerText = currentQuestion.answers.get(idx);
    broadcast(null, "Correct answer: " + letter + " – " + answerText);
}
```

---

## Code Flow Summary

### Connection Flow:
1. Client: User enters name, host, port → clicks Connect
2. Client: Creates socket, sends `ConnectionPayload` with `CLIENT_CONNECT` type
3. Server: `ServerThread.processPayload()` receives payload
4. Server: Calls `setClientName()` → `onInitialized()` → places client in Lobby
5. Server: Sends `CLIENT_ID` payload back to client
6. Client: Receives `CLIENT_ID`, stores it, shows confirmation

### Ready Flow:
1. Client: User checks Ready checkbox → `readyClicked()` → sends `/ready` command
2. Server: `GameRoom.handleMessage()` receives `/ready`
3. Server: `handleReady()` marks player as ready, broadcasts message
4. Server: Checks `allActivePlayersReady()` → if true, calls `startSession()`
5. Server: `startSession()` loads questions, starts first round
6. Server: Sends question to all clients via `sendQuestionToAll()`

### Answer Flow:
1. Client: User clicks answer button → `answerClicked()` → sends `/answer X`
2. Server: `handleAnswer()` receives command, validates (not spectator, not away, not already locked)
3. Server: Checks if answer is correct, adds to `correctOrder` if correct
4. Server: Broadcasts lock-in message, updates user list
5. Server: If all active players locked in, calls `endRound()`
6. Server: `endRound()` awards points, shows scoreboard, starts next round

### User List Sync Flow:
1. Server: `sendUserListToAll()` creates `UserListPayload`
2. Server: Loops through all clients, adds their data (id, name, points, locked, away, spectator)
3. Server: Sends payload to all clients
4. Client: `handleUserList()` receives payload, clears old list
5. Client: Loops through payload data, creates `User` objects, adds to list model
6. Client: List automatically updates display via `DefaultListModel`

---

## File Structure Reference

- **Client/Client.java** - Main client UI and networking
- **Client/User.java** - User data model for display
- **Server/GameRoom.java** - Server-side game logic
- **Server/Room.java** - Base room class for chat
- **Server/ServerThread.java** - Per-client connection handler
- **Server/Server.java** - Main server that accepts connections
- **Common/UserListPayload.java** - Payload for syncing user lists
- **Common/QAPayload.java** - Payload for sending questions
- **Common/PointsPayload.java** - Payload for point updates


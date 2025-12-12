package Server;

import Common.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**

* UCID: nhd5
* Date: December 8, 2025
*
* TriviaGuessGame GameRoom – server-side game logic (MS2 + MS3).
* * Extends Room; all non-command text is treated as chat via super.handleMessage.
* * Parses slash commands sent by Client:
* ```
   /ready
  ```
* ```
   /away, /back
  ```
* ```
   /spectate
  ```
* ```
   /categories cat1,cat2,...
  ```
* ```
   /answer indexOrLetter
  ```
* ```
   /addq category|question|a1|a2|a3|a4|correctIndex
  ```
* * Manages:
* ```
   • Ready check & session start
  ```
* ```
   • Away / Spectator
  ```
* ```
   • Categories filter
  ```
* ```
   • Question loader (Server/questions.txt)
  ```
* ```
   • Round timer + TIMER payload
  ```
* ```
   • Points + POINTS_UPDATE payload
  ```
* ```
   • User list sync + USER_LIST payload
  ```
*
* File format for questions.txt (one question per line):
* ```
   category|question text|answer A|answer B|answer C|answer D|correctIndex
  ```
* where correctIndex is 0 for A, 1 for B, 2 for C, 3 for D.
  */

public class GameRoom extends Room {

// -------- Question POJO --------
private static class Question {
    String category;
    String text;
    ArrayList<String> answers = new ArrayList<>();
    int correctIndex;
}

// -------- Game settings --------
private static final int MAX_ROUNDS = 5;
// Requested 30s timer (UI unchanged)
private static final int ROUND_SECONDS = 30;

// Question bank
private final ArrayList<Question> questionPool = new ArrayList<>();
private final ArrayList<Question> customQuestions = new ArrayList<>(); // Questions added during this session
private final Random rng = new Random();

// Per-player state
private final Map<Long, Integer> points = new HashMap<>();
private final Map<Long, Boolean> ready = new HashMap<>();
private final Map<Long, Boolean> away = new HashMap<>();
private final Map<Long, Boolean> spectator = new HashMap<>();
private final Map<Long, Boolean> lockedThisRound = new HashMap<>();

// Session state
private boolean sessionActive = false;
private int currentRound = 0;
private Question currentQuestion = null;
private final ArrayList<Long> correctOrder = new ArrayList<>();
// Collect correct answers and reveal only at game over
private final ArrayList<String> sessionAnswerKey = new ArrayList<>();

private int timerSecondsLeft = 0;
private Thread timerThread = null;

private long hostClientId = Constants.DEFAULT_CLIENT_ID;

// Categories enabled by host during ready check
private final Set<String> enabledCategories = new HashSet<>();

public GameRoom(String name) {
    super(name);

    // Start with all common categories enabled
    enabledCategories.add("geography");
    enabledCategories.add("science");
    enabledCategories.add("math");
    enabledCategories.add("history");
    enabledCategories.add("movies");
    enabledCategories.add("sports");

    loadQuestionsFromFile();
}

// =====================================================================
// Client management
// =====================================================================

@Override
protected synchronized void addClient(ServerThread client) {
    super.addClient(client); // Room will broadcast join message

    long id = client.getClientId();
    points.putIfAbsent(id, 0);
    ready.put(id, false);
    away.putIfAbsent(id, false);
    spectator.putIfAbsent(id, false);
    lockedThisRound.put(id, false);

    // First client becomes host
    if (hostClientId == Constants.DEFAULT_CLIENT_ID) {
        hostClientId = id;
    }

    // Immediately sync current phase to the newly joined client so UI shows correct view
    Phase phaseForClient = sessionActive ? Phase.IN_PROGRESS : Phase.READY;
    sendPhaseToSingleClient(client, phaseForClient);

    // Sync current ready states to the newly joined client (quietly)
    for (Map.Entry<Long, Boolean> entry : ready.entrySet()) {
        ReadyPayload rp = new ReadyPayload();
        rp.setPayloadType(PayloadType.SYNC_READY);
        rp.setClientId(entry.getKey());
        rp.setReady(entry.getValue());
        client.sendPayload(rp);
    }

    // If a game is in progress, new players come in as spectators
    if (sessionActive) {
        spectator.put(id, true);
        client.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Game already in progress – you joined as a spectator.");

        // Send current question so they see the board
        if (currentQuestion != null) {
            sendQuestionToSingleClient(client);
        }
    }

    sendUserListToAll();
}

@Override
protected synchronized void handleDisconnect(ServerThread client) {
    long id = client.getClientId();

    points.remove(id);
    ready.remove(id);
    away.remove(id);
    spectator.remove(id);
    lockedThisRound.remove(id);
    correctOrder.remove(id);

    super.handleDisconnect(client); // broadcast "left room"
    sendUserListToAll();
}

// =====================================================================
// Message / command handling
// =====================================================================

/**
 * Handles incoming messages from clients. If message starts with "/", it's a command.
 * Otherwise, it's treated as chat and forwarded to Room base class.
 */
@Override
protected synchronized void handleMessage(ServerThread sender, String msg) {
    if (msg == null) return;
    String text = msg.trim();
    if (text.isEmpty()) return;

    // Commands always start with "/" - parse command name and arguments
    if (text.startsWith(Constants.COMMAND_TRIGGER)) {
        // Remove "/" and split into command name and arguments
        String noSlash = text.substring(1);
        String[] parts = noSlash.split(Constants.SINGLE_SPACE, 2);
        String command = parts[0].toLowerCase();
        String args = (parts.length > 1) ? parts[1].trim() : "";

        // Route to appropriate command handler
        switch (command) {
            case "ready"      -> handleReady(sender);
            case "away"       -> handleAway(sender, true);
            case "back"       -> handleAway(sender, false);
            case "spectate"   -> handleSpectate(sender);
            case "categories" -> handleCategories(sender, args);
            case "answer"     -> handleAnswer(sender, args);
            case "addq"       -> handleAddQuestion(sender, args);
            default -> sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Unknown command: " + text);
        }
        return;
    }

    // Non-command text is chat; prevent spectators from chatting
    long id = sender.getClientId();
    if (spectator.getOrDefault(id, false)) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Spectators cannot send messages.");
        return;
    }
    
    // Room handles [CHAT] prefix
    super.handleMessage(sender, text);
}

// =====================================================================
// Ready / session control
// =====================================================================

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

    broadcast(null, "user \"" + sender.getDisplayName() + "\" is ready");
    sendUserListToAll();

    if (!sessionActive && allActivePlayersReady()) {
        startSession();
    }
}

/** true if all non-away, non-spectator players are ready and at least 2 exist */
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

private synchronized void startSession() {
    loadQuestionsFromFile(); // fresh question pool
    customQuestions.clear(); // Reset custom questions for new session
    sessionAnswerKey.clear();
    if (questionPool.isEmpty()) {
        broadcast(null, "No questions available. Game cannot start.");
        return;
    }

    sessionActive = true;
    currentRound = 0;
    broadcast(null, "=== Trivia Session Starting ===");

    for (Long id : ready.keySet()) {
        ready.put(id, false);
    }
    for (Long id : lockedThisRound.keySet()) {
        lockedThisRound.put(id, false);
    }

    // Notify clients the game is transitioning to active state
    sendPhaseToAll(Phase.IN_PROGRESS);
    startNextRound();
}

private synchronized void endSession() {
    sessionActive = false;
    stopTimer();

    broadcast(null, "=== GAME OVER ===");
    showScoreboard("Final scores:");
    showAnswerKey();

    // Reset points for next game
    for (Long id : points.keySet()) {
        points.put(id, 0);
    }
    currentQuestion = null;
    correctOrder.clear();
    timerSecondsLeft = 0;

    sendUserListToAll();
    sendPhaseToAll(Phase.READY); // Return to ready check
    broadcast(null, "Type /ready to play again.");
}

private synchronized void startNextRound() {
    currentRound++;
    correctOrder.clear();
    for (Long id : lockedThisRound.keySet()) {
        lockedThisRound.put(id, false);
    }

    if (currentRound > MAX_ROUNDS) {
        endSession();
        return;
    }

    currentQuestion = drawRandomQuestion();
    if (currentQuestion == null) {
        broadcast(null, "No more questions available for selected categories.");
        endSession();
        return;
    }

    sendPhaseToAll(Phase.IN_PROGRESS);
    broadcast(null, "Round " + currentRound + " of " + MAX_ROUNDS);
    sendQuestionToAll();
    sendUserListToAll();
    startTimer();
}

// =====================================================================
// Categories / away / spectator
// =====================================================================

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
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "No categories provided. Keeping previous selection: " + enabledCategories);
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

private void handleSpectate(ServerThread sender) {
    long id = sender.getClientId();
    spectator.put(id, true);
    ready.put(id, false); // spectators do not participate
    broadcast(null, sender.getDisplayName() + " joined as a spectator.");
    sendUserListToAll();
}

// =====================================================================
// Question loading & selection
// =====================================================================

/**
 * Loads questions from a text file. Format: category|question|a1|a2|a3|a4|correctIndex
 * Tries Server/questions.txt first, then questions.txt in project root.
 * If file not found, uses built-in sample questions.
 */
private void loadQuestionsFromFile() {
    questionPool.clear();

    // Primary: Server/questions.txt
    File f = new File("Server/questions.txt");
    if (!f.exists()) {
        // Fallback: questions.txt in project root
        f = new File("questions.txt");
    }

    if (!f.exists()) {
        addBuiltInSampleQuestions();
        return;
    }

    // Read file line by line, parse each question
    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            // Skip empty lines and comments (lines starting with #)
            if (line.isEmpty() || line.startsWith("#")) continue;

            // Parse the line into a Question object
            Question q = parseQuestionLine(line);
            if (q != null) {
                questionPool.add(q);
            }
        }
    } catch (IOException e) {
        // If file read fails, use built-in questions
        addBuiltInSampleQuestions();
    }
}

private Question parseQuestionLine(String line) {
    String[] parts = line.split("\\|");
    if (parts.length < 7) {
        return null;
    }

    Question q = new Question();
    q.category = parts[0].trim().toLowerCase();
    q.text = parts[1].trim();
    // Add only non-empty answers (supports 2-4 answers)
    for (int i = 2; i <= 5; i++) {
        String answer = parts[i].trim();
        if (!answer.isEmpty()) {
            q.answers.add(answer);
        }
    }
    // Validate we have 2-4 answers
    if (q.answers.size() < 2 || q.answers.size() > 4) {
        return null;
    }
    try {
        q.correctIndex = Integer.parseInt(parts[6].trim());
    } catch (NumberFormatException e) {
        q.correctIndex = 0;
    }
    if (q.correctIndex < 0 || q.correctIndex >= q.answers.size()) {
        q.correctIndex = 0;
    }
    return q;
}

/** Fallback questions if no file is found */
private void addBuiltInSampleQuestions() {
    Question q1 = new Question();
    q1.category = "geography";
    q1.text = "What is the capital of France?";
    q1.answers.addAll(Arrays.asList("Paris", "London", "Berlin", "Madrid"));
    q1.correctIndex = 0;
    questionPool.add(q1);

    Question q2 = new Question();
    q2.category = "science";
    q2.text = "What planet is known as the Red Planet?";
    q2.answers.addAll(Arrays.asList("Venus", "Mars", "Jupiter", "Saturn"));
    q2.correctIndex = 1;
    questionPool.add(q2);

    Question q3 = new Question();
    q3.category = "math";
    q3.text = "What is 7 × 8?";
    q3.answers.addAll(Arrays.asList("54", "56", "63", "49"));
    q3.correctIndex = 1;
    questionPool.add(q3);

    Question q4 = new Question();
    q4.category = "history";
    q4.text = "Who was the first President of the United States?";
    q4.answers.addAll(Arrays.asList("George Washington", "Abraham Lincoln",
            "John Adams", "Thomas Jefferson"));
    q4.correctIndex = 0;
    questionPool.add(q4);

    Question q5 = new Question();
    q5.category = "movies";
    q5.text = "In which movie does the quote 'I'll be back' appear?";
    q5.answers.addAll(Arrays.asList("Terminator", "Die Hard", "Rocky", "Predator"));
    q5.correctIndex = 0;
    questionPool.add(q5);

    Question q6 = new Question();
    q6.category = "sports";
    q6.text = "How many players are on the field for one soccer team during play?";
    q6.answers.addAll(Arrays.asList("9", "10", "11", "12"));
    q6.correctIndex = 2;
    questionPool.add(q6);
}

/** Draws & removes a random question from the pool, respecting enabledCategories if possible.
 * After round 3, prioritizes custom questions added during this session. */
private Question drawRandomQuestion() {
    if (questionPool.isEmpty()) return null;

    // After round 3, prioritize custom questions
    if (currentRound >= 3 && !customQuestions.isEmpty()) {
        // Find custom questions that are still in the pool and match enabled categories
        ArrayList<Integer> eligibleCustomIndices = new ArrayList<>();
        for (int i = 0; i < questionPool.size(); i++) {
            Question q = questionPool.get(i);
            // Check if this question is in customQuestions list (by reference)
            boolean isCustom = customQuestions.contains(q);
            if (isCustom && (enabledCategories.isEmpty() || enabledCategories.contains(q.category))) {
                eligibleCustomIndices.add(i);
            }
        }
        if (!eligibleCustomIndices.isEmpty()) {
            // Use a custom question
            int poolIndex = eligibleCustomIndices.get(rng.nextInt(eligibleCustomIndices.size()));
            Question selected = questionPool.remove(poolIndex);
            // Remove from customQuestions list (by reference)
            customQuestions.remove(selected);
            return selected;
        }
    }

    // Normal selection from question pool
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

// =====================================================================
// Answer handling
// =====================================================================

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
    if (index < 0 || index >= currentQuestion.answers.size()) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Invalid answer. Use 0–3 or A–D.");
        return;
    }

    boolean correct = (index == currentQuestion.correctIndex);
    lockedThisRound.put(id, true);

    broadcast(null, sender.getDisplayName() +
            (correct ? " locked in the CORRECT answer!" :
                    " locked in the WRONG answer."));

    if (correct) {
        correctOrder.add(id);
    }

    sendUserListToAll(); // update locked icon

    if (allActivePlayersLocked()) {
        endRound("All active players locked in.");
    }
}

/** maps 0–3 or A–D to answer index */
private int mapChoiceToIndex(String arg) {
    if (arg == null || arg.isEmpty()) return -1;
    char c = Character.toUpperCase(arg.trim().charAt(0));
    if (c >= '0' && c <= '3') return c - '0';
    if (c == 'A') return 0;
    if (c == 'B') return 1;
    if (c == 'C') return 2;
    if (c == 'D') return 3;
    return -1;
}

private boolean allActivePlayersLocked() {
    for (ServerThread st : getClients()) {
        long id = st.getClientId();
        if (spectator.getOrDefault(id, false)) continue;
        if (away.getOrDefault(id, false)) continue;
        if (!lockedThisRound.getOrDefault(id, false)) return false;
    }
    return true;
}

private synchronized void endRound(String reason) {
    broadcast(null, "Round ended: " + reason);
    // Requirement: only reveal answers after all rounds are done.
    recordCorrectAnswerForSession();
    awardPoints();
    showScoreboard("Current scores:");
    sendUserListToAll();
    stopTimer();

    if (currentRound >= MAX_ROUNDS) {
        endSession();
    } else {
        startNextRound();
    }
}

// Stores the correct answer for later reveal at game over.
private void recordCorrectAnswerForSession() {
    if (currentQuestion == null) return;
    int idx = currentQuestion.correctIndex;
    if (idx < 0 || idx >= currentQuestion.answers.size()) return;
    char letter = (char) ('A' + idx);
    String answerText = currentQuestion.answers.get(idx);
    sessionAnswerKey.add(String.format("Round %d: %s – %s", currentRound, letter, answerText));
}

private void showAnswerKey() {
    if (sessionAnswerKey.isEmpty()) return;
    broadcast(null, "Answer key:");
    for (String line : sessionAnswerKey) {
        broadcast(null, " • " + line);
    }
}

/**
 * Awards points to players who answered correctly, in order of speed.
 * Points diminish: 1st = 10, 2nd = 7, 3rd = 5, 4th = 3, rest = 1.
 * correctOrder list contains IDs in the order they answered correctly.
 */
private void awardPoints() {
    // Point values for first 4 correct answers, then 1 point for each after
    int[] awards = new int[]{10, 7, 5, 3};

    // Award points to each player in the order they answered correctly
    for (int i = 0; i < correctOrder.size(); i++) {
        long id = correctOrder.get(i);
        // Use award array for first 4, then 1 point for everyone else
        int award = (i < awards.length) ? awards[i] : 1;

        // Update total points for this player
        int newTotal = points.getOrDefault(id, 0) + award;
        points.put(id, newTotal);

        ServerThread st = clients.get(id);
        String name = (st != null) ? st.getDisplayName() : ("Player#" + id);

        // Broadcast points earned to all players
        broadcast(null, name + " earned " + award + " points (total " + newTotal + ").");

        // Send points update payload to all clients for UI sync
        PointsPayload pp = new PointsPayload();
        pp.setPayloadType(PayloadType.POINTS_UPDATE);
        pp.setClientId(Constants.DEFAULT_CLIENT_ID);
        pp.setTargetClientId(id);
        pp.setPoints(newTotal);
        pp.setMessage(name + " now has " + newTotal + " points.");

        for (ServerThread client : getClients()) {
            client.sendPayload(pp);
        }
    }
}

private void showScoreboard(String header) {
    broadcast(null, header);

    ArrayList<Map.Entry<Long, Integer>> entries =
            new ArrayList<>(points.entrySet());
    entries.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

    for (Map.Entry<Long, Integer> e : entries) {
        long id = e.getKey();
        int pts = e.getValue();
        ServerThread st = clients.get(id);
        String name = (st != null) ? st.getDisplayName() : ("Player#" + id);
        broadcast(null, " • " + name + " – " + pts + " pts");
    }
}

// =====================================================================
// User list sync
// =====================================================================

private void sendUserListToAll() {
    UserListPayload up = new UserListPayload();
    up.setPayloadType(PayloadType.USER_LIST);
    up.setClientId(Constants.DEFAULT_CLIENT_ID);
    up.setHostClientId(hostClientId);

    // Sort clients by ID to ensure same order across all clients
    java.util.ArrayList<ServerThread> sortedClients = new java.util.ArrayList<>(getClients());
    sortedClients.sort((a, b) -> Long.compare(a.getClientId(), b.getClientId()));

    for (ServerThread st : sortedClients) {
        long id = st.getClientId();
        String name = st.getDisplayName();
        int pts = points.getOrDefault(id, 0);
        boolean locked = lockedThisRound.getOrDefault(id, false);
        boolean isAway = away.getOrDefault(id, false);
        boolean isSpec = spectator.getOrDefault(id, false);

        up.addUser(id, name, pts, locked, isAway, isSpec);
    }

    for (ServerThread st : getClients()) {
        st.sendPayload(up);
    }
}

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

private void sendQuestionToSingleClient(ServerThread client) {
    if (currentQuestion == null || client == null) return;

    QAPayload qa = new QAPayload();
    qa.setPayloadType(PayloadType.QUESTION);
    qa.setClientId(Constants.DEFAULT_CLIENT_ID);
    qa.setCategory(currentQuestion.category);
    qa.setQuestionText(currentQuestion.text);
    qa.getAnswers().clear();
    qa.getAnswers().addAll(currentQuestion.answers);

    client.sendPayload(qa);
}

// =====================================================================
// Timer
// =====================================================================

/**
 * Starts a countdown timer for the current round.
 * Timer runs in a separate thread, sends updates every second.
 * When timer reaches 0, the round ends automatically.
 */
private void startTimer() {
    stopTimer(); // Stop any existing timer first

    timerSecondsLeft = ROUND_SECONDS;

    // Create a daemon thread that counts down every second
    timerThread = new Thread(() -> {
        while (timerSecondsLeft >= 0 && sessionActive && currentQuestion != null) {
            // Send current time remaining to all clients
            sendTimerUpdate(timerSecondsLeft);

            // When time runs out, end the round
            if (timerSecondsLeft == 0) {
                endRound("Time's up!");
                break;
            }

            // Wait 1 second before next countdown
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
            timerSecondsLeft--;
        }
    });
    timerThread.setDaemon(true); // Daemon thread dies when main program ends
    timerThread.start();
}

private void stopTimer() {
    if (timerThread != null && timerThread.isAlive()) {
        timerThread.interrupt();
    }
    timerThread = null;
}

private void sendTimerUpdate(int seconds) {
    TimerPayload p = new TimerPayload();
    p.setPayloadType(PayloadType.TIMER);
    p.setClientId(Constants.DEFAULT_CLIENT_ID);
    p.setTimerType(TimerType.ROUND);
    p.setTime(seconds);

    for (ServerThread st : getClients()) {
        st.sendPayload(p);
    }
}

// =====================================================================
// /addq – players adding questions at runtime
// =====================================================================

private void handleAddQuestion(ServerThread sender, String args) {
    if (sessionActive) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "You can only add questions while no game is running.");
        return;
    }
        if (Room.LOBBY.equalsIgnoreCase(this.name)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Add questions after joining a game room (not in lobby).");
            return;
        }

    Question q = parseQuestionLine(args);
    if (q == null) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Invalid /addq format. Use category|question|a1|a2|a3|a4|correctIndex");
        return;
    }

    // Add to question pool for this session
    questionPool.add(q);
    // Also track as custom question
    customQuestions.add(q);
    
    // Save to server-side file
    saveQuestionToFile(q);
    
    broadcast(null, "New question added in '" + q.category +
            "' by " + sender.getDisplayName() + ". Will be used after round 3.");
}

/**
 * Saves a custom question to the server-side questions.txt file.
 */
private void saveQuestionToFile(Question q) {
    File f = new File("Server/questions.txt");
    if (!f.exists()) {
        f = new File("questions.txt");
    }
    
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(f, true))) {
        // Format: category|question|a1|a2|a3|a4|correctIndex
        StringBuilder line = new StringBuilder();
        line.append(q.category).append("|");
        line.append(q.text).append("|");
        // Add all answers (2-4 answers), pad to 4 with empty strings
        for (int i = 0; i < 4; i++) {
            if (i < q.answers.size()) {
                line.append(q.answers.get(i));
            }
            // Always add separator after each answer slot
            line.append("|");
        }
        line.append(q.correctIndex);
        bw.newLine();
        bw.write(line.toString());
        bw.flush();
    } catch (IOException e) {
        // Log error but don't fail - question is still added to pool
        System.err.println("Failed to save question to file: " + e.getMessage());
    }
}

// =====================================================================
// Phase sync
// =====================================================================
private void sendPhaseToSingleClient(ServerThread client, Phase phase) {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.PHASE);
    p.setClientId(Constants.DEFAULT_CLIENT_ID);
    p.setMessage(phase.name());
    client.sendPayload(p);
}

private void sendPhaseToAll(Phase phase) {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.PHASE);
    p.setClientId(Constants.DEFAULT_CLIENT_ID);
    p.setMessage(phase.name());

    for (ServerThread st : getClients()) {
        st.sendPayload(p);
    }
}
}

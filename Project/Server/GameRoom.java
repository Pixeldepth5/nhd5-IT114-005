package Server;

import Common.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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

```
// -------- Question POJO --------
private static class Question {
    String category;
    String text;
    ArrayList<String> answers = new ArrayList<>();
    int correctIndex;
}

// -------- Game settings --------
private static final int MAX_ROUNDS = 5;
private static final int ROUND_SECONDS = 20;

// Question bank
private final ArrayList<Question> questionPool = new ArrayList<>();
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

@Override
protected synchronized void handleMessage(ServerThread sender, String msg) {
    if (msg == null) return;
    String text = msg.trim();
    if (text.isEmpty()) return;

    // Commands always start with "/"
    if (text.startsWith(Constants.COMMAND_TRIGGER)) {
        String noSlash = text.substring(1);
        String[] parts = noSlash.split(Constants.SINGLE_SPACE, 2);
        String command = parts[0].toLowerCase();
        String args = (parts.length > 1) ? parts[1].trim() : "";

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

    // Non-command text is chat; Room handles [CHAT] prefix
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

    broadcast(null, sender.getDisplayName() + " is ready.");
    sendUserListToAll();

    if (!sessionActive && allActivePlayersReady()) {
        startSession();
    }
}

/** true if all non-away, non-spectator players are ready and at least one exists */
private boolean allActivePlayersReady() {
    boolean anyActive = false;
    for (ServerThread st : getClients()) {
        long id = st.getClientId();
        if (spectator.getOrDefault(id, false)) continue;
        if (away.getOrDefault(id, false)) continue;

        anyActive = true;
        if (!ready.getOrDefault(id, false)) {
            return false;
        }
    }
    return anyActive;
}

private synchronized void startSession() {
    loadQuestionsFromFile(); // fresh question pool
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

    startNextRound();
}

private synchronized void endSession() {
    sessionActive = false;
    stopTimer();

    broadcast(null, "=== GAME OVER ===");
    showScoreboard("Final scores:");

    // Reset points for next game
    for (Long id : points.keySet()) {
        points.put(id, 0);
    }
    currentQuestion = null;
    correctOrder.clear();
    timerSecondsLeft = 0;

    sendUserListToAll();
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

    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            Question q = parseQuestionLine(line);
            if (q != null) {
                questionPool.add(q);
            }
        }
    } catch (IOException e) {
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
    for (int i = 2; i <= 5; i++) {
        q.answers.add(parts[i].trim());
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

/** Draws & removes a random question from the pool, respecting enabledCategories if possible */
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
    showCorrectAnswer();
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

private void showCorrectAnswer() {
    if (currentQuestion == null) return;
    int idx = currentQuestion.correctIndex;
    if (idx < 0 || idx >= currentQuestion.answers.size()) return;

    char letter = (char) ('A' + idx);
    String answerText = currentQuestion.answers.get(idx);
    broadcast(null, "Correct answer: " + letter + " – " + answerText);
}

private void awardPoints() {
    // First correct: 10, second: 7, third: 5, rest: 3 (then 1)
    int[] awards = new int[]{10, 7, 5, 3};

    for (int i = 0; i < correctOrder.size(); i++) {
        long id = correctOrder.get(i);
        int award = (i < awards.length) ? awards[i] : 1;

        int newTotal = points.getOrDefault(id, 0) + award;
        points.put(id, newTotal);

        ServerThread st = clients.get(id);
        String name = (st != null) ? st.getDisplayName() : ("Player#" + id);

        broadcast(null, name + " earned " + award + " points (total " + newTotal + ").");

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

    for (ServerThread st : getClients()) {
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

private void startTimer() {
    stopTimer(); // just in case

    timerSecondsLeft = ROUND_SECONDS;

    timerThread = new Thread(() -> {
        while (timerSecondsLeft >= 0 && sessionActive && currentQuestion != null) {
            sendTimerUpdate(timerSecondsLeft);

            if (timerSecondsLeft == 0) {
                endRound("Time's up!");
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
            timerSecondsLeft--;
        }
    });
    timerThread.setDaemon(true);
    timerThread.start();
}

private void stopTimer() {
    if (timerThread != null && timerThread.isAlive()) {
        timerThread.interrupt();
    }
    timerThread = null;
}

private void sendTimerUpdate(int seconds) {
    Payload p = new Payload();
    p.setPayloadType(PayloadType.TIMER);
    p.setClientId(Constants.DEFAULT_CLIENT_ID);
    p.setMessage(Integer.toString(seconds));

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

    Question q = parseQuestionLine(args);
    if (q == null) {
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Invalid /addq format. Use category|question|a1|a2|a3|a4|correctIndex");
        return;
    }

    questionPool.add(q);
    broadcast(null, "New question added in '" + q.category +
            "' by " + sender.getDisplayName() + ".");
    // (Optional) append to file – skipped to keep things simple
}
```

}

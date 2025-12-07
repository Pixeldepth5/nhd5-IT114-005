// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame GameRoom – core trivia logic for Milestone 2+3
// Reference:
//  - https://www.w3schools.com/java/java_arraylist.asp (ArrayList & loops)
//  - https://www.w3schools.com/java/java_hashmap.asp (HashMap for points)
//  - https://www.w3schools.com/java/java_files_read.asp (reading text files)
//  - https://www.w3schools.com/java/java_files_create.asp (writing text files)
//  - https://www.w3schools.com/java/java_random.asp (Random for picking questions)
package Server;

import Common.Constants;
import Common.Payload;
import Common.PayloadType;
import Common.PointsPayload;
import Common.QAPayload;
import Common.UserListPayload;
import java.io.*;
import java.util.*;

public class GameRoom extends Room {

    // Simple inner Question class for this room only
    private static class Question {
        String category;
        String text;
        ArrayList<String> answers;
        int correctIndex;
    }

    private static final int MAX_ROUNDS = 5;
    private static final int ROUND_SECONDS = 20;

    // Core data
    private final ArrayList<Question> questions = new ArrayList<>();
    private final Random random = new Random();

    // Player state
    private final Map<Long, Integer> points = new HashMap<>();
    private final Map<Long, Boolean> ready = new HashMap<>();
    private final Map<Long, Boolean> away = new HashMap<>();
    private final Map<Long, Boolean> spectator = new HashMap<>();
    private final Map<Long, Boolean> lockedThisRound = new HashMap<>();

    // Session state
    private boolean sessionActive = false;
    private int currentRound = 0;
    private Question currentQuestion;
    private final ArrayList<Long> correctOrder = new ArrayList<>();
    private int timerSecondsLeft = 0;
    private Thread timerThread;
    private long hostClientId = Constants.DEFAULT_CLIENT_ID;

    // Categories – enabled during ready check (host can toggle)
    private final Set<String> enabledCategories = new HashSet<>();

    public GameRoom(String name) {
        super(name);
        // Start with all main categories enabled
        enabledCategories.add("music");
        enabledCategories.add("sports");
        enabledCategories.add("arts");
        enabledCategories.add("movies");
        enabledCategories.add("history");
        enabledCategories.add("geography");
        loadQuestionsFromFile();
    }

    // ====================== Client Management ======================

    @Override
    protected synchronized void addClient(ServerThread client) {
        super.addClient(client);

        long id = client.getClientId();
        points.putIfAbsent(id, 0);
        ready.put(id, false);
        away.putIfAbsent(id, false);
        spectator.putIfAbsent(id, false);
        lockedThisRound.put(id, false);

        // New player joins mid-session -> spectator view
        if (sessionActive) {
            spectator.put(id, true);
            client.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You joined as a spectator (game already in progress).");
            sendQuestionToClient(client);
            sendUserListToAll();
        }
    }

    @Override
    protected synchronized void handleDisconnect(ServerThread sender) {
        // Let Room remove and broadcast
        super.handleDisconnect(sender);
        long id = sender.getClientId();
        points.remove(id);
        ready.remove(id);
        away.remove(id);
        spectator.remove(id);
        lockedThisRound.remove(id);
        correctOrder.remove(id);
        sendUserListToAll();
    }

    // ====================== Command Handling ======================

    @Override
    protected synchronized void handleMessage(ServerThread sender, String msg) {
        String text = msg.trim();

        if (text.equalsIgnoreCase("/ready")) {
            handleReady(sender);
            return;
        }

        if (text.toLowerCase().startsWith("/answer ")) {
            String choice = text.substring(8).trim();
            handleAnswer(sender, choice);
            return;
        }

        if (text.equalsIgnoreCase("/away")) {
            handleAway(sender, true);
            return;
        }

        if (text.equalsIgnoreCase("/back")) {
            handleAway(sender, false);
            return;
        }

        if (text.equalsIgnoreCase("/spectate")) {
            handleSpectator(sender);
            return;
        }

        if (text.toLowerCase().startsWith("/categories ")) {
            handleCategorySelection(sender, text.substring(12).trim());
            return;
        }

        if (text.toLowerCase().startsWith("/addq ")) {
            handleAddQuestion(sender, text.substring(6).trim());
            return;
        }

        // Fallback to chat if it wasn't a command
        super.handleMessage(sender, msg);
    }

    // ============ Ready / Session Control ============

    private void handleReady(ServerThread sender) {
        long id = sender.getClientId();
        ready.put(id, true);

        if (hostClientId == Constants.DEFAULT_CLIENT_ID) {
            hostClientId = id;
            broadcast(null, sender.getDisplayName() + " is the session host.");
        }

        broadcast(null, sender.getDisplayName() + " is ready!");
        sendUserListToAll();

        // Check if all non-spectators are ready and no session active
        if (!sessionActive && allActivePlayersReady()) {
            startSession();
        }
    }

    private boolean allActivePlayersReady() {
        for (ServerThread client : getClients()) {
            long id = client.getClientId();
            boolean isSpectator = spectator.getOrDefault(id, false);
            if (isSpectator) {
                continue;
            }
            if (!ready.getOrDefault(id, false)) {
                return false;
            }
        }
        return !getClients().isEmpty();
    }

    private void startSession() {
        sessionActive = true;
        currentRound = 0;
        broadcast(null, "=== Trivia Session Starting ===");
        for (Long id : ready.keySet()) {
            ready.put(id, false);
        }
        startNextRound();
    }

    private void endSession() {
        sessionActive = false;
        broadcast(null, "=== GAME OVER ===");
        showScoreboard("Final scores:");
        broadcast(null, "Type /ready to play again.");
        // Reset round state
        currentQuestion = null;
        correctOrder.clear();
        timerSecondsLeft = 0;
        sendUserListToAll();
    }

    private void startNextRound() {
        currentRound++;
        correctOrder.clear();
        lockedThisRound.replaceAll((k, v) -> false);

        if (currentRound > MAX_ROUNDS) {
            endSession();
            return;
        }

        currentQuestion = pickRandomQuestion();
        if (currentQuestion == null) {
            broadcast(null, "No questions available for the selected categories.");
            endSession();
            return;
        }

        broadcast(null, "Round " + currentRound + " of " + MAX_ROUNDS);
        sendQuestionToAll();
        sendUserListToAll();
        startTimer();
    }

    // ============ Questions / Categories ============

    private void loadQuestionsFromFile() {
        questions.clear();
        // Format: category|question|answer1|answer2|answer3|answer4|correctIndex
        try (InputStream in = getClass().getResourceAsStream("questions.txt")) {
            if (in == null) {
                // Fallback if the file is missing
                addSampleQuestions();
                return;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    Question q = parseQuestionLine(line);
                    if (q != null) {
                        questions.add(q);
                    }
                }
            }
        } catch (IOException e) {
            addSampleQuestions();
        }
    }

    private Question parseQuestionLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 4) {
            return null;
        }
        Question q = new Question();
        q.category = parts[0].toLowerCase();
        q.text = parts[1];
        q.answers = new ArrayList<>();
        int answersCount = parts.length - 3; // last element is correctIndex
        for (int i = 0; i < answersCount; i++) {
            q.answers.add(parts[2 + i]);
        }
        try {
            q.correctIndex = Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException ex) {
            q.correctIndex = 0;
        }
        return q;
    }

    private void addSampleQuestions() {
        // Simple hard-coded questions if file is missing
        Question q1 = new Question();
        q1.category = "movies";
        q1.text = "Who directed the movie 'Inception'?";
        q1.answers = new ArrayList<>(Arrays.asList(
                "Christopher Nolan", "Steven Spielberg", "James Cameron", "Ridley Scott"));
        q1.correctIndex = 0;
        questions.add(q1);

        Question q2 = new Question();
        q2.category = "sports";
        q2.text = "How many points is a touchdown worth in American football?";
        q2.answers = new ArrayList<>(Arrays.asList("3", "6", "7", "9"));
        q2.correctIndex = 1;
        questions.add(q2);
    }

    private Question pickRandomQuestion() {
        ArrayList<Question> filtered = new ArrayList<>();
        for (Question q : questions) {
            if (enabledCategories.contains(q.category)) {
                filtered.add(q);
            }
        }
        if (filtered.isEmpty()) {
            filtered.addAll(questions);
        }
        if (filtered.isEmpty()) {
            return null;
        }
        return filtered.get(random.nextInt(filtered.size()));
    }

    private void handleCategorySelection(ServerThread sender, String text) {
        if (sessionActive) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Category selection is only allowed during ready check.");
            return;
        }
        if (sender.getClientId() != hostClientId) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Only the host can change categories.");
            return;
        }
        // Expected format: /categories music,sports,movies
        enabledCategories.clear();
        String[] parts = text.split(",");
        for (String p : parts) {
            String cat = p.trim().toLowerCase();
            if (!cat.isEmpty()) {
                enabledCategories.add(cat);
            }
        }
        broadcast(null, "Host set categories to: " + enabledCategories);
    }

    private void handleAddQuestion(ServerThread sender, String text) {
        if (sessionActive) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You can only add questions outside an active session.");
            return;
        }
        // Format: category|question|answer1|answer2|answer3|answer4|correctIndex
        Question q = parseQuestionLine(text);
        if (q == null) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Invalid /addq format. Use category|question|a1|a2|a3|a4|correctIndex");
            return;
        }
        questions.add(q);
        appendQuestionToFile(text);
        broadcast(null, "New question added by " + sender.getDisplayName() +
                " in category '" + q.category + "'.");
    }

    private void appendQuestionToFile(String line) {
        // Simple file-append pattern from W3Schools write example
        try (FileWriter fw = new FileWriter("questions.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(line);
        } catch (IOException ignored) {
        }
    }

    // ============ Away / Spectator ============

    private void handleAway(ServerThread sender, boolean isAway) {
        long id = sender.getClientId();
        away.put(id, isAway);
        String msg = sender.getDisplayName() + (isAway ? " is now away." : " is no longer away.");
        broadcast(null, msg);
        sendUserListToAll();
    }

    private void handleSpectator(ServerThread sender) {
        long id = sender.getClientId();
        spectator.put(id, true);
        ready.put(id, false);
        broadcast(null, sender.getDisplayName() + " joined as a spectator.");
        sendUserListToAll();
    }

    // ============ Answers / Round Ending ============

    private void handleAnswer(ServerThread sender, String choiceText) {
        if (!sessionActive || currentQuestion == null) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "No active round. Wait for the next question.");
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
                    "You are marked away. Use /back to play again.");
            return;
        }
        if (lockedThisRound.getOrDefault(id, false)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You already locked in an answer this round.");
            return;
        }

        int index = mapChoiceToIndex(choiceText);
        if (index < 0 || index >= currentQuestion.answers.size()) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Invalid answer choice. Use A, B, C, or D.");
            return;
        }

        boolean correct = (index == currentQuestion.correctIndex);
        lockedThisRound.put(id, true);
        if (correct) {
            correctOrder.add(id);
            broadcast(null, sender.getDisplayName() + " locked in the CORRECT answer!");
        } else {
            broadcast(null, sender.getDisplayName() + " locked in the WRONG answer.");
        }

        sendUserListToAll();

        if (allActivePlayersLocked()) {
            endRound("All active players locked in.");
        }
    }

    private int mapChoiceToIndex(String choiceText) {
        if (choiceText == null || choiceText.isEmpty()) {
            return -1;
        }
        char c = Character.toUpperCase(choiceText.charAt(0));
        if (c == 'A') return 0;
        if (c == 'B') return 1;
        if (c == 'C') return 2;
        if (c == 'D') return 3;
        return -1;
    }

    private boolean allActivePlayersLocked() {
        for (ServerThread client : getClients()) {
            long id = client.getClientId();
            if (spectator.getOrDefault(id, false) || away.getOrDefault(id, false)) {
                continue;
            }
            if (!lockedThisRound.getOrDefault(id, false)) {
                return false;
            }
        }
        return true;
    }

    private void endRound(String reason) {
        broadcast(null, "Round ended: " + reason);
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

    private void awardPoints() {
        // First correct gets 10 pts, then 7, 5, 3, 1 ...
        int[] awards = new int[]{10, 7, 5, 3, 1};
        for (int i = 0; i < correctOrder.size(); i++) {
            long id = correctOrder.get(i);
            int award = (i < awards.length) ? awards[i] : 1;
            int newTotal = points.getOrDefault(id, 0) + award;
            points.put(id, newTotal);

            ServerThread st = findClient(id);
            String name = (st != null) ? st.getDisplayName() : ("Player#" + id);

            broadcast(null, name + " earned " + award + " points this round!");

            PointsPayload p = new PointsPayload();
            p.setPayloadType(PayloadType.POINTS_UPDATE);
            p.setClientId(Constants.DEFAULT_CLIENT_ID);
            p.setTargetClientId(id);
            p.setPoints(newTotal);
            p.setMessage(name + " now has " + newTotal + " points.");

            for (ServerThread client : getClients()) {
                client.sendPayload(p);
            }
        }
    }

    private ServerThread findClient(long id) {
        for (ServerThread s : getClients()) {
            if (s.getClientId() == id) {
                return s;
            }
        }
        return null;
    }

    private void showScoreboard(String header) {
        broadcast(null, header);
        // Sort by points (descending)
        ArrayList<Map.Entry<Long, Integer>> list = new ArrayList<>(points.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<Long, Integer> e : list) {
            ServerThread st = findClient(e.getKey());
            String name = (st != null) ? st.getDisplayName() : ("Player#" + e.getKey());
            broadcast(null, name + ": " + e.getValue() + " points");
        }
    }

    // ============ Sending Question / User List / Timer ============

    private void sendQuestionToAll() {
        for (ServerThread client : getClients()) {
            sendQuestionToClient(client);
        }
    }

    private void sendQuestionToClient(ServerThread client) {
        if (currentQuestion == null) {
            return;
        }
        QAPayload qp = new QAPayload();
        qp.setPayloadType(PayloadType.QUESTION);
        qp.setClientId(Constants.DEFAULT_CLIENT_ID);
        qp.setCategory(currentQuestion.category);
        qp.setQuestionText(currentQuestion.text);
        qp.setAnswers(new ArrayList<>(currentQuestion.answers));
        client.sendPayload(qp);
    }

    private void sendUserListToAll() {
        // Create an ordered list by points, highest first
        ArrayList<ServerThread> snapshot = new ArrayList<>(getClients());
        snapshot.sort((a, b) -> {
            int pa = points.getOrDefault(a.getClientId(), 0);
            int pb = points.getOrDefault(b.getClientId(), 0);
            return Integer.compare(pb, pa);
        });

        UserListPayload up = new UserListPayload();
        up.setPayloadType(PayloadType.USER_LIST);
        up.setClientId(Constants.DEFAULT_CLIENT_ID);

        for (ServerThread client : snapshot) {
            long id = client.getClientId();
            String name = client.getDisplayName();
            int pts = points.getOrDefault(id, 0);
            boolean isLocked = lockedThisRound.getOrDefault(id, false);
            boolean isAway = away.getOrDefault(id, false);
            boolean isSpectator = spectator.getOrDefault(id, false);
            up.addUser(id, name, pts, isLocked, isAway, isSpectator);
        }

        for (ServerThread client : getClients()) {
            client.sendPayload(up);
        }
    }

    private void startTimer() {
        stopTimer();
        timerSecondsLeft = ROUND_SECONDS;

        timerThread = new Thread(() -> {
            try {
                while (timerSecondsLeft >= 0 && sessionActive && currentQuestion != null) {
                    sendTimerToAll();
                    Thread.sleep(1000);
                    timerSecondsLeft--;
                }
            } catch (InterruptedException ignored) {
            }

            if (sessionActive && currentQuestion != null && timerSecondsLeft < 0) {
                synchronized (GameRoom.this) {
                    endRound("Time is up!");
                }
            }
        });
        timerThread.start();
    }

    private void stopTimer() {
        if (timerThread != null && timerThread.isAlive()) {
            timerThread.interrupt();
        }
        timerThread = null;
    }

    private void sendTimerToAll() {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.TIMER);
        p.setClientId(Constants.DEFAULT_CLIENT_ID);
        p.setMessage(Integer.toString(timerSecondsLeft));
        for (ServerThread client : getClients()) {
            client.sendToClient(p);
        }
    }
}

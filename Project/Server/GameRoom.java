// UCID: nhd5
// Date: December 8, 2025
// Description: TriviaGuessGame GameRoom – server-side round/session logic (MS2+MS3).
//  - Uses classic payload names: QUESTION and USER_LIST.
//  - Reads questions from "questions.txt" (format: category|question|a1|a2|a3|a4|correctIndex).
//  - Accepts answers as A/B/C/D OR 0/1/2/3.
//  - Supports Ready check, Away, Spectator, timer, scoring & scoreboard.
// References (beginner friendly):
//  - https://www.w3schools.com/java/java_arraylist.asp
//  - https://www.w3schools.com/java/java_hashmap.asp
//  - https://www.w3schools.com/java/java_files_read.asp
//  - https://www.w3schools.com/java/java_files_create.asp
//  - https://www.w3schools.com/java/java_random.asp

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

    // Simple POJO for a question
    private static class Question {
        String category;
        String text;
        ArrayList<String> answers = new ArrayList<>();
        int correctIndex;
    }

    // Tunable settings per rubric
    private static final int MAX_ROUNDS = 5;
    private static final int ROUND_SECONDS = 20;

    // Question bank and RNG
    private final ArrayList<Question> questions = new ArrayList<>();
    private final Random random = new Random();

    // Per-player state
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

    // Categories toggled during ready check by host
    private final Set<String> enabledCategories = new HashSet<>();

    public GameRoom(String name) {
        super(name);

        // Start with defaults enabled (host can change during Ready Check)
        enabledCategories.add("geography");
        enabledCategories.add("science");
        enabledCategories.add("math");
        enabledCategories.add("history");
        enabledCategories.add("movies");
        enabledCategories.add("sports");

        // Initial load (will re-load fresh on each new session)
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

        // Late join → spectate current session
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

    // ====================== Command + Chat Handling ======================

    @Override
    protected synchronized void handleMessage(ServerThread sender, String raw) {
        if (raw == null) return;
        String text = raw.trim();
        if (text.isEmpty()) return;

        // Non-slash → chat
        if (!text.startsWith("/")) {
            long id = sender.getClientId();
            if (spectator.getOrDefault(id, false)) {
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                        "Spectators cannot send chat messages.");
                return;
            }
            // Use base Room chat behavior (prefix [CHAT] and broadcast)
            super.handleMessage(sender, text);
            return;
        }

        // Slash commands below
        if (text.equalsIgnoreCase("/ready")) {
            handleReady(sender);
            return;
        }
        if (text.toLowerCase().startsWith("/answer ")) {
            handleAnswer(sender, text.substring(8).trim());
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

        // Unknown /command → treat as event (not chat)
        sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                "Unknown command: " + text);
    }

    // ====================== Ready / Session Control ======================

    private void handleReady(ServerThread sender) {
        long id = sender.getClientId();
        if (spectator.getOrDefault(id, false)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot ready up.");
            return;
        }
        ready.put(id, true);

        if (hostClientId == Constants.DEFAULT_CLIENT_ID) {
            hostClientId = id;
            broadcast(null, sender.getDisplayName() + " is the session host.");
        }

        broadcast(null, sender.getDisplayName() + " is ready!");
        sendUserListToAll();

        if (!sessionActive && allActivePlayersReady()) {
            startSession();
        }
    }

    private boolean allActivePlayersReady() {
        boolean any = false;
        for (ServerThread st : getClients()) {
            long id = st.getClientId();
            if (spectator.getOrDefault(id, false) || away.getOrDefault(id, false)) {
                continue;
            }
            any = true;
            if (!ready.getOrDefault(id, false)) return false;
        }
        return any;
    }

    private void startSession() {
        // Fresh question bank per session
        loadQuestionsFromFile();

        sessionActive = true;
        currentRound = 0;
        broadcast(null, "=== Trivia Session Starting ===");

        // Reset ready flags; new ready check required for next session
        for (Long id : ready.keySet()) {
            ready.put(id, false);
        }
        startNextRound();
    }

    private void endSession() {
        sessionActive = false;
        stopTimer();
        broadcast(null, "=== GAME OVER ===");
        showScoreboard("Final scores:");

        // Reset player points and round-specific state for a new session
        for (Long id : points.keySet()) {
            points.put(id, 0);
        }
        lockedThisRound.replaceAll((k, v) -> false);
        currentQuestion = null;
        correctOrder.clear();
        timerSecondsLeft = 0;

        sendUserListToAll();
        broadcast(null, "Type /ready to play again.");
    }

    private void startNextRound() {
        currentRound++;
        correctOrder.clear();
        lockedThisRound.replaceAll((k, v) -> false);

        if (currentRound > MAX_ROUNDS) {
            endSession();
            return;
        }

        currentQuestion = drawRandomQuestionFromPool();
        if (currentQuestion == null) {
            broadcast(null, "No questions available for selected categories.");
            endSession();
            return;
        }

        broadcast(null, "Round " + currentRound + " of " + MAX_ROUNDS);
        sendQuestionToAll();
        sendUserListToAll();
        startTimer();
    }

    // ====================== Questions / Categories ======================

    private void loadQuestionsFromFile() {
        questions.clear();
        // Format: category|question|a1|a2|a3|a4|correctIndex
        File f = new File("questions.txt");
        if (!f.exists()) {
            addSampleQuestions();
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                Question q = parseQuestionLine(line);
                if (q != null) questions.add(q);
            }
        } catch (IOException e) {
            addSampleQuestions();
        }
    }

    private Question parseQuestionLine(String line) {
        String[] parts = line.split("\\|");
        if (parts.length < 4) return null;

        Question q = new Question();
        q.category = parts[0].toLowerCase();
        q.text = parts[1];

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
        // Fallback questions if questions.txt is missing
        Question q1 = new Question();
        q1.category = "movies";
        q1.text = "Who directed the movie 'Inception'?";
        q1.answers.addAll(Arrays.asList(
                "Christopher Nolan", "Steven Spielberg", "James Cameron", "Ridley Scott"));
        q1.correctIndex = 0;
        questions.add(q1);

        Question q2 = new Question();
        q2.category = "sports";
        q2.text = "How many points is a touchdown worth in American football?";
        q2.answers.addAll(Arrays.asList("3", "6", "7", "9"));
        q2.correctIndex = 1;
        questions.add(q2);
    }

    /**
     * Randomly chooses and removes a question from the in-memory list.
     * Honors enabledCategories if possible.
     */
    private Question drawRandomQuestionFromPool() {
        if (questions.isEmpty()) return null;

        ArrayList<Integer> eligible = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            if (enabledCategories.isEmpty() || enabledCategories.contains(q.category)) {
                eligible.add(i);
            }
        }

        if (eligible.isEmpty()) {
            // No category-filtered questions; use all remaining
            int idx = random.nextInt(questions.size());
            return questions.remove(idx);
        } else {
            int listIndex = eligible.get(random.nextInt(eligible.size()));
            return questions.remove(listIndex);
        }
    }

    private void handleCategorySelection(ServerThread sender, String csv) {
        if (sessionActive) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Categories can be changed only during Ready Check.");
            return;
        }
        if (sender.getClientId() != hostClientId) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Only the host can change categories.");
            return;
        }

        enabledCategories.clear();
        for (String p : csv.split(",")) {
            String cat = p.trim().toLowerCase();
            if (!cat.isEmpty()) enabledCategories.add(cat);
        }
        broadcast(null, "Host set categories to: " + enabledCategories);
    }

    private void handleAddQuestion(ServerThread sender, String line) {
        if (sessionActive) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Add questions only outside an active session.");
            return;
        }
        Question q = parseQuestionLine(line);
        if (q == null) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Invalid /addq format. Use category|question|a1|a2|a3|a4|correctIndex");
            return;
        }
        questions.add(q);
        appendQuestionToFile(line);
        broadcast(null, "New question added by " + sender.getDisplayName() +
                " in '" + q.category + "'.");
    }

    private void appendQuestionToFile(String line) {
        try (FileWriter fw = new FileWriter("questions.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println(line);
        } catch (IOException ignored) {
        }
    }

    // ====================== Away / Spectator ======================

    private void handleAway(ServerThread sender, boolean isAway) {
        long id = sender.getClientId();
        away.put(id, isAway);
        broadcast(null, sender.getDisplayName() +
                (isAway ? " is now away." : " is no longer away."));
        sendUserListToAll();
    }

    private void handleSpectator(ServerThread sender) {
        long id = sender.getClientId();
        spectator.put(id, true);
        ready.put(id, false);
        broadcast(null, sender.getDisplayName() + " joined as a spectator.");
        sendUserListToAll();
    }

    // ====================== Answers / Round End ======================

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
                    "You already locked in this round.");
            return;
        }

        int index = mapChoiceToIndex(choiceText);
        if (index < 0 || index >= currentQuestion.answers.size()) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Invalid answer. Use A, B, C, D or 0,1,2,3.");
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

        sendUserListToAll(); // mark locked-in status

        if (allActivePlayersLocked()) {
            endRound("All active players locked in.");
        }
    }

    // Accepts A/B/C/D OR 0/1/2/3
    private int mapChoiceToIndex(String choiceText) {
        if (choiceText == null || choiceText.isEmpty()) return -1;
        char c = Character.toUpperCase(choiceText.trim().charAt(0));
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
            if (spectator.getOrDefault(id, false) || away.getOrDefault(id, false)) continue;
            if (!lockedThisRound.getOrDefault(id, false)) return false;
        }
        return true;
    }

    private void endRound(String reason) {
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
        String text = currentQuestion.answers.get(idx);
        broadcast(null, "Correct answer: " + letter + " – " + text);
    }

    private void awardPoints() {
        // Diminishing returns; still beginner-friendly (array based)
        int[] awards = new int[]{10, 7, 5, 3, 1};
        for (int i = 0; i < correctOrder.size(); i++) {
            long id = correctOrder.get(i);
            int award = (i < awards.length) ? awards[i] : 1;
            int newTotal = points.getOrDefault(id, 0) + award;
            points.put(id, newTotal);

            ServerThread st = findClient(id);
            String name = (st != null) ? st.getDisplayName() : ("Player#" + id);

            broadcast(null, name + " earned " + award + " points!");

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
            if (s.getClientId() == id) return s;
        }
        return null;
    }

    private void showScoreboard(String header) {
        broadcast(null, header);
        ArrayList<Map.Entry<Long, Integer>> list =

// UCID: nhd5
// Date: December 8, 2025
// Description: TriviaGuessGame GameRoom – server-side round/session logic (MS2+MS3).
//  - Uses classic payload names: QUESTION and USER_LIST (Option B).
//  - Reads questions from "questions.txt" in the working directory (fallback to samples).
//  - Accepts answers as A/B/C/D OR 0/1/2/3.
// References (beginner friendly patterns):
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

    // Tunables per rubric
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

        // Start with some defaults enabled (host can change during ready check)
        enabledCategories.add("geography");
        enabledCategories.add("science");
        enabledCategories.add("math");
        enabledCategories.add("history");
        enabledCategories.add("movies");
        enabledCategories.add("sports");

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

    // ====================== Command Handling ======================

    @Override
    protected synchronized void handleMessage(ServerThread sender, String raw) {
        String text = raw.trim();

        if (text.equalsIgnoreCase("/ready")) { handleReady(sender); return; }
        if (text.toLowerCase().startsWith("/answer ")) { handleAnswer(sender, text.substring(8).trim()); return; }
        if (text.equalsIgnoreCase("/away")) { handleAway(sender, true); return; }
        if (text.equalsIgnoreCase("/back")) { handleAway(sender, false); return; }
        if (text.equalsIgnoreCase("/spectate")) { handleSpectator(sender); return; }
        if (text.toLowerCase().startsWith("/categories ")) { handleCategorySelection(sender, text.substring(12).trim()); return; }
        if (text.toLowerCase().startsWith("/addq ")) { handleAddQuestion(sender, text.substring(6).trim()); return; }

        // Fallback: normal room chat
        super.handleMessage(sender, raw);
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
        sessionActive = true;
        currentRound = 0;
        broadcast(null, "=== Trivia Session Starting ===");
        // ready flags reset for next session
        for (Long id : ready.keySet()) ready.put(id, false);
        startNextRound();
    }

    private void endSession() {
        sessionActive = false;
        broadcast(null, "=== GAME OVER ===");
        showScoreboard("Final scores:");
        broadcast(null, "Type /ready to play again.");
        currentQuestion = null;
        correctOrder.clear();
        timerSecondsLeft = 0;
        sendUserListToAll();
    }

    private void startNextRound() {
        currentRound++;
        correctOrder.clear();
        lockedThisRound.replaceAll((k, v) -> false);

        if (currentRound > MAX_ROUNDS) { endSession(); return; }

        currentQuestion = pickRandomQuestion();
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
        for (int i = 0; i < answersCount; i++) q.answers.add(parts[2 + i]);

        try {
            q.correctIndex = Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException ex) {
            q.correctIndex = 0;
        }
        return q;
    }

    private void addSampleQuestions() {
        Question q1 = new Question();
        q1.category = "movies";
        q1.text = "Who directed the movie 'Inception'?";
        q1.answers.addAll(Arrays.asList("Christopher Nolan", "Steven Spielberg", "James Cameron", "Ridley Scott"));
        q1.correctIndex = 0;
        questions.add(q1);

        Question q2 = new Question();
        q2.category = "sports";
        q2.text = "How many points is a touchdown worth in American football?";
        q2.answers.addAll(Arrays.asList("3", "6", "7", "9"));
        q2.correctIndex = 1;
        questions.add(q2);
    }

    private Question pickRandomQuestion() {
        ArrayList<Question> filtered = new ArrayList<>();
        for (Question q : questions) if (enabledCategories.contains(q.category)) filtered.add(q);
        if (filtered.isEmpty()) filtered.addAll(questions);
        if (filtered.isEmpty()) return null;
        return filtered.get(random.nextInt(filtered.size()));
    }

    private void handleCategorySelection(ServerThread sender, String csv) {
        if (sessionActive) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Categories can be changed only during Ready Check.");
            return;
        }
        if (sender.getClientId() != hostClientId) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Only the host can change categories.");
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
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Add questions only outside an active session.");
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
        broadcast(null, "New question added by " + sender.getDisplayName() + " in '" + q.category + "'.");
    }

    private void appendQuestionToFile(String line) {
        try (FileWriter fw = new FileWriter("questions.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(line);
        } catch (IOException ignored) { }
    }

    // ====================== Away / Spectator ======================

    private void handleAway(ServerThread sender, boolean isAway) {
        long id = sender.getClientId();
        away.put(id, isAway);
        broadcast(null, sender.getDisplayName() + (isAway ? " is now away." : " is no longer away."));
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
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "No active round. Wait for the next question.");
            return;
        }

        long id = sender.getClientId();
        if (spectator.getOrDefault(id, false)) { sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot answer."); return; }
        if (away.getOrDefault(id, false)) { sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "You are marked away. Use /back to play again."); return; }
        if (lockedThisRound.getOrDefault(id, false)) { sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "You already locked in this round."); return; }

        int index = mapChoiceToIndex(choiceText);
        if (index < 0 || index >= currentQuestion.answers.size()) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid answer. Use A, B, C, D or 0,1,2,3.");
            return;
        }

        boolean correct = (index == currentQuestion.correctIndex);
        lockedThisRound.put(id, true);

        broadcast(null, sender.getDisplayName() + (correct ? " locked in the CORRECT answer!" : " locked in the WRONG answer."));
        if (correct) correctOrder.add(id);

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
        awardPoints();
        showScoreboard("Current scores:");
        sendUserListToAll();
        stopTimer();

        if (currentRound >= MAX_ROUNDS) endSession();
        else startNextRound();
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
            for (ServerThread client : getClients()) client.sendPayload(p);
        }
    }

    private ServerThread findClient(long id) {
        for (ServerThread s : getClients()) if (s.getClientId() == id) return s;
        return null;
    }

    private void showScoreboard(String header) {
        broadcast(null, header);
        ArrayList<Map.Entry<Long, Integer>> list = new ArrayList<>(points.entrySet());
        list.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        for (Map.Entry<Long, Integer> e : list) {
            ServerThread st = findClient(e.getKey());
            String name = (st != null) ? st.getDisplayName() : ("Player#" + e.getKey());
            broadcast(null, name + ": " + e.getValue() + " points");
        }
    }

    // ====================== Sync: Question / User List / Timer ======================

    private void sendQuestionToAll() {
        for (ServerThread client : getClients()) sendQuestionToClient(client);
    }

    private void sendQuestionToClient(ServerThread client) {
        if (currentQuestion == null) return;
        QAPayload qp = new QAPayload();
        qp.setPayloadType(PayloadType.QUESTION); // Option B: classic name
        qp.setClientId(Constants.DEFAULT_CLIENT_ID);
        qp.setCategory(currentQuestion.category);
        qp.setQuestionText(currentQuestion.text);
        qp.setAnswers(new ArrayList<>(currentQuestion.answers));
        client.sendPayload(qp);
    }

    private void sendUserListToAll() {
        // Order stable across clients (sorted by points desc, then id)
        ArrayList<ServerThread> snapshot = new ArrayList<>(getClients());
        snapshot.sort((a, b) -> {
            int pa = points.getOrDefault(a.getClientId(), 0);
            int pb = points.getOrDefault(b.getClientId(), 0);
            if (pb != pa) return Integer.compare(pb, pa);
            return Long.compare(a.getClientId(), b.getClientId());
        });

        UserListPayload up = new UserListPayload();
        up.setPayloadType(PayloadType.USER_LIST); // Option B: classic name
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

        for (ServerThread client : getClients()) client.sendPayload(up);
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
            } catch (InterruptedException ignored) { }

            if (sessionActive && currentQuestion != null && timerSecondsLeft < 0) {
                synchronized (GameRoom.this) { endRound("Time is up!"); }
            }
        });
        timerThread.start();
    }

    private void stopTimer() {
        if (timerThread != null && timerThread.isAlive()) timerThread.interrupt();
        timerThread = null;
    }

    private void sendTimerToAll() {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.TIMER);
        p.setClientId(Constants.DEFAULT_CLIENT_ID);
        p.setMessage(Integer.toString(timerSecondsLeft));
        for (ServerThread client : getClients()) client.sendPayload(p);
    }
}

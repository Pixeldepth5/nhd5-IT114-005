 // UCID: nhd5
// Date: December 8, 2025
// Description: TriviaGuessGame GameRoom (MS2 + MS3 fully implemented)
//  - Uses classic payload names: QUESTION, USER_LIST, TIMER, POINTS_UPDATE
//  - Supports: Ready, Away, Spectator, Categories, Add Question, 20s Timer,
//             Scoring system, Scoreboard, Lock-in tracking.
//  - Reads questions from questions.txt (format: category|question|a|b|c|d|correctIndex)
//  - If missing, generates sample questions.
//
// References (Beginner friendly):
//  - https://www.w3schools.com/java/java_arraylist.asp
//  - https://www.w3schools.com/java/java_files_read.asp
//  - https://www.w3schools.com/java/java_hashmap.asp
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

    // ======================= Question POJO =======================
    private static class Question {
        String category;
        String text;
        ArrayList<String> answers = new ArrayList<>();
        int correctIndex;
    }

    // ======================= Game Settings ========================
    private static final int MAX_ROUNDS = 5;
    private static final int ROUND_SECONDS = 20;

    // ======================= Data Stores ==========================
    private final ArrayList<Question> questions = new ArrayList<>();
    private final Random random = new Random();

    private final Map<Long, Integer> points = new HashMap<>();
    private final Map<Long, Boolean> ready = new HashMap<>();
    private final Map<Long, Boolean> away = new HashMap<>();
    private final Map<Long, Boolean> spectator = new HashMap<>();
    private final Map<Long, Boolean> lockedThisRound = new HashMap<>();

    private boolean sessionActive = false;
    private int currentRound = 0;
    private Question currentQuestion;
    private ArrayList<Long> correctOrder = new ArrayList<>();

    private int timerSecondsLeft = 0;
    private Thread timerThread;

    private long hostClientId = Constants.DEFAULT_CLIENT_ID;
    private final Set<String> enabledCategories = new HashSet<>();

    // ==============================================================
    // Constructor
    // ==============================================================
    public GameRoom(String name) {
        super(name);

        enabledCategories.add("geography");
        enabledCategories.add("science");
        enabledCategories.add("math");
        enabledCategories.add("history");
        enabledCategories.add("movies");
        enabledCategories.add("sports");

        loadQuestionsFromFile();
    }

    // ==============================================================
    // Client Join / Disconnect
    // ==============================================================
    @Override
    protected synchronized void addClient(ServerThread client) {
        super.addClient(client);

        long id = client.getClientId();
        points.putIfAbsent(id, 0);
        ready.put(id, false);
        away.put(id, false);
        spectator.put(id, false);
        lockedThisRound.put(id, false);

        if (sessionActive) {
            spectator.put(id, true);
            client.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "You joined as a spectator (session already active).");
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

    // ==============================================================
    // Chat + Command Routing
    // ==============================================================
    @Override
    protected synchronized void handleMessage(ServerThread sender, String raw) {
        if (raw == null) return;
        String text = raw.trim();
        if (text.isEmpty()) return;

        // Chat if no slash
        if (!text.startsWith("/")) {
            if (spectator.getOrDefault(sender.getClientId(), false)) {
                sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                        "Spectators cannot chat.");
                return;
            }
            super.handleMessage(sender, text);
            return;
        }

        // Commands
        if (text.equalsIgnoreCase("/ready")) { handleReady(sender); return; }
        if (text.startsWith("/answer "))   { handleAnswer(sender, text.substring(8)); return; }
        if (text.equalsIgnoreCase("/away")) { handleAway(sender, true); return; }
        if (text.equalsIgnoreCase("/back")) { handleAway(sender, false); return; }
        if (text.equalsIgnoreCase("/spectate")) { handleSpectator(sender); return; }
        if (text.startsWith("/categories ")) { handleCategorySelection(sender, text.substring(12)); return; }
        if (text.startsWith("/addq ")) { handleAddQuestion(sender, text.substring(6)); return; }

        sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Unknown command: " + text);
    }

    // ==============================================================
    // READY / SESSION CONTROL
    // ==============================================================
    private void handleReady(ServerThread sender) {
        long id = sender.getClientId();

        if (spectator.get(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot ready.");
            return;
        }

        ready.put(id, true);
        if (hostClientId == Constants.DEFAULT_CLIENT_ID) {
            hostClientId = id;
            broadcast(null, sender.getDisplayName() + " is the host.");
        }

        broadcast(null, sender.getDisplayName() + " is READY");
        sendUserListToAll();

        if (!sessionActive && allActivePlayersReady()) {
            startSession();
        }
    }

    private boolean allActivePlayersReady() {
        boolean someone = false;

        for (ServerThread c : getClients()) {
            long id = c.getClientId();
            if (spectator.get(id) || away.get(id)) continue;

            someone = true;
            if (!ready.get(id)) return false;
        }
        return someone;
    }

    private void startSession() {
        loadQuestionsFromFile();
        sessionActive = true;
        currentRound = 0;

        broadcast(null, "=== New Trivia Session Starting ===");

        for (Long id : ready.keySet()) ready.put(id, false);

        startNextRound();
    }

    private void endSession() {
        sessionActive = false;
        stopTimer();
        broadcast(null, "=== GAME OVER ===");
        showScoreboard("Final Scores:");

        for (Long id : points.keySet()) points.put(id, 0);

        lockedThisRound.replaceAll((k, v) -> false);
        currentQuestion = null;
        correctOrder.clear();
        sendUserListToAll();

        broadcast(null, "Type /ready to play again.");
    }

    private void startNextRound() {
        currentRound++;
        correctOrder = new ArrayList<>();
        lockedThisRound.replaceAll((k, v) -> false);

        if (currentRound > MAX_ROUNDS) { endSession(); return; }

        currentQuestion = drawRandomQuestion();
        if (currentQuestion == null) {
            broadcast(null, "No valid questions available.");
            endSession();
            return;
        }

        broadcast(null, "Round " + currentRound + " / " + MAX_ROUNDS);
        sendQuestionToAll();
        sendUserListToAll();
        startTimer();
    }

    // ==============================================================
    // QUESTIONS / CATEGORIES
    // ==============================================================
    private void loadQuestionsFromFile() {
        questions.clear();
        File f = new File("questions.txt");

        if (!f.exists()) { addSampleQuestions(); return; }

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                Question q = parseQuestionLine(line.trim());
                if (q != null) questions.add(q);
            }
        } catch (Exception e) {
            addSampleQuestions();
        }
    }

    private Question parseQuestionLine(String line) {
        if (line.isEmpty() || line.startsWith("#")) return null;

        String[] p = line.split("\\|");
        if (p.length < 7) return null;

        Question q = new Question();
        q.category = p[0].toLowerCase();
        q.text = p[1];

        q.answers.add(p[2]);
        q.answers.add(p[3]);
        q.answers.add(p[4]);
        q.answers.add(p[5]);

        try { q.correctIndex = Integer.parseInt(p[6]); }
        catch (Exception ex) { q.correctIndex = 0; }

        return q;
    }

    private void addSampleQuestions() {
        questions.add(sample("geography", "What is the capital of France?",
                "Paris", "Berlin", "Madrid", "Rome", 0));
        questions.add(sample("science", "What planet is known as the Red Planet?",
                "Earth", "Mars", "Jupiter", "Venus", 1));
        questions.add(sample("math", "What is 9 × 9?",
                "72", "81", "99", "108", 1));
        questions.add(sample("history", "Who was the first U.S. President?",
                "George Washington", "Abraham Lincoln", "Thomas Jefferson", "John Adams", 0));
        questions.add(sample("movies", "Who directed Inception?",
                "Nolan", "Spielberg", "Cameron", "Scott", 0));
        questions.add(sample("sports", "How many points is a touchdown?",
                "3", "6", "7", "9", 1));
    }

    private Question sample(String cat, String q, String a, String b, String c, String d, int correct) {
        Question x = new Question();
        x.category = cat;
        x.text = q;
        x.answers.add(a);
        x.answers.add(b);
        x.answers.add(c);
        x.answers.add(d);
        x.correctIndex = correct;
        return x;
    }

    private Question drawRandomQuestion() {
        ArrayList<Question> eligible = new ArrayList<>();

        for (Question q : questions) {
            if (enabledCategories.contains(q.category)) eligible.add(q);
        }

        if (eligible.isEmpty()) eligible.addAll(questions);
        if (eligible.isEmpty()) return null;

        Question q = eligible.get(random.nextInt(eligible.size()));
        questions.remove(q);
        return q;
    }

    private void handleCategorySelection(ServerThread sender, String csv) {
        if (sessionActive) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Categories can only be changed during Ready Check.");
            return;
        }
        if (sender.getClientId() != hostClientId) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Only host can change categories.");
            return;
        }

        enabledCategories.clear();
        for (String s : csv.split(",")) {
            s = s.trim().toLowerCase();
            if (!s.isEmpty()) enabledCategories.add(s);
        }

        broadcast(null, "Host enabled categories: " + enabledCategories);
    }

    private void handleAddQuestion(ServerThread sender, String line) {
        if (sessionActive) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Cannot add question during session.");
            return;
        }

        Question q = parseQuestionLine(line);
        if (q == null) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Invalid format. Use: category|question|a|b|c|d|correctIndex");
            return;
        }

        questions.add(q);
        appendQuestionToFile(line);
        broadcast(null, "New question added in category: " + q.category);
    }

    private void appendQuestionToFile(String line) {
        try (PrintWriter out = new PrintWriter(new FileWriter("questions.txt", true))) {
            out.println(line);
        } catch (Exception ignored) {}
    }

    // ==============================================================
    // AWAY / SPECTATOR
    // ==============================================================
    private void handleAway(ServerThread sender, boolean v) {
        away.put(sender.getClientId(), v);
        broadcast(null, sender.getDisplayName() + (v ? " is AWAY." : " is BACK."));
        sendUserListToAll();
    }

    private void handleSpectator(ServerThread sender) {
        spectator.put(sender.getClientId(), true);
        ready.put(sender.getClientId(), false);

        broadcast(null, sender.getDisplayName() + " is now a SPECTATOR.");
        sendUserListToAll();
    }

    // ==============================================================
    // ANSWERING / ROUND RESOLUTION
    // ==============================================================
    private void handleAnswer(ServerThread sender, String rawChoice) {
        if (!sessionActive || currentQuestion == null) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "No active round.");
            return;
        }

        long id = sender.getClientId();

        if (spectator.get(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Spectators cannot answer."); return;
        }
        if (away.get(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "You are away."); return;
        }
        if (lockedThisRound.get(id)) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID, "Already locked."); return;
        }

        int idx = mapChoiceToIndex(rawChoice);
        if (idx < 0 || idx > 3) {
            sender.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    "Use A/B/C/D or 0/1/2/3.");
            return;
        }

        boolean correct = (idx == currentQuestion.correctIndex);
        lockedThisRound.put(id, true);

        broadcast(null, sender.getDisplayName() +
                (correct ? " locked CORRECT!" : " locked WRONG!"));

        if (correct) correctOrder.add(id);

        sendUserListToAll();

        if (allActivePlayersLocked()) {
            endRound("All answers submitted.");
        }
    }

    private int mapChoiceToIndex(String s) {
        if (s == null || s.isEmpty()) return -1;

        char c = Character.toUpperCase(s.trim().charAt(0));
        if (c >= '0' && c <= '3') return c - '0';
        if (c == 'A') return 0;
        if (c == 'B') return 1;
        if (c == 'C') return 2;
        if (c == 'D') return 3;

        return -1;
    }

    private boolean allActivePlayersLocked() {
        for (ServerThread c : getClients()) {
            long id = c.getClientId();

            if (spectator.get(id) || away.get(id)) continue;
            if (!lockedThisRound.get(id)) return false;
        }
        return true;
    }

    private void endRound(String reason) {
        broadcast(null, "Round Ended: " + reason);
        showCorrectAnswer();
        awardPoints();
        showScoreboard("Scores:");
        sendUserListToAll();
        stopTimer();

        if (currentRound >= MAX_ROUNDS) endSession();
        else startNextRound();
    }

    private void showCorrectAnswer() {
        int idx = currentQuestion.correctIndex;
        char letter = (char) ('A' + idx);
        broadcast(null, "Correct Answer: " + letter + " → " + currentQuestion.answers.get(idx));
    }

    private void awardPoints() {
        int[] awards = {10, 7, 5, 3, 1};

        for (int i = 0; i < correctOrder.size(); i++) {
            long id = correctOrder.get(i);

            int gained = (i < awards.length ? awards[i] : 1);
            int total = points.get(id) + gained;
            points.put(id, total);

            ServerThread st = findClient(id);
            String name = (st != null ? st.getDisplayName() : ("Player#" + id));

            broadcast(null, name + " earned " + gained + " pts!");

            PointsPayload pp = new PointsPayload();
            pp.setPayloadType(PayloadType.POINTS_UPDATE);
            pp.setClientId(Constants.DEFAULT_CLIENT_ID);
            pp.setTargetClientId(id);
            pp.setPoints(total);
            pp.setMessage(name + " now has " + total + " points.");

            for (ServerThread c : getClients()) c.sendPayload(pp);
        }
    }

    private ServerThread findClient(long id) {
        for (ServerThread s : getClients())
            if (s.getClientId() == id) return s;
        return null;
    }

    private void showScoreboard(String header) {
        broadcast(null, header);

        ArrayList<Map.Entry<Long, Integer>> order =
                new ArrayList<>(points.entrySet());

        order.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        for (Map.Entry<Long, Integer> e : order) {
            ServerThread st = findClient(e.getKey());
            String name = (st != null ? st.getDisplayName() : ("Player#" + e.getKey()));
            broadcast(null, name + ": " + e.getValue());
        }
    }

    // ==============================================================
    // QUESTION & USER LIST SYNC
    // ==============================================================
    private void sendQuestionToAll() {
        for (ServerThread c : getClients()) sendQuestionToClient(c);
    }

    private void sendQuestionToClient(ServerThread client) {
        if (currentQuestion == null) return;

        QAPayload qp = new QAPayload();
        qp.setPayloadType(PayloadType.QUESTION);
        qp.setClientId(Constants.DEFAULT_CLIENT_ID);
        qp.setCategory(currentQuestion.category);
        qp.setQuestionText(currentQuestion.text);
        qp.setAnswers(new ArrayList<>(currentQuestion.answers));

        client.sendPayload(qp);
    }

    private void sendUserListToAll() {
        UserListPayload up = new UserListPayload();
        up.setPayloadType(PayloadType.USER_LIST);
        up.setClientId(Constants.DEFAULT_CLIENT_ID);

        ArrayList<ServerThread> sorted = new ArrayList<>(getClients());
        sorted.sort((a, b) -> {
            int pa = points.get(a.getClientId());
            int pb = points.get(b.getClientId());
            if (pa != pb) return Integer.compare(pb, pa);
            return Long.compare(a.getClientId(), b.getClientId());
        });

        for (ServerThread c : sorted) {
            long id = c.getClientId();
            up.addUser(
                    id,
                    c.getDisplayName(),
                    points.get(id),
                    lockedThisRound.get(id),
                    away.get(id),
                    spectator.get(id)
            );
        }

        for (ServerThread c : getClients()) c.sendPayload(up);
    }

    // ==============================================================
    // ROUND TIMER
    // ==============================================================
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
            } catch (InterruptedException ignored) {}

            if (sessionActive && currentQuestion != null && timerSecondsLeft < 0) {
                synchronized (GameRoom.this) {
                    endRound("Time's up!");
                }
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
        p.setMessage(String.valueOf(timerSecondsLeft));

        for (ServerThread c : getClients()) c.sendPayload(p);
    }
}

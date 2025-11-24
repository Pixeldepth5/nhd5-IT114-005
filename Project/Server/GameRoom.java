// UCID: nhd5
// Date: November 23, 2025
// Description: WordGuesserGame GameRoom – core word guessing logic for Milestone 2
// Reference: 
//   https://www.w3schools.com/java/java_arraylist.asp (ArrayList & loops)
//   https://www.w3schools.com/java/java_hashmap.asp (HashMap for points)
//   https://www.w3schools.com/java/java_files_read.asp (reading text files)
//   https://www.w3schools.com/java/java_random.asp (Random for picking words)

package Server;

import Common.*;
import java.io.*;
import java.util.*;

public class GameRoom extends Room {
    private static final int MAX_STRIKES = 6;
    private static final int MAX_ROUNDS = 3;
    private static final int POINTS_PER_LETTER = 10;
    private static final int SOLVE_BONUS_MULTIPLIER = 2;

    private final List<String> wordList = new ArrayList<>();
    private final Random random = new Random();

    private String currentWord;
    private char[] blanks;
    private final Set<Character> guessedLetters = new HashSet<>();
    private final Map<Long, Integer> points = new HashMap<>();
    private final Set<Long> readyPlayers = new HashSet<>();
    private final List<Long> turnOrder = new ArrayList<>();
    private int currentTurn = -1;
    private int strikes = 0;
    private int round = 0;
    private boolean activeSession = false;

    public GameRoom(String name) {
        super(name);
    }

    @Override
    protected synchronized void addClient(ServerThread client) {
        super.addClient(client);
        points.putIfAbsent(client.getClientId(), 0);
    }

    @Override
    protected synchronized void handleMessage(ServerThread sender, String msg) {
        String text = msg.trim();

        if (text.equalsIgnoreCase("/ready")) {
            readyPlayers.add(sender.getClientId());
            broadcast(null, sender.getDisplayName() + " is ready!");

            if (!activeSession && readyPlayers.size() == getClients().size()) {
                startSession();
            }
            return;
        }

        if (!activeSession) {
            super.handleMessage(sender, msg);
            return;
        }

        if (text.startsWith("/guess ")) {
            handleWordGuess(sender, text.substring(7).trim());
        } else if (text.startsWith("/letter ")) {
            handleLetterGuess(sender, text.substring(8).trim());
        } else if (text.equalsIgnoreCase("/skip")) {
            handleSkip(sender);
        } else {
            super.handleMessage(sender, msg);
        }
    }

    // ----------------- SESSION CONTROL ----------------- //

    private void startSession() {
        loadWords();
        readyPlayers.clear();
        points.replaceAll((k, v) -> 0);
        activeSession = true;
        round = 0;
        broadcast(null, "=== Starting Word Guesser Session ===");
        startRound();
    }

    private void startRound() {
        round++;
        strikes = 0;
        guessedLetters.clear();

        currentWord = pickRandomWord();
        blanks = makeBlanks(currentWord);
        initTurnOrder();

        broadcast(null, "Round " + round + "/" + MAX_ROUNDS);
        broadcast(null, "Word: " + showBlanks());
        currentTurn = -1;
        nextTurn();
    }

    private void endRound(String reason) {
        broadcast(null, "Round ended: " + reason);
        showScoreboard("Current scores:");
        if (round >= MAX_ROUNDS) endSession();
        else startRound();
    }

    private void endSession() {
        broadcast(null, "=== GAME OVER ===");
        showScoreboard("Final scores:");
        activeSession = false;
        readyPlayers.clear();
        broadcast(null, "Type /ready to play again!");
    }

    // ----------------- GAME LOGIC ----------------- //

    private void handleWordGuess(ServerThread sender, String guess) {
        if (!isTurn(sender)) return;

        if (guess.equalsIgnoreCase(currentWord)) {
            int missing = missingLetters();
            int score = missing * POINTS_PER_LETTER * SOLVE_BONUS_MULTIPLIER;
            addPoints(sender, score);
            broadcast(null, sender.getDisplayName() + " guessed the word '" +
                    currentWord + "' and earned " + score + " points!");
            endRound("Word solved!");
        } else {
            strikes++;
            broadcast(null, sender.getDisplayName() + " guessed '" + guess +
                    "' – wrong! (" + strikes + "/" + MAX_STRIKES + ")");
            checkStrikeEnd();
        }
    }

    private void handleLetterGuess(ServerThread sender, String arg) {
        if (!isTurn(sender)) return;
        if (arg.isEmpty()) return;

        char letter = Character.toLowerCase(arg.charAt(0));
        if (guessedLetters.contains(letter)) {
            sender.sendMessage(-1, "Letter already guessed.");
            nextTurn();
            return;
        }

        guessedLetters.add(letter);
        int matches = 0;
        for (int i = 0; i < currentWord.length(); i++) {
            if (Character.toLowerCase(currentWord.charAt(i)) == letter && blanks[i] == '_') {
                blanks[i] = currentWord.charAt(i);
                matches++;
            }
        }

        if (matches > 0) {
            int gained = matches * POINTS_PER_LETTER;
            addPoints(sender, gained);
            broadcast(null, sender.getDisplayName() + " found " + matches +
                    " '" + letter + "' and got " + gained + " points!");
            broadcast(null, "Word: " + showBlanks());
            if (isSolved()) endRound("Word completed!");
            else nextTurn();
        } else {
            strikes++;
            broadcast(null, sender.getDisplayName() + " guessed '" + letter +
                    "' – no matches!");
            checkStrikeEnd();
        }
    }

    private void handleSkip(ServerThread sender) {
        if (isTurn(sender)) {
            broadcast(null, sender.getDisplayName() + " skipped their turn.");
            nextTurn();
        } else {
            sender.sendMessage(-1, "Not your turn.");
        }
    }

    private void nextTurn() {
        if (turnOrder.isEmpty()) return;

        if (currentTurn < 0) {
            currentTurn = random.nextInt(turnOrder.size());
        } else {
            currentTurn = (currentTurn + 1) % turnOrder.size();
        }

        long id = turnOrder.get(currentTurn);
        ServerThread player = findClient(id);
        if (player == null) return;

        broadcast(null, "It's now " + player.getDisplayName() + "'s turn!");
        broadcast(null, "Word: " + showBlanks() +
                " | Strikes: " + strikes + "/" + MAX_STRIKES);
    }

    // ----------------- HELPERS ----------------- //

    private void checkStrikeEnd() {
        if (strikes >= MAX_STRIKES) endRound("Too many strikes!");
        else nextTurn();
    }

    private void loadWords() {
        if (!wordList.isEmpty()) return;

        try (InputStream in = getClass().getResourceAsStream("words.txt")) {
            if (in == null) throw new IOException("words.txt not found");
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) wordList.add(line);
                }
            }
        } catch (IOException e) {
            wordList.addAll(Arrays.asList("java", "socket", "payload", "network", "server"));
        }
    }

    private String pickRandomWord() {
        return wordList.get(random.nextInt(wordList.size()));
    }

    private char[] makeBlanks(String w) {
        char[] arr = new char[w.length()];
        for (int i = 0; i < w.length(); i++) arr[i] = Character.isLetter(w.charAt(i)) ? '_' : w.charAt(i);
        return arr;
    }

    private String showBlanks() {
        StringBuilder sb = new StringBuilder();
        for (char c : blanks) sb.append(c).append(' ');
        return sb.toString().trim();
    }

    private int missingLetters() {
        int m = 0;
        for (char c : blanks) if (c == '_') m++;
        return m;
    }

    private boolean isSolved() {
        for (char c : blanks) if (c == '_') return false;
        return true;
    }

    private void initTurnOrder() {
        turnOrder.clear();
        for (ServerThread s : getClients()) turnOrder.add(s.getClientId());
        Collections.shuffle(turnOrder, random);
    }

    private boolean isTurn(ServerThread s) {
        return !turnOrder.isEmpty() && s.getClientId() == turnOrder.get(currentTurn);
    }

    private ServerThread findClient(long id) {
        for (ServerThread s : getClients()) if (s.getClientId() == id) return s;
        return null;
    }

    private void addPoints(ServerThread s, int pts) {
        long id = s.getClientId();
        int newTotal = points.getOrDefault(id, 0) + pts;
        points.put(id, newTotal);

        PointsPayload p = new PointsPayload();
        p.setPayloadType(PayloadType.POINTS_UPDATE);
        p.setClientId(-1);
        p.setTargetClientId(id);
        p.setPoints(newTotal);
        p.setMessage(s.getDisplayName() + " now has " + newTotal + " points.");

        for (ServerThread st : getClients()) st.sendPayload(p);
    }

    private void showScoreboard(String header) {
        broadcast(null, header);
        points.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .forEach(e -> {
                    ServerThread st = findClient(e.getKey());
                    String name = (st != null) ? st.getDisplayName() : ("Player#" + e.getKey());
                    broadcast(null, name + ": " + e.getValue() + " points");
                });
    }
}

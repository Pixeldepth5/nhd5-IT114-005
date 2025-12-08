package Client;

import Common.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/**
 * UCID: nhd5
 * Date: December 8, 2025
 * Description: Final Client.java – Updated for Milestone 3, includes:
 *  - Chat-box slash commands
 *  - Ready, Away, Spectate, Categories, Add Question
 *  - Question + Answer button UI
 *  - Timer updates
 *  - User list sync + Points update
 *  - Fonts: Behind the Nineties (title) + Visby CF (subtitle)
 */

public class Client {

    // ------------------- Networking -------------------
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private long clientId = -1;

    // ------------------- UI Components -------------------
    JFrame frame;

    JLabel titleLabel;
    JTextArea chatArea;
    JTextField chatInput;
    JTextArea gameEvents;
    JLabel questionLabel;
    JLabel categoryLabel;
    JLabel timerLabel;
    JButton ansA, ansB, ansC, ansD;

    DefaultListModel<String> userListModel = new DefaultListModel<>();
    JList<String> userList = new JList<>(userListModel);

    JButton readyBtn;
    JCheckBox awayChk, spectateChk;
    JCheckBox geoChk, sciChk, mathChk, histChk;

    JButton addQuestionBtn;

    JTextField usernameField, hostField, portField;

    // ------------------- Internal State -------------------
    boolean roundActive = false;
    boolean lockedAnswer = false;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }

    public Client() {
        buildUI();
    }

    // =========================================================
    // UI BUILD
    // =========================================================
    private void buildUI() {
        frame = new JFrame("TriviaGuessGame – nhd5");
        frame.setSize(1100, 700);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.getContentPane().setBackground(new Color(245, 240, 255));

        // Title
        titleLabel = new JLabel("Trivia Guess Game", SwingConstants.CENTER);
        titleLabel.setBounds(200, 5, 700, 50);
        try {
            titleLabel.setFont(new Font("Behind The Nineties", Font.BOLD, 36));
        } catch (Exception e) {
            titleLabel.setFont(new Font("Serif", Font.BOLD, 36));
        }
        frame.add(titleLabel);

        // ---------- Connection Panel ----------
        JPanel conn = roundedPanel();
        conn.setBounds(20, 60, 300, 120);
        conn.setLayout(null);

        conn.add(label("Username:", 10, 10));
        usernameField = textField("dev", 100, 10);
        conn.add(usernameField);

        conn.add(label("Host:", 10, 45));
        hostField = textField("localhost", 100, 45);
        conn.add(hostField);

        conn.add(label("Port:", 10, 80));
        portField = textField("3000", 100, 80);
        conn.add(portField);

        JButton connectBtn = new JButton("Connect");
        connectBtn.setBounds(200, 80, 90, 28);
        connectBtn.addActionListener(e -> connect());
        conn.add(connectBtn);

        frame.add(conn);

        // ---------- Ready & Options ----------
        JPanel readyPanel = roundedPanel();
        readyPanel.setBounds(20, 190, 300, 150);
        readyPanel.setLayout(null);

        readyBtn = new JButton("READY");
        readyBtn.setBounds(10, 10, 120, 28);
        readyBtn.addActionListener(e -> sendCommand("/ready"));
        readyPanel.add(readyBtn);

        addQuestionBtn = new JButton("Add Question");
        addQuestionBtn.setBounds(150, 10, 120, 28);
        addQuestionBtn.addActionListener(e -> openAddQuestionDialog());
        readyPanel.add(addQuestionBtn);

        awayChk = new JCheckBox("Away");
        awayChk.setBounds(10, 50, 80, 20);
        awayChk.addActionListener(e -> sendCommand(awayChk.isSelected() ? "/away" : "/back"));
        readyPanel.add(awayChk);

        spectateChk = new JCheckBox("Spectator");
        spectateChk.setBounds(100, 50, 120, 20);
        spectateChk.addActionListener(e -> sendCommand("/spectate"));
        readyPanel.add(spectateChk);

        // Categories
        readyPanel.add(label("Categories:", 10, 80));

        geoChk = new JCheckBox("Geography", true);
        sciChk = new JCheckBox("Science", true);
        mathChk = new JCheckBox("Math", true);
        histChk = new JCheckBox("History", true);

        geoChk.setBounds(10, 100, 100, 20);
        sciChk.setBounds(110, 100, 80, 20);
        mathChk.setBounds(10, 125, 100, 20);
        histChk.setBounds(110, 125, 80, 20);

        ActionListener catUpdate = e -> sendCategoriesToServer();
        geoChk.addActionListener(catUpdate);
        sciChk.addActionListener(catUpdate);
        mathChk.addActionListener(catUpdate);
        histChk.addActionListener(catUpdate);

        readyPanel.add(geoChk);
        readyPanel.add(sciChk);
        readyPanel.add(mathChk);
        readyPanel.add(histChk);

        frame.add(readyPanel);

        // ---------- Users Panel ----------
        JPanel usersPanel = roundedPanel();
        usersPanel.setBounds(20, 350, 300, 300);
        usersPanel.setLayout(new BorderLayout());
        usersPanel.add(new JLabel("Users"), BorderLayout.NORTH);
        usersPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        frame.add(usersPanel);

        // ---------- Game Area ----------
        JPanel gamePanel = roundedPanel();
        gamePanel.setBounds(340, 60, 430, 590);
        gamePanel.setLayout(null);

        categoryLabel = label("Category: -", 10, 10);
        gamePanel.add(categoryLabel);

        timerLabel = label("Timer: -", 300, 10);
        gamePanel.add(timerLabel);

        questionLabel = new JLabel("Question appears here.", SwingConstants.CENTER);
        questionLabel.setBounds(20, 60, 380, 100);
        questionLabel.setFont(new Font("Visby CF", Font.PLAIN, 16));
        gamePanel.add(questionLabel);

        // Answer buttons
        ansA = answerButton("Answer 1", 40, 200);
        ansB = answerButton("Answer 2", 230, 200);
        ansC = answerButton("Answer 3", 40, 240);
        ansD = answerButton("Answer 4", 230, 240);

        gamePanel.add(ansA);
        gamePanel.add(ansB);
        gamePanel.add(ansC);
        gamePanel.add(ansD);

        frame.add(gamePanel);

        // ---------- Chat ----------
        JPanel chatPanel = roundedPanel();
        chatPanel.setBounds(780, 60, 300, 350);
        chatPanel.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputWrap = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        JButton sendBtn = new JButton("Send");

        sendBtn.addActionListener(e -> handleChatSend());
        chatInput.addActionListener(e -> handleChatSend());

        inputWrap.add(chatInput, BorderLayout.CENTER);
        inputWrap.add(sendBtn, BorderLayout.EAST);

        chatPanel.add(inputWrap, BorderLayout.SOUTH);

        frame.add(chatPanel);

        // ---------- Game Events ----------
        JPanel eventsPanel = roundedPanel();
        eventsPanel.setBounds(780, 420, 300, 230);
        eventsPanel.setLayout(new BorderLayout());
        eventsPanel.add(new JLabel("Game Events"), BorderLayout.NORTH);

        gameEvents = new JTextArea();
        gameEvents.setEditable(false);
        eventsPanel.add(new JScrollPane(gameEvents), BorderLayout.CENTER);

        frame.add(eventsPanel);

        frame.setVisible(true);
    }

    // =========================================================
    // CHAT COMMANDS
    // =========================================================

    private void handleChatSend() {
        String text = chatInput.getText().trim();
        if (text.isEmpty()) return;

        if (text.startsWith("/")) {
            sendCommand(text);
        } else {
            sendChat(text);
        }

        chatInput.setText("");
    }

    private void sendChat(String msg) {
        try {
            Payload p = new Payload();
            p.setPayloadType(PayloadType.MESSAGE);
            p.setMessage(msg);
            out.writeObject(p);
            out.flush();
        } catch (Exception ignored) {}
    }

    private void sendCommand(String cmd) {
        try {
            Payload p = new Payload();
            p.setPayloadType(PayloadType.COMMAND);
            p.setMessage(cmd);
            out.writeObject(p);
            out.flush();
        } catch (Exception ignored) {}
    }

    private void sendCategoriesToServer() {
        String csv = "";
        if (geoChk.isSelected()) csv += "geography,";
        if (sciChk.isSelected()) csv += "science,";
        if (mathChk.isSelected()) csv += "math,";
        if (histChk.isSelected()) csv += "history,";

        sendCommand("/categories " + csv);
    }

    // =========================================================
    // ANSWER BUTTON LOGIC
    // =========================================================

    private JButton answerButton(String text, int x, int y) {
        JButton b = new JButton(text);
        b.setBounds(x, y, 150, 32);
        b.addActionListener(e -> {
            if (!roundActive || lockedAnswer) return;

            int index = switch (b.getText()) {
                case "A", "Answer 1" -> 0;
                case "B", "Answer 2" -> 1;
                case "C", "Answer 3" -> 2;
                default -> 3;
            };

            sendCommand("/answer " + index);
            lockedAnswer = true;
            disableAnswerButtons();
            b.setBackground(new Color(180, 200, 255));
        });
        return b;
    }

    private void disableAnswerButtons() {
        ansA.setEnabled(false);
        ansB.setEnabled(false);
        ansC.setEnabled(false);
        ansD.setEnabled(false);
    }

    private void enableAnswerButtons() {
        ansA.setEnabled(true);
        ansB.setEnabled(true);
        ansC.setEnabled(true);
        ansD.setEnabled(true);

        ansA.setBackground(null);
        ansB.setBackground(null);
        ansC.setBackground(null);
        ansD.setBackground(null);
    }

    // =========================================================
    // CONNECTION
    // =========================================================

    private void connect() {
        try {
            socket = new Socket(hostField.getText().trim(),
                                Integer.parseInt(portField.getText().trim()));
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Send connect payload
            ConnectPayload c = new ConnectPayload();
            c.setPayloadType(PayloadType.CONNECT);
            c.setClientName(usernameField.getText().trim());
            out.writeObject(c);
            out.flush();

            new Thread(this::listen).start();

        } catch (Exception e) {
            appendChat("Connection failed: " + e.getMessage());
        }
    }

    // =========================================================
    // LISTENER LOOP
    // =========================================================
    private void listen() {
        try {
            while (true) {
                Object obj = in.readObject();

                if (obj instanceof Payload p) {
                    switch (p.getPayloadType()) {
                        case CLIENT_ID -> {
                            this.clientId = p.getClientId();
                        }
                        case MESSAGE -> appendChat(p.getMessage());
                        case SERVER_MESSAGE -> appendEvent(p.getMessage());

                        case QUESTION -> {
                            handleQuestion((QAPayload)p);
                        }
                        case USER_LIST -> {
                            updateUserList((UserListPayload)p);
                        }
                        case POINTS_UPDATE -> {
                            appendEvent(((PointsPayload)p).getMessage());
                        }
                        case TIMER -> {
                            timerLabel.setText("Timer: " + p.getNumber());
                        }
                    }
                }
            }
        } catch (Exception e) {
            appendEvent("Disconnected from server.");
        }
    }

    // =========================================================
    // UI UPDATES FROM SERVER
    // =========================================================

    private void handleQuestion(QAPayload q) {
        categoryLabel.setText("Category: " + q.getCategory());
        questionLabel.setText("<html><div style='text-align:center;'>" +
                q.getQuestion() + "</div></html>");

        ArrayList<String> ans = q.getAnswers();
        ansA.setText(ans.get(0));
        ansB.setText(ans.get(1));
        ansC.setText(ans.get(2));
        ansD.setText(ans.get(3));

        roundActive = true;
        lockedAnswer = false;
        enableAnswerButtons();
    }

    private void updateUserList(UserListPayload list) {
        userListModel.clear();
        for (String u : list.getUsers()) userListModel.addElement(u);
    }

    // =========================================================
    // UTIL UI HELPERS
    // =========================================================

    private JPanel roundedPanel() {
        JPanel p = new JPanel();
        p.setBackground(new Color(250, 245, 255));
        p.setBorder(new LineBorder(new Color(180, 160, 220), 2, true));
        return p;
    }

    private JLabel label(String text, int x, int y) {
        JLabel l = new JLabel(text);
        l.setBounds(x, y, 200, 20);
        return l;
    }

    private JTextField textField(String val, int x, int y) {
        JTextField t = new JTextField(val);
        t.setBounds(x, y, 100, 22);
        return t;
    }

    private void appendChat(String text) {
        chatArea.append(text + "\n");
    }
    private void appendEvent(String text) {
        gameEvents.append(text + "\n");
    }

    // =========================================================
    // ADD QUESTION POPUP
    // =========================================================
    private void openAddQuestionDialog() {
        JTextField cat = new JTextField();
        JTextField q = new JTextField();
        JTextField a1 = new JTextField();
        JTextField a2 = new JTextField();
        JTextField a3 = new JTextField();
        JTextField a4 = new JTextField();
        JTextField correct = new JTextField();

        Object[] msg = {
                "Category:", cat,
                "Question:", q,
                "Answer 1:", a1,
                "Answer 2:", a2,
                "Answer 3:", a3,
                "Answer 4:", a4,
                "Correct Index (0-3):", correct
        };

        int opt = JOptionPane.showConfirmDialog(frame, msg,
                "Add Question", JOptionPane.OK_CANCEL_OPTION);

        if (opt == JOptionPane.OK_OPTION) {
            String line = cat.getText() + "|" + q.getText() + "|" +
                    a1.getText() + "|" + a2.getText() + "|" +
                    a3.getText() + "|" + a4.getText() + "|" +
                    correct.getText();

            sendCommand("/addq " + line);
        }
    }
}

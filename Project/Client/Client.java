// UCID: nhd5
// Date: December 8, 2025
// Description: TriviaGuessGame Client – Swing UI + networking (MS1–MS3)
//  - Connect panel (Username / Host / Port)
//  - Ready panel (READY button, Away, Spectator, Categories)
//  - Game panel (Question, Answers, Timer)
//  - User list (name, id, points, locked/away/spectator)
//  - Chat panel (right side, separate from Game Events)
//  - Game Events panel (lock-ins, points, scoreboard, away/spectator messages)
// References (beginner-friendly):
//  - https://www.w3schools.com/java/java_swing.asp
//  - https://www.w3schools.com/java/java_arraylist.asp
//  - https://www.w3schools.com/java/java_hashmap.asp

package Client;

import Common.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class Client {

    // ===================== Networking fields =====================
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = false;
    private long clientId = -1;

    private boolean lockedThisRound = false;
    private boolean isAway = false;
    private boolean isSpectator = false;

    // ===================== UI fields =====================
    private JFrame frame;

    // Connection
    private JTextField txtUser;
    private JTextField txtHost;
    private JTextField txtPort;
    private JButton btnConnect;
    private JLabel lblConnectHint;

    // Ready + options
    private JButton btnReady;
    private JCheckBox chkAway;
    private JCheckBox chkSpectator;
    private JButton btnAddQ;

    // Categories
    private JCheckBox chkGeo;
    private JCheckBox chkSci;
    private JCheckBox chkMath;
    private JCheckBox chkHist;

    // User list
    private DefaultListModel<User> userModel;
    private JList<User> lstUsers;

    // Chat + events
    private JTextArea txtChat;
    private JTextField txtChatInput;
    private JButton btnSendChat;
    private JTextArea txtEvents;

    // Game area
    private JLabel lblCategory;
    private JLabel lblQuestion;
    private JLabel lblTimer;
    private JButton[] answerButtons = new JButton[4];

    // Simple cache of users by id
    private Map<Long, User> users = new HashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }

    public Client() {
        buildUI();
    }

    // ===================== UI construction =====================

    private void buildUI() {
        frame = new JFrame("TriviaGuessGame – nhd5");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 700);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(244, 240, 255));

        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(new EmptyBorder(10, 10, 10, 10));
        root.setOpaque(false);

        // Main title – Behind The Nineties
        JLabel lblTitle = new JLabel("Trivia Guess Game", SwingConstants.CENTER);
        TextFX.setTitleFont(lblTitle); // Behind the Nineties
        lblTitle.setForeground(new Color(40, 40, 70));
        lblTitle.setBorder(new EmptyBorder(5, 5, 15, 5));
        root.add(lblTitle, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        center.add(buildLeftColumn(), BorderLayout.WEST);
        center.add(buildGamePanel(), BorderLayout.CENTER);
        center.add(buildRightColumn(), BorderLayout.EAST);

        root.add(center, BorderLayout.CENTER);
        frame.add(root, BorderLayout.CENTER);

        frame.setVisible(true);
    }

    private JPanel buildLeftColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        col.add(buildConnectionPanel());
        col.add(Box.createVerticalStrut(8));
        col.add(buildOptionsPanel());
        col.add(Box.createVerticalStrut(8));
        col.add(buildUserPanel());

        return col;
    }

    private JPanel buildConnectionPanel() {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new TitledBorder(
                new LineBorder(new Color(210, 210, 230)),
                "Connection"));
        TitledBorder tb = (TitledBorder) panel.getBorder();
        tb.setTitleFont(TextFX.getSubtitleFont());
        tb.setTitleColor(new Color(60, 60, 90));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.gridy = 0;

        // Username
        c.gridx = 0;
        panel.add(makeLabel("Username:"), c);
        c.gridx = 1;
        txtUser = new JTextField("Player", 10);
        panel.add(txtUser, c);

        // Host
        c.gridx = 2;
        panel.add(makeLabel("Host:"), c);
        c.gridx = 3;
        txtHost = new JTextField("localhost", 10);
        panel.add(txtHost, c);

        // Port
        c.gridx = 0;
        c.gridy = 1;
        panel.add(makeLabel("Port:"), c);
        c.gridx = 1;
        txtPort = new JTextField("3000", 6);
        panel.add(txtPort, c);

        // Connect button
        c.gridx = 2;
        c.gridwidth = 2;
        btnConnect = new JButton("Connect");
        stylePrimaryButton(btnConnect);
        btnConnect.addActionListener(this::connectClicked);
        panel.add(btnConnect, c);

        // Hint label (shows step to click READY after connecting)
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 4;
        lblConnectHint = new JLabel("Step 1: Enter name, host, port and click Connect.");
        TextFX.setSubtitleFont(lblConnectHint);
        lblConnectHint.setForeground(new Color(80, 80, 110));
        panel.add(lblConnectHint, c);

        return panel;
    }

    private JPanel buildOptionsPanel() {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder(
                new LineBorder(new Color(210, 210, 230)),
                "Ready & Options"));
        TitledBorder tb = (TitledBorder) panel.getBorder();
        tb.setTitleFont(TextFX.getSubtitleFont());
        tb.setTitleColor(new Color(60, 60, 90));

        // Ready + Add Question
        JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r1.setOpaque(false);
        btnReady = new JButton("READY");
        stylePrimaryButton(btnReady);
        btnReady.addActionListener(e -> sendCommand("/ready"));
        r1.add(btnReady);

        JLabel lblReadyHint = new JLabel("Step 2: Click READY when you want to play.");
        TextFX.setSubtitleFont(lblReadyHint);
        lblReadyHint.setForeground(new Color(80, 80, 110));
        r1.add(lblReadyHint);

        btnAddQ = new JButton("Add Question");
        styleSecondaryButton(btnAddQ);
        btnAddQ.addActionListener(e -> showAddQDialog());
        r1.add(btnAddQ);

        panel.add(r1, BorderLayout.NORTH);

        // Away / Spectator
        JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        r2.setOpaque(false);
        chkAway = new JCheckBox("Away");
        chkAway.setOpaque(false);
        chkAway.addActionListener(e -> toggleAway());
        chkSpectator = new JCheckBox("Spectator");
        chkSpectator.setOpaque(false);
        chkSpectator.addActionListener(e -> toggleSpectator());
        TextFX.setSubtitleFont(chkAway);
        TextFX.setSubtitleFont(chkSpectator);
        r2.add(chkAway);
        r2.add(chkSpectator);
        panel.add(r2, BorderLayout.CENTER);

        // Categories
        RoundedPanel catPanel = new RoundedPanel();
        catPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        catPanel.setBorder(new TitledBorder(
                new LineBorder(new Color(220, 215, 235)),
                "Categories (host during ready check)"));
        TitledBorder tb2 = (TitledBorder) catPanel.getBorder();
        tb2.setTitleFont(TextFX.getSubtitleFont());
        tb2.setTitleColor(new Color(60, 60, 90));

        chkGeo = new JCheckBox("Geography");
        chkSci = new JCheckBox("Science");
        chkMath = new JCheckBox("Math");
        chkHist = new JCheckBox("History");

        JCheckBox[] boxes = {chkGeo, chkSci, chkMath, chkHist};
        for (JCheckBox box : boxes) {
            box.setOpaque(false);
            box.setSelected(true);
            TextFX.setSubtitleFont(box);
            box.addActionListener(e -> sendCategories());
            catPanel.add(box);
        }

        panel.add(catPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildUserPanel() {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new TitledBorder(
                new LineBorder(new Color(210, 210, 230)),
                "Users"));
        TitledBorder tb = (TitledBorder) panel.getBorder();
        tb.setTitleFont(TextFX.getSubtitleFont());
        tb.setTitleColor(new Color(60, 60, 90));

        userModel = new DefaultListModel<>();
        lstUsers = new JList<>(userModel);
        lstUsers.setBorder(new EmptyBorder(5, 5, 5, 5));
        lstUsers.setBackground(new Color(248, 246, 255));
        lstUsers.setSelectionBackground(new Color(210, 200, 245));
        TextFX.setSubtitleFont(lstUsers);

        // Custom cell renderer to gray out AWAY, tag SPECTATOR, etc.
        lstUsers.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                if (value instanceof User) {
                    User u = (User) value;
                    lbl.setText(u.toDisplayString());
                    if (u.isAway()) {
                        lbl.setForeground(new Color(140, 140, 160)); // gray-ish
                    } else if (u.isSpectator()) {
                        lbl.setForeground(new Color(80, 120, 180)); // blue-ish
                    } else {
                        lbl.setForeground(new Color(40, 40, 70));
                    }
                }
                TextFX.setSubtitleFont(lbl);
                return lbl;
            }
        });

        panel.add(new JScrollPane(lstUsers), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(290, 260));
        return panel;
    }

    private JPanel buildRightColumn() {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);
        col.setPreferredSize(new Dimension(300, 0));

        col.add(buildChatPanel());
        col.add(Box.createVerticalStrut(8));
        col.add(buildEventsPanel());

        return col;
    }

    private JPanel buildChatPanel() {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new TitledBorder(
                new LineBorder(new Color(210, 210, 230)),
                "Chat"));
        TitledBorder tb = (TitledBorder) panel.getBorder();
        tb.setTitleFont(TextFX.getSubtitleFont());
        tb.setTitleColor(new Color(60, 60, 90));

        txtChat = new JTextArea();
        txtChat.setEditable(false);
        txtChat.setLineWrap(true);
        txtChat.setWrapStyleWord(true);
        txtChat.setBackground(new Color(248, 246, 255));
        txtChat.setBorder(new EmptyBorder(5, 5, 5, 5));
        TextFX.setSubtitleFont(txtChat);

        panel.add(new JScrollPane(txtChat), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setOpaque(false);
        txtChatInput = new JTextField();
        TextFX.setSubtitleFont(txtChatInput);
        btnSendChat = new JButton("Send");
        styleSecondaryButton(btnSendChat);
        btnSendChat.addActionListener(e -> sendChat());

        inputPanel.add(txtChatInput, BorderLayout.CENTER);
        inputPanel.add(btnSendChat, BorderLayout.EAST);
        inputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildEventsPanel() {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new TitledBorder(
                new LineBorder(new Color(210, 210, 230)),
                "Game Events"));
        TitledBorder tb = (TitledBorder) panel.getBorder();
        tb.setTitleFont(TextFX.getSubtitleFont());
        tb.setTitleColor(new Color(60, 60, 90));

        txtEvents = new JTextArea();
        txtEvents.setEditable(false);
        txtEvents.setLineWrap(true);
        txtEvents.setWrapStyleWord(true);
        txtEvents.setBackground(new Color(248, 246, 255));
        txtEvents.setBorder(new EmptyBorder(5, 5, 5, 5));
        TextFX.setSubtitleFont(txtEvents);

        panel.add(new JScrollPane(txtEvents), BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(260, 220));
        return panel;
    }

    private JPanel buildGamePanel() {
        RoundedPanel panel = new RoundedPanel();
        panel.setLayout(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder(
                new LineBorder(new Color(210, 210, 230)),
                "Game"));
        TitledBorder tb = (TitledBorder) panel.getBorder();
        tb.setTitleFont(TextFX.getSubtitleFont());
        tb.setTitleColor(new Color(60, 60, 90));

        // Top bar
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        lblCategory = new JLabel("Category: -");
        TextFX.setSubtitleFont(lblCategory);
        lblTimer = new JLabel("Timer: -", SwingConstants.RIGHT);
        TextFX.setSubtitleFont(lblTimer);

        top.add(lblCategory, BorderLayout.WEST);
        top.add(lblTimer, BorderLayout.EAST);
        top.setBorder(new EmptyBorder(5, 10, 5, 10));
        panel.add(top, BorderLayout.NORTH);

        // Question
        lblQuestion = new JLabel("Question appears here.", SwingConstants.CENTER);
        lblQuestion.setBorder(new EmptyBorder(40, 20, 40, 20));
        lblQuestion.setForeground(new Color(70, 70, 100));
        TextFX.setSubtitleFont(lblQuestion);
        panel.add(lblQuestion, BorderLayout.CENTER);

        // Answers
        JPanel bottom = new JPanel(new GridLayout(2, 2, 10, 10));
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(10, 40, 20, 40));

        for (int i = 0; i < 4; i++) {
            int idx = i;
            JButton btn = new JButton("Answer " + (i + 1));
            styleAnswerButton(btn);
            btn.addActionListener(e -> answerClicked(idx));
            answerButtons[i] = btn;
            bottom.add(btn);
        }

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        TextFX.setSubtitleFont(lbl); // Visby CF
        return lbl;
    }

    private void stylePrimaryButton(JButton btn) {
        btn.setBackground(new Color(130, 110, 250));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(110, 90, 230), 1, true));
        TextFX.setSubtitleFont(btn);
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setBackground(new Color(240, 236, 255));
        btn.setForeground(new Color(70, 70, 110));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(200, 190, 240), 1, true));
        TextFX.setSubtitleFont(btn);
    }

    private void styleAnswerButton(JButton btn) {
        btn.setBackground(new Color(240, 236, 255));
        btn.setForeground(new Color(70, 70, 110));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(200, 190, 240), 1, true));
        TextFX.setSubtitleFont(btn);
    }

    // ===================== Networking =====================

    private void connectClicked(ActionEvent e) {
        if (connected) return;

        try {
            String host = txtHost.getText().trim();
            int port = Integer.parseInt(txtPort.getText().trim());
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            appendEvent("Connected to server " + host + ":" + port);
            lblConnectHint.setText("Connected as " + txtUser.getText().trim()
                    + " — Step 2: Click READY to join the next game.");

            ConnectionPayload cp = new ConnectionPayload();
            cp.setPayloadType(PayloadType.CLIENT_CONNECT);
            cp.setClientName(txtUser.getText().trim());
            send(cp);

            startReaderThread();
        } catch (Exception ex) {
            appendEvent("Connect failed: " + ex.getMessage());
        }
    }

    private void startReaderThread() {
        Thread t = new Thread(() -> {
            try {
                while (connected) {
                    Object obj = in.readObject();
                    if (obj instanceof Payload p) {
                        handlePayload(p);
                    }
                }
            } catch (Exception ex) {
                appendEvent("Disconnected from server.");
                connected = false;
            }
        });
        t.start();
    }

    private void handlePayload(Payload p) {
        if (p == null || p.getPayloadType() == null) return;

        switch (p.getPayloadType()) {
            case CLIENT_ID -> {
                clientId = p.getClientId();
                appendEvent("Your ID: " + clientId);
            }
            case MESSAGE -> handleMessagePayload(p);
            case TIMER -> lblTimer.setText("Timer: " + p.getMessage());
            case POINTS_UPDATE -> appendEvent(p.getMessage());
            case QUESTION -> handleQA((QAPayload) p);
            case USER_LIST -> handleUserList((UserListPayload) p);
            default -> {
                // ignore other types for now
            }
        }
    }

    // Split MESSAGE payloads into Chat vs Game Events
    private void handleMessagePayload(Payload p) {
        String msg = p.getMessage();
        if (msg == null) return;

        if (msg.startsWith("[CHAT] ")) {
            appendChat(msg.substring(7));
        } else {
            appendEvent(msg);

            // If game over, hint user to click READY again
            if (msg.contains("GAME OVER")) {
                lblQuestion.setText("Game over. Click READY to play again.");
                for (JButton btn : answerButtons) {
                    btn.setEnabled(false);
                    btn.setBackground(new Color(240, 236, 255));
                }
            }
        }
    }

    private void handleQA(QAPayload q) {
        if (q == null) return;

        lockedThisRound = false;
        lblCategory.setText("Category: " + q.getCategory());
        lblQuestion.setText(q.getQuestionText());

        ArrayList<String> answers = q.getAnswers();
        for (int i = 0; i < 4; i++) {
            if (answers != null && i < answers.size()) {
                answerButtons[i].setText(answers.get(i));
                answerButtons[i].setEnabled(true);
                answerButtons[i].setBackground(new Color(240, 236, 255));
            } else {
                answerButtons[i].setText("-");
                answerButtons[i].setEnabled(false);
            }
        }
    }

    private void handleUserList(UserListPayload up) {
        if (up == null) return;

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

    // ===================== Actions =====================

    private void answerClicked(int index) {
        if (!connected) {
            appendEvent("Not connected.");
            return;
        }
        if (lockedThisRound || isSpectator || isAway) {
            return;
        }

        // Send numeric index (0–3) as per server logic
        sendCommand("/answer " + index);
        lockedThisRound = true;

        for (JButton btn : answerButtons) {
            btn.setEnabled(false);
        }
        answerButtons[index].setBackground(new Color(170, 230, 190));
    }

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

    private void toggleAway() {
        isAway = chkAway.isSelected();
        if (isAway) sendCommand("/away");
        else sendCommand("/back");
    }

    private void toggleSpectator() {
        boolean newState = chkSpectator.isSelected();
        if (newState && !isSpectator) {
            isSpectator = true;
            sendCommand("/spectate");
        }
    }

    private void showAddQDialog() {
        if (!connected) {
            appendEvent("Not connected.");
            return;
        }

        String cat = JOptionPane.showInputDialog(frame, "Category (geography, science, math, history, movies, sports):");
        if (cat == null || cat.trim().isEmpty()) return;

        String q = JOptionPane.showInputDialog(frame, "Question:");
        if (q == null || q.trim().isEmpty()) return;

        String a1 = JOptionPane.showInputDialog(frame, "Answer A:");
        String a2 = JOptionPane.showInputDialog(frame, "Answer B:");
        String a3 = JOptionPane.showInputDialog(frame, "Answer C:");
        String a4 = JOptionPane.showInputDialog(frame, "Answer D:");
        String correct = JOptionPane.showInputDialog(frame, "Correct index (0=A, 1=B, 2=C, 3=D):");

        if (a1 == null || a2 == null || a3 == null || a4 == null || correct == null) return;

        String line = cat + "|" + q + "|" + a1 + "|" + a2 + "|" + a3 + "|" + a4 + "|" + correct;
        sendCommand("/addq " + line);
    }

    private void sendChat() {
        if (!connected) {
            appendEvent("Not connected.");
            return;
        }
        if (isSpectator) {
            appendEvent("You are a spectator and cannot send chat messages.");
            return;
        }

        String text = txtChatInput.getText().trim();
        if (text.isEmpty()) return;

        // Plain text (no slash) → treated as chat on server side
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setClientId(clientId);
        p.setMessage(text);
        send(p);

        txtChatInput.setText("");
    }

    private void sendCommand(String text) {
        if (!connected) return;

        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setClientId(clientId);
        p.setMessage(text);
        send(p);
    }

    private void send(Payload p) {
        try {
            out.writeObject(p);
            out.flush();
        } catch (Exception ex) {
            appendEvent("Send failed: " + ex.getMessage());
        }
    }

    private void appendEvent(String msg) {
        txtEvents.append(msg + "\n");
        txtEvents.setCaretPosition(txtEvents.getDocument().getLength());
    }

    private void appendChat(String msg) {
        txtChat.append(msg + "\n");
        txtChat.setCaretPosition(txtChat.getDocument().getLength());
    }

    // ===================== Helper inner class =====================

    // Rounded card panel (simple Swing custom painting)
    static class RoundedPanel extends JPanel {
        public RoundedPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 20;
            Color fill = new Color(252, 250, 255);
            Color border = new Color(220, 215, 240);

            // Drop shadow
            g2.setColor(new Color(220, 215, 240, 120));
            g2.fillRoundRect(4, 6, getWidth() - 4, getHeight() - 4, arc, arc);

            // Card
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 6, arc, arc);

            // Border
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 6, getHeight() - 6, arc, arc);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}

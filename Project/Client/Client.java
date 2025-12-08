// UCID: nhd5
// Date: December 8, 2025
// Description: TriviaGuessGame Client – Swing UI + networking (host-controlled start)
// References:
//  - https://www.w3schools.com/java/java_swing.asp
//  - https://www.w3schools.com/java/java_arraylist.asp

package Client;

import Common.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

public class Client {

    // ----- Networking -----
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = false;
    private long clientId = -1;
    private boolean lockedThisRound = false;
    private boolean isAway = false;
    private boolean isSpectator = false;

    // ----- Fonts (installed locally on your Mac) -----
    // If loading fails, Swing will just use the default font.
    private static final String FONT_BEHIND_90S =
            "/Users/nilka/Library/Fonts/Behind The Nineties Regular.ttf";
    private static final String FONT_VISBY =
            "/Users/nilka/Library/Fonts/VisbyCF-Regular.otf";

    private Font titleFont;
    private Font subtitleFont;
    private Font bodyFont;

    // ----- UI -----
    private JFrame frame;

    private JTextField txtUser;
    private JTextField txtHost;
    private JTextField txtPort;
    private JButton btnConnect;

    private JButton btnReady;
    private JCheckBox chkAway;
    private JCheckBox chkSpectator;
    private JButton btnAddQ;

    private DefaultListModel<String> userModel;
    private JList<String> lstUsers;

    private JTextArea txtEvents;

    private JLabel lblCategory;
    private JLabel lblQuestion;
    private JLabel lblTimer;

    private JButton[] answerButtons = new JButton[4];

    // Category toggles
    private JCheckBox chkGeo;
    private JCheckBox chkSci;
    private JCheckBox chkMath;
    private JCheckBox chkHist;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }

    public Client() {
        loadFonts();
        buildUI();
    }

    // ============================= Fonts =============================

    private void loadFonts() {
        // Default fallbacks
        titleFont = new JLabel().getFont().deriveFont(Font.BOLD, 26f);
        subtitleFont = new JLabel().getFont().deriveFont(Font.BOLD, 14f);
        bodyFont = new JLabel().getFont().deriveFont(Font.PLAIN, 13f);

        // Try to load custom fonts from your local Fonts folder
        titleFont = loadCustomFont(FONT_BEHIND_90S, 28f, titleFont);
        subtitleFont = loadCustomFont(FONT_VISBY, 14f, subtitleFont);
        bodyFont = loadCustomFont(FONT_VISBY, 13f, bodyFont);
    }

    private Font loadCustomFont(String path, float size, Font fallback) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                return fallback;
            }
            FileInputStream fis = new FileInputStream(file);
            Font f = Font.createFont(Font.TRUETYPE_FONT, fis);
            fis.close();
            return f.deriveFont(size);
        } catch (Exception e) {
            return fallback;
        }
    }

    // ============================= UI =============================

    private void buildUI() {
        frame = new JFrame("TriviaGuessGame – nhd5");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 680);
        frame.setLocationRelativeTo(null);

        // So the light purple shows through
        frame.getContentPane().setBackground(new Color(245, 240, 255));
        frame.setLayout(new BorderLayout(10, 10));
        ((JComponent) frame.getContentPane()).setBorder(new EmptyBorder(8, 8, 8, 8));

        // Top title
        JLabel title = new JLabel("Trivia Guess Game", SwingConstants.CENTER);
        title.setFont(titleFont);
        title.setForeground(new Color(60, 50, 100));
        frame.add(title, BorderLayout.NORTH);

        // Center layout: left options/users + main game + events
        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        frame.add(center, BorderLayout.CENTER);

        // Left side (connection + options + users)
        center.add(buildLeftColumn(), BorderLayout.WEST);

        // Game panel in the middle
        center.add(buildGamePanel(), BorderLayout.CENTER);

        // Events on the right
        center.add(buildEventsPanel(), BorderLayout.EAST);

        frame.setVisible(true);
    }

    private JPanel createCardPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 210, 240), 2, true),
                new EmptyBorder(6, 6, 6, 6)
        ));
        panel.setBackground(new Color(249, 246, 255));

        if (title != null && !title.isEmpty()) {
            TitledBorder tb = BorderFactory.createTitledBorder(title);
            tb.setTitleFont(subtitleFont);
            tb.setTitleColor(new Color(90, 70, 120));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(220, 210, 240), 2, true),
                    new EmptyBorder(4, 4, 4, 4)
            ));
            panel.setBorder(tb);
        }

        return panel;
    }

    private JPanel buildLeftColumn() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BorderLayout(0, 8));
        column.setPreferredSize(new Dimension(360, 0));

        column.add(buildConnectionPanel(), BorderLayout.NORTH);
        column.add(buildOptionsAndUsersPanel(), BorderLayout.CENTER);

        return column;
    }

    private JPanel buildConnectionPanel() {
        JPanel outer = createCardPanel("Connection");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        outer.add(panel, BorderLayout.CENTER);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 4, 3, 4);
        c.gridy = 0;

        // Row: Username / Host / Port / Connect
        c.gridx = 0;
        panel.add(createLabel("Username:"), c);
        c.gridx = 1;
        txtUser = new JTextField("Player", 9);
        txtUser.setFont(bodyFont);
        panel.add(txtUser, c);

        c.gridx = 2;
        panel.add(createLabel("Host:"), c);
        c.gridx = 3;
        txtHost = new JTextField("localhost", 9);
        txtHost.setFont(bodyFont);
        panel.add(txtHost, c);

        c.gridx = 0;
        c.gridy = 1;
        panel.add(createLabel("Port:"), c);
        c.gridx = 1;
        txtPort = new JTextField("3000", 5);
        txtPort.setFont(bodyFont);
        panel.add(txtPort, c);

        c.gridx = 3;
        btnConnect = new JButton("Connect");
        stylePrimaryButton(btnConnect);
        btnConnect.addActionListener(this::connectClicked);
        panel.add(btnConnect, c);

        return outer;
    }

    private JPanel buildOptionsAndUsersPanel() {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BorderLayout(0, 8));

        wrapper.add(buildReadyPanel(), BorderLayout.NORTH);
        wrapper.add(buildUserPanel(), BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel buildReadyPanel() {
        JPanel outer = createCardPanel("Options");
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        outer.add(panel, BorderLayout.CENTER);

        // Row 1: Ready + Add Question
        JPanel r1 = new JPanel();
        r1.setOpaque(false);
        btnReady = new JButton("READY");
        stylePrimaryButton(btnReady);
        btnReady.addActionListener(e -> sendCommand("/ready"));
        r1.add(btnReady);

        btnAddQ = new JButton("Add Question");
        styleSecondaryButton(btnAddQ);
        btnAddQ.addActionListener(e -> showAddQDialog());
        r1.add(btnAddQ);
        panel.add(r1);

        // Row 2: Away / Spectator
        JPanel r2 = new JPanel();
        r2.setOpaque(false);
        chkAway = new JCheckBox("Away");
        chkSpectator = new JCheckBox("Spectator");
        chkAway.setFont(bodyFont);
        chkSpectator.setFont(bodyFont);
        chkAway.setOpaque(false);
        chkSpectator.setOpaque(false);

        chkAway.addActionListener(e -> toggleAway());
        chkSpectator.addActionListener(e -> toggleSpectator());

        r2.add(chkAway);
        r2.add(chkSpectator);
        panel.add(r2);

        // Row 3: Categories
        JPanel r3 = new JPanel();
        r3.setOpaque(false);
        r3.setBorder(BorderFactory.createTitledBorder("Categories"));

        chkGeo = new JCheckBox("Geo", true);
        chkSci = new JCheckBox("Science", true);
        chkMath = new JCheckBox("Math", true);
        chkHist = new JCheckBox("History", true);

        JCheckBox[] cbs = {chkGeo, chkSci, chkMath, chkHist};
        for (JCheckBox cb : cbs) {
            cb.setOpaque(false);
            cb.setFont(bodyFont);
            cb.addActionListener(e -> sendCategories());
            r3.add(cb);
        }

        panel.add(r3);

        return outer;
    }

    private JPanel buildUserPanel() {
        JPanel outer = createCardPanel("Users");
        outer.setPreferredSize(new Dimension(0, 220));

        userModel = new DefaultListModel<>();
        lstUsers = new JList<>(userModel);
        lstUsers.setFont(bodyFont);
        lstUsers.setBackground(new Color(252, 250, 255));

        outer.add(new JScrollPane(lstUsers), BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildEventsPanel() {
        JPanel outer = createCardPanel("Game Events");
        outer.setPreferredSize(new Dimension(270, 0));

        txtEvents = new JTextArea();
        txtEvents.setEditable(false);
        txtEvents.setLineWrap(true);
        txtEvents.setWrapStyleWord(true);
        txtEvents.setFont(bodyFont);
        txtEvents.setBackground(new Color(252, 250, 255));

        outer.add(new JScrollPane(txtEvents), BorderLayout.CENTER);
        return outer;
    }

    private JPanel buildGamePanel() {
        JPanel outer = createCardPanel("Game");

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setOpaque(false);
        outer.add(panel, BorderLayout.CENTER);

        // Top bar: Category + Timer
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        lblCategory = new JLabel("Category: -");
        lblCategory.setFont(subtitleFont);
        lblCategory.setForeground(new Color(60, 50, 100));
        top.add(lblCategory, BorderLayout.WEST);

        lblTimer = new JLabel("Timer: -");
        lblTimer.setFont(subtitleFont);
        lblTimer.setForeground(new Color(180, 80, 80));
        lblTimer.setHorizontalAlignment(SwingConstants.RIGHT);
        top.add(lblTimer, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);

        // Question label
        lblQuestion = new JLabel("Question appears here.", SwingConstants.CENTER);
        lblQuestion.setFont(bodyFont.deriveFont(16f));
        lblQuestion.setForeground(new Color(70, 60, 110));
        lblQuestion.setBorder(new EmptyBorder(30, 10, 20, 10));
        panel.add(lblQuestion, BorderLayout.CENTER);

        // Answer buttons
        JPanel bottom = new JPanel(new GridLayout(2, 2, 10, 10));
        bottom.setOpaque(false);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            JButton btn = new JButton("Answer " + (i + 1));
            styleAnswerButton(btn);
            btn.setEnabled(false);
            btn.addActionListener(e -> answerClicked(idx));
            answerButtons[i] = btn;
            bottom.add(btn);
        }

        panel.add(bottom, BorderLayout.SOUTH);

        return outer;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(bodyFont);
        return lbl;
    }

    private void stylePrimaryButton(JButton btn) {
        btn.setFont(bodyFont);
        btn.setBackground(new Color(120, 100, 220));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(90, 75, 180), 1, true));
    }

    private void styleSecondaryButton(JButton btn) {
        btn.setFont(bodyFont);
        btn.setBackground(new Color(230, 225, 250));
        btn.setForeground(new Color(60, 50, 100));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(200, 190, 240), 1, true));
    }

    private void styleAnswerButton(JButton btn) {
        btn.setFont(bodyFont);
        btn.setBackground(new Color(240, 235, 255));
        btn.setForeground(new Color(60, 50, 100));
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(new Color(210, 200, 240), 1, true));
    }

    // ============================= Networking =============================

    private void connectClicked(ActionEvent e) {
        if (connected) {
            return;
        }

        try {
            String host = txtHost.getText().trim();
            int port = Integer.parseInt(txtPort.getText().trim());

            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            append("Connected to server.");

            ConnectionPayload cp = new ConnectionPayload();
            cp.setPayloadType(PayloadType.CLIENT_CONNECT);
            cp.setClientName(txtUser.getText().trim());
            send(cp);

            startReaderThread();

        } catch (Exception ex) {
            append("Connect failed: " + ex.getMessage());
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
                append("Disconnected from server.");
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void handlePayload(Payload p) {
        // Support BOTH old and new enum names
        switch (p.getPayloadType()) {
            case CLIENT_ID -> {
                clientId = p.getClientId();
                append("Your ID: " + clientId);
            }
            case MESSAGE -> append(p.getMessage());
            case TIMER -> lblTimer.setText("Timer: " + p.getMessage());
            case POINTS_UPDATE -> append(p.getMessage());

            case QA_UPDATE, QUESTION -> handleQA((QAPayload) p);

            case USERLIST_UPDATE, USER_LIST -> handleUserList((UserListPayload) p);

            default -> {
                // ignore other payloads
            }
        }
    }

    private void handleQA(QAPayload q) {
        lockedThisRound = false;

        lblCategory.setText("Category: " + q.getCategory());
        lblQuestion.setText("<html><div style='text-align:center;'>" +
                q.getQuestionText() + "</div></html>");

        ArrayList<String> answers = q.getAnswers();

        for (int i = 0; i < 4; i++) {
            JButton btn = answerButtons[i];
            btn.setBackground(new Color(240, 235, 255));

            if (answers != null && i < answers.size()) {
                btn.setText(answers.get(i));
                btn.setEnabled(true);
            } else {
                btn.setText("—");
                btn.setEnabled(false);
            }
        }
    }

    private void handleUserList(UserListPayload up) {
        userModel.clear();

        ArrayList<Long> ids = up.getClientIds();
        ArrayList<String> names = up.getDisplayNames();
        ArrayList<Integer> ptsList = up.getPoints();
        ArrayList<Boolean> locked = up.getLockedIn();
        ArrayList<Boolean> away = up.getAway();
        ArrayList<Boolean> spec = up.getSpectator();

        for (int i = 0; i < ids.size(); i++) {
            String text = names.get(i)
                    + " | pts:" + ptsList.get(i)
                    + (locked.get(i) ? " [LOCKED]" : "")
                    + (away.get(i) ? " [AWAY]" : "")
                    + (spec.get(i) ? " [SPECTATOR]" : "");
            userModel.addElement(text);
        }
    }

    // ============================= Actions =============================

    private void answerClicked(int index) {
        if (lockedThisRound || isSpectator || isAway) {
            return;
        }

        sendCommand("/answer " + index);
        lockedThisRound = true;

        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setEnabled(false);
            if (i == index) {
                answerButtons[i].setBackground(new Color(190, 235, 200)); // light green
            }
        }
    }

    private void sendCategories() {
        String cats = "";

        if (chkGeo.isSelected()) {
            cats += "geography,";
        }
        if (chkSci.isSelected()) {
            cats += "science,";
        }
        if (chkMath.isSelected()) {
            cats += "math,";
        }
        if (chkHist.isSelected()) {
            cats += "history,";
        }

        if (!cats.isEmpty()) {
            cats = cats.substring(0, cats.length() - 1); // remove last comma
            sendCommand("/categories " + cats);
        }
    }

    private void toggleAway() {
        isAway = chkAway.isSelected();
        if (isAway) {
            sendCommand("/away");
        } else {
            sendCommand("/back");
        }
    }

    private void toggleSpectator() {
        isSpectator = chkSpectator.isSelected();
        if (isSpectator) {
            sendCommand("/spectate");
        } else {
            // many servers don't support "unspectate", but we can send a message anyway
            sendCommand("/play");
        }
    }

    private void showAddQDialog() {
        if (!connected) {
            append("Not connected.");
            return;
        }

        String cat = JOptionPane.showInputDialog(frame, "Category:");
        if (cat == null) return;

        String q = JOptionPane.showInputDialog(frame, "Question:");
        if (q == null) return;

        String a1 = JOptionPane.showInputDialog(frame, "Answer A:");
        if (a1 == null) return;
        String a2 = JOptionPane.showInputDialog(frame, "Answer B:");
        if (a2 == null) return;
        String a3 = JOptionPane.showInputDialog(frame, "Answer C:");
        if (a3 == null) return;
        String a4 = JOptionPane.showInputDialog(frame, "Answer D:");
        if (a4 == null) return;

        String correct = JOptionPane.showInputDialog(frame,
                "Correct index (0-3, where 0 = Answer A):");
        if (correct == null) return;

        // The server expects the whole line after /addq
        String line = cat + "|" + q + "|" + a1 + "|" + a2 + "|" + a3 + "|" + a4 + "|" + correct;
        sendCommand("/addq " + line);
    }

    private void sendCommand(String text) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setClientId(clientId);
        p.setMessage(text);
        send(p);
    }

    private void send(Payload p) {
        try {
            if (out != null) {
                out.writeObject(p);
                out.flush();
            }
        } catch (Exception ex) {
            append("Send failed: " + ex.getMessage());
        }
    }

    private void append(String msg) {
        txtEvents.append(msg + "\n");
        txtEvents.setCaretPosition(txtEvents.getDocument().getLength());
    }
}

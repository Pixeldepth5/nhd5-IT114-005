// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame Client – Swing UI with connection, ready check, user list, game events, and game area
// Reference:
//  - https://www.w3schools.com/java/java_swing.asp (basic Swing components)
//  - https://www.w3schools.com/java/java_arraylist.asp (ArrayList usage)
//  - https://www.w3schools.com/java/java_threads.asp (basic threads)
package Client;

import Common.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class Client {

    // Network
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private boolean connected = false;
    private boolean lockedThisRound = false;
    private boolean isSpectator = false;
    private boolean isAway = false;

    // UI components
    private JFrame frame;
    private JTextField txtUserName;
    private JTextField txtHost;
    private JTextField txtPort;
    private JButton btnConnect;

    private JButton btnReady;
    private JCheckBox chkAway;
    private JCheckBox chkSpectator;
    private JButton btnAddQuestion;

    private JList<String> lstUsers;
    private DefaultListModel<String> userListModel;

    private JTextArea txtEvents;

    private JLabel lblCategory;
    private JLabel lblQuestion;
    private JButton[] answerButtons = new JButton[4];
    private JLabel lblTimer;

    private JCheckBox chkMusic;
    private JCheckBox chkSports;
    private JCheckBox chkArts;
    private JCheckBox chkMovies;
    private JCheckBox chkHistory;
    private JCheckBox chkGeography;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }

    public Client() {
        buildUI();
    }

    // ====================== UI ======================

    private void buildUI() {
        frame = new JFrame("TriviaGuessGame - nhd5");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.add(buildConnectionPanel(), BorderLayout.NORTH);
        frame.add(buildMainCenterPanel(), BorderLayout.CENTER);
        frame.add(buildEventsPanel(), BorderLayout.EAST);

        frame.setSize(1100, 650);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Top: connection
    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Connection"));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.gridy = 0;

        c.gridx = 0;
        panel.add(new JLabel("Username:"), c);
        c.gridx = 1;
        txtUserName = new JTextField("Player", 10);
        panel.add(txtUserName, c);

        c.gridx = 2;
        panel.add(new JLabel("Host:"), c);
        c.gridx = 3;
        txtHost = new JTextField("localhost", 10);
        panel.add(txtHost, c);

        c.gridx = 4;
        panel.add(new JLabel("Port:"), c);
        c.gridx = 5;
        txtPort = new JTextField("3000", 5);
        panel.add(txtPort, c);

        c.gridx = 6;
        btnConnect = new JButton("Connect");
        btnConnect.addActionListener(this::onConnectClicked);
        panel.add(btnConnect, c);

        return panel;
    }

    // Center = Left (user list / ready / categories), Right (game area)
    private JPanel buildMainCenterPanel() {
        JPanel wrapper = new JPanel(new GridLayout(1, 2));

        // LEFT SIDE
        JPanel left = new JPanel();
        left.setLayout(new BorderLayout());

        left.add(buildReadyAndOptionsPanel(), BorderLayout.NORTH);
        left.add(buildUserListPanel(), BorderLayout.CENTER);

        // RIGHT SIDE
        JPanel right = buildGameAreaPanel();

        wrapper.add(left);
        wrapper.add(right);
        return wrapper;
    }

    private JPanel buildReadyAndOptionsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.setBorder(new TitledBorder("Ready / Options"));

        // Row 1: Ready + Add Question
        JPanel row1 = new JPanel();
        btnReady = new JButton("Mark READY");
        btnReady.addActionListener(e -> sendChatCommand("/ready"));
        row1.add(btnReady);

        btnAddQuestion = new JButton("Add Question");
        btnAddQuestion.addActionListener(e -> showAddQuestionDialog());
        row1.add(btnAddQuestion);
        panel.add(row1);

        // Row 2: Away / Spectator
        JPanel row2 = new JPanel();
        chkAway = new JCheckBox("Away");
        chkAway.addActionListener(e -> toggleAway());
        row2.add(chkAway);

        chkSpectator = new JCheckBox("Spectator");
        chkSpectator.addActionListener(e -> toggleSpectator());
        row2.add(chkSpectator);
        panel.add(row2);

        // Row 3: Categories for host (still sends command even if not host; server validates)
        JPanel row3 = new JPanel();
        row3.setBorder(new TitledBorder("Categories (host during Ready Check)"));
        chkMusic = new JCheckBox("Music", true);
        chkSports = new JCheckBox("Sports", true);
        chkArts = new JCheckBox("Arts", true);
        chkMovies = new JCheckBox("Movies", true);
        chkHistory = new JCheckBox("History", true);
        chkGeography = new JCheckBox("Geography", true);

        Action catAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendCategorySelection();
            }
        };
        chkMusic.addActionListener(catAction);
        chkSports.addActionListener(catAction);
        chkArts.addActionListener(catAction);
        chkMovies.addActionListener(catAction);
        chkHistory.addActionListener(catAction);
        chkGeography.addActionListener(catAction);

        row3.add(chkMusic);
        row3.add(chkSports);
        row3.add(chkArts);
        row3.add(chkMovies);
        row3.add(chkHistory);
        row3.add(chkGeography);

        panel.add(row3);

        return panel;
    }

    private JPanel buildUserListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("User List"));

        userListModel = new DefaultListModel<>();
        lstUsers = new JList<>(userListModel);
        panel.add(new JScrollPane(lstUsers), BorderLayout.CENTER);

        JLabel lblHint = new JLabel("Shows username#id, points, and lock/away/spectator states.");
        lblHint.setFont(lblHint.getFont().deriveFont(Font.ITALIC, 10f));
        panel.add(lblHint, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildEventsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Game Events"));

        txtEvents = new JTextArea();
        txtEvents.setEditable(false);
        txtEvents.setLineWrap(true);
        txtEvents.setWrapStyleWord(true);

        panel.add(new JScrollPane(txtEvents), BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildGameAreaPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(new TitledBorder("Game Area"));

        // Top: category + timer
        JPanel top = new JPanel(new BorderLayout());
        lblCategory = new JLabel("Category: -");
        lblCategory.setFont(lblCategory.getFont().deriveFont(Font.BOLD, 16f));
        top.add(lblCategory, BorderLayout.WEST);

        lblTimer = new JLabel("Timer: -");
        lblTimer.setHorizontalAlignment(SwingConstants.RIGHT);
        top.add(lblTimer, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);

        // Center: question text
        lblQuestion = new JLabel("Question will appear here.", SwingConstants.CENTER);
        lblQuestion.setFont(lblQuestion.getFont().deriveFont(Font.PLAIN, 16f));
        lblQuestion.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(lblQuestion, BorderLayout.CENTER);

        // Bottom: answer buttons
        JPanel bottom = new JPanel(new GridLayout(2, 2, 8, 8));
        String[] labels = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            int index = i;
            JButton btn = new JButton(labels[i]);
            btn.addActionListener(e -> onAnswerClicked(index));
            answerButtons[i] = btn;
            bottom.add(btn);
        }
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ====================== Button Handlers ======================

    private void onConnectClicked(ActionEvent e) {
        if (connected) {
            appendEvent("Already connected.");
            return;
        }
        String name = txtUserName.getText().trim();
        String host = txtHost.getText().trim();
        int port = Integer.parseInt(txtPort.getText().trim());

        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            appendEvent("Connected to server.");

            ConnectionPayload cp = new ConnectionPayload();
            cp.setPayloadType(PayloadType.CLIENT_CONNECT);
            cp.setClientName(name);
            sendPayload(cp);

            startReaderThread();
        } catch (Exception ex) {
            appendEvent("Failed to connect: " + ex.getMessage());
        }
    }

    private void onAnswerClicked(int index) {
        if (!connected) return;
        if (lockedThisRound) return;
        if (isSpectator || isAway) return;

        String[] letters = {"A", "B", "C", "D"};
        String choice = letters[index];
        sendChatCommand("/answer " + choice);
        lockedThisRound = true;
        setAnswerButtonsEnabled(false);
        answerButtons[index].setBackground(Color.GREEN);
    }

    private void toggleAway() {
        if (!connected) return;
        isAway = chkAway.isSelected();
        if (isAway) {
            sendChatCommand("/away");
        } else {
            sendChatCommand("/back");
        }
    }

    private void toggleSpectator() {
        if (!connected) return;
        boolean newValue = chkSpectator.isSelected();
        if (newValue && !isSpectator) {
            isSpectator = true;
            sendChatCommand("/spectate");
            setAnswerButtonsEnabled(false);
        }
    }

    private void sendCategorySelection() {
        if (!connected) return;
        // Build comma-separated list
        ArrayList<String> cats = new ArrayList<>();
        if (chkMusic.isSelected()) cats.add("music");
        if (chkSports.isSelected()) cats.add("sports");
        if (chkArts.isSelected()) cats.add("arts");
        if (chkMovies.isSelected()) cats.add("movies");
        if (chkHistory.isSelected()) cats.add("history");
        if (chkGeography.isSelected()) cats.add("geography");

        if (cats.isEmpty()) {
            appendEvent("At least one category should be selected.");
            return;
        }
        String joined = String.join(",", cats);
        sendChatCommand("/categories " + joined);
    }

    private void showAddQuestionDialog() {
        if (!connected) {
            appendEvent("Connect first.");
            return;
        }

        String category = JOptionPane.showInputDialog(frame,
                "Enter category (music/sports/arts/movies/history/geography):",
                "Add Question", JOptionPane.QUESTION_MESSAGE);
        if (category == null || category.trim().isEmpty()) return;

        String question = JOptionPane.showInputDialog(frame,
                "Enter question text:", "Add Question", JOptionPane.QUESTION_MESSAGE);
        if (question == null || question.trim().isEmpty()) return;

        String a1 = JOptionPane.showInputDialog(frame, "Answer A:", "Add Question", JOptionPane.QUESTION_MESSAGE);
        String a2 = JOptionPane.showInputDialog(frame, "Answer B:", "Add Question", JOptionPane.QUESTION_MESSAGE);
        String a3 = JOptionPane.showInputDialog(frame, "Answer C (optional, can leave blank):",
                "Add Question", JOptionPane.QUESTION_MESSAGE);
        String a4 = JOptionPane.showInputDialog(frame, "Answer D (optional, can leave blank):",
                "Add Question", JOptionPane.QUESTION_MESSAGE);

        String correct = JOptionPane.showInputDialog(frame,
                "Correct answer index (0 = A, 1 = B, 2 = C, 3 = D):",
                "Add Question", JOptionPane.QUESTION_MESSAGE);
        if (correct == null || correct.trim().isEmpty()) return;

        // Build /addq line like server expects
        StringBuilder sb = new StringBuilder();
        sb.append(category.trim()).append("|")
                .append(question.trim()).append("|")
                .append(a1 == null ? "" : a1.trim()).append("|")
                .append(a2 == null ? "" : a2.trim());
        if (a3 != null && !a3.trim().isEmpty()) {
            sb.append("|").append(a3.trim());
        }
        if (a4 != null && !a4.trim().isEmpty()) {
            sb.append("|").append(a4.trim());
        }
        sb.append("|").append(correct.trim());

        sendChatCommand("/addq " + sb.toString());
    }

    // ====================== Networking ======================

    private void startReaderThread() {
        Thread t = new Thread(() -> {
            try {
                while (connected) {
                    Object obj = in.readObject();
                    if (obj == null) break;

                    if (obj instanceof PointsPayload) {
                        handlePointsPayload((PointsPayload) obj);
                    } else if (obj instanceof QAPayload) {
                        handleQAPayload((QAPayload) obj);
                    } else if (obj instanceof UserListPayload) {
                        handleUserListPayload((UserListPayload) obj);
                    } else if (obj instanceof Payload) {
                        handleBasePayload((Payload) obj);
                    }
                }
            } catch (Exception ex) {
                appendEvent("Disconnected from server.");
            } finally {
                connected = false;
            }
        });
        t.start();
    }

    private void handleBasePayload(Payload p) {
        if (p.getPayloadType() == PayloadType.CLIENT_ID) {
            clientId = p.getClientId();
            appendEvent("Your client id is " + clientId);
            return;
        }

        if (p.getPayloadType() == PayloadType.MESSAGE) {
            appendEvent(p.getMessage());
            return;
        }

        if (p.getPayloadType() == PayloadType.TIMER) {
            lblTimer.setText("Timer: " + p.getMessage() + "s");
        }
    }

    private void handlePointsPayload(PointsPayload p) {
        // Just show the message – the user list will handle ordering/values
        appendEvent(p.getMessage());
    }

    private void handleQAPayload(QAPayload qp) {
        lockedThisRound = false;
        setAnswerButtonsEnabled(true);
        resetAnswerButtonColors();

        lblCategory.setText("Category: " + qp.getCategory());
        lblQuestion.setText("<html><body style='text-align:center;'>" +
                qp.getQuestionText() + "</body></html>");

        ArrayList<String> answers = qp.getAnswers();
        for (int i = 0; i < 4; i++) {
            if (i < answers.size()) {
                answerButtons[i].setText(answers.get(i));
                answerButtons[i].setEnabled(true);
            } else {
                answerButtons[i].setText("-");
                answerButtons[i].setEnabled(false);
            }
        }
    }

    private void handleUserListPayload(UserListPayload up) {
        userListModel.clear();
        ArrayList<Long> ids = up.getClientIds();
        ArrayList<String> names = up.getDisplayNames();
        ArrayList<Integer> pts = up.getPoints();
        ArrayList<Boolean> locked = up.getLockedIn();
        ArrayList<Boolean> awayList = up.getAway();
        ArrayList<Boolean> spectList = up.getSpectator();

        for (int i = 0; i < ids.size(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(names.get(i)).append(" | pts: ").append(pts.get(i));

            if (Boolean.TRUE.equals(locked.get(i))) {
                sb.append(" [LOCKED]");
            }
            if (Boolean.TRUE.equals(awayList.get(i))) {
                sb.append(" [AWAY]");
            }
            if (Boolean.TRUE.equals(spectList.get(i))) {
                sb.append(" [SPECTATOR]");
            }

            userListModel.addElement(sb.toString());
        }
    }

    private void sendPayload(Payload p) {
        try {
            out.writeObject(p);
            out.flush();
        } catch (Exception ex) {
            appendEvent("Failed to send payload: " + ex.getMessage());
        }
    }

    private void sendChatCommand(String commandText) {
        if (!connected) return;
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setClientId(clientId);
        p.setMessage(commandText);
        sendPayload(p);
    }

    // ====================== Helpers ======================

    private void appendEvent(String msg) {
        txtEvents.append(msg + "\n");
        txtEvents.setCaretPosition(txtEvents.getDocument().getLength());
    }

    private void setAnswerButtonsEnabled(boolean enabled) {
        for (JButton btn : answerButtons) {
            btn.setEnabled(enabled);
        }
    }

    private void resetAnswerButtonColors() {
        for (JButton btn : answerButtons) {
            btn.setBackground(null);
        }
    }
}

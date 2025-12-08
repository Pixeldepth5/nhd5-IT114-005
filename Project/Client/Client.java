// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame Client – Full Swing UI and networking

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

    // Networking
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = false;
    private long clientId = -1;
    private boolean lockedThisRound = false;
    private boolean isAway = false;
    private boolean isSpectator = false;

    // UI
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
        buildUI();
    }

    // ============================= UI =============================

    private void buildUI() {
        frame = new JFrame("TriviaGuessGame - nhd5");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 650);
        frame.setLayout(new BorderLayout());

        frame.add(buildConnectionPanel(), BorderLayout.NORTH);
        frame.add(buildCenterPanel(), BorderLayout.CENTER);
        frame.add(buildEventsPanel(), BorderLayout.EAST);

        frame.setVisible(true);
    }

    private JPanel buildConnectionPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new TitledBorder("Connection"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);

        c.gridx = 0; panel.add(new JLabel("Username:"), c);
        c.gridx = 1; txtUser = new JTextField("Player", 10); panel.add(txtUser, c);

        c.gridx = 2; panel.add(new JLabel("Host:"), c);
        c.gridx = 3; txtHost = new JTextField("localhost", 10); panel.add(txtHost, c);

        c.gridx = 4; panel.add(new JLabel("Port:"), c);
        c.gridx = 5; txtPort = new JTextField("3000", 5); panel.add(txtPort, c);

        c.gridx = 6;
        btnConnect = new JButton("Connect");
        btnConnect.addActionListener(this::connectClicked);
        panel.add(btnConnect, c);

        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel wrapper = new JPanel(new GridLayout(1, 2));

        wrapper.add(buildLeftPanel());
        wrapper.add(buildGamePanel());
        return wrapper;
    }

    private JPanel buildLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        panel.add(buildReadyPanel(), BorderLayout.NORTH);
        panel.add(buildUserPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildReadyPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1));
        panel.setBorder(new TitledBorder("Options / Ready"));

        // Row 1
        JPanel r1 = new JPanel();
        btnReady = new JButton("READY");
        btnReady.addActionListener(e -> sendCommand("/ready"));
        r1.add(btnReady);

        btnAddQ = new JButton("Add Question");
        btnAddQ.addActionListener(e -> showAddQDialog());
        r1.add(btnAddQ);
        panel.add(r1);

        // Row 2
        JPanel r2 = new JPanel();
        chkAway = new JCheckBox("Away");
        chkAway.addActionListener(e -> toggleAway());
        r2.add(chkAway);

        chkSpectator = new JCheckBox("Spectator");
        chkSpectator.addActionListener(e -> toggleSpectator());
        r2.add(chkSpectator);

        panel.add(r2);

        // Row 3 - Categories
        JPanel r3 = new JPanel();
        r3.setBorder(new TitledBorder("Categories"));

        chkGeo = new JCheckBox("Geography", true);
        chkSci = new JCheckBox("Science", true);
        chkMath = new JCheckBox("Math", true);
        chkHist = new JCheckBox("History", true);

        chkGeo.addActionListener(e -> sendCategories());
        chkSci.addActionListener(e -> sendCategories());
        chkMath.addActionListener(e -> sendCategories());
        chkHist.addActionListener(e -> sendCategories());

        r3.add(chkGeo);
        r3.add(chkSci);
        r3.add(chkMath);
        r3.add(chkHist);

        panel.add(r3);

        return panel;
    }

    private JPanel buildUserPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Users"));

        userModel = new DefaultListModel<>();
        lstUsers = new JList<>(userModel);

        panel.add(new JScrollPane(lstUsers), BorderLayout.CENTER);

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

    private JPanel buildGamePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Game"));

        JPanel top = new JPanel(new BorderLayout());
        lblCategory = new JLabel("Category: -");
        lblCategory.setFont(lblCategory.getFont().deriveFont(Font.BOLD, 16f));
        top.add(lblCategory, BorderLayout.WEST);

        lblTimer = new JLabel("Timer: -");
        lblTimer.setHorizontalAlignment(SwingConstants.RIGHT);
        top.add(lblTimer, BorderLayout.EAST);

        panel.add(top, BorderLayout.NORTH);

        lblQuestion = new JLabel("Question appears here.", SwingConstants.CENTER);
        lblQuestion.setFont(lblQuestion.getFont().deriveFont(Font.PLAIN, 16f));
        panel.add(lblQuestion, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(2, 2, 8, 8));
        for (int i = 0; i < 4; i++) {
            int idx = i;
            answerButtons[i] = new JButton("Answer " + (i + 1));
            answerButtons[i].addActionListener(e -> answerClicked(idx));
            bottom.add(answerButtons[i]);
        }
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    // ============================= Networking =============================

    private void connectClicked(ActionEvent e) {
        if (connected) return;

        try {
            socket = new Socket(txtHost.getText().trim(), Integer.parseInt(txtPort.getText().trim()));
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
            } catch (Exception ignored) {
                append("Disconnected.");
            }
        });

        t.start();
    }

    private void handlePayload(Payload p) {

        switch (p.getPayloadType()) {

            case CLIENT_ID -> {
                clientId = p.getClientId();
                append("Your ID: " + clientId);
            }

            case MESSAGE -> append(p.getMessage());

            case TIMER -> lblTimer.setText("Timer: " + p.getMessage());

            case POINTS_UPDATE -> append(p.getMessage());

            case QA_UPDATE -> handleQA((QAPayload) p);

            case USERLIST_UPDATE -> handleUserList((UserListPayload) p);
        }
    }

    private void handleQA(QAPayload q) {
        lockedThisRound = false;
        lblCategory.setText("Category: " + q.getCategory());
        lblQuestion.setText(q.getQuestionText());

        ArrayList<String> answers = q.getAnswers();

        for (int i = 0; i < 4; i++) {
            if (i < answers.size()) {
                answerButtons[i].setText(answers.get(i));
                answerButtons[i].setEnabled(true);
                answerButtons[i].setBackground(null);
            } else {
                answerButtons[i].setText("-");
                answerButtons[i].setEnabled(false);
            }
        }
    }

    private void handleUserList(UserListPayload up) {
        userModel.clear();
        for (int i = 0; i < up.getClientIds().size(); i++) {
            String text = up.getDisplayNames().get(i)
                    + " | pts:" + up.getPoints().get(i)
                    + (up.getLockedIn().get(i) ? " [LOCKED]" : "")
                    + (up.getAway().get(i) ? " [AWAY]" : "")
                    + (up.getSpectator().get(i) ? " [SPECTATOR]" : "");

            userModel.addElement(text);
        }
    }

    // ============================= Actions =============================

    private void answerClicked(int index) {
        if (lockedThisRound || isSpectator || isAway) return;

        sendCommand("/answer " + index);
        lockedThisRound = true;

        for (JButton btn : answerButtons) btn.setEnabled(false);
        answerButtons[index].setBackground(Color.GREEN);
    }

    private void sendCategories() {
        String cats = "";

        if (chkGeo.isSelected()) cats += "geography,";
        if (chkSci.isSelected()) cats += "science,";
        if (chkMath.isSelected()) cats += "math,";
        if (chkHist.isSelected()) cats += "history,";

        if (!cats.isEmpty())
            sendCommand("/categories " + cats.substring(0, cats.length() - 1));
    }

    private void toggleAway() {
        isAway = chkAway.isSelected();
        sendCommand(isAway ? "/away" : "/back");
    }

    private void toggleSpectator() {
        isSpectator = chkSpectator.isSelected();
        if (isSpectator) sendCommand("/spectate");
    }

    private void showAddQDialog() {
        if (!connected) {
            append("Not connected.");
            return;
        }

        String cat = JOptionPane.showInputDialog(frame, "Category:");
        if (cat == null) return;

        String q = JOptionPane.showInputDialog(frame, "Question:");
        String a1 = JOptionPane.showInputDialog(frame, "Answer A:");
        String a2 = JOptionPane.showInputDialog(frame, "Answer B:");
        String a3 = JOptionPane.showInputDialog(frame, "Answer C:");
        String a4 = JOptionPane.showInputDialog(frame, "Answer D:");
        String correct = JOptionPane.showInputDialog(frame, "Correct index (0-3):");

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
            out.writeObject(p);
            out.flush();
        } catch (Exception ignored) {
            append("Send failed.");
        }
    }

    private void append(String msg) {
        txtEvents.append(msg + "\n");
        txtEvents.setCaretPosition(txtEvents.getDocument().getLength());
    }
}

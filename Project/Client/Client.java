// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame Client – Prettier Swing UI with beginner-friendly code
// Fonts: Behind The Nineties (title) + Visby CF (UI)

package Client;

import Common.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.*;

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

    // UI Components
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

    private JCheckBox chkGeo;
    private JCheckBox chkSci;
    private JCheckBox chkMath;
    private JCheckBox chkHist;

    // Fonts
    private Font titleFont;
    private Font uiFont;

    // Colors (Soft Lavender Theme)
    private final Color BG = new Color(238, 234, 255);
    private final Color PANEL_BG = new Color(248, 245, 255);
    private final Color BTN_COLOR = new Color(167, 136, 255);
    private final Color BTN_HOVER = new Color(150, 120, 240);
    private final Color TEXT_DARK = new Color(50, 45, 70);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Client::new);
    }

    public Client() {
        loadFonts();
        buildUI();
    }

    // ==========================
    // LOAD CUSTOM FONTS
    // ==========================
    private void loadFonts() {
        try {
            titleFont = Font.createFont(
                    Font.TRUETYPE_FONT,
                    new File("/Users/nilka/Library/Fonts/Behind The Nineties Regular.ttf")
            ).deriveFont(28f);

            uiFont = Font.createFont(
                    Font.TRUETYPE_FONT,
                    new File("/Users/nilka/Library/Fonts/VisbyCF-Regular.otf")
            ).deriveFont(16f);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(titleFont);
            ge.registerFont(uiFont);

        } catch (Exception e) {
            System.out.println("Failed to load fonts — using system defaults.");
            titleFont = new Font("SansSerif", Font.BOLD, 28);
            uiFont = new Font("SansSerif", Font.PLAIN, 16);
        }
    }

    // ==========================
    // BUILD UI
    // ==========================
    private void buildUI() {
        frame = new JFrame("TriviaGuessGame – nhd5");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1150, 700);

        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(10, 10));

        frame.add(buildTitle(), BorderLayout.NORTH);
        frame.add(buildCenterPanel(), BorderLayout.CENTER);
        frame.add(buildEventsPanel(), BorderLayout.EAST);

        frame.setVisible(true);
    }

    private JPanel buildTitle() {
        JPanel p = new JPanel();
        p.setBackground(BG);

        JLabel lbl = new JLabel("Trivia Guess Game");
        lbl.setFont(titleFont);
        lbl.setForeground(TEXT_DARK);

        p.add(lbl);

        return p;
    }

    private JPanel buildCenterPanel() {
        JPanel wrapper = new JPanel(new GridLayout(1, 2, 10, 10));
        wrapper.setBackground(BG);

        wrapper.add(buildLeftSide());
        wrapper.add(buildGamePanel());
        return wrapper;
    }

    private JPanel buildLeftSide() {
        JPanel left = new JPanel(new BorderLayout(10, 10));
        left.setBackground(BG);

        left.add(buildConnectionPanel(), BorderLayout.NORTH);
        left.add(buildOptionsPanel(), BorderLayout.CENTER);
        left.add(buildUserPanel(), BorderLayout.SOUTH);

        return left;
    }

    private JPanel buildConnectionPanel() {
        JPanel p = new RoundedPanel();
        p.setBorder(new TitledBorder("Connection"));
        p.setBackground(PANEL_BG);
        p.setLayout(new GridLayout(2, 4, 5, 5));

        JLabel lbl1 = new JLabel("Username:");
        lbl1.setFont(uiFont);
        txtUser = new JTextField("Player");
        txtUser.setFont(uiFont);

        JLabel lbl2 = new JLabel("Host:");
        lbl2.setFont(uiFont);
        txtHost = new JTextField("localhost");
        txtHost.setFont(uiFont);

        JLabel lbl3 = new JLabel("Port:");
        lbl3.setFont(uiFont);
        txtPort = new JTextField("3000");
        txtPort.setFont(uiFont);

        btnConnect = prettyButton("Connect");
        btnConnect.addActionListener(this::connectClicked);

        p.add(lbl1); p.add(txtUser);
        p.add(lbl2); p.add(txtHost);
        p.add(lbl3); p.add(txtPort);
        p.add(new JLabel("")); p.add(btnConnect);

        return p;
    }

    private JPanel buildOptionsPanel() {
        JPanel p = new RoundedPanel();
        p.setBackground(PANEL_BG);
        p.setBorder(new TitledBorder("Options"));

        p.setLayout(new GridLayout(3, 1, 5, 5));

        // --- Row 1: Ready + Add Question ---
        JPanel r1 = new JPanel();
        r1.setBackground(PANEL_BG);

        btnReady = prettyButton("READY");
        btnReady.addActionListener(e -> sendCommand("/ready"));

        btnAddQ = prettyButton("Add Question");
        btnAddQ.addActionListener(e -> showAddQDialog());

        r1.add(btnReady);
        r1.add(btnAddQ);

        // --- Row 2: Away + Spectator ---
        JPanel r2 = new JPanel();
        r2.setBackground(PANEL_BG);

        chkAway = new JCheckBox("Away");
        chkAway.setFont(uiFont);
        chkAway.setBackground(PANEL_BG);
        chkAway.addActionListener(e -> toggleAway());

        chkSpectator = new JCheckBox("Spectator");
        chkSpectator.setFont(uiFont);
        chkSpectator.setBackground(PANEL_BG);
        chkSpectator.addActionListener(e -> toggleSpectator());

        r2.add(chkAway);
        r2.add(chkSpectator);

        // --- Row 3: Categories ---
        JPanel r3 = new JPanel();
        r3.setBackground(PANEL_BG);
        r3.setBorder(new TitledBorder("Categories"));

        chkGeo = makeCategory("Geo");
        chkSci = makeCategory("Science");
        chkMath = makeCategory("Math");
        chkHist = makeCategory("History");

        r3.add(chkGeo); r3.add(chkSci);
        r3.add(chkMath); r3.add(chkHist);

        p.add(r1); p.add(r2); p.add(r3);

        return p;
    }

    private JCheckBox makeCategory(String name) {
        JCheckBox c = new JCheckBox(name, true);
        c.setFont(uiFont);
        c.setBackground(PANEL_BG);
        c.addActionListener(e -> sendCategories());
        return c;
    }

    private JPanel buildUserPanel() {
        JPanel p = new RoundedPanel();
        p.setBackground(PANEL_BG);
        p.setBorder(new TitledBorder("Users"));

        userModel = new DefaultListModel<>();
        lstUsers = new JList<>(userModel);
        lstUsers.setFont(uiFont);

        p.setLayout(new BorderLayout());
        p.add(new JScrollPane(lstUsers), BorderLayout.CENTER);

        return p;
    }

    private JPanel buildEventsPanel() {
        JPanel p = new RoundedPanel();
        p.setBackground(PANEL_BG);
        p.setBorder(new TitledBorder("Game Events"));
        p.setPreferredSize(new Dimension(280, 600));

        txtEvents = new JTextArea();
        txtEvents.setEditable(false);
        txtEvents.setFont(uiFont);
        txtEvents.setLineWrap(true);
        txtEvents.setWrapStyleWord(true);

        p.setLayout(new BorderLayout());
        p.add(new JScrollPane(txtEvents), BorderLayout.CENTER);

        return p;
    }

    private JPanel buildGamePanel() {
        JPanel p = new RoundedPanel();
        p.setBackground(PANEL_BG);
        p.setBorder(new TitledBorder("Game"));
        p.setLayout(new BorderLayout(10, 10));

        // Category + Timer row
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(PANEL_BG);

        lblCategory = new JLabel("Category: -");
        lblCategory.setFont(titleFont);

        lblTimer = new JLabel("Timer: -");
        lblTimer.setFont(uiFont);
        lblTimer.setHorizontalAlignment(SwingConstants.RIGHT);

        top.add(lblCategory, BorderLayout.WEST);
        top.add(lblTimer, BorderLayout.EAST);

        // QUESTION TEXT
        lblQuestion = new JLabel("Question appears here.", SwingConstants.CENTER);
        lblQuestion.setFont(uiFont.deriveFont(20f));
        lblQuestion.setForeground(TEXT_DARK);

        // ANSWER BUTTON GRID
        JPanel answerGrid = new JPanel(new GridLayout(2, 2, 10, 10));
        answerGrid.setBackground(PANEL_BG);

        for (int i = 0; i < 4; i++) {
            final int idx = i;
            answerButtons[i] = prettyButton("Answer " + (i + 1));
            answerButtons[i].addActionListener(e -> answerClicked(idx));
            answerGrid.add(answerButtons[i]);
        }

        p.add(top, BorderLayout.NORTH);
        p.add(lblQuestion, BorderLayout.CENTER);
        p.add(answerGrid, BorderLayout.SOUTH);

        return p;
    }

    // ==========================
    // BEAUTIFUL BUTTON CREATOR
    // ==========================
    private JButton prettyButton(String text) {
        JButton b = new JButton(text);
        b.setFont(uiFont);
        b.setBackground(BTN_COLOR);
        b.setForeground(Color.white);
        b.setFocusPainted(false);
        b.setBorder(new RoundedBorder(10));

        // Hover effect
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { b.setBackground(BTN_HOVER); }
            public void mouseExited(MouseEvent e) { b.setBackground(BTN_COLOR); }
        });

        return b;
    }

    // ==========================
    // ROUNDED PANEL CLASS
    // ==========================
    private static class RoundedPanel extends JPanel {
        public RoundedPanel() {
            super();
            setOpaque(false);
        }

        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            super.paintComponent(g);
        }
    }

    // Rounded border for buttons
    private static class RoundedBorder implements Border {
        private int radius;
        RoundedBorder(int r) { radius = r; }

        public Insets getBorderInsets(Component c) { return new Insets(radius + 1, radius + 1, radius + 1, radius + 1); }
        public boolean isBorderOpaque() { return false; }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
        }
    }

    // ==========================
    // NETWORKING LOGIC
    // ==========================
    private void connectClicked(ActionEvent e) {
        if (connected) return;

        try {
            socket = new Socket(txtHost.getText(), Integer.parseInt(txtPort.getText()));
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            append("Connected.");

            ConnectionPayload cp = new ConnectionPayload();
            cp.setPayloadType(PayloadType.CLIENT_CONNECT);
            cp.setClientName(txtUser.getText());
            send(cp);

            startReaderThread();

        } catch (Exception ex) {
            append("Connect failed: " + ex.getMessage());
        }
    }

    private void startReaderThread() {
        new Thread(() -> {
            try {
                while (connected) {
                    Object obj = in.readObject();
                    if (obj instanceof Payload p) handlePayload(p);
                }
            } catch (Exception e) {
                append("Disconnected.");
            }
        }).start();
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
            case QA_UPDATE -> handleQA((QAPayload)p);
            case USERLIST_UPDATE -> handleUserList((UserListPayload)p);
        }
    }

    private void handleQA(QAPayload q) {
        lockedThisRound = false;
        lblCategory.setText("Category: " + q.getCategory());
        lblQuestion.setText(q.getQuestionText());

        ArrayList<String> ans = q.getAnswers();

        for (int i = 0; i < 4; i++) {
            if (i < ans.size()) {
                answerButtons[i].setText(ans.get(i));
                answerButtons[i].setEnabled(true);
                answerButtons[i].setBackground(BTN_COLOR);
            } else {
                answerButtons[i].setText("-");
                answerButtons[i].setEnabled(false);
            }
        }
    }

    private void handleUserList(UserListPayload up) {
        userModel.clear();

        for (int i = 0; i < up.getClientIds().size(); i++) {
            String s =
                up.getDisplayNames().get(i) +
                " | pts:" + up.getPoints().get(i) +
                (up.getLockedIn().get(i) ? " [LOCKED]" : "") +
                (up.getAway().get(i) ? " [AWAY]" : "") +
                (up.getSpectator().get(i) ? " [SPEC]" : "");

            userModel.addElement(s);
        }
    }

    // ==========================
    // ACTIONS
    // ==========================
    private void answerClicked(int index) {
        if (lockedThisRound || isSpectator || isAway) return;

        sendCommand("/answer " + index);
        lockedThisRound = true;

        for (JButton b : answerButtons) b.setEnabled(false);
        answerButtons[index].setBackground(new Color(120, 200, 120));
    }

    private void sendCategories() {
        String s = "";

        if (chkGeo.isSelected()) s += "geo,";
        if (chkSci.isSelected()) s += "science,";
        if (chkMath.isSelected()) s += "math,";
        if (chkHist.isSelected()) s += "history,";

        if (!s.isEmpty())
            sendCommand("/categories " + s.substring(0, s.length() - 1));
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
        if (!connected) { append("Not connected."); return; }

        String cat = JOptionPane.showInputDialog("Category:");
        if (cat == null) return;

        String q = JOptionPane.showInputDialog("Question:");
        String a1 = JOptionPane.showInputDialog("Answer A:");
        String a2 = JOptionPane.showInputDialog("Answer B:");
        String a3 = JOptionPane.showInputDialog("Answer C:");
        String a4 = JOptionPane.showInputDialog("Answer D:");
        String correct = JOptionPane.showInputDialog("Correct (0-3):");

        String line = cat + "|" + q + "|" + a1 + "|" + a2 + "|" + a3 + "|" + a4 + "|" + correct;

        sendCommand("/addq " + line);
    }

    private void sendCommand(String msg) {
        Payload p = new Payload();
        p.setPayloadType(PayloadType.MESSAGE);
        p.setClientId(clientId);
        p.setMessage(msg);
        send(p);
    }

    private void send(Payload p) {
        try {
            out.writeObject(p);
            out.flush();
        } catch (Exception e) {
            append("Send failed.");
        }
    }

    private void append(String msg) {
        txtEvents.append(msg + "\n");
        txtEvents.setCaretPosition(txtEvents.getDocument().getLength());
    }
}

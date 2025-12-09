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

/**

* UCID: nhd5
* Final clean TriviaGuessGame Client
* * Fully compatible with your final payload definitions
* * No missing methods (getQuestionText(), getAnswers(), etc.)
* * No invalid payload types (COMMAND removed)
* * Splits CHAT vs GAME EVENTS using "[CHAT]" prefix convention
    */
    public class Client {

  // ===================== Networking =====================
  private Socket socket;
  private ObjectOutputStream out;
  private ObjectInputStream in;
  private boolean connected = false;
  private long clientId = -1;

  private boolean lockedThisRound = false;
  private boolean isAway = false;
  private boolean isSpectator = false;
  private boolean isReady = false;

  // ===================== UI =====================
  private JFrame frame;

  // Connection
  private JTextField txtUser;
  private JTextField txtHost;
  private JTextField txtPort;
  private JButton btnSetName;
  private JButton btnConnect;
  private JLabel lblConnectHint;
  private String currentName = "Player";

  // Ready & Options
  private JCheckBox chkAway;
  private JCheckBox chkSpectator;

  // Categories
  private JCheckBox chkGeo;
  private JCheckBox chkSci;
  private JCheckBox chkMath;
  private JCheckBox chkHist;

  // Users
  private DefaultListModel<User> userModel;
  private JList<User> lstUsers;
  private Map<Long, User> users = new HashMap<>();

  // Chat + events
  private JTextArea txtChat;
  private JTextField txtChatInput;
  private JButton btnSendChat;
  private JTextArea txtEvents;

  // Game area
  private JLabel lblCategory;
  private JTextArea txtQuestion;  // Changed from JLabel to JTextArea for full question display
  private JLabel lblTimer;
  private JButton[] answerButtons = new JButton[4];

  public static void main(String[] args) {
  SwingUtilities.invokeLater(Client::new);
  }

  public Client() {
  buildUI();
  }

  // ===========================================================
  // UI LAYOUT
  // ===========================================================
  private void buildUI() {
  frame = new JFrame("TriviaGuessGame – nhd5");
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  frame.setSize(1200, 800);
  frame.setLocationRelativeTo(null);
  frame.setLayout(new BorderLayout());
  // Light grey background matching mockup
  frame.getContentPane().setBackground(new Color(240, 240, 240));

  JPanel root = new JPanel(new BorderLayout());
  root.setBorder(new EmptyBorder(10, 10, 10, 10));
  root.setOpaque(false);

  JLabel lblTitle = new JLabel("Trivia Guess Game", SwingConstants.CENTER);
  TextFX.setTitleFont(lblTitle);
  // Dark blue title matching mockup
  lblTitle.setForeground(new Color(30, 60, 120));
  lblTitle.setBorder(new EmptyBorder(20, 5, 20, 5));
  root.add(lblTitle, BorderLayout.NORTH);

  JPanel center = new JPanel(new BorderLayout(10, 0));
  center.setOpaque(false);
  center.add(buildLeftColumn(), BorderLayout.WEST);
  center.add(buildGamePanel(), BorderLayout.CENTER);
  center.add(buildRightColumn(), BorderLayout.EAST);

  root.add(center, BorderLayout.CENTER);
  frame.add(root);
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

  // ===========================================================
  // CONNECTION PANEL
  // ===========================================================
  private JPanel buildConnectionPanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new GridBagLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Username"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  GridBagConstraints c = new GridBagConstraints();
  c.insets = new Insets(2, 4, 2, 4);
  c.gridy = 0;

  // Username field
  c.gridx = 0; panel.add(label("Username:"), c);
  c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
  txtUser = new JTextField("Player", 15); 
  txtUser.setBackground(Color.WHITE);
  txtUser.setForeground(new Color(30, 30, 30));
  txtUser.setBorder(new LineBorder(new Color(200, 200, 200), 1));
  panel.add(txtUser, c);
  
  // Host field
  c.gridx = 0; c.gridy = 1; c.gridwidth = 1;
  panel.add(label("Host:"), c);
  c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
  txtHost = new JTextField("localhost", 15); 
  txtHost.setBackground(Color.WHITE);
  txtHost.setForeground(new Color(30, 30, 30));
  txtHost.setBorder(new LineBorder(new Color(200, 200, 200), 1));
  panel.add(txtHost, c);

  // Port field
  c.gridx = 0; c.gridy = 2; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
  panel.add(label("Port:"), c);
  c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
  txtPort = new JTextField("3000", 15); 
  txtPort.setBackground(Color.WHITE);
  txtPort.setForeground(new Color(30, 30, 30));
  txtPort.setBorder(new LineBorder(new Color(200, 200, 200), 1));
  panel.add(txtPort, c);

  // Connect button
  c.gridx = 0; c.gridy = 3; c.gridwidth = 3; c.fill = GridBagConstraints.NONE;
  btnConnect = new JButton("CONNECT");
  stylePrimary(btnConnect);
  btnConnect.addActionListener(this::connectClicked);
  panel.add(btnConnect, c);

  // Hint
  c.gridx = 0; c.gridy = 4; c.gridwidth = 3;
  lblConnectHint = new JLabel("Step 1: Set your name, then click Connect.");
  lblConnectHint.setForeground(new Color(100, 100, 100));
  TextFX.setSubtitleFont(lblConnectHint);
  panel.add(lblConnectHint, c);

  return panel;
  }

  // ===========================================================
  // OPTIONS PANEL
  // ===========================================================
  private JPanel buildOptionsPanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout(8, 8));
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Status"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  // Status checkboxes matching mockup
  JPanel statusContainer = new JPanel();
  statusContainer.setLayout(new BoxLayout(statusContainer, BoxLayout.Y_AXIS));
  statusContainer.setOpaque(false);
  statusContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
  
  // Ready checkbox
  JCheckBox chkReady = new JCheckBox("Ready");
  chkReady.setOpaque(false);
  chkReady.setForeground(new Color(30, 60, 120));
  TextFX.setSubtitleFont(chkReady);
  chkReady.addActionListener(e -> {
      if (chkReady.isSelected() && !isReady) {
          readyClicked();
      }
  });
  statusContainer.add(chkReady);
  statusContainer.add(Box.createVerticalStrut(5));

  chkSpectator = new JCheckBox("Spectator");
  chkSpectator.setOpaque(false);
  chkSpectator.setForeground(new Color(30, 60, 120));
  chkSpectator.addActionListener(e -> toggleSpectator());
  TextFX.setSubtitleFont(chkSpectator);
  statusContainer.add(chkSpectator);
  statusContainer.add(Box.createVerticalStrut(5));

  chkAway = new JCheckBox("Away");
  chkAway.setOpaque(false);
  chkAway.setForeground(new Color(30, 60, 120));
  chkAway.addActionListener(e -> toggleAway());
  TextFX.setSubtitleFont(chkAway);
  statusContainer.add(chkAway);
  
  panel.add(statusContainer, BorderLayout.CENTER);
  return panel;
  }

  private JCheckBox makeCategory(String text) {
  JCheckBox box = new JCheckBox(text);
  box.setOpaque(false);
  box.setSelected(true);
  box.setForeground(new Color(30, 60, 120));
  TextFX.setSubtitleFont(box);
  box.addActionListener(e -> sendCategories());
  return box;
  }

  // ===========================================================
  // USER PANEL
  // ===========================================================
  private JPanel buildUserPanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Players"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  userModel = new DefaultListModel<>();
  lstUsers = new JList<>(userModel);
  // White background matching mockup
  lstUsers.setBackground(Color.WHITE);
  lstUsers.setForeground(new Color(30, 60, 120));
  lstUsers.setBorder(new EmptyBorder(5, 5, 5, 5));

  lstUsers.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(
              JList<?> list, Object value, int index,
              boolean isSelected, boolean cellHasFocus) {

          JLabel lbl = (JLabel) super.getListCellRendererComponent(
                  list, value, index, isSelected, cellHasFocus);

          if (value instanceof User u) {
              lbl.setText(u.toDisplayString());
              lbl.setBackground(Color.WHITE);
              lbl.setForeground(new Color(30, 60, 120));
          }
          return lbl;
      }
  });

  panel.add(new JScrollPane(lstUsers), BorderLayout.CENTER);
  panel.setPreferredSize(new Dimension(290, 260));
  return panel;
  }

  // ===========================================================
  // RIGHT COLUMN (CHAT + EVENTS)
  // ===========================================================
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
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Chat"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  txtChat = new JTextArea();
  txtChat.setEditable(false);
  txtChat.setLineWrap(true);
  txtChat.setWrapStyleWord(true);
  // White background matching mockup
  txtChat.setBackground(Color.WHITE);
  txtChat.setForeground(new Color(30, 30, 30));
  txtChat.setBorder(new EmptyBorder(5, 5, 5, 5));

  panel.add(new JScrollPane(txtChat), BorderLayout.CENTER);

  JPanel input = new JPanel(new BorderLayout(5, 5));
  input.setOpaque(false);

  txtChatInput = new JTextField("Type message...");
  txtChatInput.setBackground(Color.WHITE);
  txtChatInput.setForeground(new Color(100, 100, 100));
  txtChatInput.setBorder(new LineBorder(new Color(200, 200, 200), 1));
  // Allow Enter key to send chat
  txtChatInput.addActionListener(e -> sendChat());
  
  btnSendChat = new JButton("SEND");
  styleSecondary(btnSendChat);
  btnSendChat.addActionListener(e -> sendChat());

  input.add(txtChatInput, BorderLayout.CENTER);
  input.add(btnSendChat, BorderLayout.EAST);

  panel.add(input, BorderLayout.SOUTH);
  return panel;
  }

  private JPanel buildEventsPanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Game Events"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  txtEvents = new JTextArea();
  txtEvents.setEditable(false);
  txtEvents.setLineWrap(true);
  txtEvents.setWrapStyleWord(true);
  // White background matching mockup
  txtEvents.setBackground(Color.WHITE);
  txtEvents.setForeground(new Color(30, 30, 30));
  txtEvents.setBorder(new EmptyBorder(5, 5, 5, 5));

  panel.add(new JScrollPane(txtEvents), BorderLayout.CENTER);
  panel.setPreferredSize(new Dimension(260, 220));
  return panel;
  }

  // ===========================================================
  // GAME PANEL
  // ===========================================================
  private JPanel buildGamePanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout(10, 10));
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Question"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  JPanel top = new JPanel(new BorderLayout());
  top.setOpaque(false);

  lblCategory = new JLabel("Category: -");
  lblCategory.setForeground(new Color(30, 100, 200)); // Light blue matching mockup
  lblTimer = new JLabel("Timer: -", SwingConstants.RIGHT);
  lblTimer.setForeground(new Color(30, 100, 200)); // Light blue matching mockup
  TextFX.setSubtitleFont(lblCategory);
  TextFX.setSubtitleFont(lblTimer);

  top.add(lblCategory, BorderLayout.WEST);
  top.add(lblTimer, BorderLayout.EAST);
  panel.add(top, BorderLayout.NORTH);

  // Large, prominent question area matching mockup
  txtQuestion = new JTextArea("Question appears here.");
  txtQuestion.setEditable(false);
  txtQuestion.setLineWrap(true);
  txtQuestion.setWrapStyleWord(true);
  txtQuestion.setOpaque(true); // Make opaque so it's visible
  txtQuestion.setBackground(Color.WHITE); // White background for visibility
  txtQuestion.setForeground(new Color(30, 60, 120)); // Dark blue matching mockup
  // Larger, bolder font for prominence
  txtQuestion.setFont(TextFX.getSubtitleFont().deriveFont(Font.BOLD, 18f));
  txtQuestion.setBorder(new EmptyBorder(40, 30, 40, 30));
  
  JScrollPane questionScroll = new JScrollPane(txtQuestion);
  questionScroll.setOpaque(false);
  questionScroll.getViewport().setOpaque(false);
  questionScroll.setBorder(null);
  questionScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
  
  panel.add(questionScroll, BorderLayout.CENTER);

  // Answer buttons layout matching mockup
  JPanel answers = new JPanel(new GridLayout(2, 2, 10, 10));
  answers.setOpaque(false);
  answers.setBorder(new EmptyBorder(20, 40, 10, 40));

  for (int i = 0; i < 4; i++) {
      int idx = i;
      JButton btn = new JButton("Answer " + (i + 1));
      styleAnswer(btn);
      btn.addActionListener(e -> answerClicked(idx));
      answerButtons[i] = btn;
      answers.add(btn);
  }
  
  // Submit button matching mockup
  JButton btnSubmit = new JButton("SUBMIT");
  stylePrimary(btnSubmit);
  btnSubmit.addActionListener(e -> {
      // Submit is handled by answer selection, but keep for UI consistency
  });
  JPanel submitPanel = new JPanel();
  submitPanel.setOpaque(false);
  submitPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
  submitPanel.add(btnSubmit);
  
  // Container for answers and submit button - goes in SOUTH
  JPanel bottomContainer = new JPanel();
  bottomContainer.setLayout(new BoxLayout(bottomContainer, BoxLayout.Y_AXIS));
  bottomContainer.setOpaque(false);
  bottomContainer.add(answers);
  bottomContainer.add(submitPanel);
  
  panel.add(bottomContainer, BorderLayout.SOUTH);
  
  return panel;
  }

  // ===========================================================
  // NETWORKING
  // ===========================================================
  private void setNameClicked() {
  String name = txtUser.getText().trim();
  if (name.isEmpty()) {
      lblConnectHint.setText("Please enter a name.");
      return;
  }
  currentName = name;
  txtUser.setEditable(false);
  btnSetName.setEnabled(false);
  lblConnectHint.setText("Name set to: " + name + ". Now click Connect.");
  }

  private void connectClicked(ActionEvent e) {
  if (connected) return;

  if (currentName == null || currentName.isEmpty()) {
      lblConnectHint.setText("Please set your name first!");
      return;
  }

  try {
      String host = txtHost.getText().trim();
      int port = Integer.parseInt(txtPort.getText().trim());

      socket = new Socket(host, port);
      out = new ObjectOutputStream(socket.getOutputStream());
      in = new ObjectInputStream(socket.getInputStream());
      connected = true;

      appendEvent("Connected to server.");
      lblConnectHint.setText("Connected as " + currentName + ". Step 2: Check Ready.");
      btnConnect.setEnabled(false);

      ConnectionPayload cp = new ConnectionPayload();
      cp.setPayloadType(PayloadType.CLIENT_CONNECT);
      cp.setClientName(currentName);
      send(cp);

      startReaderThread();
  } catch (Exception ex) {
      appendEvent("Connect failed: " + ex.getMessage());
      connected = false;
  }
  }

  private void startReaderThread() {
  Thread t = new Thread(() -> {
  try {
  while (connected) {
  Object obj = in.readObject();
  if (obj instanceof Payload p) handlePayload(p);
  }
  } catch (Exception ex) {
  appendEvent("Disconnected.");
  connected = false;
  }
  });
  t.start();
  }

  private void handlePayload(Payload p) {
  switch (p.getPayloadType()) {
      case CLIENT_ID -> {
          clientId = p.getClientId();
          appendEvent("Your ID: " + clientId);
      }

      case MESSAGE -> handleMessagePayload(p);

      case TIMER -> lblTimer.setText("Timer: " + p.getMessage());

      case QUESTION -> handleQA((QAPayload) p);

      case USER_LIST -> handleUserList((UserListPayload) p);

      case POINTS_UPDATE -> appendEvent(p.getMessage());

      default -> {}
  }
  }

  private void handleMessagePayload(Payload p) {
  if (p.getMessage() == null) return;

  String msg = p.getMessage();

  if (msg.startsWith("[CHAT] ")) {
      appendChat(msg.substring(7));
  } else {
      appendEvent(msg);
      if (msg.contains("GAME OVER")) {
          txtQuestion.setText("Game over. Click READY to play again.");
          for (JButton btn : answerButtons) {
              btn.setEnabled(false);
          }
          isReady = false;
      }
      if (msg.contains("Trivia Session Starting") || msg.contains("Round 1")) {
          isReady = false;
      }
  }
  }

  private void handleQA(QAPayload q) {
  lockedThisRound = false;
  lblCategory.setText("Category: " + q.getCategory());
  // Set full question text in text area
  txtQuestion.setText(q.getQuestionText());
  txtQuestion.setCaretPosition(0); // Scroll to top

  ArrayList<String> ans = q.getAnswers();
  for (int i = 0; i < 4; i++) {
      if (ans != null && i < ans.size()) {
          answerButtons[i].setText(ans.get(i));
          answerButtons[i].setEnabled(true);
          // Reset to white matching mockup
          answerButtons[i].setBackground(Color.WHITE);
          answerButtons[i].setForeground(new Color(30, 60, 120));
      } else {
          answerButtons[i].setText("-");
          answerButtons[i].setEnabled(false);
          answerButtons[i].setBackground(new Color(240, 240, 240));
      }
  }
  }

  private void handleUserList(UserListPayload up) {
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

  // ===========================================================
  // ACTIONS
  // ===========================================================
  private void answerClicked(int idx) {
  if (!connected || lockedThisRound || isSpectator || isAway) return;

  sendCommand("/answer " + idx);
  lockedThisRound = true;

  for (JButton btn : answerButtons) btn.setEnabled(false);
  // Light blue highlight when answer is selected matching mockup
  answerButtons[idx].setBackground(new Color(200, 220, 255));
  answerButtons[idx].setForeground(new Color(30, 60, 120));
  }

  private void sendChat() {
  if (!connected) return;
  if (isSpectator) {
  appendEvent("Spectators cannot chat.");
  return;
  }

  String msg = txtChatInput.getText().trim();
  if (msg.isEmpty()) return;

  Payload p = new Payload();
  p.setPayloadType(PayloadType.MESSAGE);
  p.setClientId(clientId);
  p.setMessage(msg);
  send(p);

  txtChatInput.setText("");
  }

  private void readyClicked() {
  if (!connected) return;
  if (isReady) {
      appendEvent("You are already ready!");
      return;
  }
  sendCommand("/ready");
  isReady = true;
  }

  private void sendCommand(String cmd) {
  if (!connected) return;

  Payload p = new Payload();
  p.setPayloadType(PayloadType.MESSAGE); // server interprets slash commands inside MESSAGE
  p.setClientId(clientId);
  p.setMessage(cmd);
  send(p);
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

  String cat = JOptionPane.showInputDialog(frame,
          "Category (geography, science, math, history):");
  if (cat == null || cat.trim().isEmpty()) return;

  String q = JOptionPane.showInputDialog(frame, "Question:");
  if (q == null || q.trim().isEmpty()) return;

  String a1 = JOptionPane.showInputDialog(frame, "Answer A:");
  String a2 = JOptionPane.showInputDialog(frame, "Answer B:");
  String a3 = JOptionPane.showInputDialog(frame, "Answer C:");
  String a4 = JOptionPane.showInputDialog(frame, "Answer D:");
  String correct = JOptionPane.showInputDialog(frame,
          "Correct index (0=A, 1=B, 2=C, 3=D):");

  if (a1 == null || a2 == null || a3 == null || a4 == null || correct == null) return;

  String line = cat + "|" + q + "|" + a1 + "|" + a2 + "|" + a3 + "|" + a4 + "|" + correct;
  sendCommand("/addq " + line);
  }

  private void send(Payload p) {
  try {
  out.writeObject(p);
  out.flush();
  } catch (Exception ex) {
  appendEvent("Send failed: " + ex.getMessage());
  }
  }

  // ===========================================================
  // HELPERS
  // ===========================================================
  private JLabel label(String text) {
  JLabel lbl = new JLabel(text);
  lbl.setForeground(new Color(30, 60, 120));
  TextFX.setSubtitleFont(lbl);
  return lbl;
  }

  private void stylePrimary(JButton btn) {
  // Primary button matching mockup - light blue
  btn.setBackground(new Color(100, 150, 255));
  btn.setForeground(Color.WHITE);
  btn.setBorder(new LineBorder(new Color(80, 130, 235), 1, true));
  TextFX.setSubtitleFont(btn);
  btn.setFont(TextFX.getSubtitleFont().deriveFont(Font.BOLD, 14f));
  btn.setPreferredSize(new Dimension(150, 40));
  }

  private void styleSecondary(JButton btn) {
  // Secondary button matching mockup
  btn.setBackground(new Color(100, 150, 255));
  btn.setForeground(Color.WHITE);
  btn.setBorder(new LineBorder(new Color(80, 130, 235), 1, true));
  TextFX.setSubtitleFont(btn);
  btn.setFont(TextFX.getSubtitleFont().deriveFont(Font.BOLD, 12f));
  btn.setPreferredSize(new Dimension(80, 30));
  }

  private void styleAnswer(JButton btn) {
  // Answer buttons matching mockup - white with dark text
  btn.setBackground(Color.WHITE);
  btn.setForeground(new Color(30, 60, 120));
  btn.setBorder(new LineBorder(new Color(200, 200, 200), 1, true));
  TextFX.setSubtitleFont(btn);
  btn.setFont(TextFX.getSubtitleFont().deriveFont(Font.PLAIN, 14f));
  btn.setPreferredSize(new Dimension(250, 40));
  }

  private void appendEvent(String msg) {
  txtEvents.append(msg + "\n");
  txtEvents.setCaretPosition(txtEvents.getDocument().getLength());
  }

  private void appendChat(String msg) {
  txtChat.append(msg + "\n");
  txtChat.setCaretPosition(txtChat.getDocument().getLength());
  }

  }

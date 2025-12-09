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
* Date: December 8, 2025
* Description: TriviaGuessGame Client – GUI client application for trivia game.
*              Handles connection to server, UI rendering, user input, and game state.
*              Fully compatible with payload definitions. Splits CHAT vs GAME EVENTS using "[CHAT]" prefix.
* References:
*   - W3Schools: https://www.w3schools.com/java/java_networking.asp
*   - W3Schools: https://www.w3schools.com/java/java_methods.asp (for event handling)
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
  /**
   * Creates the connection panel with username, host, port fields and connect button.
   * This panel allows users to enter their connection details and connect to the server.
   */
  private JPanel buildConnectionPanel() {
  // Create the main panel with white background
  JPanel panel = new JPanel();
  panel.setLayout(new GridBagLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Username"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  // GridBagConstraints helps position components in a grid layout
  GridBagConstraints c = new GridBagConstraints();
  c.insets = new Insets(2, 4, 2, 4);
  c.gridy = 0; // Start at row 0

  // Add username field (row 0)
  c.gridx = 0; panel.add(label("Username:"), c);
  c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
  txtUser = createTextField("Player", 15);
  panel.add(txtUser, c);
  
  // Add host field (row 1)
  c.gridx = 0; c.gridy = 1; c.gridwidth = 1;
  panel.add(label("Host:"), c);
  c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
  txtHost = createTextField("localhost", 15);
  panel.add(txtHost, c);

  // Add port field (row 2)
  c.gridx = 0; c.gridy = 2; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
  panel.add(label("Port:"), c);
  c.gridx = 1; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL;
  txtPort = createTextField("3000", 15);
  panel.add(txtPort, c);

  // Add connect button (row 3)
  c.gridx = 0; c.gridy = 3; c.gridwidth = 3; c.fill = GridBagConstraints.NONE;
  btnConnect = createConnectButton();
  panel.add(btnConnect, c);

  // Add hint label (row 4)
  c.gridx = 0; c.gridy = 4; c.gridwidth = 3;
  lblConnectHint = createConnectionHint();
  panel.add(lblConnectHint, c);

  return panel;
  }

  /**
   * Helper method: Creates a styled text field for user input.
   * @param defaultText The default text to show in the field
   * @param columns The width of the text field
   * @return A styled JTextField
   */
  private JTextField createTextField(String defaultText, int columns) {
  JTextField field = new JTextField(defaultText, columns);
  field.setBackground(Color.WHITE);
  field.setForeground(new Color(30, 30, 30));
  field.setBorder(new LineBorder(new Color(200, 200, 200), 1));
  return field;
  }

  /**
   * Helper method: Creates the connect button with styling and click handler.
   * @return A styled JButton for connecting to the server
   */
  private JButton createConnectButton() {
  JButton btn = new JButton("Connect");
  stylePrimary(btn);
  btn.addActionListener(this::connectClicked);
  return btn;
  }

  /**
   * Helper method: Creates the hint label that shows connection instructions.
   * @return A JLabel with connection instructions
   */
  private JLabel createConnectionHint() {
  JLabel hint = new JLabel("Step 1: Enter your name above, then click the Connect button below.");
  hint.setForeground(new Color(100, 100, 100));
  TextFX.setSubtitleFont(hint);
  return hint;
  }

  // ===========================================================
  // OPTIONS PANEL (STATUS PANEL)
  // ===========================================================
  /**
   * Creates the status panel with Ready, Spectator, and Away checkboxes.
   * This panel allows users to set their game status.
   */
  private JPanel buildOptionsPanel() {
  // Create the main panel
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout(8, 8));
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Status"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  // Create container for all status checkboxes
  JPanel statusContainer = createStatusCheckboxesContainer();
  panel.add(statusContainer, BorderLayout.CENTER);
  return panel;
  }

  /**
   * Helper method: Creates a container with all status checkboxes (Ready, Spectator, Away).
   * @return A JPanel containing all status checkboxes
   */
  private JPanel createStatusCheckboxesContainer() {
  JPanel container = new JPanel();
  container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
  container.setOpaque(false);
  container.setBorder(new EmptyBorder(10, 10, 10, 10));
  
  // Add Ready checkbox
  JCheckBox chkReady = createReadyCheckbox();
  container.add(chkReady);
  container.add(Box.createVerticalStrut(5));

  // Add Spectator checkbox
  chkSpectator = createSpectatorCheckbox();
  container.add(chkSpectator);
  container.add(Box.createVerticalStrut(5));

  // Add Away checkbox
  chkAway = createAwayCheckbox();
  container.add(chkAway);
  
  return container;
  }

  /**
   * Helper method: Creates the Ready checkbox.
   * When checked, marks the player as ready to start the game.
   * @return A styled JCheckBox for Ready status
   */
  private JCheckBox createReadyCheckbox() {
  JCheckBox chkReady = new JCheckBox("Ready");
  chkReady.setOpaque(false);
  chkReady.setForeground(new Color(30, 60, 120));
  TextFX.setSubtitleFont(chkReady);
  chkReady.addActionListener(e -> {
      if (chkReady.isSelected() && !isReady) {
          readyClicked();
      }
  });
  return chkReady;
  }

  /**
   * Helper method: Creates the Spectator checkbox.
   * When checked, marks the player as a spectator (can watch but not play).
   * @return A styled JCheckBox for Spectator status
   */
  private JCheckBox createSpectatorCheckbox() {
  JCheckBox box = new JCheckBox("Spectator");
  box.setOpaque(false);
  box.setForeground(new Color(30, 60, 120));
  box.addActionListener(e -> toggleSpectator());
  TextFX.setSubtitleFont(box);
  return box;
  }

  /**
   * Helper method: Creates the Away checkbox.
   * When checked, marks the player as away (skipped in rounds but still in game).
   * @return A styled JCheckBox for Away status
   */
  private JCheckBox createAwayCheckbox() {
  JCheckBox box = new JCheckBox("Away");
  box.setOpaque(false);
  box.setForeground(new Color(30, 60, 120));
  box.addActionListener(e -> toggleAway());
  TextFX.setSubtitleFont(box);
  return box;
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
  // USER PANEL (PLAYERS LIST)
  // ===========================================================
  /**
   * Creates the players list panel that shows all connected players with their points and status.
   * @return A JPanel containing the user list
   */
  private JPanel buildUserPanel() {
  // Create the main panel
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Players"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  // Create and setup the user list
  createUserList();
  setupUserListRenderer();

  // Add the list to a scroll pane and add to panel
  panel.add(new JScrollPane(lstUsers), BorderLayout.CENTER);
  panel.setPreferredSize(new Dimension(290, 260));
  return panel;
  }

  /**
   * Helper method: Creates the user list component.
   * This list will display all players with their IDs, names, points, and status.
   */
  private void createUserList() {
  userModel = new DefaultListModel<>();
  lstUsers = new JList<>(userModel);
  lstUsers.setBackground(Color.WHITE);
  lstUsers.setForeground(new Color(30, 60, 120));
  lstUsers.setBorder(new EmptyBorder(5, 5, 5, 5));
  }

  /**
   * Helper method: Sets up how each user is displayed in the list.
   * This custom renderer formats each user with their name, ID, points, and status indicators.
   */
  private void setupUserListRenderer() {
  lstUsers.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(
              JList<?> list, Object value, int index,
              boolean isSelected, boolean cellHasFocus) {

          // Get the default label from parent class
          JLabel lbl = (JLabel) super.getListCellRendererComponent(
                  list, value, index, isSelected, cellHasFocus);

          // If the value is a User object, format it for display
          if (value instanceof User u) {
              lbl.setText(u.toDisplayString());
              lbl.setBackground(Color.WHITE);
              lbl.setForeground(new Color(30, 60, 120));
          }
          return lbl;
      }
  });
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

  /**
   * Creates the chat panel where players can send messages to each other.
   * @return A JPanel containing chat display and input
   */
  private JPanel buildChatPanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Chat"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  // Create chat display area
  createChatDisplay();
  panel.add(new JScrollPane(txtChat), BorderLayout.CENTER);

  // Create chat input area
  JPanel inputPanel = createChatInputPanel();
  panel.add(inputPanel, BorderLayout.SOUTH);
  
  return panel;
  }

  /**
   * Helper method: Creates the chat text area for displaying messages.
   */
  private void createChatDisplay() {
  txtChat = new JTextArea();
  txtChat.setEditable(false);
  txtChat.setLineWrap(true);
  txtChat.setWrapStyleWord(true);
  txtChat.setBackground(Color.WHITE);
  txtChat.setForeground(new Color(30, 30, 30));
  txtChat.setBorder(new EmptyBorder(5, 5, 5, 5));
  }

  /**
   * Helper method: Creates the chat input panel with text field and send button.
   * @return A JPanel containing the input field and send button
   */
  private JPanel createChatInputPanel() {
  JPanel input = new JPanel(new BorderLayout(5, 5));
  input.setOpaque(false);

  // Create chat input field
  txtChatInput = new JTextField("Type message...");
  txtChatInput.setBackground(Color.WHITE);
  txtChatInput.setForeground(new Color(100, 100, 100));
  txtChatInput.setBorder(new LineBorder(new Color(200, 200, 200), 1));
  txtChatInput.addActionListener(e -> sendChat()); // Enter key sends message
  
  // Create send button
  btnSendChat = new JButton("SEND");
  styleSecondary(btnSendChat);
  btnSendChat.addActionListener(e -> sendChat());

  input.add(txtChatInput, BorderLayout.CENTER);
  input.add(btnSendChat, BorderLayout.EAST);
  
  return input;
  }

  /**
   * Creates the game events panel that shows game-related messages and updates.
   * @return A JPanel containing the events display area
   */
  private JPanel buildEventsPanel() {
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout());
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Game Events"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  // Create events display area
  createEventsDisplay();
  panel.add(new JScrollPane(txtEvents), BorderLayout.CENTER);
  panel.setPreferredSize(new Dimension(260, 220));
  
  return panel;
  }

  /**
   * Helper method: Creates the game events text area for displaying game messages.
   */
  private void createEventsDisplay() {
  txtEvents = new JTextArea();
  txtEvents.setEditable(false);
  txtEvents.setLineWrap(true);
  txtEvents.setWrapStyleWord(true);
  txtEvents.setBackground(Color.WHITE);
  txtEvents.setForeground(new Color(30, 30, 30));
  txtEvents.setBorder(new EmptyBorder(5, 5, 5, 5));
  }

  // ===========================================================
  // GAME PANEL
  // ===========================================================
  /**
   * Creates the main game panel that displays questions, answers, category, and timer.
   * This is the central area where players see and answer trivia questions.
   * @return A JPanel containing all game display elements
   */
  private JPanel buildGamePanel() {
  // Create the main panel
  JPanel panel = new JPanel();
  panel.setLayout(new BorderLayout(10, 10));
  panel.setBackground(Color.WHITE);
  panel.setBorder(new TitledBorder(new LineBorder(new Color(200, 200, 200), 1), "Question"));
  ((TitledBorder) panel.getBorder()).setTitleFont(TextFX.getSubtitleFont());
  ((TitledBorder) panel.getBorder()).setTitleColor(new Color(30, 60, 120));

  // Add category and timer at the top
  JPanel topPanel = createCategoryTimerPanel();
  panel.add(topPanel, BorderLayout.NORTH);

  // Add question display in the center
  JScrollPane questionDisplay = createQuestionDisplay();
  panel.add(questionDisplay, BorderLayout.CENTER);

  // Add answer buttons and submit button at the bottom
  JPanel bottomContainer = createAnswerButtonsContainer();
  panel.add(bottomContainer, BorderLayout.SOUTH);
  
  return panel;
  }

  /**
   * Helper method: Creates the top panel showing category and timer.
   * @return A JPanel with category label on left and timer on right
   */
  private JPanel createCategoryTimerPanel() {
  JPanel top = new JPanel(new BorderLayout());
  top.setOpaque(false);

  // Create category label
  lblCategory = new JLabel("Category: -");
  lblCategory.setForeground(new Color(30, 100, 200));
  TextFX.setSubtitleFont(lblCategory);

  // Create timer label
  lblTimer = new JLabel("Timer: -", SwingConstants.RIGHT);
  lblTimer.setForeground(new Color(30, 100, 200));
  TextFX.setSubtitleFont(lblTimer);

  // Add to panel
  top.add(lblCategory, BorderLayout.WEST);
  top.add(lblTimer, BorderLayout.EAST);
  return top;
  }

  /**
   * Helper method: Creates the question display area.
   * This is a scrollable text area that shows the current trivia question.
   * @return A JScrollPane containing the question text area
   */
  private JScrollPane createQuestionDisplay() {
  // Create text area for question
  txtQuestion = new JTextArea("Question appears here.");
  txtQuestion.setEditable(false);
  txtQuestion.setLineWrap(true);
  txtQuestion.setWrapStyleWord(true);
  txtQuestion.setOpaque(true);
  txtQuestion.setBackground(Color.WHITE);
  txtQuestion.setForeground(new Color(30, 60, 120));
  txtQuestion.setFont(TextFX.getSubtitleFont().deriveFont(Font.BOLD, 18f));
  txtQuestion.setBorder(new EmptyBorder(40, 30, 40, 30));
  
  // Wrap in scroll pane for long questions
  JScrollPane scroll = new JScrollPane(txtQuestion);
  scroll.setOpaque(false);
  scroll.getViewport().setOpaque(false);
  scroll.setBorder(null);
  scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
  
  return scroll;
  }

  /**
   * Helper method: Creates the container with answer buttons and submit button.
   * @return A JPanel containing 4 answer buttons and a submit button
   */
  private JPanel createAnswerButtonsContainer() {
  // Create panel for answer buttons (2x2 grid)
  JPanel answers = createAnswerButtons();
  
  // Create submit button
  JButton btnSubmit = createSubmitButton();
  JPanel submitPanel = new JPanel();
  submitPanel.setOpaque(false);
  submitPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
  submitPanel.add(btnSubmit);
  
  // Combine answers and submit in a vertical container
  JPanel container = new JPanel();
  container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
  container.setOpaque(false);
  container.add(answers);
  container.add(submitPanel);
  
  return container;
  }

  /**
   * Helper method: Creates the 4 answer buttons in a 2x2 grid.
   * @return A JPanel with 4 answer buttons
   */
  private JPanel createAnswerButtons() {
  JPanel answers = new JPanel(new GridLayout(2, 2, 10, 10));
  answers.setOpaque(false);
  answers.setBorder(new EmptyBorder(20, 40, 10, 40));

  // Create 4 answer buttons
  for (int i = 0; i < 4; i++) {
      int idx = i; // Store index for lambda
      JButton btn = new JButton("Answer " + (i + 1));
      styleAnswer(btn);
      btn.addActionListener(e -> answerClicked(idx));
      answerButtons[i] = btn;
      answers.add(btn);
  }
  return answers;
  }

  /**
   * Helper method: Creates the submit button.
   * Note: Answer submission is actually handled when clicking answer buttons.
   * @return A styled JButton for submitting answers
   */
  private JButton createSubmitButton() {
  JButton btn = new JButton("SUBMIT");
  stylePrimary(btn);
  btn.addActionListener(e -> {
      // Submit is handled by answer selection, but keep for UI consistency
  });
  return btn;
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

  /**
   * Handles MESSAGE payloads from the server.
   * Separates chat messages (prefixed with [CHAT]) from game events.
   * @param p The message payload
   */
  private void handleMessagePayload(Payload p) {
  if (p.getMessage() == null) return;

  String msg = p.getMessage();

  // Check if this is a chat message (prefixed with [CHAT])
  if (msg.startsWith("[CHAT] ")) {
      // Remove [CHAT] prefix and show in chat panel
      appendChat(msg.substring(7));
  } else {
      // This is a game event, show in events panel
      appendEvent(msg);
      
      // Handle special game events
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

  /**
   * Sends a chat message to the server.
   * Spectators cannot send chat messages.
   */
  private void sendChat() {
  if (!connected) return;
  
  // Spectators cannot chat
  if (isSpectator) {
  appendEvent("Spectators cannot chat.");
  return;
  }

  // Get message from input field
  String msg = txtChatInput.getText().trim();
  if (msg.isEmpty()) return;

  // Create payload and send to server
  Payload p = new Payload();
  p.setPayloadType(PayloadType.MESSAGE);
  p.setClientId(clientId);
  p.setMessage(msg);
  send(p);

  // Clear the input field
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

  /**
   * Sends a command to the server (commands start with /).
   * Examples: /ready, /answer 0, /away, /spectate, /categories geography,science
   * @param cmd The command string (e.g., "/ready" or "/answer 2")
   */
  private void sendCommand(String cmd) {
  if (!connected) return;

  // Commands are sent as MESSAGE payloads
  // The server checks if the message starts with / and handles it as a command
  Payload p = new Payload();
  p.setPayloadType(PayloadType.MESSAGE);
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

  /**
   * Helper method: Sends a payload to the server.
   * @param p The payload to send
   */
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
  /**
   * Helper method: Creates a styled label with dark blue text.
   * @param text The text to display on the label
   * @return A styled JLabel
   */
  private JLabel label(String text) {
  JLabel lbl = new JLabel(text);
  lbl.setForeground(new Color(30, 60, 120));
  TextFX.setSubtitleFont(lbl);
  return lbl;
  }

  /**
   * Helper method: Styles primary buttons (like CONNECT, SUBMIT).
   * Primary buttons are light blue with white text.
   * @param btn The button to style
   */
  private void stylePrimary(JButton btn) {
  btn.setBackground(new Color(100, 150, 255));
  btn.setForeground(Color.WHITE);
  btn.setBorder(new LineBorder(new Color(80, 130, 235), 1, true));
  TextFX.setSubtitleFont(btn);
  btn.setFont(TextFX.getSubtitleFont().deriveFont(Font.BOLD, 14f));
  btn.setPreferredSize(new Dimension(150, 40));
  }

  /**
   * Helper method: Styles secondary buttons (like SEND).
   * Secondary buttons are smaller light blue buttons.
   * @param btn The button to style
   */
  private void styleSecondary(JButton btn) {
  btn.setBackground(new Color(100, 150, 255));
  btn.setForeground(Color.WHITE);
  btn.setBorder(new LineBorder(new Color(80, 130, 235), 1, true));
  TextFX.setSubtitleFont(btn);
  btn.setFont(TextFX.getSubtitleFont().deriveFont(Font.BOLD, 12f));
  btn.setPreferredSize(new Dimension(80, 30));
  }

  /**
   * Helper method: Styles answer buttons.
   * Answer buttons are white with dark blue text and borders.
   * @param btn The button to style
   */
  private void styleAnswer(JButton btn) {
  btn.setBackground(Color.WHITE);
  btn.setForeground(new Color(30, 60, 120));
  btn.setBorder(new LineBorder(new Color(200, 200, 200), 1, true));
  TextFX.setSubtitleFont(btn);
  btn.setFont(TextFX.getSubtitleFont().deriveFont(Font.PLAIN, 14f));
  btn.setPreferredSize(new Dimension(250, 40));
  }

  /**
   * Helper method: Adds a message to the game events panel.
   * Automatically scrolls to the bottom to show the latest message.
   * @param msg The message to display
   */
  private void appendEvent(String msg) {
  txtEvents.append(msg + "\n");
  txtEvents.setCaretPosition(txtEvents.getDocument().getLength());
  }

  /**
   * Helper method: Adds a message to the chat panel.
   * Automatically scrolls to the bottom to show the latest message.
   * @param msg The chat message to display
   */
  private void appendChat(String msg) {
  txtChat.append(msg + "\n");
  txtChat.setCaretPosition(txtChat.getDocument().getLength());
  }

  }

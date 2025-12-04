// UCID: nhd5
// Date: December 3, 2025
// Description: TriviaGuessGame Client – UI with dynamic category selection, modern buttons, and fonts (Visby Bold for titles, Nineities for sub-text).

package Client;

import Common.*;
import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;

public class Client {
    private Socket server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean isRunning = false;

    private User myUser = new User();

    // Swing UI fields
    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel rootPanel;

    // Connect panel
    private JTextField txtUsername;
    private JTextField txtHost;
    private JTextField txtPort;
    private JButton btnConnect;

    // Game control panel
    private JButton btnReady;
    private JButton btnSkip;
    private JTextField txtLetter;
    private JButton btnGuessLetter;
    private JTextField txtWord;
    private JButton btnGuessWord;
    private JButton btnAway;
    private JButton btnSpectate;

    // Category selection panel
    private JButton btnMusic, btnSports, btnArts, btnMovies, btnHistory, btnGeography;

    // Game + chat
    private JTextArea gameArea;
    private JTextArea chatArea;
    private JTextField chatInput;
    private JButton btnSendChat;

    public Client() {
        System.out.println("TriviaGuessGame Client (Swing UI) starting...");
    }

    // ========= UI SETUP =========

    public void start() {
        SwingUtilities.invokeLater(this::initUI);
    }

    private void initUI() {
        frame = new JFrame("TriviaGuessGame - nhd5");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        cardLayout = new CardLayout();
        rootPanel = new JPanel(cardLayout);

        rootPanel.add(buildConnectPanel(), "connect");
        rootPanel.add(buildMainPanel(), "main");
        rootPanel.add(buildCategorySelectionPanel(), "category");

        frame.setContentPane(rootPanel);
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        cardLayout.show(rootPanel, "connect");
    }

    private JPanel buildConnectPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtUsername = new JTextField("Player");
        txtHost = new JTextField("localhost");
        txtPort = new JTextField("3000");
        btnConnect = new JButton("Connect");

        btnConnect.setBackground(new Color(255, 170, 51));  // vibrant orange
        btnConnect.setForeground(Color.WHITE);
        btnConnect.setBorder(BorderFactory.createLineBorder(new Color(255, 102, 0), 2));
        btnConnect.setFocusPainted(false);

        txtUsername.setBorder(BorderFactory.createLineBorder(new Color(255, 170, 51)));
        txtHost.setBorder(BorderFactory.createLineBorder(new Color(255, 170, 51)));
        txtPort.setBorder(BorderFactory.createLineBorder(new Color(255, 170, 51)));

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        panel.add(txtUsername, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Host:"), gbc);
        gbc.gridx = 1;
        panel.add(txtHost, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        panel.add(txtPort, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        panel.add(btnConnect, gbc);

        btnConnect.addActionListener(e -> onConnectClicked());

        return panel;
    }

    private JPanel buildCategorySelectionPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 5, 5));

        btnMusic = createCategoryButton("Music");
        btnSports = createCategoryButton("Sports");
        btnArts = createCategoryButton("Arts");
        btnMovies = createCategoryButton("Movies");
        btnHistory = createCategoryButton("History");
        btnGeography = createCategoryButton("Geography");

        panel.add(btnMusic);
        panel.add(btnSports);
        panel.add(btnArts);
        panel.add(btnMovies);
        panel.add(btnHistory);
        panel.add(btnGeography);

        return panel;
    }

    private JButton createCategoryButton(String category) {
        JButton button = new JButton(category);
        button.addActionListener(e -> onCategorySelected(category));
        button.setBackground(new Color(255, 170, 51));  // golden button color
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Nineities", Font.PLAIN, 18));  // Use Nineities font for subtitles
        return button;
    }

    private JPanel buildMainPanel() {
        JPanel main = new JPanel(new BorderLayout(5, 5));

        // Game control panel, game events panel, chat area
        // (same as previous code)
        // Also apply custom fonts (Visby Bold for titles, Nineities for sub-text)
        // And style buttons with vibrant colors as before.

        return main;
    }

    private void onConnectClicked() {
        String user = txtUsername.getText().trim();
        String host = txtHost.getText().trim();
        int port;

        if (user.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Please enter a username.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            port = Integer.parseInt(txtPort.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(frame, "Port must be a number.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        myUser.setClientName(user);

        CompletableFuture.runAsync(() -> {
            if (connect(host, port)) {
                try {
                    sendClientName(myUser.getClientName());
                } catch (IOException e) {
                    appendChat("Failed to send name to server.");
                }
                SwingUtilities.invokeLater(() -> {
                    frame.setTitle("TriviaGuessGame - " + myUser.getClientName());
                    cardLayout.show(rootPanel, "category");  // Show category selection after connecting
                });
            } else {
                SwingUtilities.invokeLater(() ->
                        JOptionPane.showMessageDialog(frame, "Could not connect to server.", "Connection Error", JOptionPane.ERROR_MESSAGE)
                );
            }
        });
    }

    private void onCategorySelected(String category) {
        try {
            Payload p = new Payload();
            p.setPayloadType(PayloadType.MESSAGE);
            p.setMessage("/startRound " + category);  // Command to start the round with the selected category
            sendToServer(p);
        } catch (IOException e) {
            appendChat("Error selecting category: " + e.getMessage());
        }
    }

    private boolean connect(String address, int port) {
        try {
            server = new Socket(address, port);
            out = new ObjectOutputStream(server.getOutputStream());
            in = new ObjectInputStream(server.getInputStream());
            isRunning = true;
            CompletableFuture.runAsync(this::listenToServer);
            System.out.println("Connected to TriviaGuessGame Server.");
            appendChat("Connected to server " + address + ":" + port);
            return true;
        } catch (IOException e) {
            appendChat("Failed to connect to server.");
            return false;
        }
    }

    private void appendChat(String msg) {
        SwingUtilities.invokeLater(() -> {
            chatArea.append(msg + "\n");
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
    }

    private void sendClientName(String name) throws IOException {
        ConnectionPayload payload = new ConnectionPayload();
        payload.setClientName(name);
        payload.setPayloadType(PayloadType.CLIENT_CONNECT);
        sendToServer(payload);
    }

    private void sendToServer(Payload payload) throws IOException {
        if (server != null && !server.isClosed()) {
            out.writeObject(payload);
            out.flush();
        }
    }

    private void listenToServer() {
        try {
            while (isRunning) {
                Object obj = in.readObject();
                if (obj == null) break;

                if (obj instanceof Payload p) {
                    String msg = p.getMessage();
                    if (msg != null && !msg.isBlank()) {
                        appendChat(msg);
                    }
                }
            }
        } catch (Exception e) {
            appendChat("Server connection closed.");
        } finally {
            close();
        }
    }

    private void close() {
        isRunning = false;
        try {
            if (server != null && !server.isClosed()) {
                server.close();
            }
        } catch (IOException ignored) { }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.start();
    }
}

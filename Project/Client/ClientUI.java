package Client;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import Client.Interfaces.ICardControls;
import Client.Interfaces.IConnectionEvents;
import Client.Interfaces.IRoomEvents;
import Client.Views.ChatGameView;
import Client.Views.ConnectionView;
import Client.Views.MenuBar;
import Client.Views.RoomsView;
import Client.Views.UserDetailsView;
import Common.Constants;
import Common.LoggerUtil;

public class ClientUI extends JFrame implements ICardControls, IConnectionEvents, IRoomEvents {
    private CardLayout cardLayout = new CardLayout();
    private Container frameContainer;
    private JPanel cardContainer;
    private JPanel activeCardViewPanel;
    private CardViewName activeCardViewEnum;
    private String originalTitle = "";
    private JMenuBar menuBar;
    private JLabel currentRoomLabel = new JLabel(Constants.NOT_CONNECTED);
    private ConnectionView connectionView;
    private UserDetailsView userDetailsView;
    private ChatGameView chatGameView;
    private RoomsView roomsView;

    // Rooms "popup" window (instead of always-on panel)
    private JDialog roomsDialog;
    private JTextArea roomsTextArea;
    private JTextField roomNameField;
    {
        LoggerUtil.LoggerConfig config = new LoggerUtil.LoggerConfig();
        config.setFileSizeLimit(2048 * 1024); // 2MB
        config.setFileCount(1);
        config.setLogLocation("client-ui.log");
        LoggerUtil.INSTANCE.setConfig(config);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {

            try {

                new ClientUI("nhd5-Client");

            } catch (Throwable t) {
                LoggerUtil.INSTANCE.severe("Unhandled exception in main thread", t);
            }
        });

    }

    public ClientUI(String title) {
        super(title);
        originalTitle = title;
        setMinimumSize(new Dimension(400, 400));
        setSize(getMinimumSize());
        setLocationRelativeTo(null);
        Client.INSTANCE.registerCallback(this);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                int response = JOptionPane.showConfirmDialog(cardContainer,
                        "Are you sure you want to close this window?", "Close Window?",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (response == JOptionPane.YES_OPTION) {
                    try {
                        Client.INSTANCE.sendDisconnect();
                    } catch (NullPointerException | IOException e) {
                        LoggerUtil.INSTANCE.severe("Error during disconnect: " + e.getMessage());
                    }
                    System.exit(0);
                }
            }
        });

        menuBar = new MenuBar(this);
        this.setJMenuBar(menuBar);

        frameContainer = getContentPane();

        cardContainer = new JPanel();
        cardContainer.setLayout(cardLayout);

        frameContainer.add(currentRoomLabel, BorderLayout.NORTH);
        frameContainer.add(cardContainer, BorderLayout.CENTER);

        connectionView = new ConnectionView(this);
        userDetailsView = new UserDetailsView(this);
        chatGameView = new ChatGameView(this);
        roomsView = new RoomsView(this);
        roomsView.setVisible(false);
        initRoomsDialog();

        showView(CardViewName.CONNECT);
        pack();
        setVisible(true);
    }

    private void initRoomsDialog() {
        roomsDialog = new JDialog(this, "Rooms", false);
        roomsDialog.setMinimumSize(new Dimension(360, 360));
        roomsDialog.setSize(420, 420);
        roomsDialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        roomsTextArea = new JTextArea();
        roomsTextArea.setEditable(false);
        root.add(new JScrollPane(roomsTextArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        roomNameField = new JTextField();
        roomNameField.setToolTipText("Room name");
        bottom.add(roomNameField, BorderLayout.CENTER);

        JPanel buttons = new JPanel();
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(_ -> requestRoomsList());
        JButton create = new JButton("Create");
        create.addActionListener(_ -> createRoomFromField());
        JButton join = new JButton("Join");
        join.addActionListener(_ -> joinRoomFromField());
        buttons.add(refresh);
        buttons.add(create);
        buttons.add(join);
        bottom.add(buttons, BorderLayout.EAST);

        root.add(bottom, BorderLayout.SOUTH);
        roomsDialog.setContentPane(root);
    }

    private void requestRoomsList() {
        try {
            Client.INSTANCE.sendMessage("/listrooms");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error requesting rooms: " + e.getMessage());
        }
    }

    private void createRoomFromField() {
        String name = roomNameField.getText() == null ? "" : roomNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a room name to create.");
            return;
        }
        try {
            Client.INSTANCE.sendMessage("/createroom " + name);
            requestRoomsList();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error creating room: " + e.getMessage());
        }
    }

    private void joinRoomFromField() {
        String name = roomNameField.getText() == null ? "" : roomNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter a room name to join.");
            return;
        }
        try {
            Client.INSTANCE.sendMessage("/joinroom " + name);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error joining room: " + e.getMessage());
        }
    }

    public void showRoomsDialog() {
        if (!roomsDialog.isVisible()) {
            roomsDialog.setLocationRelativeTo(this);
            roomsDialog.setVisible(true);
        }
        requestRoomsList();
    }

    private void findAndSetCurrentView() {
        Component panel = List.of(cardContainer.getComponents()).stream().filter(Component::isVisible)
                .findFirst().orElseThrow();
        if (panel != null) {
            activeCardViewPanel = (JPanel) panel;
            activeCardViewEnum = Enum.valueOf(CardViewName.class, activeCardViewPanel.getName());

            if (!Client.INSTANCE.isMyClientIdSet() && CardViewName.viewRequiresConnection(activeCardViewEnum)) {
                showView(CardViewName.CONNECT.name());
                setSize(getMinimumSize());
                revalidate();
            }
        }
        LoggerUtil.INSTANCE.fine("Current View: " + activeCardViewPanel.getName());
    }

    @Override
    public void nextView() {
        cardLayout.next(cardContainer);
        findAndSetCurrentView();
    }

    @Override
    public void previousView() {
        cardLayout.previous(cardContainer);
        findAndSetCurrentView();
    }

    @Override
    public void showView(String viewName) {
        cardLayout.show(cardContainer, viewName);
        findAndSetCurrentView();
    }

    @Override
    public void showView(CardViewName viewEnum) {
        showView(viewEnum.name());
    }

    @Override
    public void registerView(String viewName, JPanel panelView) {
        cardContainer.add(panelView, viewName);
    }

    @Override
    public void connect() {
        String username = connectionView.getUsername();
        String host = connectionView.getHost();
        int port = connectionView.getPort();
        setTitle(String.format("%s - %s", originalTitle, username));

        Client.INSTANCE.connect(host, port, username);
    }

    @Override
    public void onClientDisconnect(long clientId) {
        if (!CardViewName.viewRequiresConnection(activeCardViewEnum)) {
            LoggerUtil.INSTANCE.warning("Received onClientDisconnect while in a view prior to CHAT");
            return;
        }

        if (Client.INSTANCE.isMyClientId(clientId)) {
            currentRoomLabel.setText(Constants.NOT_CONNECTED);
            showView(CardViewName.CONNECT);
        }
    }

    @Override
    public void onReceiveClientId(long clientId) {
        LoggerUtil.INSTANCE.fine("Received client id: " + clientId);
        showView(CardViewName.CHAT_GAME_SCREEN);
        chatGameView.showChatOnlyView();
        // Start in lobby with a reasonable default size; user can resize as needed.
        setSize(new Dimension(520, 520));
        revalidate();
        // Bring up the Rooms popup so the user can create/join first.
        SwingUtilities.invokeLater(this::showRoomsDialog);
    }

    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        LoggerUtil.INSTANCE.fine(String.format("onRoomAction: clientId=%d, roomName=%s, isJoin=%b, isQuiet=%b",
                clientId, roomName, isJoin, isQuiet));
        if (Client.INSTANCE.isMyClientId(clientId) && isJoin) {
            currentRoomLabel.setText(String.format("Room: %s", roomName));
            // If we've successfully joined a game room, hide the rooms popup.
            if (roomName != null && !"lobby".equalsIgnoreCase(roomName) && roomsDialog != null) {
                roomsDialog.setVisible(false);
            }
        }
    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        StringBuilder builder = new StringBuilder();
        if (message != null && !message.isBlank()) {
            builder.append(message).append(System.lineSeparator());
        }
        if (rooms != null && !rooms.isEmpty()) {
            rooms.forEach(r -> builder.append(r).append(System.lineSeparator()));
        } else {
            builder.append("No rooms available");
        }
        String text = builder.toString().trim();
        if (roomsTextArea != null) {
            roomsTextArea.setText(text);
        }
        // Auto-pop the rooms window when we receive a rooms list.
        if (roomsDialog != null && !roomsDialog.isVisible()) {
            roomsDialog.setLocationRelativeTo(this);
            roomsDialog.setVisible(true);
        }
    }

}

package Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;

import Client.CardViewName;
import Client.Client;
import Client.Interfaces.ICardControls;
import Client.Interfaces.IPhaseEvent;
import Client.Interfaces.IRoomEvents;
import Common.Constants;
import Common.Phase;

public class ChatGameView extends JPanel implements IRoomEvents, IPhaseEvent {
    private final ChatView chatView;
    private final GameView gameView;
    private final JSplitPane splitPane;

    public ChatGameView(ICardControls controls) {
        super();
        setLayout(new BorderLayout());

        setName(CardViewName.CHAT_GAME_SCREEN.name());
        controls.registerView(CardViewName.CHAT_GAME_SCREEN.name(), this);

        chatView = new ChatView(controls);
        gameView = new GameView(controls);
        gameView.setVisible(false);
        gameView.setBackground(Color.BLUE);
        chatView.setBackground(Color.GRAY);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gameView, chatView);
        splitPane.setResizeWeight(0.6);
        splitPane.setOneTouchExpandable(false);
        // Keep the split pane enabled so the chat panel can receive focus/clicks.
        // If you don't want users dragging the divider, make it non-draggable instead.
        splitPane.setDividerSize(0);

        add(splitPane, BorderLayout.CENTER);
        gameView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.6);
            }
        });
        showChatOnlyView();
        Client.INSTANCE.registerCallback(this);
    }

    public void showGameView() {
        gameView.setVisible(true);
        splitPane.setDividerLocation(0.6);
        // When the game view is visible, you may want a divider. Keep it non-draggable by default.
        // Avoid duplicate player lists (GameView already shows Players on the right).
        chatView.setShowUserList(false);
    }

    public void showChatOnlyView() {
        gameView.setVisible(false);
        chatView.setVisible(true);
        // With a horizontal split (left=game, right=chat), a divider location near 0
        // gives almost all space to the chat panel.
        splitPane.setDividerLocation(0.0);
        revalidate();
        repaint();
        // Ensure the lobby chat input is ready to type into immediately.
        SwingUtilities.invokeLater(() -> chatView.focusInput());
        // In lobby, show the Players list next to chat.
        chatView.setShowUserList(true);

    }

    @Override
    public void onReceiveRoomList(List<String> rooms, String message) {
        // unused
    }

    @Override
    public void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet) {
        if (isJoin && Constants.LOBBY.equalsIgnoreCase(roomName)) {
            showChatOnlyView();
        }
        // Entering a game room should reveal the game/ready panels
        if (isJoin && !Constants.LOBBY.equalsIgnoreCase(roomName)) {
            showGameView();
        }

    }

    @Override
    public void onReceivePhase(Phase phase) {
        showGameView();

    }
}

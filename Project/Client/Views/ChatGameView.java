package Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

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
        splitPane.setEnabled(false);

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
    }

    public void showChatOnlyView() {
        gameView.setVisible(false);
        chatView.setVisible(true);
        splitPane.setDividerLocation(1.0);
        revalidate();
        repaint();

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

    }

    @Override
    public void onReceivePhase(Phase phase) {
        showGameView();

    }
}

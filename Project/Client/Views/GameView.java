package Client.Views;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;
import javax.swing.JSplitPane;

import Client.CardViewName;
import Client.Client;
import Client.Interfaces.ICardControls;
import Client.Interfaces.IPhaseEvent;
import Client.Interfaces.IUserListEvent;
import Common.Phase;
import Common.UserListPayload;

public class GameView extends JPanel implements IPhaseEvent, IUserListEvent {
    private PlayView playView;
    private CardLayout cardLayout;
    private static final String READY_PANEL = "READY";
    private static final String PLAY_PANEL = "PLAY";
    private final JSplitPane splitPane;

    public GameView(ICardControls controls) {
        super(new BorderLayout());

        JPanel gameContainer = new JPanel(new CardLayout());
        cardLayout = (CardLayout) gameContainer.getLayout();
        this.setName(CardViewName.GAME_SCREEN.name());
        Client.INSTANCE.registerCallback(this);

        ReadyView readyView = new ReadyView();
        readyView.setName(READY_PANEL);
        gameContainer.add(READY_PANEL, readyView);

        playView = new PlayView(PLAY_PANEL);
        gameContainer.add(PLAY_PANEL, playView);

        GameEventsView gameEventsView = new GameEventsView();
        // Put the game events pane on the right (where the old Players panel was).
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, gameContainer, gameEventsView);
        splitPane.setResizeWeight(0.72);

        playView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.72);
            }
        });
        playView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                playView.revalidate();
                playView.repaint();
            }
        });

        this.add(splitPane, BorderLayout.CENTER);

        controls.registerView(CardViewName.GAME_SCREEN.name(), this);
        setVisible(false);
    }

    @Override
    public void onReceivePhase(Phase phase) {
        if (phase == Phase.READY) {
            cardLayout.show(playView.getParent(), READY_PANEL);
        } else if (phase == Phase.IN_PROGRESS) {
            cardLayout.show(playView.getParent(), PLAY_PANEL);
        }
        playView.changePhase(phase);
    }

    @Override
    public void onUserListUpdate(UserListPayload payload) {
        // Players list is now rendered compactly under the timer in PlayView.
    }

}

package Client.Views;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;

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
    private final JEditorPane userListArea = new JEditorPane();

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
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, gameContainer, gameEventsView);
        splitPane.setResizeWeight(0.7);

        playView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                splitPane.setDividerLocation(0.7);
            }
        });
        playView.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                playView.revalidate();
                playView.repaint();
            }
        });

        // User list shown during gameplay (lobby and in-session)
        userListArea.setEditable(false);
        userListArea.setContentType("text/html");
        userListArea.setBorder(BorderFactory.createTitledBorder("Players"));
        JScrollPane userScroll = new JScrollPane(userListArea);
        userScroll.setPreferredSize(new Dimension(180, 100));

        this.add(splitPane, BorderLayout.CENTER);
        this.add(userScroll, BorderLayout.EAST);

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
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        for (int i = 0; i < payload.getClientIds().size(); i++) {
            String name = payload.getDisplayNames().get(i);
            long id = payload.getClientIds().get(i);
            int pts = payload.getPoints().get(i);
            boolean locked = payload.getLockedIn().get(i);
            boolean isAway = payload.getAway().get(i);
            boolean isSpectator = payload.getSpectator().get(i);
            
            // Build display string with visual indicators
            String displayName = name;
            String style = "";
            
            if (isAway) {
                style = "color:gray; font-style:italic;";
                displayName = "[AWAY] " + displayName;
            }
            if (isSpectator) {
                displayName = "[SPECTATOR] " + displayName;
                if (style.isEmpty()) {
                    style = "color:blue;";
                }
            }
            if (locked) {
                displayName = "🔒 " + displayName;
            }
            
            sb.append("<div style='").append(style).append("'>");
            sb.append(displayName)
              .append(" — ")
              .append(pts)
              .append(" pts");
            sb.append("</div>");
        }
        sb.append("</html>");
        userListArea.setContentType("text/html");
        userListArea.setText(sb.toString());
    }

}

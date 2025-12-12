package Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import Client.Client;
import Client.Interfaces.IMessageEvents;
import Client.Interfaces.IPhaseEvent;
import Client.Interfaces.IReadyEvent;
import Client.Interfaces.ITimeEvents;
import Common.Constants;
import Common.Phase;
import Common.TimerType;

public class GameEventsView extends JPanel implements IPhaseEvent, IReadyEvent, IMessageEvents, ITimeEvents {
    private final JPanel content;
    private final boolean debugMode = false;
    private final JLabel timerText;
    private final GridBagConstraints gbcGlue = new GridBagConstraints();

    // Collect final scoreboard lines and show as a popup at game over.
    private boolean collectingFinalScores = false;
    private final StringBuilder finalScores = new StringBuilder();
    // Collect answer key lines and show as a popup at game over.
    private boolean collectingAnswerKey = false;
    private final StringBuilder answerKey = new StringBuilder();

    public GameEventsView() {
        super(new BorderLayout(10, 10));
        content = new JPanel(new GridBagLayout());

        if (debugMode) {
            content.setBorder(BorderFactory.createLineBorder(Color.RED));
            content.setBackground(new Color(240, 240, 240));
        }

        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        if (debugMode) {
            scroll.setBorder(BorderFactory.createLineBorder(Color.GREEN));
        } else {
            scroll.setBorder(BorderFactory.createEmptyBorder());
        }
        this.add(scroll, BorderLayout.CENTER);

        gbcGlue.gridx = 0;
        gbcGlue.gridy = GridBagConstraints.RELATIVE;
        gbcGlue.weighty = 1.0;
        gbcGlue.fill = GridBagConstraints.BOTH;
        content.add(Box.createVerticalGlue(), gbcGlue);

        timerText = new JLabel();
        this.add(timerText, BorderLayout.NORTH);
        timerText.setVisible(false);
        Client.INSTANCE.registerCallback(this);
    }

    public void addText(String text) {
        SwingUtilities.invokeLater(() -> {
            JEditorPane textContainer = new JEditorPane("text/plain", text);
            textContainer.setEditable(false);
            if (debugMode) {
                textContainer.setBorder(BorderFactory.createLineBorder(Color.BLUE));
                textContainer.setBackground(new Color(255, 255, 200));
            } else {
                textContainer.setBorder(BorderFactory.createEmptyBorder());
                textContainer.setBackground(new Color(0, 0, 0, 0));
            }
            textContainer.setText(text);
            int width = content.getWidth() > 0 ? content.getWidth() : 200;
            Dimension preferredSize = textContainer.getPreferredSize();
            textContainer.setPreferredSize(new Dimension(width, preferredSize.height));
            int lastIdx = content.getComponentCount() - 1;
            if (lastIdx >= 0 && content.getComponent(lastIdx) instanceof Box.Filler) {
                content.remove(lastIdx);
            }
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = content.getComponentCount() - 1;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(0, 0, 5, 0);
            content.add(textContainer, gbc);
            content.add(Box.createVerticalGlue(), gbcGlue);
            content.revalidate();
            content.repaint();
            JScrollPane parentScrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, content);
            if (parentScrollPane != null) {
                SwingUtilities.invokeLater(() -> {
                    JScrollBar vertical = parentScrollPane.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                });
            }
        });
    }

    @Override
    public void onReceivePhase(Phase phase) {
        addText(String.format("The current phase is %s", phase));
    }

    @Override
    public void onReceiveReady(long clientId, boolean isReady, boolean isQuiet) {
        if (isQuiet) {
            return;
        }
        String displayName = Client.INSTANCE.getDisplayNameFromId(clientId);
        addText(String.format("%s is %s", displayName, isReady ? "ready" : "not ready"));
    }

    @Override
    public void onMessageReceive(long id, String message) {
        if (id == Constants.GAME_EVENT_CHANNEL || id == Constants.DEFAULT_CLIENT_ID) {
            // When the server announces final scores, collect the bullet lines and pop a dialog.
            if ("Final scores:".equals(message)) {
                collectingFinalScores = true;
                finalScores.setLength(0);
                finalScores.append("Final scores:\n");
                collectingAnswerKey = false;
            } else if (collectingFinalScores) {
                if (message.startsWith(" • ")) {
                    // Keep the bullets in the popup (strip the leading space).
                    finalScores.append(message.substring(1)).append("\n");
                } else {
                    collectingFinalScores = false;
                    String text = finalScores.toString().trim();
                    if (!text.isBlank()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                this,
                                text,
                                "Game Over",
                                JOptionPane.INFORMATION_MESSAGE));
                    }
                }
            }

            // Answer key popup (revealed only at end of session)
            if ("Answer key:".equals(message)) {
                collectingAnswerKey = true;
                answerKey.setLength(0);
                answerKey.append("Answer key:\n");
            } else if (collectingAnswerKey) {
                if (message.startsWith(" • ")) {
                    answerKey.append(message.substring(1)).append("\n");
                } else {
                    collectingAnswerKey = false;
                    String text = answerKey.toString().trim();
                    if (!text.isBlank()) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                                this,
                                text,
                                "Answer Key",
                                JOptionPane.INFORMATION_MESSAGE));
                    }
                }
            }
            addText(message);
        }
    }

    @Override
    public void onTimerUpdate(TimerType timerType, int time) {
        if (time >= 0) {
            timerText.setText(String.format("%s timer: %s", timerType.name(), time));
        } else {
            timerText.setText(" ");
        }
        timerText.setVisible(true);
    }
}

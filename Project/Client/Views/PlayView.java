package Client.Views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import Client.Client;
import Client.Interfaces.IMessageEvents;
import Client.Interfaces.IQuestionEvent;
import Client.Interfaces.ITimeEvents;
import Common.Constants;
import Common.Phase;
import Common.QAPayload;
import Common.TimerType;

/**
 * PlayView shows the trivia question and answer buttons.
 */
public class PlayView extends JPanel implements IQuestionEvent, ITimeEvents, IMessageEvents {
    private final JLabel categoryLabel = new JLabel("Category: ");
    private final JLabel questionLabel = new JLabel("Question will appear here");
    private final JLabel timerLabel = new JLabel("Timer: --");
    private final JButton[] answerButtons = new JButton[4];
    private final JLabel statusLabel = new JLabel(" ");
    private List<String> currentAnswers = new ArrayList<>();
    private int lockedIndex = -1;
    private int correctAnswerIndex = -1;
    private boolean answerRevealed = false;
    private String myDisplayName = ""; // reserved if server starts sending per-user feedback

    public PlayView(String name) {
        super(new BorderLayout(5, 5));
        this.setName(name);

        JPanel questionPanel = new JPanel(new GridLayout(0, 1));
        questionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        questionPanel.add(categoryLabel);
        questionPanel.add(questionLabel);
        questionPanel.add(timerLabel);
        add(questionPanel, BorderLayout.NORTH);

        JPanel answersPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        String[] labels = { "A", "B", "C", "D" };
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            answerButtons[i] = new JButton(labels[i]);
            answerButtons[i].setEnabled(false);
            answerButtons[i].addActionListener(_ -> submitAnswer(idx));
            answersPanel.add(answerButtons[i]);
        }
        add(answersPanel, BorderLayout.CENTER);

        add(statusLabel, BorderLayout.SOUTH);

        Client.INSTANCE.registerCallback(this);
    }

    private void submitAnswer(int idx) {
        disableAnswers();
        char letter = (char) ('A' + idx);
        statusLabel.setText("Answer sent: " + letter);
        lockedIndex = idx;
        // Requirement: when user locks in, make that choice green and the rest red.
        lockInColors(idx);
        try {
            Client.INSTANCE.sendMessage("/answer " + idx);
        } catch (IOException e) {
            statusLabel.setText("Error sending answer: " + e.getMessage());
        }
    }

    private void enableAnswers() {
        for (JButton b : answerButtons) {
            b.setEnabled(true);
        }
    }

    private void disableAnswers() {
        for (JButton b : answerButtons) {
            b.setEnabled(false);
        }
    }

    private void resetAnswerStyles() {
        lockedIndex = -1;
        correctAnswerIndex = -1;
        answerRevealed = false;
        for (JButton b : answerButtons) {
            b.setBackground(UIManager.getColor("Button.background"));
            b.setEnabled(false);
        }
    }

    private void lockInColors(int idx) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < answerButtons.length; i++) {
                if (i >= currentAnswers.size()) {
                    answerButtons[i].setBackground(UIManager.getColor("Button.background"));
                    continue;
                }
                answerButtons[i].setBackground(i == idx ? Color.GREEN : Color.RED);
            }
        });
    }

    private void colorAnswerButtons(boolean isCorrect) {
        if (lockedIndex < 0) return;
        
        SwingUtilities.invokeLater(() -> {
            if (isCorrect) {
                // User clicked correct answer: green for correct, red for others
                for (int i = 0; i < answerButtons.length; i++) {
                    if (i == lockedIndex) {
                        answerButtons[i].setBackground(Color.GREEN);
                    } else if (i < currentAnswers.size()) {
                        answerButtons[i].setBackground(Color.RED);
                    }
                }
            } else {
                // User clicked wrong answer: red for wrong, wait for correct answer reveal
                answerButtons[lockedIndex].setBackground(Color.RED);
            }
        });
    }

    private void revealCorrectAnswer(int correctIdx) {
        correctAnswerIndex = correctIdx;
        answerRevealed = true;
        
        SwingUtilities.invokeLater(() -> {
            // Color correct answer green
            if (correctIdx >= 0 && correctIdx < answerButtons.length) {
                answerButtons[correctIdx].setBackground(Color.GREEN);
            }
            // Color other answers red (including user's wrong selection)
            for (int i = 0; i < answerButtons.length; i++) {
                if (i != correctIdx && i < currentAnswers.size()) {
                    answerButtons[i].setBackground(Color.RED);
                }
            }

            // Status text: correct / wrong / no answer
            if (lockedIndex < 0) {
                statusLabel.setText("No answer locked in. Correct was " + (char) ('A' + correctIdx) + ".");
            } else if (lockedIndex == correctIdx) {
                statusLabel.setText("Correct! You picked " + (char) ('A' + lockedIndex) + ".");
            } else {
                statusLabel.setText("Wrong. You picked " + (char) ('A' + lockedIndex) + "; correct was "
                        + (char) ('A' + correctIdx) + ".");
            }
        });
    }

    public void changePhase(Phase phase) {
        if (phase == Phase.READY) {
            resetAnswerStyles();
            statusLabel.setText("Waiting for next round...");
        } else if (phase == Phase.IN_PROGRESS) {
            statusLabel.setText("Game in progress");
        }
    }

    @Override
    public void onQuestion(QAPayload qa) {
        SwingUtilities.invokeLater(() -> {
            currentAnswers = qa.getAnswers();
            categoryLabel.setText("Category: " + qa.getCategory());
            questionLabel.setText("<html><body style='width:300px'>" + qa.getQuestionText() + "</body></html>");
            for (int i = 0; i < answerButtons.length; i++) {
                String text = (i < currentAnswers.size()) ? currentAnswers.get(i) : "";
                answerButtons[i].setText((char) ('A' + i) + ": " + text);
                answerButtons[i].setEnabled(i < currentAnswers.size());
            }
            resetAnswerStyles();
            enableAnswers();
            statusLabel.setText("Select an answer.");
            timerLabel.setText("Timer: --");
        });
    }

    @Override
    public void onMessageReceive(long id, String message) {
        // Listen for answer feedback and correct answer reveals
        if (id == Constants.DEFAULT_CLIENT_ID || id == Constants.GAME_EVENT_CHANNEL) {
            // Parse "Correct answer: A – Paris" format (this is the definitive answer)
            if (message.startsWith("Correct answer: ")) {
                String answerPart = message.substring("Correct answer: ".length());
                if (answerPart.length() > 0) {
                    char letter = answerPart.charAt(0);
                    int correctIdx = letter - 'A';
                    if (correctIdx >= 0 && correctIdx < 4) {
                        revealCorrectAnswer(correctIdx);
                    }
                }
            }
        }
    }

    @Override
    public void onTimerUpdate(TimerType timerType, int time) {
        if (timerType != TimerType.ROUND) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            if (time < 0) {
                timerLabel.setText("Timer: --");
            } else {
                timerLabel.setText("Timer: " + time + "s");
            }
        });
    }
}

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
    private String myDisplayName = "";

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
        highlightChoice(idx);
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

    private void highlightChoice(int idx) {
        // Initially show cyan for selected answer
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setBackground(i == idx ? Color.CYAN : UIManager.getColor("Button.background"));
        }
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
            // Color wrong answers red (if user clicked wrong)
            if (lockedIndex >= 0 && lockedIndex != correctIdx) {
                answerButtons[lockedIndex].setBackground(Color.RED);
            }
            // Color other non-selected answers red
            for (int i = 0; i < answerButtons.length; i++) {
                if (i != correctIdx && i != lockedIndex && i < currentAnswers.size()) {
                    answerButtons[i].setBackground(Color.RED);
                }
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
            // Also check for immediate feedback if we've locked in
            else if (lockedIndex >= 0 && !answerRevealed) {
                // Try to parse if message is about our answer
                // Format: "DisplayName locked in the CORRECT answer!" or "DisplayName locked in the WRONG answer!"
                // We'll check by seeing if message contains the pattern and we just locked in
                // Note: This is a heuristic - ideally server would send direct feedback
                if (message.contains("locked in the CORRECT answer")) {
                    // Could be about us - color immediately if we selected correctly
                    SwingUtilities.invokeLater(() -> {
                        Color currentBg = answerButtons[lockedIndex].getBackground();
                        if (currentBg.equals(Color.CYAN) || currentBg.getRGB() == Color.CYAN.getRGB()) {
                            // Assume this is about us if we just locked in
                            // Color our selection green, others red
                            for (int i = 0; i < answerButtons.length; i++) {
                                if (i == lockedIndex) {
                                    answerButtons[i].setBackground(Color.GREEN);
                                } else if (i < currentAnswers.size()) {
                                    answerButtons[i].setBackground(Color.RED);
                                }
                            }
                        }
                    });
                } else if (message.contains("locked in the WRONG answer")) {
                    // Color our selection red (correct will be revealed later)
                    SwingUtilities.invokeLater(() -> {
                        Color currentBg = answerButtons[lockedIndex].getBackground();
                        if (currentBg.equals(Color.CYAN) || currentBg.getRGB() == Color.CYAN.getRGB()) {
                            answerButtons[lockedIndex].setBackground(Color.RED);
                        }
                    });
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

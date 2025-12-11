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
import Client.Interfaces.IQuestionEvent;
import Client.Interfaces.ITimeEvents;
import Common.Phase;
import Common.QAPayload;
import Common.TimerType;

/**
 * PlayView shows the trivia question and answer buttons.
 */
public class PlayView extends JPanel implements IQuestionEvent, ITimeEvents {
    private final JLabel categoryLabel = new JLabel("Category: ");
    private final JLabel questionLabel = new JLabel("Question will appear here");
    private final JLabel timerLabel = new JLabel("Timer: --");
    private final JButton[] answerButtons = new JButton[4];
    private final JLabel statusLabel = new JLabel(" ");
    private List<String> currentAnswers = new ArrayList<>();
    private int lockedIndex = -1;

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
        for (JButton b : answerButtons) {
            b.setBackground(UIManager.getColor("Button.background"));
            b.setEnabled(false);
        }
    }

    private void highlightChoice(int idx) {
        for (int i = 0; i < answerButtons.length; i++) {
            answerButtons[i].setBackground(i == idx ? Color.CYAN : UIManager.getColor("Button.background"));
        }
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

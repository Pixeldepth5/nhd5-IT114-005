package Client.Views;

import java.io.IOException;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import Client.Client;
import Client.ClientUI;
import Client.Interfaces.ICardControls;
import Common.Phase;

public class MenuBar extends JMenuBar {
    public MenuBar(ICardControls controls) {
        JMenu navigation = new JMenu("Navigate");
        JMenuItem connect = new JMenuItem("Connect");
        connect.addActionListener(_ -> controls.showView("CONNECT"));
        navigation.add(connect);

        JMenuItem rooms = new JMenuItem("Rooms...");
        rooms.addActionListener(_ -> {
            if (controls instanceof ClientUI ui) {
                ui.showRoomsDialog();
            }
        });
        navigation.add(rooms);
        this.add(navigation);

        JMenu game = new JMenu("Game");

        JMenuItem addQuestion = new JMenuItem("Add Question");
        addQuestion.addActionListener(_ -> showAddQuestionDialog());
        game.add(addQuestion);

        this.add(game);
    }

    private void showAddQuestionDialog() {
        // Allowed anywhere during READY, but not while a round is in progress.
        if (Client.INSTANCE.getCurrentPhase() != Phase.READY) {
            JOptionPane.showMessageDialog(this, "You can only add questions during Ready Check (not during gameplay).");
            return;
        }

        String category = JOptionPane.showInputDialog(this,
                "Category (e.g., geography, science, math, history, movies, sports):");
        if (category == null || category.trim().isEmpty())
            return;

        String question = JOptionPane.showInputDialog(this, "Question:");
        if (question == null || question.trim().isEmpty())
            return;

        // Collect 2-4 answers
        java.util.ArrayList<String> answers = new java.util.ArrayList<>();
        String[] answerLabels = {"A", "B", "C", "D"};
        
        for (int i = 0; i < 4; i++) {
            String answer = JOptionPane.showInputDialog(this, 
                    String.format("Answer %s (leave blank to finish, minimum 2 answers required):", answerLabels[i]));
            if (answer == null) {
                // User cancelled
                if (answers.size() < 2) {
                    JOptionPane.showMessageDialog(this,
                            "You need at least 2 answers. Question not added.");
                    return;
                }
                break;
            }
            String trimmed = answer.trim();
            if (trimmed.isEmpty()) {
                // Empty answer - stop here if we have at least 2
                if (answers.size() < 2) {
                    JOptionPane.showMessageDialog(this,
                            "You need at least 2 answers. Question not added.");
                    return;
                }
                break;
            }
            answers.add(trimmed);
            
            // If we have 4 answers, stop
            if (answers.size() == 4) {
                break;
            }
        }

        if (answers.size() < 2) {
            JOptionPane.showMessageDialog(this,
                    "You need at least 2 answers. Question not added.");
            return;
        }

        // Build answer string with empty strings for missing answers (to maintain format)
        String answerA = answers.size() > 0 ? answers.get(0) : "";
        String answerB = answers.size() > 1 ? answers.get(1) : "";
        String answerC = answers.size() > 2 ? answers.get(2) : "";
        String answerD = answers.size() > 3 ? answers.get(3) : "";

        String correctStr = JOptionPane.showInputDialog(this,
                String.format("Correct answer index (0-%d):", answers.size() - 1));
        if (correctStr == null || correctStr.trim().isEmpty())
            return;

        try {
            int correctIndex = Integer.parseInt(correctStr.trim());
            if (correctIndex < 0 || correctIndex >= answers.size()) {
                JOptionPane.showMessageDialog(this,
                        String.format("Invalid index. Must be between 0 and %d.", answers.size() - 1));
                return;
            }

            String questionLine = String.format("%s|%s|%s|%s|%s|%s|%d",
                    category.trim(), question.trim(),
                    answerA.trim(), answerB.trim(), answerC.trim(), answerD.trim(),
                    correctIndex);

            try {
                Client.INSTANCE.sendMessage("/addq " + questionLine);
                JOptionPane.showMessageDialog(this,
                        "Question added for this session.");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Error sending question: " + e.getMessage());
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "Invalid number format for correct answer index.");
        }
    }
}

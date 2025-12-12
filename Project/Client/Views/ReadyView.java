package Client.Views;

import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import Client.Client;
import Client.Interfaces.IPhaseEvent;
import Client.Interfaces.IUserListEvent;
import Common.Phase;
import Common.UserListPayload;

// UCID: nhd5
// Date: December 8, 2025
// Description: ReadyView – UI panel for ready check and adding questions
// Reference: https://www.w3schools.com/java/

public class ReadyView extends JPanel implements IPhaseEvent, IUserListEvent {
    private boolean isAway = false;
    private boolean isSpectator = false;
    private final JButton addQuestionButton;
    private final JButton categoriesButton;
    private Phase currentPhase = Phase.READY;
    
    public ReadyView() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        JButton readyButton = new JButton("Ready");
        readyButton.addActionListener(_ -> {
            try {
                Client.INSTANCE.sendReady();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        this.add(readyButton);

        addQuestionButton = new JButton("Add Question");
        addQuestionButton.addActionListener(_ -> {
            showAddQuestionDialog();
        });
        this.add(addQuestionButton);
        
        Client.INSTANCE.registerCallback(this);

        categoriesButton = new JButton("Categories");
        categoriesButton.addActionListener(_ -> showCategoryDialog());
        this.add(categoriesButton);
        updateCategoriesButtonState();

        JButton awayButton = new JButton("Away");
        awayButton.addActionListener(_ -> {
            toggleAway();
            awayButton.setText(isAway ? "Back" : "Away");
        });
        this.add(awayButton);

        JButton spectatorButton = new JButton("Spectator");
        spectatorButton.addActionListener(_ -> {
            if (!isSpectator) {
                isSpectator = true;
                try {
                    Client.INSTANCE.sendMessage("/spectate");
                    spectatorButton.setEnabled(false); // Can't un-spectate once set
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this,
                            "Error setting spectator mode: " + e.getMessage());
                    isSpectator = false;
                }
            }
        });
        this.add(spectatorButton);
    }

    private void toggleAway() {
        isAway = !isAway;
        try {
            Client.INSTANCE.sendMessage(isAway ? "/away" : "/back");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error setting away status: " + e.getMessage());
            isAway = !isAway; // Revert on error
        }
    }

    private void showAddQuestionDialog() {
        String currentRoom = Client.INSTANCE.getCurrentRoom();
        if (currentRoom == null || "lobby".equalsIgnoreCase(currentRoom)) {
            JOptionPane.showMessageDialog(this,
                    "You can only add questions after joining a game room (not in lobby).");
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

    private void showCategoryDialog() {
        if (!Client.INSTANCE.isHost()) {
            JOptionPane.showMessageDialog(this,
                    "Only the host can change categories during Ready Check.");
            return;
        }
        if (currentPhase != Phase.READY) {
            JOptionPane.showMessageDialog(this,
                    "Categories can only be set during Ready Check.");
            return;
        }
        String cats = JOptionPane.showInputDialog(this,
                "Comma-separated categories to enable (leave blank for all):");
        if (cats == null) {
            return;
        }
        try {
            Client.INSTANCE.sendMessage("/categories " + cats);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error sending categories: " + e.getMessage());
        }
    }

    @Override
    public void onReceivePhase(Phase phase) {
        currentPhase = phase;
        // Only enable Add Question during Ready Check (not during active gameplay)
        addQuestionButton.setEnabled(phase == Phase.READY);
        updateCategoriesButtonState();
    }

    @Override
    public void onUserListUpdate(UserListPayload payload) {
        updateCategoriesButtonState();
    }

    private void updateCategoriesButtonState() {
        boolean enable = currentPhase == Phase.READY && Client.INSTANCE.isHost();
        categoriesButton.setEnabled(enable);
    }
}

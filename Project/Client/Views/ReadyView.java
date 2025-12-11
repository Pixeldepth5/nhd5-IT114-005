package Client.Views;

import java.io.IOException;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import Client.Client;

// UCID: nhd5
// Date: December 8, 2025
// Description: ReadyView – UI panel for ready check and adding questions
// Reference: https://www.w3schools.com/java/

public class ReadyView extends JPanel {
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

        JButton addQuestionButton = new JButton("Add Question");
        addQuestionButton.addActionListener(_ -> {
            showAddQuestionDialog();
        });
        this.add(addQuestionButton);

        JButton categoriesButton = new JButton("Categories");
        categoriesButton.addActionListener(_ -> showCategoryDialog());
        this.add(categoriesButton);
    }

    private void showAddQuestionDialog() {
        String category = JOptionPane.showInputDialog(this,
                "Category (e.g., geography, science, math, history, movies, sports):");
        if (category == null || category.trim().isEmpty())
            return;

        String question = JOptionPane.showInputDialog(this, "Question:");
        if (question == null || question.trim().isEmpty())
            return;

        String answerA = JOptionPane.showInputDialog(this, "Answer A:");
        if (answerA == null || answerA.trim().isEmpty())
            return;

        String answerB = JOptionPane.showInputDialog(this, "Answer B:");
        if (answerB == null || answerB.trim().isEmpty())
            return;

        String answerC = JOptionPane.showInputDialog(this, "Answer C:");
        if (answerC == null || answerC.trim().isEmpty())
            return;

        String answerD = JOptionPane.showInputDialog(this, "Answer D:");
        if (answerD == null || answerD.trim().isEmpty())
            return;

        String correctStr = JOptionPane.showInputDialog(this,
                "Correct answer index (0=A, 1=B, 2=C, 3=D):");
        if (correctStr == null || correctStr.trim().isEmpty())
            return;

        try {
            int correctIndex = Integer.parseInt(correctStr.trim());
            if (correctIndex < 0 || correctIndex > 3) {
                JOptionPane.showMessageDialog(this,
                        "Invalid index. Must be 0, 1, 2, or 3.");
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
}

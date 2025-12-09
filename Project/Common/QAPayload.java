// UCID: nhd5
// Date: December 8, 2025
// Description: QAPayload – extends Payload to send question and answer options to clients.
//              Used during gameplay to display questions with multiple choice answers.
//              Does NOT include the correct answer (clients must guess).
// References:
//   - W3Schools: https://www.w3schools.com/java/java_arraylist.asp

package Common;

import java.util.ArrayList;

public class QAPayload extends Payload {

    // Question category (e.g., "geography", "science", "history")
    private String category;
    // The question text displayed to players
    private String question;
    // List of answer options (typically 4 options: A, B, C, D)
    private ArrayList<String> answers = new ArrayList<>();
    // Index of correct answer (0=A, 1=B, 2=C, 3=D) - kept server-side only
    private int correctIndex;

    // Getter and setter for question category
    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

    // Getter and setter for question text (getQuestionText is alias for compatibility)
    public String getQuestion() {
        return question;
    }
    public String getQuestionText() {
        return question;
    }
    public void setQuestion(String question) {
        this.question = question;
    }
    public void setQuestionText(String question) {
        this.question = question;
    }

    // Getter and setter for answer options list
    public ArrayList<String> getAnswers() {
        return answers;
    }
    public void setAnswers(ArrayList<String> answers) {
        this.answers = answers;
    }

    // Getter and setter for correct answer index (server-side only, not sent to clients)
    public int getCorrectIndex() {
        return correctIndex;
    }
    public void setCorrectIndex(int correctIndex) {
        this.correctIndex = correctIndex;
    }
}

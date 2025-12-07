// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame QAPayload – sends question + answers to clients
// Reference: https://www.w3schools.com/java/java_arraylist.asp
package Common;

import java.util.ArrayList;

public class QAPayload extends Payload {
    private String category;
    private String questionText;
    private ArrayList<String> answers = new ArrayList<>();
    // Correct answer index is kept server-side for scoring, not shown to players
    private int correctIndex;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public ArrayList<String> getAnswers() {
        return answers;
    }

    public void setAnswers(ArrayList<String> answers) {
        this.answers = answers;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public void setCorrectIndex(int correctIndex) {
        this.correctIndex = correctIndex;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" Category[%s] Question[%s] AnswerCount[%d]",
                category, questionText, answers.size());
    }
}

package Common;

import java.util.ArrayList;

public class QAPayload extends Payload {

    private String category;
    private String question;
    private ArrayList<String> answers = new ArrayList<>();
    private int correctIndex;

    public String getCategory() {
        return category;
    }
    public void setCategory(String category) {
        this.category = category;
    }

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
}

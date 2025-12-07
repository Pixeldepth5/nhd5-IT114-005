// UCID: nhd5
// Date: December 8, 2025
// Description: Sends question + answers + category to clients
// Reference: https://www.w3schools.com/java/java_arraylist.asp

package Common;

import java.util.ArrayList;

public class QAPayload extends Payload {

    private String category;
    private String questionText;
    private ArrayList<String> answers = new ArrayList<>();

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

    @Override
    public String toString() {
        return super.toString() + " Category[" + category + "] Question[" + questionText + "]";
    }
}

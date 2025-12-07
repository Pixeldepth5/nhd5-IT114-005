// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame PointsPayload – used to sync player points to all clients
// Reference: https://www.w3schools.com/java/java_classes.asp
package Common;

public class PointsPayload extends Payload {
    private long targetClientId;
    private int points; // total points after update

    public long getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(long targetClientId) {
        this.targetClientId = targetClientId;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    @Override
    public String toString() {
        // Reference: https://www.w3schools.com/java/java_strings_format.asp
        return super.toString() + String.format(" TargetClientId[%d] Points[%d]", targetClientId, points);
    }
}

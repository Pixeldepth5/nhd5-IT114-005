// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame PointsPayload – used to sync player points to all clients.
//              Sent when a player earns points after answering correctly.

package Common;

public class PointsPayload extends Payload {
    // ID of the client whose points were updated
    private long targetClientId;
    // Total points after the update (not the amount earned, but the new total)
    private int points;

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
        return super.toString() +
                String.format(" TargetClientId[%d] Points[%d]", targetClientId, points);
    }
}

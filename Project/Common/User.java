// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame Server User – stores ID and name for connected clients
// Reference: https://www.w3schools.com/java/

package Common;

public class User {
    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private String clientName;
    private boolean ready = false;
    private boolean tookTurn = false;
    private int points = -1;

    public long getClientId() { return clientId; }
    public void setClientId(long id) { this.clientId = id; }
    public String getClientName() { return clientName; }
    public void setClientName(String name) { this.clientName = name; }

    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public boolean didTakeTurn() { return tookTurn; }
    public void setTookTurn(boolean tookTurn) { this.tookTurn = tookTurn; }

    public int getPoints() { return points; }
    public void setPoints(int points) { this.points = points; }

    public String getDisplayName() { return clientName + "#" + clientId; }

    public void reset() {
        this.clientId = Constants.DEFAULT_CLIENT_ID;
        this.clientName = null;
        this.ready = false;
        this.tookTurn = false;
        this.points = -1;
    }
}

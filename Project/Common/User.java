// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame Server User – stores ID and name for connected clients
// Reference: https://www.w3schools.com/java/

package Common;

public class User {
    private long clientId = Constants.DEFAULT_CLIENT_ID;
    private String clientName;

    public long getClientId() { return clientId; }
    public void setClientId(long id) { this.clientId = id; }
    public String getClientName() { return clientName; }
    public void setClientName(String name) { this.clientName = name; }

    public String getDisplayName() { return clientName + "#" + clientId; }

    public void reset() {
        this.clientId = Constants.DEFAULT_CLIENT_ID;
        this.clientName = null;
    }
}

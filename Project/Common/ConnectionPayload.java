// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame ConnectionPayload – used during client connection handshake
// Reference: https://www.w3schools.com/java/

package Common;

public class ConnectionPayload extends Payload {
    private String clientName;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String name) {
        this.clientName = name;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" ClientName[%s]", clientName);
    }
}

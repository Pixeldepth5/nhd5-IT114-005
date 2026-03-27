// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame Payload – base object used to send data between client and server
// Reference: https://www.w3schools.com/java/

package Common;

import java.io.Serializable;

public class Payload implements Serializable {
    private PayloadType payloadType;
    private long clientId;
    private String message;

    public PayloadType getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return String.format("Payload[%s] ClientId[%d] Message[%s]", payloadType, clientId, message);
    }
}

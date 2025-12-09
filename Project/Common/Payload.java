// UCID: nhd5
// Date: December 8, 2025
// Description: Payload – base class for all data sent between Client and Server.
//              Implements Serializable so objects can be sent over network streams.
// References:
//   - W3Schools: https://www.w3schools.com/java/java_serialization.asp

package Common;

import java.io.Serializable;

public class Payload implements Serializable {
    private PayloadType payloadType;
    private long clientId;
    private String message;

    // Numeric field used for timer updates and other numeric data
    private int number;

    // Getter and setter for payload type (determines what kind of message this is)
    public PayloadType getPayloadType() {
        return payloadType;
    }
    public void setPayloadType(PayloadType payloadType) {
        this.payloadType = payloadType;
    }

    // Getter and setter for client ID (identifies which client sent/receives this)
    public long getClientId() {
        return clientId;
    }
    public void setClientId(long clientId) {
        this.clientId = clientId;
    }

    // Getter and setter for message text (used for chat, events, timer countdown)
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }

    // Getter and setter for numeric value (used for timer seconds, etc.)
    public int getNumber() {
        return number;
    }
    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return String.format(
            "Payload[%s] ClientId[%d] Message[%s] Number[%d]",
            payloadType, clientId, message, number
        );
    }
}

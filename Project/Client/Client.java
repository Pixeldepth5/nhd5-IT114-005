// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame UserListPayload – syncs the user list / status to all clients
// Reference: https://www.w3schools.com/java/java_arraylist.asp
package Common;

import java.util.ArrayList;

public class UserListPayload extends Payload {

    private ArrayList<Long> clientIds = new ArrayList<>();
    private ArrayList<String> displayNames = new ArrayList<>();
    private ArrayList<Integer> points = new ArrayList<>();
    private ArrayList<Boolean> lockedIn = new ArrayList<>();
    private ArrayList<Boolean> away = new ArrayList<>();
    private ArrayList<Boolean> spectator = new ArrayList<>();

    public ArrayList<Long> getClientIds() {
        return clientIds;
    }

    public ArrayList<String> getDisplayNames() {
        return displayNames;
    }

    public ArrayList<Integer> getPoints() {
        return points;
    }

    public ArrayList<Boolean> getLockedIn() {
        return lockedIn;
    }

    public ArrayList<Boolean> getAway() {
        return away;
    }

    public ArrayList<Boolean> getSpectator() {
        return spectator;
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" Users[%d]", clientIds.size());
    }
}

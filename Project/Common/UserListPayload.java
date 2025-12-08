// UCID: nhd5
// Date: December 8, 2025
// Description: TriviaGuessGame UserListPayload – syncs user list / status to all clients
// Reference: https://www.w3schools.com/java/java_arraylist.asp

package Common;

import java.util.ArrayList;

public class UserListPayload extends Payload {

    // One entry at index i belongs to the same user across all lists.
    private ArrayList<Long> clientIds = new ArrayList<Long>();
    private ArrayList<String> displayNames = new ArrayList<String>();
    private ArrayList<Integer> points = new ArrayList<Integer>();
    private ArrayList<Boolean> lockedIn = new ArrayList<Boolean>();
    private ArrayList<Boolean> away = new ArrayList<Boolean>();
    private ArrayList<Boolean> spectator = new ArrayList<Boolean>();

    // Convenience method used by GameRoom on the server
    public void addUser(long id, String name, int pts,
                        boolean isLocked, boolean isAway, boolean isSpectator) {
        clientIds.add(id);
        displayNames.add(name);
        points.add(pts);
        lockedIn.add(isLocked);
        away.add(isAway);
        spectator.add(isSpectator);
    }

    // ---- Getters used by Client UI ----
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
        // Simple summary for debugging
        return super.toString()
                + String.format(" Users[%d]", clientIds.size());
    }
}

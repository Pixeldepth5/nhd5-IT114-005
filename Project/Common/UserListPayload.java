// UCID: nhd5
// Date: December 8, 2025
// Description: UserListPayload - sends synced user list/status to all clients

package Common;

import java.io.Serializable;
import java.util.ArrayList;

public class UserListPayload extends Payload implements Serializable {

    private ArrayList<Long> clientIds = new ArrayList<>();
    private ArrayList<String> displayNames = new ArrayList<>();
    private ArrayList<Integer> points = new ArrayList<>();
    private ArrayList<Boolean> lockedIn = new ArrayList<>();
    private ArrayList<Boolean> away = new ArrayList<>();
    private ArrayList<Boolean> spectator = new ArrayList<>();

    public UserListPayload() {
        setPayloadType(PayloadType.USERLIST_UPDATE);
    }

    // ======================
    // Add a full user entry
    // ======================
    public void addUser(long id, String name, int pts, boolean isLocked, boolean isAway, boolean isSpectator) {
        clientIds.add(id);
        displayNames.add(name);
        points.add(pts);
        lockedIn.add(isLocked);
        away.add(isAway);
        spectator.add(isSpectator);
    }

    // ======================
    // Getters
    // ======================

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
        return "UserListPayload (" + clientIds.size() + " users)";
    }
}

// UCID: nhd5
// Date: December 8, 2025
// Description: UserListPayload – syncs user list with points, locked status, away, and spectator flags
//              to all clients in the game room.
// References:
//   - W3Schools: https://www.w3schools.com/java/java_arraylist.asp

package Common;

import java.util.ArrayList;

public class UserListPayload extends Payload {

    private ArrayList<Long> clientIds = new ArrayList<>();
    private ArrayList<String> displayNames = new ArrayList<>();
    private ArrayList<Integer> points = new ArrayList<>();
    private ArrayList<Boolean> lockedIn = new ArrayList<>();
    private ArrayList<Boolean> away = new ArrayList<>();
    private ArrayList<Boolean> spectator = new ArrayList<>();

    /**
     * Adds a user to the synchronized user list.
     * All lists are kept in parallel (same index = same user).
     * @param id Client ID
     * @param name Display name
     * @param pts Current points
     * @param locked Whether user locked in answer this round
     * @param isAway Whether user is marked away
     * @param isSpectator Whether user is a spectator
     */
    public void addUser(long id, String name, int pts, boolean locked, boolean isAway, boolean isSpectator) {
        clientIds.add(id);
        displayNames.add(name);
        points.add(pts);
        lockedIn.add(locked);
        away.add(isAway);
        spectator.add(isSpectator);
    }

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
        return super.toString() + String.format(" UserList[%d users]", clientIds.size());
    }
}

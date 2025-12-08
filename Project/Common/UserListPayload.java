// UCID: nhd5
// Date: December 7, 2025
// Description: TriviaGuessGame UserListPayload – sends user state to all clients

package Common;

import java.util.ArrayList;

public class UserListPayload extends Payload {

    private ArrayList<Long> clientIds = new ArrayList<>();
    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<Integer> points = new ArrayList<>();
    private ArrayList<Boolean> lockedIn = new ArrayList<>();
    private ArrayList<Boolean> away = new ArrayList<>();
    private ArrayList<Boolean> spectator = new ArrayList<>();

    public void addUser(long id, String name, int pts, boolean isLocked, boolean isAway, boolean isSpectator) {
        clientIds.add(id);
        names.add(name);
        points.add(pts);
        lockedIn.add(isLocked);
        away.add(isAway);
        spectator.add(isSpectator);
    }

    public ArrayList<Long> getClientIds() { return clientIds; }
    public ArrayList<String> getDisplayNames() { return names; }
    public ArrayList<Integer> getPoints() { return points; }
    public ArrayList<Boolean> getLockedIn() { return lockedIn; }
    public ArrayList<Boolean> getAway() { return away; }
    public ArrayList<Boolean> getSpectator() { return spectator; }
}

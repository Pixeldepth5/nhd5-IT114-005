// UCID: nhd5
// Date: December 8, 2025
// Description: Syncs user list (score, locked, away, spectator)
// Reference: https://www.w3schools.com/java/java_arraylist.asp

package Common;

import java.util.ArrayList;

public class UserListPayload extends Payload {

    private ArrayList<Long> clientIds = new ArrayList<>();
    private ArrayList<String> names = new ArrayList<>();
    private ArrayList<Integer> points = new ArrayList<>();
    private ArrayList<Boolean> locked = new ArrayList<>();
    private ArrayList<Boolean> away = new ArrayList<>();
    private ArrayList<Boolean> spectator = new ArrayList<>();

    public void addUser(long id, String name, int pts, boolean isLocked, boolean isAway, boolean isSpec) {
        clientIds.add(id);
        names.add(name);
        points.add(pts);
        locked.add(isLocked);
        away.add(isAway);
        spectator.add(isSpec);
    }

    public ArrayList<Long> getClientIds() { return clientIds; }
    public ArrayList<String> getNames() { return names; }
    public ArrayList<Integer> getPoints() { return points; }
    public ArrayList<Boolean> getLocked() { return locked; }
    public ArrayList<Boolean> getAway() { return away; }
    public ArrayList<Boolean> getSpectator() { return spectator; }

    @Override
    public String toString() {
        return super.toString() + " UserCount[" + clientIds.size() + "]";
    }
}

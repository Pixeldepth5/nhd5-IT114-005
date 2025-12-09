// UCID: nhd5
// Date: December 8, 2025
// Description: Simple client-side User model for displaying user list.
// Reference: https://www.w3schools.com/java/java_classes.asp

package Client;

public class User {
    private long id;
    private String name;
    private int points;
    private boolean locked;
    private boolean away;
    private boolean spectator;

    public User(long id, String name, int points,
                boolean locked, boolean away, boolean spectator) {
        this.id = id;
        this.name = name;
        this.points = points;
        this.locked = locked;
        this.away = away;
        this.spectator = spectator;
    }

    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" (").append(id).append(") ");
        sb.append("| pts: ").append(points);
        if (locked) sb.append(" [LOCKED]");
        if (away) sb.append(" [AWAY]");
        if (spectator) sb.append(" [SPECTATOR]");
        return sb.toString();
    }

    // Getters (in case you need them later)
    public long getId() { return id; }
    public String getName() { return name; }
    public int getPoints() { return points; }
    public boolean isLocked() { return locked; }
    public boolean isAway() { return away; }
    public boolean isSpectator() { return spectator; }
}

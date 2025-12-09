// UCID: nhd5
// Date: November 3, 2025
// Description: TriviaGuessGame Command – defines command keywords used in the client (e.g., /joinroom)
// Reference: https://www.w3schools.com/java/

package Common;

import java.util.HashMap;

public enum Command {
    QUIT("quit"),
    DISCONNECT("disconnect"),
    REVERSE("reverse"),     // unused for trivia but left for baseline consistency
    CREATE_ROOM("createroom"),
    JOIN_ROOM("joinroom"),
    LEAVE_ROOM("leaveroom"),
    NAME("name"),
    LIST_USERS("users");

    public final String command;
    private static final HashMap<String, Command> BY_COMMAND = new HashMap<>();

    static {
        for (Command c : values()) {
            BY_COMMAND.put(c.command, c);
        }
    }

    Command(String command) {
        this.command = command;
    }

    public static Command fromString(String input) {
        return BY_COMMAND.get(input.toLowerCase());
    }
}

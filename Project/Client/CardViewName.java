package Client;

/**
 * Represents the card names for the multi-panel client UI.
 */
public enum CardViewName {
    CONNECT(false),
    USER_INFO(false),
    CHAT_GAME_SCREEN(true),
    GAME_SCREEN(true),
    ROOMS(true);

    private final boolean requiresConnection;

    CardViewName(boolean requiresConnection) {
        this.requiresConnection = requiresConnection;
    }

    public boolean requiresConnection() {
        return requiresConnection;
    }

    public static boolean viewRequiresConnection(CardViewName view) {
        return view != null && view.requiresConnection();
    }
}

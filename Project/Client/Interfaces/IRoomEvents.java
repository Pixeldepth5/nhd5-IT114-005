package Client.Interfaces;

import java.util.List;

public interface IRoomEvents extends IClientEvents {
    void onReceiveRoomList(List<String> rooms, String message);

    void onRoomAction(long clientId, String roomName, boolean isJoin, boolean isQuiet);
}

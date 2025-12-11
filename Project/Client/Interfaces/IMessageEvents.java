package Client.Interfaces;

public interface IMessageEvents extends IClientEvents {
    void onMessageReceive(long id, String message);
}

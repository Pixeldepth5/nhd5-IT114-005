package Client.Interfaces;

public interface IConnectionEvents extends IClientEvents {
    void onClientDisconnect(long clientId);

    void onReceiveClientId(long clientId);
}

package Client.Interfaces;

public interface IReadyEvent extends IClientEvents {
    void onReceiveReady(long clientId, boolean isReady, boolean isQuiet);
}

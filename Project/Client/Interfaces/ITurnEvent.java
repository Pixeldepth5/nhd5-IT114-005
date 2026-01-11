package Client.Interfaces;

public interface ITurnEvent extends IClientEvents {
    void onTookTurn(long clientId, boolean tookTurn);
}

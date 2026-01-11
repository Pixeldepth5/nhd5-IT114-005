package Client.Interfaces;

public interface IPointsEvent extends IClientEvents {
    void onPointsUpdate(long clientId, int points);
}

package Client.Interfaces;

import Common.Phase;

public interface IPhaseEvent extends IClientEvents {
    void onReceivePhase(Phase phase);
}

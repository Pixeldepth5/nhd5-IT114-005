package Client.Interfaces;

import Common.TimerType;

public interface ITimeEvents extends IClientEvents {
    void onTimerUpdate(TimerType timerType, int time);
}

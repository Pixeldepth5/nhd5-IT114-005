package Client.Interfaces;

import Common.QAPayload;

public interface IQuestionEvent extends IClientEvents {
    void onQuestion(QAPayload qa);
}

package Client.Interfaces;

import Common.UserListPayload;

public interface IUserListEvent extends IClientEvents {
    void onUserListUpdate(UserListPayload payload);
}

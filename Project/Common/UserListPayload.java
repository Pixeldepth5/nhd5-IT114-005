package Common;

import java.util.ArrayList;

public class UserListPayload extends Payload {

    private ArrayList<String> users = new ArrayList<>();

    public void addUser(String name) {
        users.add(name);
    }

    public ArrayList<String> getUsers() {
        return users;
    }
}

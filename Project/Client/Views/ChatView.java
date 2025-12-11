package Client.Views;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import Client.Client;
import Client.Interfaces.ICardControls;
import Client.Interfaces.IMessageEvents;
import Client.Interfaces.IUserListEvent;
import Common.UserListPayload;

public class ChatView extends JPanel implements IMessageEvents, IUserListEvent {
    private final JTextArea chatArea = new JTextArea();
    private final JTextField input = new JTextField();
    private final JTextArea userListArea = new JTextArea();

    public ChatView(ICardControls controls) {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Chat"));
        chatArea.setEditable(false);

        JScrollPane chatScroll = new JScrollPane(chatArea);

        // User list on the right side (non-editable)
        userListArea.setEditable(false);
        userListArea.setBorder(BorderFactory.createTitledBorder("Players"));
        userListArea.setRows(8);
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(new JScrollPane(userListArea));
        rightPanel.add(Box.createVerticalGlue());

        add(chatScroll, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);

        input.addActionListener(_ -> send());
        add(input, BorderLayout.SOUTH);
        Client.INSTANCE.registerCallback(this);
    }

    private void send() {
        String msg = input.getText().trim();
        if (msg.isEmpty()) {
            return;
        }
        try {
            Client.INSTANCE.sendMessage(msg);
            input.setText("");
        } catch (IOException e) {
            chatArea.append("Send failed: " + e.getMessage() + "\n");
        }
    }

    public void addMessage(String msg) {
        chatArea.append(msg + "\n");
    }

    @Override
    public void onMessageReceive(long id, String message) {
        addMessage(message);
    }

    @Override
    public void onUserListUpdate(UserListPayload payload) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < payload.getClientIds().size(); i++) {
            String name = payload.getDisplayNames().get(i);
            int pts = payload.getPoints().get(i);
            boolean locked = payload.getLockedIn().get(i);
            sb.append(name)
              .append(" — ")
              .append(pts)
              .append(" pts")
              .append(locked ? " [locked]" : "")
              .append(System.lineSeparator());
        }
        userListArea.setText(sb.toString().trim());
    }
}

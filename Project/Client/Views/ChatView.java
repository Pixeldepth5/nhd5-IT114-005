package Client.Views;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import Client.Client;
import Client.Interfaces.ICardControls;
import Client.Interfaces.IMessageEvents;

public class ChatView extends JPanel implements IMessageEvents {
    private final JTextArea chatArea = new JTextArea();
    private final JTextField input = new JTextField();

    public ChatView(ICardControls controls) {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Chat"));
        chatArea.setEditable(false);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
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
}

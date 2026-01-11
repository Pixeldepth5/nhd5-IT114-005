package Client.Views;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import Client.CardViewName;
import Client.Interfaces.ICardControls;
import Common.LoggerUtil;

public class UserDetailsView extends JPanel {
    private String username;
    private final JTextField userField = new JTextField();
    private final JLabel userError = new JLabel();

    public UserDetailsView(ICardControls controls) {
        super(new BorderLayout(10, 10));

        setName(CardViewName.USER_INFO.name());
        controls.registerView(CardViewName.USER_INFO.name(), this);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(10, 10, 10, 10));

        content.add(new JLabel("Username: "));
        content.add(userField);
        userError.setVisible(false);
        content.add(userError);
        content.add(Box.createRigidArea(new Dimension(0, 200)));

        JButton previousButton = new JButton("Previous");
        previousButton.addActionListener(_ -> controls.previousView());
        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(_ -> onConnect(controls));

        JPanel buttons = new JPanel();
        buttons.add(previousButton);
        buttons.add(connectButton);

        content.add(Box.createVerticalGlue());
        content.add(buttons);

        add(content, BorderLayout.CENTER);
        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    private void onConnect(ICardControls controls) {
        String incomingUsername = userField.getText().trim();
        if (incomingUsername.isEmpty()) {
            userError.setText("Username must be provided");
            userError.setVisible(true);
        } else {
            username = incomingUsername;
            LoggerUtil.INSTANCE.info("Chosen username: " + username);
            userError.setVisible(false);
            controls.connect();
        }
    }

    public String getUsername() {
        return username;
    }
}

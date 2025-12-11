package Client.Views;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import Client.CardViewName;
import Client.Interfaces.ICardControls;

public class RoomsView extends JPanel {
    private final JTextArea roomsArea = new JTextArea();

    public RoomsView(ICardControls controls) {
        super(new BorderLayout());
        setName(CardViewName.ROOMS.name());
        controls.registerView(CardViewName.ROOMS.name(), this);
        roomsArea.setEditable(false);
        add(new JScrollPane(roomsArea), BorderLayout.CENTER);
    }

    public void setRooms(String rooms) {
        roomsArea.setText(rooms);
    }
}

package Client.Views;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import Client.Interfaces.ICardControls;

public class MenuBar extends JMenuBar {
    public MenuBar(ICardControls controls) {
        JMenu navigation = new JMenu("Navigate");
        JMenuItem connect = new JMenuItem("Connect");
        connect.addActionListener(_ -> controls.showView("CONNECT"));
        navigation.add(connect);
        this.add(navigation);
    }
}

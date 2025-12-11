package Client.Interfaces;

import javax.swing.JPanel;

import Client.CardViewName;

/**
 * Allows views to control navigation between card-based panels.
 */
public interface ICardControls {
    void nextView();

    void previousView();

    void showView(String viewName);

    void showView(CardViewName viewEnum);

    void registerView(String viewName, JPanel panelView);

    void connect();
}

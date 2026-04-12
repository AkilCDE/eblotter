package main.eblotterr;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class mayn {
    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException e) {
            System.err.println("Error setting look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(
                () -> new LoadingScreen(() -> new login().setVisible(true)).setVisible(true));
    }
}

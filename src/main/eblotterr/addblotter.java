package main.eblotterr;

import java.awt.*;
import javax.swing.*;

public class addblotter extends JFrame {
    public addblotter() {
        setTitle("Add Blotter");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new GridLayout(5, 2, 10, 10));

        // Add components
        add(new JLabel("Case Number:"));
        add(new JTextField());

        add(new JLabel("Description:"));
        add(new JTextArea(3, 20));

        add(new JLabel("Date:"));
        add(new JTextField());

        add(new JLabel("Location:"));
        add(new JTextField());

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        add(saveButton);
        add(cancelButton);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new addblotter());
    }
}

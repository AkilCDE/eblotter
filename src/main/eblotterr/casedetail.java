package main.eblotterr;

import java.awt.*;
import javax.swing.*;

public class casedetail extends JFrame {
    public casedetail() {
        setTitle("Case Detail");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Add components
        JLabel titleLabel = new JLabel("Case Details", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridLayout(4, 2, 10, 10));

        centerPanel.add(new JLabel("Case Number:"));
        centerPanel.add(new JLabel("12345")); // Example

        centerPanel.add(new JLabel("Description:"));
        centerPanel.add(new JTextArea("Sample description", 3, 20));

        centerPanel.add(new JLabel("Date:"));
        centerPanel.add(new JLabel("2023-10-01"));

        centerPanel.add(new JLabel("Location:"));
        centerPanel.add(new JLabel("Sample Location"));

        add(centerPanel, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        add(closeButton, BorderLayout.SOUTH);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new casedetail());
    }
}

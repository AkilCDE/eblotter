package main.eblotterr;

import java.awt.*;
import javax.swing.*;

public class printpreview extends JFrame {
    public printpreview() {
        setTitle("Print Preview");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());

        // Add components
        JLabel titleLabel = new JLabel("Print Preview", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        add(titleLabel, BorderLayout.NORTH);

        JTextArea previewArea = new JTextArea("This is a preview of the blotter report.\n\nCase Number: 12345\nDescription: Sample\nDate: 2023-10-01\nLocation: Sample");
        previewArea.setEditable(false);
        add(new JScrollPane(previewArea), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton printButton = new JButton("Print");
        JButton closeButton = new JButton("Close");

        buttonPanel.add(printButton);
        buttonPanel.add(closeButton);

        add(buttonPanel, BorderLayout.SOUTH);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new printpreview());
    }
}

package main.eblotterr;

import java.awt.*;
import java.awt.event.ActionEvent;
import javax.swing.*;

/**
 * Splash / loading screen matching {@link Login} styling; shows for 3 seconds then runs {@code onComplete}.
 */
public class LoadingScreen extends JFrame {

    private static final int DURATION_MS = 3000;
    private static final int TICK_MS = 50;

    public LoadingScreen(Runnable onComplete) {
        setTitle("Barangay e-Blotter - Digital Blotter Management System");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(500, 650);
        setMinimumSize(new Dimension(360, 450));
        setLocationRelativeTo(null);
        setResizable(true);

        JPanel root = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(25, 55, 90),
                        0, getHeight(), new Color(15, 35, 55));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(BorderFactory.createEmptyBorder(48, 50, 48, 50));

        JLabel line1 = new JLabel("REPUBLIC OF THE PHILIPPINES");
        line1.setFont(new Font("Segoe UI", Font.BOLD, 14));
        line1.setForeground(new Color(200, 210, 230));
        line1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel line2 = new JLabel("Barangay e-Blotter");
        line2.setFont(new Font("Segoe UI", Font.BOLD, 28));
        line2.setForeground(new Color(255, 215, 100));
        line2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel line3 = new JLabel("Digital Blotter Management System");
        line3.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        line3.setForeground(new Color(180, 200, 230));
        line3.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel loading = new JLabel("Loading…");
        loading.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        loading.setForeground(new Color(200, 215, 235));
        loading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        bar.setString("Preparing application");
        bar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        bar.setForeground(new Color(50, 140, 90));
        bar.setBackground(new Color(25, 45, 65));
        bar.setAlignmentX(Component.CENTER_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        bar.setPreferredSize(new Dimension(400, 22));

        root.add(Box.createVerticalGlue());
        root.add(line1);
        root.add(Box.createVerticalStrut(8));
        root.add(line2);
        root.add(Box.createVerticalStrut(5));
        root.add(line3);
        root.add(Box.createVerticalStrut(48));
        root.add(loading);
        root.add(Box.createVerticalStrut(16));
        root.add(bar);
        root.add(Box.createVerticalGlue());

        setContentPane(root);

        final long startNanos = System.nanoTime();
        Timer timer = new Timer(TICK_MS, (ActionEvent e) -> {
            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
            bar.setValue((int) Math.min(100, (elapsedMs * 100) / DURATION_MS));
            if (elapsedMs >= DURATION_MS) {
                ((Timer) e.getSource()).stop();
                onComplete.run();
                dispose();
            }
        });
        timer.setCoalesce(false);
        timer.setInitialDelay(0);
        timer.start();
    }
}

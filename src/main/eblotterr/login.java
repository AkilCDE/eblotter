package main.eblotterr;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;

public class login extends JFrame {

    // ── Modern color palette (copied from Login.java) ─────────────────────────
    private static final Color WHITE = Color.WHITE;
    private static final Color BLUE = new Color(24, 95, 165);
    private static final Color BLUE_HOVER = new Color(14, 76, 136);
    private static final Color BLUE_LIGHT = new Color(230, 241, 251);
    private static final Color BG = new Color(245, 246, 248);
    private static final Color BORDER = new Color(215, 220, 228);
    private static final Color TEXT_PRI = new Color(28, 32, 40);
    private static final Color TEXT_SEC = new Color(110, 120, 140);
    private static final Color TEXT_HINT = new Color(160, 170, 185);
    private static final Color ERROR_COLOR = new Color(220, 53, 69);
    private static final Color SUCCESS = new Color(25, 135, 84);

    // ── UI Fields ────────────────────────────────────────────────────────────
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton eyeButton;
    private JLabel statusLabel;
    private JPanel rootPanel;
    private boolean passwordVisible = false;
    private boolean isLoggingIn = false;

    private static final String U_HINT = "Enter username";
    private static final String P_HINT = "Enter password";

    // ── Constructor (original logic preserved) ───────────────────────────────
    public login() {
        setTitle("Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 640);
        setLocationRelativeTo(null);
        setResizable(false);
        setContentPane(buildRoot());
        getRootPane().setDefaultButton(loginButton);
    }

    // ── Build root panel with card layout (for potential loading state) ──────
    private JPanel buildRoot() {
        rootPanel = new JPanel(new CardLayout());
        rootPanel.setBackground(BG);

        JPanel loginContainer = new JPanel(new GridBagLayout());
        loginContainer.setBackground(BG);
        loginContainer.add(buildCard());

        rootPanel.add(loginContainer, "LOGIN");
        return rootPanel;
    }

    // ── Main login card (modern, clean layout) ───────────────────────────────
    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(WHITE);
        card.setBorder(new CompoundBorder(
                new RoundedBorder(BORDER, 1, 16),
                new EmptyBorder(36, 36, 32, 36)));
        card.setMaximumSize(new Dimension(390, 560));
        card.setPreferredSize(new Dimension(390, 560));

        card.add(buildHeader());
        card.add(Box.createVerticalStrut(28));
        card.add(buildUsernameRow());
        card.add(Box.createVerticalStrut(18));
        card.add(buildPasswordRow());
        card.add(Box.createVerticalStrut(26));
        card.add(buildLoginButton());
        card.add(Box.createVerticalStrut(14));
        card.add(buildStatus());
        card.add(Box.createVerticalGlue());
        card.add(buildFooter());

        return card;
    }

    // ── Header with logo / badge and title ───────────────────────────────────
    private JPanel buildHeader() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(WHITE);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLUE_LIGHT);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(BLUE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawRoundRect(10, 17, 28, 19, 5, 5);
                g2.drawArc(14, 8, 20, 18, 0, 180);
                g2.fillOval(21, 22, 6, 6);
                g2.drawLine(24, 28, 24, 32);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(52, 52));
        badge.setMaximumSize(new Dimension(52, 52));
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel republic = label("REPUBLIC OF THE PHILIPPINES", 10, Font.PLAIN, TEXT_SEC);
        republic.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = label("Barangay e-Blotter", 22, Font.BOLD, TEXT_PRI);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = label("Digital Blotter Management System", 13, Font.PLAIN, TEXT_SEC);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(badge);
        p.add(Box.createVerticalStrut(12));
        p.add(republic);
        p.add(Box.createVerticalStrut(4));
        p.add(title);
        p.add(Box.createVerticalStrut(3));
        p.add(sub);
        return p;
    }

    // ── Username row with icon and hint text ─────────────────────────────────
    private JPanel buildUsernameRow() {
        JPanel col = fieldColumn("Username");

        JPanel wrap = inputWrap();
        paintUserIcon(wrap);

        usernameField = new JTextField(U_HINT);
        styleTextField(usernameField);
        usernameField.setForeground(TEXT_HINT);

        usernameField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (usernameField.getText().equals(U_HINT)) {
                    usernameField.setText("");
                    usernameField.setForeground(TEXT_PRI);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (usernameField.getText().isEmpty()) {
                    usernameField.setText(U_HINT);
                    usernameField.setForeground(TEXT_HINT);
                }
            }
        });

        wrap.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                usernameField.requestFocusInWindow();
            }
        });

        wrap.add(usernameField, BorderLayout.CENTER);
        col.add(wrap);
        return col;
    }

    // ── Password row with icon, toggle eye button, and hint ──────────────────
    private JPanel buildPasswordRow() {
        JPanel col = fieldColumn("Password");

        JPanel wrap = inputWrap();
        paintLockIcon(wrap);

        passwordField = new JPasswordField(P_HINT);
        styleTextField(passwordField);
        passwordField.setForeground(TEXT_HINT);
        passwordField.setEchoChar((char) 0);

        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                String txt = new String(passwordField.getPassword());
                if (txt.equals(P_HINT) || txt.isEmpty()) {
                    passwordField.setText("");
                    passwordField.setForeground(TEXT_PRI);
                    passwordField.setEchoChar(passwordVisible ? (char) 0 : '●');
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                String txt = new String(passwordField.getPassword());
                if (txt.isEmpty()) {
                    passwordField.setText(P_HINT);
                    passwordField.setForeground(TEXT_HINT);
                    passwordField.setEchoChar((char) 0);
                }
            }
        });

        wrap.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                passwordField.requestFocusInWindow();
            }
        });

        eyeButton = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLUE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;

                if (!passwordVisible) {
                    g2.draw(new Ellipse2D.Double(cx - 10, cy - 6, 20, 12));
                    g2.fillOval(cx - 3, cy - 3, 7, 7);
                } else {
                    g2.drawArc(cx - 10, cy - 6, 20, 12, 0, 180);
                    g2.drawLine(cx - 10, cy + 4, cx + 10, cy - 8);
                }
                g2.dispose();
            }
        };
        eyeButton.setOpaque(false);
        eyeButton.setContentAreaFilled(false);
        eyeButton.setBorderPainted(false);
        eyeButton.setFocusPainted(false);
        eyeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eyeButton.setPreferredSize(new Dimension(38, 38));
        eyeButton.setToolTipText("Show / Hide Password");
        eyeButton.addActionListener(e -> togglePassword());

        wrap.add(passwordField, BorderLayout.CENTER);
        wrap.add(eyeButton, BorderLayout.EAST);
        col.add(wrap);
        return col;
    }

    // ── Styled login button with arrow icon ──────────────────────────────────
    private JPanel buildLoginButton() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(WHITE);
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginButton = new JButton("Login to System") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isRollover() ? BLUE_HOVER : BLUE;
                if (!isEnabled()) bg = new Color(150, 175, 205);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(WHITE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int iy = getHeight() / 2;
                int ix = 20;
                g2.drawLine(ix, iy, ix + 10, iy);
                g2.drawLine(ix + 6, iy - 4, ix + 10, iy);
                g2.drawLine(ix + 6, iy + 4, ix + 10, iy);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String txt = getText();
                int tx = (getWidth() - fm.stringWidth(txt)) / 2 + 8;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(txt, tx, ty);
                g2.dispose();
            }
        };
        loginButton.setForeground(WHITE);
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(0, 44));
        loginButton.setBorderPainted(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.addActionListener(e -> performLogin());

        p.add(loginButton, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(320, 44));
        return p;
    }

    // ── Status label for error / success messages ────────────────────────────
    private JPanel buildStatus() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setBackground(WHITE);
        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(ERROR_COLOR);
        p.add(statusLabel);
        p.setMaximumSize(new Dimension(9999, 22));
        return p;
    }

    // ── Footer with barangay info ────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(WHITE);
        p.setBorder(new MatteBorder(1, 0, 0, 0, BORDER));
        p.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel l1 = label("Authorized barangay personnel only", 11, Font.PLAIN, TEXT_SEC);
        JLabel l2 = label("Barangay Mabini — Central Visayas", 12, Font.BOLD, TEXT_PRI);
        l1.setAlignmentX(Component.CENTER_ALIGNMENT);
        l2.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(Box.createVerticalStrut(16));
        p.add(l1);
        p.add(Box.createVerticalStrut(3));
        p.add(l2);
        return p;
    }

    // ── Helper: field column with label ──────────────────────────────────────
    private JPanel fieldColumn(String labelText) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBackground(WHITE);
        col.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.setMaximumSize(new Dimension(320, 76));

        JLabel lbl = label(labelText, 13, Font.BOLD, TEXT_PRI);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        col.add(lbl);
        col.add(Box.createVerticalStrut(6));
        return col;
    }

    // ── Helper: input wrapper with border ────────────────────────────────────
    private JPanel inputWrap() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(new Color(250, 251, 252));
        wrap.setBorder(new CompoundBorder(
                new RoundedBorder(BORDER, 1, 8),
                new EmptyBorder(0, 0, 0, 0)));
        wrap.setMaximumSize(new Dimension(320, 44));
        wrap.setPreferredSize(new Dimension(320, 44));
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        return wrap;
    }

    // ── User icon (silhouette) ───────────────────────────────────────────────
    private void paintUserIcon(JPanel wrap) {
        JPanel icon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_SEC);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.drawOval(cx - 5, cy - 10, 10, 10);
                g2.drawArc(cx - 8, cy + 1, 16, 10, 0, 180);
                g2.dispose();
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(38, 44));
        wrap.add(icon, BorderLayout.WEST);
    }

    // ── Lock icon ────────────────────────────────────────────────────────────
    private void paintLockIcon(JPanel wrap) {
        JPanel icon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TEXT_SEC);
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = getWidth() / 2, cy = getHeight() / 2;
                g2.drawRoundRect(cx - 7, cy - 1, 14, 11, 3, 3);
                g2.drawArc(cx - 5, cy - 8, 10, 10, 0, 180);
                g2.dispose();
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(38, 44));
        wrap.add(icon, BorderLayout.WEST);
    }

    // ── Style for text fields ────────────────────────────────────────────────
    private void styleTextField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBackground(new Color(250, 251, 252));
        f.setBorder(new EmptyBorder(0, 2, 0, 8));
        f.setOpaque(true);
    }

    // ── Helper to create styled labels ───────────────────────────────────────
    private JLabel label(String txt, int size, int style, Color color) {
        JLabel l = new JLabel(txt);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }

    // ── Toggle password visibility ───────────────────────────────────────────
    private void togglePassword() {
        passwordVisible = !passwordVisible;
        String cur = new String(passwordField.getPassword());
        if (!cur.equals(P_HINT)) {
            passwordField.setEchoChar(passwordVisible ? (char) 0 : '●');
        }
        eyeButton.repaint();
        passwordField.requestFocusInWindow();
    }

    // ── Authentication logic (exactly as original) ───────────────────────────
    private void performLogin() {
        if (isLoggingIn) return;  // Prevent multiple simultaneous logins
        isLoggingIn = true;
        loginButton.setEnabled(false);  // Disable immediately

        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || username.equals(U_HINT)) {
            flash("Please enter your username.", false);
            usernameField.requestFocusInWindow();
            isLoggingIn = false;  // Reset flag
            loginButton.setEnabled(true);  // Re-enable
            return;
        }
        if (password.isEmpty() || password.equals(P_HINT)) {
            flash("Please enter your password.", false);
            passwordField.requestFocusInWindow();
            isLoggingIn = false;  // Reset flag
            loginButton.setEnabled(true);  // Re-enable
            return;
        }

        flash("Authenticating...", true);

        // Run authentication in background to keep UI responsive
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return authenticate(username, password);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        flash("Login successful!", true);
                        new dashboard().setVisible(true);
                        dispose();
                    } else {
                        isLoggingIn = false;  // Reset flag on failure
                        flash("Invalid username or password.", false);
                        loginButton.setEnabled(true);
                        passwordField.setText("");
                        passwordField.setForeground(TEXT_HINT);
                        passwordField.setEchoChar((char) 0);
                        passwordField.setText(P_HINT);
                        passwordVisible = false;
                        eyeButton.repaint();
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException ex) {
                    isLoggingIn = false;  // Reset flag on error
                    String message = "Authentication error";
                    if (ex.getCause() != null) {
                        message += ": " + ex.getCause().getMessage();
                    }
                    flash(message, false);
                    loginButton.setEnabled(true);
                }
            }
        }.execute();
    }

    // ── Flash message helper ─────────────────────────────────────────────────
    private void flash(String msg, boolean ok) {
        statusLabel.setText(msg);
        statusLabel.setForeground(ok ? SUCCESS : ERROR_COLOR);
        if (!ok) {
            Timer t = new Timer(3000, e -> statusLabel.setText(" "));
            t.setRepeats(false);
            t.start();
        }
    }

    // ── ORIGINAL AUTHENTICATION METHOD (unchanged) ───────────────────────────
    private boolean authenticate(String username, String password) {
        String url = "jdbc:mysql://localhost:3306/ebs";
        String user = "root";
        String pass = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                String query = "SELECT * FROM users WHERE username = ? AND password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, username);
                    stmt.setString(2, password);

                    try (ResultSet rs = stmt.executeQuery()) {
                        return rs.next();
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
            return false;
        }
    }

    // ── MAIN (unchanged) ─────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ignored) {
            }
            new login().setVisible(true);
            
        });
    }

    // ── Custom rounded border (from Login.java) ──────────────────────────────
    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        RoundedBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
        }
    }
}
package main.eblotterr;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

public class dashboard extends JFrame {

    // ── Professional blue and white color palette ─────────────────────────────
    private static final Color WHITE        = Color.WHITE;
    private static final Color BLACK        = Color.BLACK;
    private static final Color BLUE         = new Color(45, 118, 200);
    private static final Color BLUE_HOVER   = new Color(35, 95, 160);
    private static final Color BLUE_DARK    = new Color(25, 60, 110);
    private static final Color BLUE_LIGHT   = new Color(230, 241, 251);
    private static final Color BG           = new Color(245, 248, 255);
    private static final Color BORDER_CLR   = new Color(200, 210, 230);
    private static final Color TEXT_PRI     = Color.BLACK;
    private static final Color TEXT_SEC     = new Color(80, 90, 110);
    private static final Color TEXT_LIGHT   = new Color(180, 190, 210);
    private static final Color STAT_BLUE    = new Color(52, 134, 235);
    private static final Color STAT_ORANGE  = new Color(45, 118, 200);
    private static final Color STAT_GREEN   = new Color(35, 95, 160);
    private static final Color PENDING_BG   = new Color(230, 241, 251);
    private static final Color PENDING_FG   = new Color(45, 118, 200);
    private static final Color RESOLVED_BG  = new Color(240, 245, 255);
    private static final Color RESOLVED_FG  = new Color(35, 95, 160);
    private static final Color SIDEBAR_BG   = new Color(20, 50, 90);
    private static final Color SIDEBAR_HOVER = new Color(35, 75, 120);
    private static final Color PAGE_BG      = new Color(240, 245, 255);

    private final String currentUsername;
    private final String currentRole;
    
    // ── Card panels ─────────────────────────────────────────────────────────
    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JPanel dashboardPanel;
    private JPanel addBlotterPanel;
    private JPanel historyPanel;
    private JPanel profilePanel;
    private JPanel usersPanel;
    private JPanel settingsPanel;
    
    // ── Dashboard components ────────────────────────────────────────────────
    private JTable table;
    private DefaultTableModel tableModel;
    private final List<Object[]> blotterData = new ArrayList<>();
    private JButton addBtn;
    private JButton searchBtn;
    private JTextField searchField;
    private JLabel totalValueLabel;
    private JLabel pendingValueLabel;
    private JLabel resolvedValueLabel;
    
    // ── Sidebar buttons ─────────────────────────────────────────────────────
    private JButton homeBtn;
    private JButton addBlotterBtn;
    private JButton historyBtn;
    private JButton profileBtn;
    private JButton usersBtn;
    private JButton settingsBtn;
    private List<JButton> sidebarButtons = new ArrayList<>();

    // ── Extracted feature handlers ─────────────────────────────────────────
    private ViewBlotterDialog viewBlotterDialog;
    private PrintBlotterFrame printBlotterFrame;
    
    // ── Current active panel reference ──────────────────────────────────────
    private AddBlotterPanel currentAddBlotterPanel;
    
    // ── Users table model and data ──────────────────────────────────────────
    private DefaultTableModel usersTableModel;
    private List<UserData> usersData = new ArrayList<>();

    public dashboard() {
        this("User", "secretary");
    }

    public dashboard(String username) {
        this(username, "secretary");
    }

    public boolean isSecretary() {
        return "secretary".equalsIgnoreCase(currentRole);
    }

    public dashboard(String username, String role) {
        this.currentUsername = username;
        this.currentRole = (role != null) ? role.toLowerCase() : "secretary";
        setTitle("Barangay e-Blotter — Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setMinimumSize(new Dimension(1000, 650));
        setResizable(true);
        setLocationRelativeTo(null);
        
        loadBlotterData();
        
        printBlotterFrame = new PrintBlotterFrame(this, blotterData);
        viewBlotterDialog = new ViewBlotterDialog(
            this, blotterData,
            this::showUpdateStatusDialog,
            row -> printBlotterFrame.show(row),
            this::getConnection,
            () -> {
                loadBlotterData();
                refreshTableAndStats();
                refreshHistoryPanel();
            },
            currentRole
        );

        setContentPane(buildRoot());
    }

    // ── Database helpers ───────────────────────────────────────────────────
    Connection getConnection() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/ebs", "root", "");
    }

    private void loadBlotterData() {
        blotterData.clear();
        try (Connection conn = getConnection()) {
            String sql = "SELECT b.blotter_id, " +
                         "c.complainant_id, c.first_name AS c_fname, c.middle_name AS c_mname, " +
                         "c.last_name AS c_lname, c.suffix AS c_suffix, c.mobile_number AS c_mobile, c.purok AS c_purok, " +
                         "r.respondent_id, r.first_name AS r_fname, r.middle_name AS r_mname, " +
                         "r.last_name AS r_lname, r.suffix AS r_suffix, " +
                         "b.date, b.status, b.complt_type, b.description " +
                         "FROM blotter b " +
                         "JOIN complainant c ON b.complainant_id = c.complainant_id " +
                         "JOIN respondent r ON b.respondent_id = r.respondent_id " +
                         "ORDER BY b.date DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String cFname = rs.getString("c_fname") != null ? rs.getString("c_fname") : "";
                    String cMname = rs.getString("c_mname") != null ? rs.getString("c_mname") : "";
                    String cLname = rs.getString("c_lname") != null ? rs.getString("c_lname") : "";
                    String cSuffix = rs.getString("c_suffix") != null ? rs.getString("c_suffix") : "";
                    String rFname = rs.getString("r_fname") != null ? rs.getString("r_fname") : "";
                    String rMname = rs.getString("r_mname") != null ? rs.getString("r_mname") : "";
                    String rLname = rs.getString("r_lname") != null ? rs.getString("r_lname") : "";
                    String rSuffix = rs.getString("r_suffix") != null ? rs.getString("r_suffix") : "";

                    blotterData.add(new Object[]{
                        rs.getInt("blotter_id"),
                        buildFullName(cFname, cMname, cLname, cSuffix),
                        buildFullName(rFname, rMname, rLname, rSuffix),
                        rs.getDate("date") != null ? rs.getDate("date").toString() : "N/A",
                        rs.getString("status"),
                        rs.getString("c_purok"),
                        rs.getString("complt_type"),
                        rs.getString("description"),
                        rs.getInt("complainant_id"),
                        cFname, cMname, cLname, cSuffix,
                        rs.getInt("respondent_id"),
                        rFname, rMname, rLname, rSuffix,
                        rs.getString("c_mobile")
                    });
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error loading blotter data: " + e.getMessage());
        }
    }

    private String buildFullName(String first, String middle, String last, String suffix) {
        StringBuilder sb = new StringBuilder();
        if (first != null && !first.isEmpty()) sb.append(first);
        if (middle != null && !middle.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(middle);
        }
        if (last != null && !last.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(last);
        }
        if (suffix != null && !suffix.isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(suffix);
        }
        return sb.length() > 0 ? sb.toString() : "N/A";
    }

    private void updateStatCards() {
        long total = blotterData.size();
        long pending = blotterData.stream()
                .filter(r -> "pending".equalsIgnoreCase(r[4].toString())).count();
        long resolved = blotterData.stream()
                .filter(r -> "resolved".equalsIgnoreCase(r[4].toString())).count();

        if (totalValueLabel != null) totalValueLabel.setText(String.valueOf(total));
        if (pendingValueLabel != null) pendingValueLabel.setText(String.valueOf(pending));
        if (resolvedValueLabel != null) resolvedValueLabel.setText(String.valueOf(resolved));
    }

    private void refreshTableAndStats() {
        if (tableModel != null) {
            tableModel.setRowCount(0);
            for (Object[] row : blotterData) {
                String statusDisplay = "pending".equalsIgnoreCase(row[4].toString())
                        ? "Pending" : "Resolved";
                tableModel.addRow(new Object[]{
                    row[0], row[1], row[2], row[3], statusDisplay, "View"
                });
            }
        }
        updateStatCards();
        if (table != null) table.repaint();
    }

    private void setAllButtonsEnabled(boolean enabled) {
        if (addBtn != null) addBtn.setEnabled(enabled);
        if (searchBtn != null) searchBtn.setEnabled(enabled);
        if (searchField != null) searchField.setEnabled(enabled);
    }

    // ── Root layout with CardLayout ────────────────────────────────────────
    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        
        JPanel mainContent = new JPanel(new BorderLayout(0, 0));
        mainContent.setBackground(BG);
        mainContent.add(buildSidebar(), BorderLayout.WEST);
        
        // Create CardLayout panel
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(BG);
        
        // Build all panels
        dashboardPanel = buildDashboardPanel();
        addBlotterPanel = buildAddBlotterPanelWrapper();
        historyPanel = buildHistoryPanelWrapper();
        profilePanel = buildProfilePanel();
        usersPanel = buildUsersPanel();
        settingsPanel = buildSettingsPanel();
        
        // Add panels to card layout
        cardPanel.add(dashboardPanel, "dashboard");
        cardPanel.add(addBlotterPanel, "addBlotter");
        cardPanel.add(historyPanel, "history");
        cardPanel.add(profilePanel, "profile");
        cardPanel.add(usersPanel, "users");
        cardPanel.add(settingsPanel, "settings");
        
        mainContent.add(cardPanel, BorderLayout.CENTER);
        root.add(mainContent, BorderLayout.CENTER);
        
        return root;
    }

    // ── Header ─────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BLUE_DARK);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        JPanel badge = createHeaderBadge();
        JPanel titles = createHeaderTitles();
        
        left.add(badge);
        left.add(titles);

        JPanel right = createHeaderRight();
        
        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private JPanel createHeaderBadge() {
        JPanel badge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(WHITE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND));
                g2.drawRoundRect(8, 14, 22, 15, 4, 4);
                g2.drawArc(11, 6, 16, 14, 0, 180);
                g2.fillOval(17, 18, 4, 4);
                g2.drawLine(19, 22, 19, 26);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(42, 42));
        return badge;
    }

    private JPanel createHeaderTitles() {
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        JLabel appName = new JLabel("BARANGAY E-BLOTTER");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 10));
        appName.setForeground(TEXT_LIGHT);

        JLabel dashLabel = new JLabel("Dashboard");
        dashLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        dashLabel.setForeground(WHITE);

        titles.add(appName);
        titles.add(dashLabel);
        return titles;
    }

    private JPanel createHeaderRight() {
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);

        JPanel userInfoPanel = new JPanel();
        userInfoPanel.setOpaque(false);
        userInfoPanel.setLayout(new BoxLayout(userInfoPanel, BoxLayout.Y_AXIS));

        String displayName = currentUsername != null ? currentUsername : "Barangay User";
        if (displayName.contains("_")) {
            String[] p = displayName.split("_");
            displayName = cap(p[0]) + " " + (p.length > 1 ? cap(p[1]) : "");
        } else {
            displayName = cap(displayName);
        }

        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String roleDisplay = cap(currentRole);
        JLabel dateLabel = new JLabel(roleDisplay + "  ·  " + today);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dateLabel.setForeground(TEXT_LIGHT);
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        userInfoPanel.add(nameLabel);
        userInfoPanel.add(dateLabel);

        JPanel avatar = createAvatar();
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setForeground(WHITE);
        logoutBtn.setBackground(new Color(220, 53, 69));
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setPreferredSize(new Dimension(80, 36));
        logoutBtn.addActionListener(e -> logout());

        right.add(userInfoPanel);
        right.add(avatar);
        right.add(Box.createHorizontalStrut(12));
        right.add(logoutBtn);
        return right;
    }

    private JPanel createAvatar() {
        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLUE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String initial = (currentUsername != null && !currentUsername.isEmpty())
                        ? currentUsername.substring(0, 1).toUpperCase() : "U";
                g2.drawString(initial,
                    (getWidth() - fm.stringWidth(initial)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(40, 40));
        return avatar;
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    // ── Sidebar ─────────────────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Gradient background
                GradientPaint gp = new GradientPaint(0, 0, SIDEBAR_BG, 0, getHeight(), 
                    new Color(20, 55, 90));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setPreferredSize(new Dimension(260, 0));
        sidebar.setBorder(new EmptyBorder(20, 12, 20, 12));

        // Brand panel
        JPanel brandPanel = createBrandPanel();
        sidebar.add(brandPanel);
        sidebar.add(Box.createVerticalStrut(20));

        // Navigation label
        JLabel navLabel = new JLabel("MAIN NAVIGATION");
        navLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        navLabel.setForeground(new Color(150, 180, 210));
        navLabel.setBorder(new EmptyBorder(0, 12, 8, 0));
        navLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(navLabel);

        // Navigation buttons
        homeBtn = createSidebarButton("Dashboard", "home", true);
        homeBtn.addActionListener(e -> {
            cardLayout.show(cardPanel, "dashboard");
            setActiveButton(homeBtn);
        });
        sidebar.add(homeBtn);
        sidebar.add(Box.createVerticalStrut(4));

        addBlotterBtn = createSidebarButton("Add New Blotter", "plus", isSecretary());
        addBlotterBtn.addActionListener(e -> {
            if (isSecretary()) {
                refreshAddBlotterPanel();
                cardLayout.show(cardPanel, "addBlotter");
                setActiveButton(addBlotterBtn);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Only secretaries can add new blotter entries.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
            }
        });
        sidebar.add(addBlotterBtn);
        sidebar.add(Box.createVerticalStrut(4));

        historyBtn = createSidebarButton("Blotter History", "clock", true);
        historyBtn.addActionListener(e -> {
            refreshHistoryPanel();
            cardLayout.show(cardPanel, "history");
            setActiveButton(historyBtn);
        });
        sidebar.add(historyBtn);
        sidebar.add(Box.createVerticalStrut(16));

        // Management section
        JLabel mgmtLabel = new JLabel("MANAGEMENT");
        mgmtLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        mgmtLabel.setForeground(new Color(150, 180, 210));
        mgmtLabel.setBorder(new EmptyBorder(0, 12, 8, 0));
        mgmtLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(mgmtLabel);

        profileBtn = createSidebarButton("Profile", "user", true);
        profileBtn.addActionListener(e -> {
            refreshProfilePanel();
            cardLayout.show(cardPanel, "profile");
            setActiveButton(profileBtn);
        });
        sidebar.add(profileBtn);
        sidebar.add(Box.createVerticalStrut(4));

        usersBtn = createSidebarButton("Users", "users", isSecretary());
        usersBtn.addActionListener(e -> {
            if (isSecretary()) {
                refreshUsersPanel();
                cardLayout.show(cardPanel, "users");
                setActiveButton(usersBtn);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Only secretaries can manage users.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
            }
        });
        sidebar.add(usersBtn);
        sidebar.add(Box.createVerticalStrut(4));

        settingsBtn = createSidebarButton("Settings", "settings", true);
        settingsBtn.addActionListener(e -> {
            refreshSettingsPanel();
            cardLayout.show(cardPanel, "settings");
            setActiveButton(settingsBtn);
        });
        sidebar.add(settingsBtn);

        sidebar.add(Box.createVerticalGlue());
        
        // Logout button
        JButton logoutSideBtn = createSidebarLogoutButton();
        sidebar.add(logoutSideBtn);

        sidebarButtons.add(homeBtn);
        sidebarButtons.add(addBlotterBtn);
        sidebarButtons.add(historyBtn);
        sidebarButtons.add(profileBtn);
        sidebarButtons.add(usersBtn);
        sidebarButtons.add(settingsBtn);

        return sidebar;
    }

    private JPanel createBrandPanel() {
        JPanel brand = new JPanel();
        brand.setOpaque(false);
        brand.setLayout(new BoxLayout(brand, BoxLayout.X_AXIS));
        brand.setBorder(new EmptyBorder(8, 12, 8, 12));
        brand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel iconBox = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(52, 134, 235));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
                FontMetrics fm = g2.getFontMetrics();
                String b = "B";
                g2.drawString(b, (getWidth() - fm.stringWidth(b)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        iconBox.setPreferredSize(new Dimension(44, 44));
        iconBox.setMaximumSize(new Dimension(44, 44));
        iconBox.setMinimumSize(new Dimension(44, 44));
        iconBox.setOpaque(false);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.setBorder(new EmptyBorder(0, 12, 0, 0));

        JLabel brandTitle = new JLabel("E-Blotter");
        brandTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        brandTitle.setForeground(WHITE);

        JLabel brandSub = new JLabel("Barangay System");
        brandSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        brandSub.setForeground(new Color(150, 180, 210));

        textBox.add(brandTitle);
        textBox.add(brandSub);

        brand.add(iconBox);
        brand.add(textBox);
        brand.add(Box.createHorizontalGlue());

        return brand;
    }

    private JButton createSidebarButton(String text, String iconName, boolean enabled) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // Background
                if (!isEnabled()) {
                    g2.setColor(new Color(100, 100, 100, 30));
                } else if (getModel().isPressed()) {
                    g2.setColor(SIDEBAR_HOVER);
                } else if (getModel().isRollover()) {
                    g2.setColor(SIDEBAR_HOVER);
                } else {
                    g2.setColor(new Color(255, 255, 255, 0));
                }
                g2.fillRoundRect(4, 2, w - 8, h - 4, 10, 10);

                // Icon
                if (isEnabled()) {
                    g2.setColor(new Color(100, 180, 255));
                } else {
                    g2.setColor(new Color(120, 120, 130));
                }
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int iconX = 16;
                int iconY = h / 2;
                drawSidebarIcon(g2, iconName, iconX, iconY);

                // Text
                if (isEnabled()) {
                    g2.setColor(WHITE);
                } else {
                    g2.setColor(new Color(160, 160, 170));
                }
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int textX = 48;
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }

            private void drawSidebarIcon(Graphics2D g2, String icon, int x, int y) {
                switch (icon) {
                    case "home":
                        g2.drawLine(x - 2, y + 4, x + 4, y - 6);
                        g2.drawLine(x + 4, y - 6, x + 10, y + 4);
                        g2.drawRect(x, y + 4, 8, 8);
                        break;
                    case "plus":
                        g2.drawOval(x - 4, y - 8, 16, 16);
                        g2.drawLine(x + 4, y - 4, x + 4, y + 4);
                        g2.drawLine(x, y, x + 8, y);
                        break;
                    case "clock":
                        g2.drawOval(x - 4, y - 10, 16, 16);
                        g2.drawLine(x + 4, y - 10, x + 4, y - 5);
                        g2.drawLine(x + 4, y - 2, x + 8, y - 2);
                        break;
                    case "user":
                        g2.drawOval(x, y - 12, 10, 10);
                        g2.drawArc(x - 4, y - 2, 18, 12, 0, 180);
                        break;
                    case "users":
                        g2.drawOval(x - 2, y - 12, 8, 8);
                        g2.drawArc(x - 6, y - 4, 16, 8, 0, 180);
                        g2.drawOval(x + 8, y - 8, 6, 6);
                        g2.drawArc(x + 6, y, 10, 6, 0, 180);
                        break;
                    case "settings":
                        g2.drawOval(x, y - 10, 10, 10);
                        g2.drawLine(x + 5, y - 14, x + 5, y - 10);
                        g2.drawLine(x + 5, y, x + 5, y + 4);
                        break;
                }
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(WHITE);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setPreferredSize(new Dimension(220, 48));
        button.setEnabled(enabled);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        return button;
    }

    private void setActiveButton(JButton activeBtn) {
        for (JButton btn : sidebarButtons) {
            btn.repaint();
        }
    }

    private JButton createSidebarLogoutButton() {
        JButton button = new JButton("Logout") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                if (getModel().isPressed()) {
                    g2.setColor(new Color(220, 53, 69, 180));
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(220, 53, 69, 200));
                } else {
                    g2.setColor(new Color(220, 53, 69, 100));
                }
                g2.fillRoundRect(4, 2, w - 8, h - 4, 10, 10);

                g2.setColor(new Color(255, 150, 150));
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int iconX = 16;
                int iconY = h / 2;
                g2.drawLine(iconX + 4, iconY - 4, iconX + 12, iconY - 4);
                g2.drawLine(iconX + 8, iconY - 8, iconX + 12, iconY - 4);
                g2.drawLine(iconX + 8, iconY, iconX + 12, iconY - 4);
                g2.drawRect(iconX - 2, iconY - 10, 10, 16);

                g2.setColor(new Color(255, 200, 200));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                FontMetrics fm = g2.getFontMetrics();
                int textX = 48;
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), textX, textY);

                g2.dispose();
            }
        };

        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(WHITE);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        button.setPreferredSize(new Dimension(220, 48));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);
        button.addActionListener(e -> logout());
        
        return button;
    }

    // ── Dashboard Panel (Home) ──────────────────────────────────────────────
    private JPanel buildDashboardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PAGE_BG);
        panel.setBorder(new EmptyBorder(20, 24, 20, 24));
        
        panel.add(buildWelcomeBanner());
        panel.add(Box.createVerticalStrut(20));
        panel.add(buildStatCards());
        panel.add(Box.createVerticalStrut(24));
        panel.add(buildTablePanel());
        
        return panel;
    }

    private JPanel buildWelcomeBanner() {
        JPanel banner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(4, 4, getWidth() - 6, getHeight() - 6, 16, 16);
                GradientPaint gp = new GradientPaint(0, 0, BLUE_DARK, getWidth(), 0, new Color(45, 80, 130));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 6, getHeight() - 6, 14, 14);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setLayout(new BorderLayout());
        banner.setBorder(new EmptyBorder(20, 28, 20, 28));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        banner.setPreferredSize(new Dimension(0, 100));

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        String greeting = "Good " + getTimeOfDay() + ", " + 
            (currentUsername != null ? cap(currentUsername.split("_")[0]) : "User") + "!";
        JLabel greetingLabel = new JLabel(greeting);
        greetingLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        greetingLabel.setForeground(WHITE);

        JLabel subLabel = new JLabel("Welcome back to Barangay E-Blotter System");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(new Color(220, 235, 255));

        textPanel.add(greetingLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(subLabel);

        banner.add(textPanel, BorderLayout.WEST);
        
        return banner;
    }

    private String getTimeOfDay() {
        int hour = java.time.LocalTime.now().getHour();
        if (hour < 12) return "Morning";
        else if (hour < 18) return "Afternoon";
        else return "Evening";
    }

    private JPanel buildStatCards() {
        JPanel row = new JPanel(new GridLayout(1, 3, 20, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        row.setPreferredSize(new Dimension(0, 130));

        long total = blotterData.size();
        long pending = blotterData.stream()
                .filter(r -> "pending".equalsIgnoreCase(r[4].toString())).count();
        long resolved = blotterData.stream()
                .filter(r -> "resolved".equalsIgnoreCase(r[4].toString())).count();

        totalValueLabel = new JLabel(String.valueOf(total));
        pendingValueLabel = new JLabel(String.valueOf(pending));
        resolvedValueLabel = new JLabel(String.valueOf(resolved));

        row.add(statCard("TOTAL RECORDS", totalValueLabel, STAT_BLUE, ""));
        row.add(statCard("PENDING CASES", pendingValueLabel, STAT_ORANGE, ""));
        row.add(statCard("RESOLVED", resolvedValueLabel, STAT_GREEN, ""));
        return row;
    }

    private JPanel statCard(String label, JLabel valueLabel, Color accent, String emoji) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(4, 5, getWidth()-6, getHeight()-6, 16, 16);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 14, 14);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 6, getHeight()-4, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 28, 18, 20));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        topRow.setOpaque(false);
        
        JLabel emojiLabel = new JLabel(emoji);
        emojiLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        topRow.add(emojiLabel);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 42));
        valueLabel.setForeground(TEXT_PRI);
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_SEC);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(topRow);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(lbl);
        
        return card;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        addBtn = createStyledButton("+ New Blotter", BLUE, BLUE_HOVER, WHITE);
        addBtn.addActionListener(e -> {
            if (isSecretary()) {
                refreshAddBlotterPanel();
                cardLayout.show(cardPanel, "addBlotter");
                setActiveButton(addBlotterBtn);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Only secretaries can add new blotter entries.",
                    "Access Denied", JOptionPane.WARNING_MESSAGE);
            }
        });

        JPanel searchBar = createSearchBar();
        
        searchBtn = createStyledButton("Search", BLUE, BLUE_HOVER, WHITE);
        searchBtn.addActionListener(e -> filterTable());

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTools.setOpaque(false);
        if (isSecretary()) {
            leftTools.add(addBtn);
        }

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);
        rightTools.add(searchBar);
        rightTools.add(searchBtn);

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(rightTools, BorderLayout.EAST);

        String[] cols = {"ID", "Complainant", "Respondent", "Date", "Status", "Action"};
        tableModel = new DefaultTableModel(null, cols) {
            @Override public boolean isCellEditable(int row, int col) { return col == 5; }
        };
        
        for (Object[] row : blotterData) {
            String sd = "pending".equalsIgnoreCase(row[4].toString()) ? "Pending" : "Resolved";
            tableModel.addRow(new Object[]{row[0], row[1], row[2], row[3], sd, "View"});
        }

        table = createStyledTable();
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        scroll.setBackground(WHITE);

        JPanel tableContainer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(4, 5, getWidth()-6, getHeight()-6, 16, 16);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 14, 14);
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth()-5, getHeight()-5, 14, 14);
                g2.dispose();
            }
        };
        tableContainer.setOpaque(false);
        tableContainer.setBorder(new EmptyBorder(4, 4, 4, 4));
        tableContainer.add(scroll, BorderLayout.CENTER);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(tableContainer, BorderLayout.CENTER);
        
        return panel;
    }

    private JPanel createSearchBar() {
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(WHITE);
        searchBar.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_CLR, 1, 8),
            new EmptyBorder(0, 12, 0, 0)));
        searchBar.setPreferredSize(new Dimension(220, 38));

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchIcon.setForeground(TEXT_SEC);

        searchField = new JTextField("Search blotter...");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(TEXT_SEC);
        searchField.setBorder(null);
        searchField.setOpaque(false);
        searchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search blotter...")) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_PRI);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search blotter...");
                    searchField.setForeground(TEXT_SEC);
                }
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { filterTable(); }
        });

        searchBar.add(searchIcon, BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER);
        
        return searchBar;
    }

    private JTable createStyledTable() {
        JTable tbl = new JTable(tableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
                }
                c.setForeground(TEXT_PRI);
                if (c instanceof JComponent jcomp) {
                    jcomp.setBorder(new EmptyBorder(10, 12, 10, 12));
                }
                return c;
            }
        };

        tbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent evt) {
                int col = tbl.columnAtPoint(evt.getPoint());
                int row = tbl.rowAtPoint(evt.getPoint());
                if (col == 4 && row >= 0 && isSecretary()) showUpdateStatusDialog(row);
            }
        });

        tbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tbl.setRowHeight(48);
        tbl.setShowGrid(false);
        tbl.setIntercellSpacing(new Dimension(0, 0));
        tbl.setSelectionBackground(BLUE_LIGHT);
        tbl.setSelectionForeground(TEXT_PRI);

        JTableHeader header = tbl.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(240, 245, 250));
        header.setForeground(TEXT_PRI);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_CLR));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);

        int[] widths = {80, 180, 180, 110, 100, 120};
        for (int i = 0; i < widths.length; i++)
            tbl.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        tbl.getColumnModel().getColumn(0).setCellRenderer(
            (t, val, sel, foc, row, col) -> {
                JLabel l = new JLabel(val == null ? "" : "#" + val);
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setForeground(BLUE);
                l.setBorder(new EmptyBorder(0, 12, 0, 0));
                l.setOpaque(true);
                l.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
                return l;
            });

        tbl.getColumnModel().getColumn(4).setCellRenderer(
            (t, val, sel, foc, row, col) -> {
                String status = val == null ? "" : val.toString();
                boolean isPending = "Pending".equals(status);

                JLabel l = new JLabel(status);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setForeground(isPending ? PENDING_FG : RESOLVED_FG);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setOpaque(true);
                l.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));

                JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
                wrap.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));

                JPanel pill = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                            RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(isPending ? PENDING_BG : RESOLVED_BG);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                        g2.dispose();
                    }
                };
                pill.setLayout(new BorderLayout());
                pill.setOpaque(false);
                pill.add(l, BorderLayout.CENTER);
                pill.setBorder(new EmptyBorder(4, 14, 4, 14));

                wrap.add(pill);
                return wrap;
            });

        tbl.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        tbl.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        return tbl;
    }

    // ── Add Blotter Panel Wrapper ──────────────────────────────────────────
    private JPanel buildAddBlotterPanelWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(PAGE_BG);
        return wrapper;
    }

    private void refreshAddBlotterPanel() {
        addBlotterPanel.removeAll();
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(PAGE_BG);
        
        // Header
        content.add(buildAddBlotterHeader(), BorderLayout.NORTH);
        
        // Main content - Create AddBlotterPanel without dialog
        currentAddBlotterPanel = new AddBlotterPanel(
            null,
            this::getConnection,
            blotterData.size() + 1,
            () -> {
                loadBlotterData();
                refreshTableAndStats();
                refreshHistoryPanel();
                cardLayout.show(cardPanel, "dashboard");
                setActiveButton(homeBtn);
                JOptionPane.showMessageDialog(this,
                    "Blotter entry saved successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        );
        currentAddBlotterPanel.setBackground(PAGE_BG);
        
        content.add(currentAddBlotterPanel, BorderLayout.CENTER);
        
        addBlotterPanel.add(content);
        addBlotterPanel.revalidate();
        addBlotterPanel.repaint();
    }

    private JPanel buildAddBlotterHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(4, 4, getWidth() - 6, getHeight() - 6, 16, 16);
                GradientPaint gp = new GradientPaint(0, 0, BLUE_DARK, getWidth(), 0, new Color(45, 80, 130));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 6, getHeight() - 6, 14, 14);
                g2.dispose();
            }
        };
          header.setOpaque(false);
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(24, 32, 24, 32));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        header.setPreferredSize(new Dimension(0, 100));

       

        JButton backBtn = createBackButton();
        backBtn.addActionListener(e -> {
            cardLayout.show(cardPanel, "dashboard");
            setActiveButton(homeBtn);
        });

        JLabel titleLabel = new JLabel("Add New Blotter Entry");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(WHITE);

        header.add(backBtn, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        
        return header;
    }

    private JButton createBackButton() {
        JButton backBtn = new JButton("← Back to Dashboard");
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backBtn.setForeground(WHITE);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return backBtn;
    }

    // ── History Panel Wrapper ───────────────────────────────────────────────
    private JPanel buildHistoryPanelWrapper() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(PAGE_BG);
        return wrapper;
    }

    private void refreshHistoryPanel() {
        historyPanel.removeAll();
        
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(PAGE_BG);
        
        // Header
        content.add(buildHistoryHeader(), BorderLayout.NORTH);
        
        // Main content
        content.add(buildHistoryContent(), BorderLayout.CENTER);
        
        historyPanel.add(content);
        historyPanel.revalidate();
        historyPanel.repaint();
    }

    private JPanel buildHistoryHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(4, 4, getWidth() - 6, getHeight() - 6, 16, 16);
                GradientPaint gp = new GradientPaint(0, 0, BLUE_DARK, getWidth(), 0, new Color(45, 80, 130));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 6, getHeight() - 6, 14, 14);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setLayout(new BorderLayout());
        header.setBorder(new EmptyBorder(20, 28, 20, 28));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        header.setPreferredSize(new Dimension(0, 100));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        JButton backBtn = createBackButton();
        backBtn.addActionListener(e -> {
            cardLayout.show(cardPanel, "dashboard");
            setActiveButton(homeBtn);
        });

        JLabel titleLabel = new JLabel("Blotter History — Complete Records");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(WHITE);

        leftPanel.add(backBtn);
        leftPanel.add(titleLabel);

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        statsPanel.setOpaque(false);

        long total = blotterData.size();
        long pending = blotterData.stream().filter(r -> "pending".equalsIgnoreCase(r[4].toString())).count();
        long resolved = blotterData.stream().filter(r -> "resolved".equalsIgnoreCase(r[4].toString())).count();

        statsPanel.add(createHeaderStat("Total", String.valueOf(total), STAT_BLUE));
        statsPanel.add(createHeaderStat("Pending", String.valueOf(pending), STAT_ORANGE));
        statsPanel.add(createHeaderStat("Resolved", String.valueOf(resolved), STAT_GREEN));

        header.add(leftPanel, BorderLayout.WEST);
        header.add(statsPanel, BorderLayout.EAST);
        
        return header;
    }

    private JPanel createHeaderStat(String label, String value, Color accent) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valueLabel.setForeground(WHITE);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel labelLabel = new JLabel(label);
        labelLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        labelLabel.setForeground(TEXT_LIGHT);
        labelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        panel.add(valueLabel);
        panel.add(Box.createVerticalStrut(2));
        panel.add(labelLabel);

        return panel;
    }

    private JPanel buildHistoryContent() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(PAGE_BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));
        
        content.add(buildHistoryFilterBar(), BorderLayout.NORTH);
        content.add(buildHistoryTableContainer(), BorderLayout.CENTER);
        
        return content;
    }

    private JPanel buildHistoryFilterBar() {
        JPanel filterCard = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(3, 4, getWidth()-4, getHeight()-4, 12, 12);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 10, 10);
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth()-5, getHeight()-5, 10, 10);
                g2.dispose();
            }
        };
        filterCard.setOpaque(false);
        filterCard.setBorder(new EmptyBorder(12, 20, 12, 20));
        filterCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        filterCard.setPreferredSize(new Dimension(0, 70));

        JPanel filterLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterLeft.setOpaque(false);

        JLabel filterLabel = new JLabel("Status:");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterLabel.setForeground(TEXT_PRI);

        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Records", "Pending", "Resolved"});
        statusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusFilter.setPreferredSize(new Dimension(130, 36));
        statusFilter.setBackground(WHITE);

        JLabel timeLabel = new JLabel("Period:");
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        timeLabel.setForeground(TEXT_PRI);

        JComboBox<String> timeFilter = new JComboBox<>(new String[]{
            "All Time", "Today", "Last 7 Days", "Last 30 Days"
        });
        timeFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeFilter.setPreferredSize(new Dimension(130, 36));
        timeFilter.setBackground(WHITE);

        filterLeft.add(filterLabel);
        filterLeft.add(statusFilter);
        filterLeft.add(timeLabel);
        filterLeft.add(timeFilter);

        JPanel filterRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRight.setOpaque(false);

        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        searchBar.setBackground(WHITE);
        searchBar.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_CLR, 1, 8),
            new EmptyBorder(0, 12, 0, 0)));
        searchBar.setPreferredSize(new Dimension(220, 36));

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchIcon.setForeground(TEXT_SEC);

        JTextField historySearchField = new JTextField("Search history...");
        historySearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historySearchField.setForeground(TEXT_SEC);
        historySearchField.setBorder(null);
        historySearchField.setOpaque(false);

        searchBar.add(searchIcon, BorderLayout.WEST);
        searchBar.add(historySearchField, BorderLayout.CENTER);

        JButton exportBtn = createCardButton("Export", STAT_GREEN, new Color(40, 150, 100), WHITE);
        exportBtn.setPreferredSize(new Dimension(100, 36));
        exportBtn.addActionListener(e -> exportHistoryData());

        filterRight.add(searchBar);
        filterRight.add(exportBtn);

        filterCard.add(filterLeft, BorderLayout.WEST);
        filterCard.add(filterRight, BorderLayout.EAST);

        return filterCard;
    }

    private JPanel buildHistoryTableContainer() {
        String[] columns = {"ID", "Complainant", "Respondent", "Type", "Date", "Status", "Description"};
        DefaultTableModel historyModel = new DefaultTableModel(null, columns) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        for (Object[] row : blotterData) {
            String statusDisplay = "pending".equalsIgnoreCase(row[4].toString()) ? "Pending" : "Resolved";
            String description = row[7] != null ? row[7].toString() : "";
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            historyModel.addRow(new Object[]{
                "#" + row[0], row[1], row[2], 
                row[6] != null ? row[6].toString() : "N/A",
                row[3], statusDisplay, description
            });
        }

        JTable historyTable = new JTable(historyModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
                }
                if (c instanceof JComponent jc) {
                    jc.setBorder(new EmptyBorder(8, 12, 8, 12));
                }
                return c;
            }
        };

        historyTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        historyTable.setRowHeight(42);
        historyTable.setShowGrid(false);
        historyTable.setIntercellSpacing(new Dimension(0, 0));
        historyTable.setSelectionBackground(BLUE_LIGHT);

        JTableHeader header = historyTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(240, 245, 250));
        header.setForeground(TEXT_PRI);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_CLR));
        header.setPreferredSize(new Dimension(0, 42));

        historyTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = historyTable.getSelectedRow();
                    if (row >= 0) {
                        viewBlotterDialog.show(row);
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);

        JPanel container = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(4, 5, getWidth()-6, getHeight()-6, 16, 16);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth()-4, getHeight()-4, 14, 14);
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth()-5, getHeight()-5, 14, 14);
                g2.dispose();
            }
        };
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(4, 4, 4, 4));
        container.add(scroll, BorderLayout.CENTER);

        return container;
    }

    private void exportHistoryData() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export History Data");
        fileChooser.setSelectedFile(new java.io.File("blotter_history_" +
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(fileChooser.getSelectedFile())) {
                writer.println("ID,Complainant,Respondent,Complaint Type,Date,Status,Description,Address");
                for (Object[] row : blotterData) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        row[0], row[1], row[2], row[6], row[3], row[4], row[7], row[5]);
                }
                JOptionPane.showMessageDialog(this,
                    "History data exported successfully!",
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting data: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Profile Panel ───────────────────────────────────────────────────────
    private JPanel buildProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BG);
        panel.add(buildProfileHeader(), BorderLayout.NORTH);
        panel.add(buildProfileContent(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildProfileHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(4, 4, getWidth() - 6, getHeight() - 6, 16, 16);
                GradientPaint gp = new GradientPaint(0, 0, BLUE_DARK, getWidth(), 0, new Color(45, 80, 130));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 6, getHeight() - 6, 14, 14);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));
        header.setPreferredSize(new Dimension(0, 70));

        JButton backBtn = createBackButton();
        backBtn.addActionListener(e -> {
            cardLayout.show(cardPanel, "dashboard");
            setActiveButton(homeBtn);
        });

        JLabel titleLabel = new JLabel("My Profile");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(WHITE);

        header.add(backBtn, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);
        
        return header;
    }

    private JPanel buildProfileContent() {
        JPanel content = new JPanel();
        content.setBackground(PAGE_BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 24, 24, 24));

        JPanel profileCard = createProfileInfoCard();
        content.add(profileCard);
        content.add(Box.createVerticalStrut(20));

        JPanel passwordCard = createPasswordChangeCard();
        content.add(passwordCard);

        return content;
    }

    private JPanel createProfileInfoCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(4, 5, getWidth() - 6, getHeight() - 6, 16, 16);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 5, getHeight() - 5, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 28, 28, 28));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JLabel sectionLabel = new JLabel("PROFILE INFORMATION");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sectionLabel.setForeground(TEXT_SEC);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel avatarPanel = createProfileAvatar();
        avatarPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel infoGrid = new JPanel(new GridLayout(0, 2, 20, 12));
        infoGrid.setOpaque(false);
        infoGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        infoGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoGrid.add(createInfoLabel("Username:"));
        infoGrid.add(createInfoValue(currentUsername));
        infoGrid.add(createInfoLabel("Role:"));
        infoGrid.add(createInfoValue(cap(currentRole)));

        card.add(sectionLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(avatarPanel);
        card.add(Box.createVerticalStrut(20));
        card.add(infoGrid);

        return card;
    }

    private JPanel createProfileAvatar() {
        JPanel avatarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        avatarPanel.setOpaque(false);

        JPanel avatar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLUE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 28));
                FontMetrics fm = g2.getFontMetrics();
                String initial = (currentUsername != null && !currentUsername.isEmpty())
                        ? currentUsername.substring(0, 1).toUpperCase() : "U";
                g2.drawString(initial,
                    (getWidth() - fm.stringWidth(initial)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(80, 80));

        JPanel namePanel = new JPanel();
        namePanel.setOpaque(false);
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setBorder(new EmptyBorder(0, 16, 0, 0));

        String displayName = currentUsername != null ? currentUsername : "User";
        if (displayName.contains("_")) {
            String[] parts = displayName.split("_");
            displayName = cap(parts[0]) + " " + (parts.length > 1 ? cap(parts[1]) : "");
        } else {
            displayName = cap(displayName);
        }

        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        nameLabel.setForeground(TEXT_PRI);

        JLabel roleLabel = new JLabel(cap(currentRole));
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleLabel.setForeground(TEXT_SEC);

        namePanel.add(nameLabel);
        namePanel.add(Box.createVerticalStrut(4));
        namePanel.add(roleLabel);

        avatarPanel.add(avatar);
        avatarPanel.add(namePanel);

        return avatarPanel;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT_SEC);
        return label;
    }

    private JLabel createInfoValue(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(TEXT_PRI);
        return label;
    }

    private JPanel createPasswordChangeCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(4, 5, getWidth() - 6, getHeight() - 6, 16, 16);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 5, getHeight() - 5, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 28, 28, 28));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));

        JLabel sectionLabel = new JLabel("CHANGE PASSWORD");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sectionLabel.setForeground(TEXT_SEC);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField currentPasswordField = createStyledPasswordField("Current password");
        currentPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentPasswordField.setMaximumSize(new Dimension(400, 38));

        JPasswordField newPasswordField = createStyledPasswordField("New password");
        newPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        newPasswordField.setMaximumSize(new Dimension(400, 38));

        JPasswordField confirmPasswordField = createStyledPasswordField("Confirm new password");
        confirmPasswordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmPasswordField.setMaximumSize(new Dimension(400, 38));

        JButton changePasswordBtn = createCardButton("Change Password", BLUE, BLUE_HOVER, WHITE);
        changePasswordBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        changePasswordBtn.addActionListener(e -> {
            String currentPass = new String(currentPasswordField.getPassword());
            String newPass = new String(newPasswordField.getPassword());
            String confirmPass = new String(confirmPasswordField.getPassword());
            
            if (currentPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "New passwords do not match.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (newPass.length() < 6) {
                JOptionPane.showMessageDialog(this, "Password must be at least 6 characters long.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            JOptionPane.showMessageDialog(this,
                "Password change functionality requires user table setup.\nThis is a demo feature.",
                "Information", JOptionPane.INFORMATION_MESSAGE);
            
            currentPasswordField.setText("");
            newPasswordField.setText("");
            confirmPasswordField.setText("");
        });

        card.add(sectionLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(createFieldLabel("Current Password"));
        card.add(Box.createVerticalStrut(4));
        card.add(currentPasswordField);
        card.add(Box.createVerticalStrut(16));
        card.add(createFieldLabel("New Password"));
        card.add(Box.createVerticalStrut(4));
        card.add(newPasswordField);
        card.add(Box.createVerticalStrut(16));
        card.add(createFieldLabel("Confirm New Password"));
        card.add(Box.createVerticalStrut(4));
        card.add(confirmPasswordField);
        card.add(Box.createVerticalStrut(20));
        card.add(changePasswordBtn);

        return card;
    }

    private JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(TEXT_PRI);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private JPasswordField createStyledPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRI);
        field.setBackground(WHITE);
        field.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_CLR, 1, 8),
            new EmptyBorder(8, 12, 8, 12)));
        field.setEchoChar('•');
        
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (new String(field.getPassword()).equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRI);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (new String(field.getPassword()).isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_SEC);
                    field.setEchoChar((char) 0);
                } else {
                    field.setEchoChar('•');
                }
            }
        });
        
        field.setText(placeholder);
        field.setForeground(TEXT_SEC);
        field.setEchoChar((char) 0);
        
        return field;
    }

    private void refreshProfilePanel() {
        cardPanel.remove(profilePanel);
        profilePanel = buildProfilePanel();
        cardPanel.add(profilePanel, "profile");
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    // ── Users Panel ─────────────────────────────────────────────────────────
    private static class UserData {
        int id;
        String username;
        String fullName;
        String role;
        String status;
        String lastLogin;

        UserData(int id, String username, String fullName, String role, String status, String lastLogin) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.role = role;
            this.status = status;
            this.lastLogin = lastLogin;
        }
    }

    private JPanel buildUsersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BG);
        panel.add(buildUsersHeader(), BorderLayout.NORTH);
        panel.add(buildUsersContent(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildUsersHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(4, 4, getWidth() - 6, getHeight() - 6, 16, 16);
                GradientPaint gp = new GradientPaint(0, 0, BLUE_DARK, getWidth(), 0, new Color(45, 80, 130));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 6, getHeight() - 6, 14, 14);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));
        header.setPreferredSize(new Dimension(0, 70));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        leftPanel.setOpaque(false);

        JButton backBtn = createBackButton();
        backBtn.addActionListener(e -> {
            cardLayout.show(cardPanel, "dashboard");
            setActiveButton(homeBtn);
        });

        JLabel titleLabel = new JLabel("User Management");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(WHITE);

        leftPanel.add(backBtn);
        leftPanel.add(titleLabel);

        JButton addUserBtn = createCardButton("+ Add New User", STAT_GREEN, new Color(40, 150, 100), WHITE);
        addUserBtn.addActionListener(e -> showAddUserDialog());

        header.add(leftPanel, BorderLayout.WEST);
        header.add(addUserBtn, BorderLayout.EAST);

        return header;
    }

    private JPanel buildUsersContent() {
        JPanel content = new JPanel(new BorderLayout(0, 16));
        content.setBackground(PAGE_BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        content.add(buildUsersFilterBar(), BorderLayout.NORTH);
        content.add(buildUsersTableContainer(), BorderLayout.CENTER);

        return content;
    }

    private JPanel buildUsersFilterBar() {
        JPanel filterCard = new JPanel(new BorderLayout(12, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(3, 4, getWidth() - 4, getHeight() - 4, 12, 12);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 10, 10);
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 5, getHeight() - 5, 10, 10);
                g2.dispose();
            }
        };
        filterCard.setOpaque(false);
        filterCard.setBorder(new EmptyBorder(12, 20, 12, 20));
        filterCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        filterCard.setPreferredSize(new Dimension(0, 70));

        JPanel filterLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterLeft.setOpaque(false);

        JLabel filterLabel = new JLabel("Role:");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterLabel.setForeground(TEXT_PRI);

        JComboBox<String> roleFilter = new JComboBox<>(new String[]{"All Users", "Secretary", "Staff", "Admin"});
        roleFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        roleFilter.setPreferredSize(new Dimension(130, 36));
        roleFilter.setBackground(WHITE);

        filterLeft.add(filterLabel);
        filterLeft.add(roleFilter);

        JPanel filterRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterRight.setOpaque(false);

        JPanel searchBar = new JPanel(new BorderLayout(10, 0));
        searchBar.setBackground(WHITE);
        searchBar.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_CLR, 1, 8),
            new EmptyBorder(0, 12, 0, 0)));
        searchBar.setPreferredSize(new Dimension(220, 36));

        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchIcon.setForeground(TEXT_SEC);

        JTextField usersSearchField = new JTextField("Search users...");
        usersSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        usersSearchField.setForeground(TEXT_SEC);
        usersSearchField.setBorder(null);
        usersSearchField.setOpaque(false);

        searchBar.add(searchIcon, BorderLayout.WEST);
        searchBar.add(usersSearchField, BorderLayout.CENTER);

        filterRight.add(searchBar);

        filterCard.add(filterLeft, BorderLayout.WEST);
        filterCard.add(filterRight, BorderLayout.EAST);

        return filterCard;
    }

    private JPanel buildUsersTableContainer() {
        String[] columns = {"ID", "Username", "Full Name", "Role", "Status", "Actions"};
        usersTableModel = new DefaultTableModel(null, columns) {
            @Override public boolean isCellEditable(int row, int col) { return col == 5; }
        };

        loadUsersData();

        JTable usersTable = new JTable(usersTableModel) {
            @Override public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row)) {
                    c.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
                }
                if (c instanceof JComponent jc) {
                    jc.setBorder(new EmptyBorder(8, 12, 8, 12));
                }
                return c;
            }
        };

        usersTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        usersTable.setRowHeight(48);
        usersTable.setShowGrid(false);
        usersTable.setIntercellSpacing(new Dimension(0, 0));
        usersTable.setSelectionBackground(BLUE_LIGHT);

        JTableHeader header = usersTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(240, 245, 250));
        header.setForeground(TEXT_PRI);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_CLR));
        header.setPreferredSize(new Dimension(0, 42));

        usersTable.getColumnModel().getColumn(4).setCellRenderer((t, val, sel, foc, row, col) -> {
            String status = val == null ? "" : val.toString();
            boolean isActive = "Active".equals(status);

            JLabel l = new JLabel(status);
            l.setFont(new Font("Segoe UI", Font.BOLD, 11));
            l.setForeground(isActive ? new Color(40, 150, 100) : new Color(200, 80, 80));
            l.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 8));
            wrap.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));

            JPanel pill = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(isActive ? new Color(230, 245, 235) : new Color(255, 235, 235));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    g2.dispose();
                }
            };
            pill.setLayout(new BorderLayout());
            pill.setOpaque(false);
            pill.add(l, BorderLayout.CENTER);
            pill.setBorder(new EmptyBorder(4, 14, 4, 14));

            wrap.add(pill);
            return wrap;
        });

        usersTable.getColumnModel().getColumn(5).setCellRenderer(new UserActionsRenderer());
        usersTable.getColumnModel().getColumn(5).setCellEditor(new UserActionsEditor(new JCheckBox()));

        JScrollPane scroll = new JScrollPane(usersTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);

        JPanel container = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(4, 5, getWidth() - 6, getHeight() - 6, 16, 16);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 5, getHeight() - 5, 14, 14);
                g2.dispose();
            }
        };
        container.setOpaque(false);
        container.setBorder(new EmptyBorder(4, 4, 4, 4));
        container.add(scroll, BorderLayout.CENTER);

        return container;
    }

    private void loadUsersData() {
        usersData.clear();
        usersTableModel.setRowCount(0);
        
        // Add current user
        usersData.add(new UserData(1, currentUsername, cap(currentUsername), currentRole, "Active", "Today"));
        usersTableModel.addRow(new Object[]{"#1", currentUsername, cap(currentUsername), cap(currentRole), "Active", "Actions"});
        
        // Add demo users if secretary
        if (isSecretary()) {
            usersData.add(new UserData(2, "staff_user", "Staff User", "staff", "Active", "Yesterday"));
            usersTableModel.addRow(new Object[]{"#2", "staff_user", "Staff User", "Staff", "Active", "Actions"});
            
            usersData.add(new UserData(3, "admin_user", "Admin User", "admin", "Active", "Nov 15, 2024"));
            usersTableModel.addRow(new Object[]{"#3", "admin_user", "Admin User", "Admin", "Active", "Actions"});
        }
    }

    private void showAddUserDialog() {
        JDialog dialog = new JDialog(this, "Add New User", true);
        dialog.setSize(450, 400);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setBackground(WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("Add New User");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRI);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField usernameField = createStyledTextField("Username");
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField fullNameField = createStyledTextField("Full Name");
        fullNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        fullNameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField passwordField = createStyledPasswordField("Password");
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"staff", "secretary", "admin"});
        roleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleCombo.setBackground(WHITE);
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton saveBtn = createCardButton("Create User", BLUE, BLUE_HOVER, WHITE);
        JButton cancelBtn = createCardButton("Cancel", new Color(108, 117, 125), new Color(90, 98, 104), WHITE);

        saveBtn.addActionListener(e -> {
            String username = usernameField.getText().trim();
            String fullName = fullNameField.getText().trim();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();

            if (username.isEmpty() || username.equals("Username")) {
                JOptionPane.showMessageDialog(dialog, "Username is required.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int newId = usersData.size() + 1;
            usersData.add(new UserData(newId, username, fullName, role, "Active", "Never"));
            usersTableModel.addRow(new Object[]{"#" + newId, username, fullName, cap(role), "Active", "Actions"});
            
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "User created successfully!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        content.add(titleLabel);
        content.add(Box.createVerticalStrut(20));
        content.add(createFieldLabel("Username"));
        content.add(Box.createVerticalStrut(4));
        content.add(usernameField);
        content.add(Box.createVerticalStrut(16));
        content.add(createFieldLabel("Full Name"));
        content.add(Box.createVerticalStrut(4));
        content.add(fullNameField);
        content.add(Box.createVerticalStrut(16));
        content.add(createFieldLabel("Password"));
        content.add(Box.createVerticalStrut(4));
        content.add(passwordField);
        content.add(Box.createVerticalStrut(16));
        content.add(createFieldLabel("Role"));
        content.add(Box.createVerticalStrut(4));
        content.add(roleCombo);
        content.add(Box.createVerticalStrut(24));
        content.add(buttonPanel);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void showEditUserDialog(UserData user) {
        JDialog dialog = new JDialog(this, "Edit User", true);
        dialog.setSize(450, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setBackground(WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel titleLabel = new JLabel("Edit User: " + user.username);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_PRI);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField fullNameField = createStyledTextField("Full Name");
        fullNameField.setText(user.fullName);
        fullNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        fullNameField.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"staff", "secretary", "admin"});
        roleCombo.setSelectedItem(user.role.toLowerCase());
        roleCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleCombo.setBackground(WHITE);
        roleCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Active", "Inactive"});
        statusCombo.setSelectedItem(user.status);
        statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusCombo.setBackground(WHITE);
        statusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        statusCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JButton saveBtn = createCardButton("Save Changes", BLUE, BLUE_HOVER, WHITE);
        JButton cancelBtn = createCardButton("Cancel", new Color(108, 117, 125), new Color(90, 98, 104), WHITE);

        saveBtn.addActionListener(e -> {
            user.fullName = fullNameField.getText().trim();
            user.role = (String) roleCombo.getSelectedItem();
            user.status = (String) statusCombo.getSelectedItem();
            
            // Update table
            for (int i = 0; i < usersTableModel.getRowCount(); i++) {
                if (usersTableModel.getValueAt(i, 1).equals(user.username)) {
                    usersTableModel.setValueAt(user.fullName, i, 2);
                    usersTableModel.setValueAt(cap(user.role), i, 3);
                    usersTableModel.setValueAt(user.status, i, 4);
                    break;
                }
            }
            
            dialog.dispose();
            JOptionPane.showMessageDialog(this, "User updated successfully!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        content.add(titleLabel);
        content.add(Box.createVerticalStrut(20));
        content.add(createFieldLabel("Full Name"));
        content.add(Box.createVerticalStrut(4));
        content.add(fullNameField);
        content.add(Box.createVerticalStrut(16));
        content.add(createFieldLabel("Role"));
        content.add(Box.createVerticalStrut(4));
        content.add(roleCombo);
        content.add(Box.createVerticalStrut(16));
        content.add(createFieldLabel("Status"));
        content.add(Box.createVerticalStrut(4));
        content.add(statusCombo);
        content.add(Box.createVerticalStrut(24));
        content.add(buttonPanel);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void deleteUser(int userId, String username) {
        if (username.equals(currentUsername)) {
            JOptionPane.showMessageDialog(this,
                "You cannot delete your own account.",
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to delete user '" + username + "'?",
            "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            usersData.removeIf(u -> u.id == userId);
            for (int i = 0; i < usersTableModel.getRowCount(); i++) {
                if (usersTableModel.getValueAt(i, 1).equals(username)) {
                    usersTableModel.removeRow(i);
                    break;
                }
            }
            JOptionPane.showMessageDialog(this, "User deleted successfully!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void refreshUsersPanel() {
        cardPanel.remove(usersPanel);
        usersPanel = buildUsersPanel();
        cardPanel.add(usersPanel, "users");
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    class UserActionsRenderer extends JPanel implements TableCellRenderer {
        private final JButton editBtn;
        private final JButton deleteBtn;

        UserActionsRenderer() {
            setOpaque(true);
            setLayout(new FlowLayout(FlowLayout.CENTER, 6, 6));
            
            editBtn = createActionButton("Edit", BLUE, new Color(230, 242, 255));
            deleteBtn = createActionButton("Delete", new Color(220, 53, 69), new Color(255, 235, 235));
            
            add(editBtn);
            add(deleteBtn);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setBackground(isSelected
                ? table.getSelectionBackground()
                : (row % 2 == 0 ? WHITE : new Color(248, 250, 252)));
            return this;
        }
    }

    class UserActionsEditor extends DefaultCellEditor {
        private final JPanel panel;
        private final JButton editBtn;
        private final JButton deleteBtn;
        private int currentRow;

        UserActionsEditor(JCheckBox checkBox) {
            super(checkBox);
            
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 6));
            panel.setOpaque(true);
            
            editBtn = createActionButton("Edit", BLUE, new Color(230, 242, 255));
            deleteBtn = createActionButton("Delete", new Color(220, 53, 69), new Color(255, 235, 235));
            
            editBtn.addActionListener(e -> {
                stopCellEditing();
                SwingUtilities.invokeLater(() -> {
                    if (currentRow >= 0 && currentRow < usersData.size()) {
                        showEditUserDialog(usersData.get(currentRow));
                    }
                });
            });
            
            deleteBtn.addActionListener(e -> {
                stopCellEditing();
                SwingUtilities.invokeLater(() -> {
                    if (currentRow >= 0 && currentRow < usersData.size()) {
                        UserData user = usersData.get(currentRow);
                        deleteUser(user.id, user.username);
                    }
                });
            });
            
            panel.add(editBtn);
            panel.add(deleteBtn);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            panel.setBackground(isSelected
                ? table.getSelectionBackground()
                : (row % 2 == 0 ? WHITE : new Color(248, 250, 252)));
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    // ── Settings Panel ──────────────────────────────────────────────────────
    private static class SettingsData {
        boolean autoBackup;
        String backupFrequency;
        boolean emailNotifications;
        boolean newCaseNotifications;
        boolean statusChangeNotifications;

        SettingsData(boolean autoBackup, String backupFrequency, boolean emailNotifications,
                     boolean newCaseNotifications, boolean statusChangeNotifications) {
            this.autoBackup = autoBackup;
            this.backupFrequency = backupFrequency;
            this.emailNotifications = emailNotifications;
            this.newCaseNotifications = newCaseNotifications;
            this.statusChangeNotifications = statusChangeNotifications;
        }
    }

    private SettingsData currentSettings = new SettingsData(false, "Daily", false, true, true);

    private JPanel buildSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PAGE_BG);
        panel.add(buildSettingsHeader(), BorderLayout.NORTH);
        panel.add(buildSettingsContent(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSettingsHeader() {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 20));
                g2.fillRoundRect(4, 4, getWidth() - 6, getHeight() - 6, 16, 16);
                GradientPaint gp = new GradientPaint(0, 0, BLUE_DARK, getWidth(), 0, new Color(45, 80, 130));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 6, getHeight() - 6, 14, 14);
                g2.dispose();
            }
        };
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));
        header.setPreferredSize(new Dimension(0, 70));

        JButton backBtn = createBackButton();
        backBtn.addActionListener(e -> {
            cardLayout.show(cardPanel, "dashboard");
            setActiveButton(homeBtn);
        });

        JLabel titleLabel = new JLabel("System Settings");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(WHITE);

        header.add(backBtn, BorderLayout.WEST);
        header.add(titleLabel, BorderLayout.CENTER);

        return header;
    }

    private JPanel buildSettingsContent() {
        JPanel content = new JPanel();
        content.setBackground(PAGE_BG);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 24, 24, 24));

        // General Settings Card
        JPanel generalCard = createSettingsCard("GENERAL SETTINGS");
        generalCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        JCheckBox autoBackupCheck = createStyledCheckBox("Enable automatic database backup");
        autoBackupCheck.setSelected(currentSettings.autoBackup);
        autoBackupCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> backupFreqCombo = new JComboBox<>(new String[]{"Daily", "Weekly", "Monthly"});
        backupFreqCombo.setSelectedItem(currentSettings.backupFrequency);
        backupFreqCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        backupFreqCombo.setBackground(WHITE);
        backupFreqCombo.setMaximumSize(new Dimension(300, 38));
        backupFreqCombo.setAlignmentX(Component.LEFT_ALIGNMENT);

        generalCard.add(autoBackupCheck);
        generalCard.add(Box.createVerticalStrut(12));
        generalCard.add(createFieldLabel("Backup Frequency"));
        generalCard.add(Box.createVerticalStrut(4));
        generalCard.add(backupFreqCombo);

        // Notification Settings Card
        JPanel notifCard = createSettingsCard("NOTIFICATION SETTINGS");
        notifCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

        JCheckBox emailNotifCheck = createStyledCheckBox("Enable email notifications");
        emailNotifCheck.setSelected(currentSettings.emailNotifications);
        emailNotifCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox newCaseNotifCheck = createStyledCheckBox("Notify on new blotter cases");
        newCaseNotifCheck.setSelected(currentSettings.newCaseNotifications);
        newCaseNotifCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox statusNotifCheck = createStyledCheckBox("Notify on status changes");
        statusNotifCheck.setSelected(currentSettings.statusChangeNotifications);
        statusNotifCheck.setAlignmentX(Component.LEFT_ALIGNMENT);

        notifCard.add(emailNotifCheck);
        notifCard.add(Box.createVerticalStrut(8));
        notifCard.add(newCaseNotifCheck);
        notifCard.add(Box.createVerticalStrut(8));
        notifCard.add(statusNotifCheck);

        // System Settings Card
        JPanel systemCard = createSettingsCard("SYSTEM MAINTENANCE");
        systemCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        buttonRow.setOpaque(false);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton backupNowBtn = createCardButton("Backup Now", BLUE, BLUE_HOVER, WHITE);
        backupNowBtn.addActionListener(e -> performBackup());

        JButton clearCacheBtn = createCardButton("Clear Cache", new Color(108, 117, 125), new Color(90, 98, 104), WHITE);
        clearCacheBtn.addActionListener(e -> clearCache());

        JButton resetSettingsBtn = createCardButton("Reset to Default", new Color(255, 193, 7), new Color(230, 170, 0), TEXT_PRI);
        resetSettingsBtn.addActionListener(e -> resetSettingsToDefault());

        buttonRow.add(backupNowBtn);
        buttonRow.add(clearCacheBtn);
        buttonRow.add(resetSettingsBtn);

        systemCard.add(buttonRow);

        // Save Button
        JButton saveSettingsBtn = createCardButton("Save All Settings", STAT_GREEN, new Color(40, 150, 100), WHITE);
        saveSettingsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        saveSettingsBtn.setMaximumSize(new Dimension(200, 44));
        saveSettingsBtn.addActionListener(e -> {
            currentSettings = new SettingsData(
                autoBackupCheck.isSelected(),
                (String) backupFreqCombo.getSelectedItem(),
                emailNotifCheck.isSelected(),
                newCaseNotifCheck.isSelected(),
                statusNotifCheck.isSelected()
            );
            JOptionPane.showMessageDialog(this, "Settings saved successfully!",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        content.add(generalCard);
        content.add(Box.createVerticalStrut(20));
        content.add(notifCard);
        content.add(Box.createVerticalStrut(20));
        content.add(systemCard);
        content.add(Box.createVerticalStrut(20));
        content.add(saveSettingsBtn);

        return content;
    }

    private JPanel createSettingsCard(String title) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(4, 5, getWidth() - 6, getHeight() - 6, 16, 16);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, 14, 14);
                g2.setColor(BORDER_CLR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(1, 1, getWidth() - 5, getHeight() - 5, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 28, 28, 28));

        JLabel sectionLabel = new JLabel(title);
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sectionLabel.setForeground(TEXT_SEC);
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(sectionLabel);
        card.add(Box.createVerticalStrut(16));

        return card;
    }

    private JCheckBox createStyledCheckBox(String text) {
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        checkBox.setForeground(TEXT_PRI);
        checkBox.setOpaque(false);
        checkBox.setFocusPainted(false);
        return checkBox;
    }

    private void performBackup() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Backup Location");
        fileChooser.setSelectedFile(new java.io.File("eblotter_backup_" +
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".sql"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            JOptionPane.showMessageDialog(this,
                "Backup feature requires MySQL to be in system PATH.\n" +
                "File would be saved to: " + fileChooser.getSelectedFile().getAbsolutePath(),
                "Backup Information", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearCache() {
        blotterData.clear();
        loadBlotterData();
        refreshTableAndStats();
        JOptionPane.showMessageDialog(this,
            "Cache cleared successfully! Data reloaded from database.",
            "Success", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetSettingsToDefault() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "Reset all settings to default values?",
            "Confirm Reset", JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            currentSettings = new SettingsData(false, "Daily", false, true, true);
            refreshSettingsPanel();
            JOptionPane.showMessageDialog(this,
                "Settings reset to default values.",
                "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void refreshSettingsPanel() {
        cardPanel.remove(settingsPanel);
        settingsPanel = buildSettingsPanel();
        cardPanel.add(settingsPanel, "settings");
        cardPanel.revalidate();
        cardPanel.repaint();
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRI);
        field.setBackground(WHITE);
        field.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_CLR, 1, 8),
            new EmptyBorder(8, 12, 8, 12)));

        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRI);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_SEC);
                }
            }
        });

        field.setText(placeholder);
        field.setForeground(TEXT_SEC);
        return field;
    }

    // ── Button Renderer/Editor ─────────────────────────────────────────────
    class ButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton viewBtn;
        private final JButton printBtn;

        ButtonRenderer() {
            setOpaque(true);
            setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
            
            viewBtn = createActionButton("View", BLUE, new Color(230, 242, 255));
            printBtn = createActionButton("Print", STAT_GREEN, new Color(230, 245, 239));
            
            add(viewBtn);
            add(printBtn);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            setBackground(isSelected
                ? table.getSelectionBackground()
                : (row % 2 == 0 ? WHITE : new Color(248, 250, 252)));
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private final JPanel panel;
        private final JButton viewBtn;
        private final JButton printBtn;
        private int currentRow;

        ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
            panel.setOpaque(true);
            
            viewBtn = createActionButton("View", BLUE, new Color(230, 242, 255));
            printBtn = createActionButton("Print", STAT_GREEN, new Color(230, 245, 239));
            
            viewBtn.addActionListener(e -> {
                stopCellEditing();
                SwingUtilities.invokeLater(() -> viewBlotterDialog.show(currentRow));
            });
            
            printBtn.addActionListener(e -> {
                stopCellEditing();
                SwingUtilities.invokeLater(() -> printBlotterFrame.show(currentRow));
            });
            
            panel.add(viewBtn);
            panel.add(printBtn);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            panel.setBackground(isSelected
                ? table.getSelectionBackground()
                : (row % 2 == 0 ? WHITE : new Color(248, 250, 252)));
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    private JButton createActionButton(String text, Color textColor, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setForeground(textColor);
        button.setPreferredSize(new Dimension(65, 28));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                
                AbstractButton b = (AbstractButton) c;
                
                if (b.getModel().isPressed()) {
                    g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), 
                                         bgColor.getBlue(), 180));
                } else if (b.getModel().isRollover()) {
                    g2.setColor(new Color(bgColor.getRed(), bgColor.getGreen(), 
                                         bgColor.getBlue(), 220));
                } else {
                    g2.setColor(bgColor);
                }
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 14, 14);
                
                super.paint(g2, c);
                g2.dispose();
            }
        });
        
        return button;
    }

    private JButton createStyledButton(String text, Color bg, Color hover, Color fg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? hover : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(fg);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                    (getWidth() - fm.stringWidth(getText())) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }

            @Override public Insets getInsets() { return new Insets(0, 0, 0, 0); }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(fg);
        b.setPreferredSize(new Dimension(110, 38));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JButton createCardButton(String text, Color bg, Color hover, Color fg) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                g2.setColor(new Color(0, 0, 0, 15));
                g2.fillRoundRect(2, 3, w - 2, h - 2, 10, 10);
                
                if (getModel().isPressed()) {
                    g2.setColor(hover.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(hover);
                } else {
                    g2.setColor(bg);
                }
                g2.fillRoundRect(0, 0, w - 2, h - 2, 10, 10);
                
                g2.setColor(fg);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                int textX = (w - fm.stringWidth(getText())) / 2;
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(getText(), textX, textY);
                
                g2.dispose();
            }
        };
        
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 36));
        return btn;
    }

    // ── Search/Filter ─────────────────────────────────────────────────────
    private void filterTable() {
        String query = searchField.getText().toLowerCase().trim();
        tableModel.setRowCount(0);
        for (Object[] row : blotterData) {
            if (query.isEmpty() || query.equals("search blotter...")) {
                String sd = "pending".equalsIgnoreCase(row[4].toString()) ? "Pending" : "Resolved";
                tableModel.addRow(new Object[]{row[0], row[1], row[2], row[3], sd, "View"});
            } else {
                boolean match = false;
                for (int i = 0; i < 4; i++) {
                    if (row[i] != null && row[i].toString().toLowerCase().contains(query)) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    String sd = "pending".equalsIgnoreCase(row[4].toString()) ? "Pending" : "Resolved";
                    tableModel.addRow(new Object[]{row[0], row[1], row[2], row[3], sd, "View"});
                }
            }
        }
    }

    // ── Status Update ─────────────────────────────────────────────────────
    private void showUpdateStatusDialog(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) return;

        String blotterNum = tableModel.getValueAt(selectedRow, 0).toString();
        String currentStatus = tableModel.getValueAt(selectedRow, 4).toString();

        if ("Resolved".equals(currentStatus)) {
            JOptionPane.showMessageDialog(this,
                "This case is already resolved and cannot be edited.",
                "Case Locked", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JDialog dialog = new JDialog(this, "Update Status", true);
        dialog.setSize(450, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setBackground(WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Update Status for ID #" + blotterNum);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        titleLabel.setForeground(TEXT_PRI);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel currentLabel = new JLabel("Current Status: " + currentStatus);
        currentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        currentLabel.setForeground(TEXT_SEC);
        currentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel newStatusLabel = new JLabel("New Status:");
        newStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        newStatusLabel.setForeground(TEXT_PRI);
        newStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Pending", "Resolved"});
        statusCombo.setSelectedItem(currentStatus);
        statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        statusCombo.setBackground(BLUE_LIGHT);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(WHITE);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton saveBtn = createStyledButton("Save", BLUE, BLUE_HOVER, WHITE);
        saveBtn.addActionListener(e -> {
            String newStatus = (String) statusCombo.getSelectedItem();
            if (newStatus != null && !newStatus.equals(currentStatus)) {
                updateStatus(selectedRow, newStatus.toLowerCase(), blotterNum);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog,
                    "No changes made.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton cancelBtn = createStyledButton("Cancel",
            new Color(108, 117, 125), new Color(90, 98, 104), WHITE);
        cancelBtn.addActionListener(e -> dialog.dispose());

        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        content.add(titleLabel);
        content.add(Box.createVerticalStrut(8));
        content.add(currentLabel);
        content.add(Box.createVerticalStrut(16));
        content.add(newStatusLabel);
        content.add(Box.createVerticalStrut(6));
        content.add(statusCombo);
        content.add(Box.createVerticalStrut(20));
        content.add(buttonPanel);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    private void updateStatus(int row, String newStatus, String blotterNumber) {
        setAllButtonsEnabled(false);
        table.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try (Connection conn = getConnection()) {
                    String update = "UPDATE blotter SET status = ? WHERE blotter_id = ?";
                    try (PreparedStatement pstmt = conn.prepareStatement(update)) {
                        pstmt.setString(1, newStatus);
                        pstmt.setString(2, blotterNumber);
                        pstmt.executeUpdate();
                    }
                }
                return true;
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        String display = "pending".equalsIgnoreCase(newStatus) ? "Pending" : "Resolved";
                        if (row < blotterData.size()) blotterData.get(row)[4] = newStatus;
                        tableModel.setValueAt(display, row, 4);
                        updateStatCards();
                        refreshHistoryPanel();
                        JOptionPane.showMessageDialog(dashboard.this,
                            "Status updated to: " + display, "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(dashboard.this,
                        "Error updating status: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setAllButtonsEnabled(true);
                    table.setEnabled(true);
                }
            }
        }.execute();
    }

    // ── Logout ─────────────────────────────────────────────────────────────
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
            this, "Are you sure you want to logout?",
            "Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            new login().setVisible(true);
            dispose();
        }
    }

    // ── Rounded Border ─────────────────────────────────────────────────────
    static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness, radius;

        RoundedBorder(Color c, int t, int r) { color = c; thickness = t; radius = r; }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 3, radius / 3, radius / 3, radius / 3);
        }
    }

    // ── Inner class: AddBlotterPanel ──────────────────────────────────────────
    private class AddBlotterPanel extends JPanel {
        // Color palette - blue and white only
        private final Color HEADER_BG      = new Color(25, 60, 110);
        private final Color BLOTTER_BAR_BG = new Color(25, 60, 110);
        private final Color PAGE_BG        = new Color(240, 245, 255);
        private final Color CARD_BG        = Color.WHITE;
        private final Color SECTION_LABEL  = new Color(25, 60, 110);
        private final Color FIELD_BORDER   = new Color(200, 210, 230);
        private final Color FIELD_BG       = Color.WHITE;
        private final Color RESPONDENT_BG  = new Color(230, 241, 251);
        private final Color RESPONDENT_BDR = new Color(180, 200, 220);
        private final Color BTN_SAVE_BG    = new Color(45, 118, 200);
        private final Color BTN_SAVE_FG    = Color.WHITE;
        private final Color LABEL_FG       = Color.BLACK;
        private final Color TEXT_FG        = Color.BLACK;
        private final Color HEADER_SUBTITLE= new Color(180, 190, 210);

        // Fonts
        private final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 18);
        private final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 11);
        private final Font FONT_SECTION  = new Font("Segoe UI", Font.BOLD, 11);
        private final Font FONT_LABEL    = new Font("Segoe UI", Font.PLAIN, 11);
        private final Font FONT_INPUT    = new Font("Segoe UI", Font.PLAIN, 13);
        private final Font FONT_BLOTTER  = new Font("Segoe UI", Font.BOLD, 13);
        private final Font FONT_BTN      = new Font("Segoe UI", Font.BOLD, 12);

        // Form fields
        private JTextField tfCFirstName, tfCMiddleName, tfCLastName, tfCSuffix;
        private JTextField tfMobileNumber, tfPurok;
        private JTextField tfRFirstName, tfRMiddleName, tfRLastName, tfRSuffix;
        private JButton btnDatePicker;
        private JComboBox<String> cbComplaintType;
        private JTextArea taDescription;
        private java.util.Date selectedDate = new java.util.Date();

        private final JDialog parentDialog;
        private final ConnectionProvider connectionProvider;
        private final Runnable onSaveCallback;
        private final int nextBlotterNumber;

        @FunctionalInterface
        public interface ConnectionProvider {
            Connection getConnection() throws ClassNotFoundException, SQLException;
        }

        public AddBlotterPanel(JDialog parent, ConnectionProvider connectionProvider,
                               int nextBlotterNumber, Runnable onSaveCallback) {
            this.parentDialog = parent;
            this.connectionProvider = connectionProvider;
            this.nextBlotterNumber = nextBlotterNumber;
            this.onSaveCallback = onSaveCallback;
            setLayout(new BorderLayout());
            setBackground(PAGE_BG);
            add(buildHeader(), BorderLayout.NORTH);
            add(buildScrollBody(), BorderLayout.CENTER);
        }

        private JPanel buildHeader() {
            JPanel header = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                  
                }
            };
           
            return header;
        }

        private JScrollPane buildScrollBody() {
            JPanel body = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(PAGE_BG);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            body.setOpaque(false);
            body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
            body.setBorder(BorderFactory.createEmptyBorder(16, 24, 24, 24));

            body.add(buildBlotterBar());
            body.add(Box.createVerticalStrut(14));
            body.add(buildPartiesCard());
            body.add(Box.createVerticalStrut(16));
            body.add(buildIncidentDetailsCard());
            body.add(Box.createVerticalStrut(20));
            body.add(buildButtonRow());
            body.add(Box.createVerticalStrut(10));

            JScrollPane scroll = new JScrollPane(body);
            scroll.setBorder(null);
            scroll.getViewport().setOpaque(false);
            scroll.setOpaque(false);
            scroll.getVerticalScrollBar().setUnitIncrement(12);
            return scroll;
        }

        private JPanel buildBlotterBar() {
            JPanel bar = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(BLOTTER_BAR_BG);
                    g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                    g2.dispose();
                }
            };
            bar.setOpaque(false);
            bar.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            JLabel lKey = new JLabel("Blotter Number");
            lKey.setFont(FONT_BLOTTER);
            lKey.setForeground(new Color(0xA8C4E0));

            String nextNumber = "#" + String.format("%04d", nextBlotterNumber);
            JLabel lVal = new JLabel(nextNumber + " (auto-generated)");
            lVal.setFont(FONT_BLOTTER);
            lVal.setForeground(Color.WHITE);

            bar.add(lKey, BorderLayout.WEST);
            bar.add(lVal, BorderLayout.EAST);
            return bar;
        }

        private JPanel buildPartiesCard() {
            JPanel card = createCard();

            JLabel complainantSection = sectionHeader("COMPLAINANT INFORMATION");

            JPanel cRow1 = new JPanel(new GridLayout(1, 2, 14, 0));
            cRow1.setOpaque(false);
            tfCFirstName = styledField("First name", false);
            tfCMiddleName = styledField("Middle name", false);
            cRow1.add(labeledField("First Name", tfCFirstName));
            cRow1.add(labeledField("Middle Name", tfCMiddleName));

            JPanel cRow2 = new JPanel(new GridLayout(1, 2, 14, 0));
            cRow2.setOpaque(false);
            tfCLastName = styledField("Last name", false);
            tfCSuffix = styledField("e.g. Jr., Sr., III (Optional)", false);
            cRow2.add(labeledField("Last Name", tfCLastName));
            cRow2.add(labeledField("Suffix (Optional)", tfCSuffix));

            JPanel cRow3 = new JPanel(new GridLayout(1, 2, 14, 0));
            cRow3.setOpaque(false);
            tfMobileNumber = styledField("Mobile number", false);
            tfPurok = styledField("Purok", false);
            cRow3.add(labeledField("Mobile Number", tfMobileNumber));
            cRow3.add(labeledField("Purok", tfPurok));

            JLabel respondentSection = sectionHeader("RESPONDENT INFORMATION");

            JPanel rRow1 = new JPanel(new GridLayout(1, 2, 14, 0));
            rRow1.setOpaque(false);
            tfRFirstName = styledField("First name", true);
            tfRMiddleName = styledField("Middle name", true);
            rRow1.add(labeledField("First Name", tfRFirstName));
            rRow1.add(labeledField("Middle Name", tfRMiddleName));

            JPanel rRow2 = new JPanel(new GridLayout(1, 2, 14, 0));
            rRow2.setOpaque(false);
            tfRLastName = styledField("Last name", true);
            tfRSuffix = styledField("e.g. Jr., Sr., III (Optional)", true);
            rRow2.add(labeledField("Last Name", tfRLastName));
            rRow2.add(labeledField("Suffix (Optional)", tfRSuffix));

            JPanel dateRow = new JPanel(new GridLayout(1, 2, 14, 0));
            dateRow.setOpaque(false);

            JPanel datePanel = new JPanel();
            datePanel.setOpaque(false);
            datePanel.setLayout(new BoxLayout(datePanel, BoxLayout.Y_AXIS));

            JLabel dateLabel = new JLabel("Date of Incident");
            dateLabel.setFont(FONT_LABEL);
            dateLabel.setForeground(LABEL_FG);
            dateLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel dateInputPanel = new JPanel(new BorderLayout(5, 0));
            dateInputPanel.setOpaque(false);
            dateInputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            btnDatePicker = new JButton(new java.text.SimpleDateFormat("MM/dd/yyyy").format(selectedDate));
            btnDatePicker.setFont(FONT_INPUT);
            btnDatePicker.setForeground(TEXT_FG);
            btnDatePicker.setBackground(FIELD_BG);
            btnDatePicker.setHorizontalAlignment(SwingConstants.LEFT);
            btnDatePicker.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(FIELD_BORDER, 1, 8),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
            btnDatePicker.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDatePicker.addActionListener(e -> showDatePicker());

            dateInputPanel.add(btnDatePicker, BorderLayout.CENTER);
            datePanel.add(dateLabel);
            datePanel.add(Box.createVerticalStrut(4));
            datePanel.add(dateInputPanel);

            dateRow.add(datePanel);
            dateRow.add(new JPanel());

            card.add(complainantSection);
            card.add(Box.createVerticalStrut(10));
            card.add(cRow1);
            card.add(Box.createVerticalStrut(10));
            card.add(cRow2);
            card.add(Box.createVerticalStrut(10));
            card.add(cRow3);
            card.add(Box.createVerticalStrut(16));
            card.add(respondentSection);
            card.add(Box.createVerticalStrut(10));
            card.add(rRow1);
            card.add(Box.createVerticalStrut(10));
            card.add(rRow2);
            card.add(Box.createVerticalStrut(12));
            card.add(dateRow);

            return card;
        }

        private void showDatePicker() {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(selectedDate);
            
            JSpinner daySpinner = new JSpinner(new SpinnerNumberModel(
                cal.get(java.util.Calendar.DAY_OF_MONTH), 1, 31, 1));
            JSpinner monthSpinner = new JSpinner(new SpinnerNumberModel(
                cal.get(java.util.Calendar.MONTH) + 1, 1, 12, 1));
            JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(
                cal.get(java.util.Calendar.YEAR), 2000, 2100, 1));

            JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
            panel.add(new JLabel("Day:"));
            panel.add(daySpinner);
            panel.add(new JLabel("Month:"));
            panel.add(monthSpinner);
            panel.add(new JLabel("Year:"));
            panel.add(yearSpinner);

            int result = JOptionPane.showConfirmDialog(this, panel, "Select Date",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            
            if (result == JOptionPane.OK_OPTION) {
                cal.set(java.util.Calendar.DAY_OF_MONTH, (Integer) daySpinner.getValue());
                cal.set(java.util.Calendar.MONTH, (Integer) monthSpinner.getValue() - 1);
                cal.set(java.util.Calendar.YEAR, (Integer) yearSpinner.getValue());
                selectedDate = cal.getTime();
                btnDatePicker.setText(new java.text.SimpleDateFormat("MM/dd/yyyy").format(selectedDate));
            }
        }

        private JPanel buildIncidentDetailsCard() {
            JPanel card = createCard();

            JLabel sectionLabel = sectionHeader("INCIDENT DETAILS");

            String[] complaintTypes = {
                "Noise Disturbance", "Verbal Dispute", "Property Damage", "Theft",
                "Physical Altercation", "Threats/Harassment", "Other"
            };
            cbComplaintType = new JComboBox<>(complaintTypes);
            cbComplaintType.setFont(FONT_INPUT);
            cbComplaintType.setForeground(TEXT_FG);
            cbComplaintType.setBackground(FIELD_BG);
            cbComplaintType.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(FIELD_BORDER, 1, 8),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            cbComplaintType.setPreferredSize(new Dimension(200, 38));
            cbComplaintType.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            taDescription = new JTextArea(4, 20);
            taDescription.setFont(FONT_INPUT);
            taDescription.setForeground(TEXT_FG);
            taDescription.setBackground(FIELD_BG);
            taDescription.setLineWrap(true);
            taDescription.setWrapStyleWord(true);
            taDescription.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(FIELD_BORDER, 1, 8),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

            JScrollPane descriptionScroll = new JScrollPane(taDescription);
            descriptionScroll.setBorder(null);
            descriptionScroll.setOpaque(false);
            descriptionScroll.getViewport().setOpaque(false);
            descriptionScroll.setPreferredSize(new Dimension(0, 110));
            descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

            card.add(sectionLabel);
            card.add(Box.createVerticalStrut(12));
            card.add(labeledField("Type of Complaint", cbComplaintType));
            card.add(Box.createVerticalStrut(16));
            card.add(labeledField("Description / Incident Details", descriptionScroll));

            return card;
        }

        private JPanel buildButtonRow() {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            JButton saveBtn = createCardButton("Save Blotter Entry", BTN_SAVE_BG,
                new Color(30, 80, 140), BTN_SAVE_FG);
            saveBtn.addActionListener(e -> saveBlotterToDB());

            row.add(saveBtn);
            return row;
        }

        private String getFieldText(JTextField tf, String placeholder) {
            String val = tf.getText().trim();
            return val.equals(placeholder) ? "" : val;
        }

        private void saveBlotterToDB() {
            final String cFirstName = getFieldText(tfCFirstName, "First name");
            final String cMiddleName = getFieldText(tfCMiddleName, "Middle name");
            final String cLastName = getFieldText(tfCLastName, "Last name");
            final String cSuffix = getFieldText(tfCSuffix, "e.g. Jr., Sr., III (Optional)");
            final String rFirstName = getFieldText(tfRFirstName, "First name");
            final String rMiddleName = getFieldText(tfRMiddleName, "Middle name");
            final String rLastName = getFieldText(tfRLastName, "Last name");
            final String rSuffix = getFieldText(tfRSuffix, "e.g. Jr., Sr., III (Optional)");
            final String mobileNumber = getFieldText(tfMobileNumber, "Mobile number");
            final String purok = getFieldText(tfPurok, "Purok");
            final String complaintType = (String) cbComplaintType.getSelectedItem();
            final String description = taDescription.getText().trim();

            if (cFirstName.isEmpty() || cMiddleName.isEmpty() || cLastName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please complete complainant information.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (rFirstName.isEmpty() || rMiddleName.isEmpty() || rLastName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please complete respondent information.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (mobileNumber.isEmpty() || purok.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter mobile number and purok.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (description.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter incident description.",
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            setAllFieldsEnabled(false);

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    try (Connection conn = connectionProvider.getConnection()) {
                        conn.setAutoCommit(false);
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
                            String formattedDate = sdf.format(selectedDate);

                            int complainantId;
                            String insertComplainant = "INSERT INTO complainant " +
                                "(first_name, middle_name, last_name, suffix, mobile_number, purok) " +
                                "VALUES (?, ?, ?, ?, ?, ?)";
                            try (PreparedStatement pstmt = conn.prepareStatement(
                                    insertComplainant, Statement.RETURN_GENERATED_KEYS)) {
                                pstmt.setString(1, cFirstName);
                                pstmt.setString(2, cMiddleName);
                                pstmt.setString(3, cLastName);
                                pstmt.setString(4, cSuffix.isEmpty() ? null : cSuffix);
                                pstmt.setString(5, mobileNumber);
                                pstmt.setString(6, purok);
                                pstmt.executeUpdate();
                                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                                    keys.next();
                                    complainantId = keys.getInt(1);
                                }
                            }

                            int respondentId;
                            String insertRespondent = "INSERT INTO respondent " +
                                "(first_name, middle_name, last_name, suffix) " +
                                "VALUES (?, ?, ?, ?)";
                            try (PreparedStatement pstmt = conn.prepareStatement(
                                    insertRespondent, Statement.RETURN_GENERATED_KEYS)) {
                                pstmt.setString(1, rFirstName);
                                pstmt.setString(2, rMiddleName);
                                pstmt.setString(3, rLastName);
                                pstmt.setString(4, rSuffix.isEmpty() ? null : rSuffix);
                                pstmt.executeUpdate();
                                try (ResultSet keys = pstmt.getGeneratedKeys()) {
                                    keys.next();
                                    respondentId = keys.getInt(1);
                                }
                            }

                            String insertBlotter = "INSERT INTO blotter " +
                                "(complainant_id, respondent_id, date, complt_type, description, status) " +
                                "VALUES (?, ?, ?, ?, ?, ?)";
                            try (PreparedStatement pstmt = conn.prepareStatement(insertBlotter)) {
                                pstmt.setInt(1, complainantId);
                                pstmt.setInt(2, respondentId);
                                pstmt.setDate(3, java.sql.Date.valueOf(formattedDate));
                                pstmt.setString(4, complaintType);
                                pstmt.setString(5, description);
                                pstmt.setString(6, "pending");
                                pstmt.executeUpdate();
                            }

                            conn.commit();
                        } catch (Exception e) {
                            conn.rollback();
                            throw e;
                        }
                    }
                    return true;
                }

                @Override
                protected void done() {
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(AddBlotterPanel.this,
                                "Blotter entry saved successfully!",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                            if (onSaveCallback != null) onSaveCallback.run();
                            if (parentDialog != null) parentDialog.dispose();
                        }
                    } catch (Exception e) {
                        String message = "Error saving blotter: ";
                        Throwable cause = e.getCause();
                        if (cause != null && cause.getMessage() != null) {
                            message += cause.getMessage();
                        } else if (e.getMessage() != null) {
                            message += e.getMessage();
                        } else {
                            message += "Unknown error occurred";
                        }
                        JOptionPane.showMessageDialog(AddBlotterPanel.this, message,
                            "Error", JOptionPane.ERROR_MESSAGE);
                        setAllFieldsEnabled(true);
                    }
                }
            }.execute();
        }

        private void setAllFieldsEnabled(boolean enabled) {
            tfCFirstName.setEnabled(enabled);
            tfCMiddleName.setEnabled(enabled);
            tfCLastName.setEnabled(enabled);
            tfCSuffix.setEnabled(enabled);
            tfRFirstName.setEnabled(enabled);
            tfRMiddleName.setEnabled(enabled);
            tfRLastName.setEnabled(enabled);
            tfRSuffix.setEnabled(enabled);
            tfMobileNumber.setEnabled(enabled);
            tfPurok.setEnabled(enabled);
            btnDatePicker.setEnabled(enabled);
            cbComplaintType.setEnabled(enabled);
            taDescription.setEnabled(enabled);
        }

        private JPanel createCard() {
            JPanel card = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0, 0, 0, 25));
                    g2.fill(new java.awt.geom.RoundRectangle2D.Double(4, 6, getWidth()-6, getHeight()-6, 16, 16));
                    GradientPaint gradient = new GradientPaint(0, 0, CARD_BG, 0, getHeight(), new Color(250, 252, 255));
                    g2.setPaint(gradient);
                    g2.fill(new java.awt.geom.RoundRectangle2D.Double(0, 0, getWidth()-4, getHeight()-4, 14, 14));
                    g2.setColor(new Color(200, 210, 225));
                    g2.setStroke(new BasicStroke(1f));
                    g2.draw(new java.awt.geom.RoundRectangle2D.Double(1, 1, getWidth()-5, getHeight()-5, 14, 14));
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            return card;
        }

        private JLabel sectionHeader(String text) {
            JLabel lbl = new JLabel(text);
            lbl.setFont(FONT_SECTION);
            lbl.setForeground(SECTION_LABEL);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            return lbl;
        }

        private JPanel labeledField(String labelText, JComponent field) {
            JPanel pnl = new JPanel();
            pnl.setOpaque(false);
            pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));

            JLabel lbl = new JLabel(labelText);
            lbl.setFont(FONT_LABEL);
            lbl.setForeground(LABEL_FG);
            lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            field.setAlignmentX(Component.LEFT_ALIGNMENT);

            pnl.add(lbl);
            pnl.add(Box.createVerticalStrut(4));
            pnl.add(field);
            return pnl;
        }

        private JTextField styledField(String placeholder, boolean isRespondent) {
            JTextField tf = new JTextField();
            tf.setFont(FONT_INPUT);
            tf.setForeground(TEXT_FG);
            tf.setBackground(isRespondent ? RESPONDENT_BG : FIELD_BG);
            tf.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(isRespondent ? RESPONDENT_BDR : FIELD_BORDER, 1, 8),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
            tf.setPreferredSize(new Dimension(200, 38));
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

            tf.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    if (tf.getText().equals(placeholder)) {
                        tf.setText("");
                        tf.setForeground(TEXT_FG);
                    }
                }
                @Override public void focusLost(FocusEvent e) {
                    if (tf.getText().isEmpty()) {
                        tf.setText(placeholder);
                        tf.setForeground(TEXT_SEC);
                    }
                }
            });

            tf.setText(placeholder);
            tf.setForeground(TEXT_SEC);
            return tf;
        }

        private JButton createCardButton(String text, Color bg, Color hover, Color fg) {
            JButton btn = new JButton(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth();
                    int h = getHeight();
                    g2.setColor(new Color(0, 0, 0, 15));
                    g2.fillRoundRect(2, 3, w - 2, h - 2, 12, 12);
                    if (getModel().isPressed()) {
                        g2.setColor(hover.darker());
                    } else if (getModel().isRollover()) {
                        g2.setColor(hover);
                    } else {
                        g2.setColor(bg);
                    }
                    g2.fillRoundRect(0, 0, w - 2, h - 2, 12, 12);
                    g2.setColor(fg);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = (w - fm.stringWidth(getText())) / 2;
                    int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                    g2.drawString(getText(), textX, textY);
                    g2.dispose();
                }
            };
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setForeground(fg);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(140, 44));
            return btn;
        }
    }
}
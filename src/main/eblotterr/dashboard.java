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

    // ── Modern color palette ───────────────────────────────────────────────
    private static final Color WHITE        = Color.WHITE;
    private static final Color BLUE         = new Color(45, 118, 200);
    private static final Color BLUE_HOVER   = new Color(35, 95, 160);
    private static final Color BLUE_DARK    = new Color(25, 60, 110);
    private static final Color BLUE_LIGHT   = new Color(230, 241, 251);
    private static final Color BG           = new Color(248, 249, 250);
    private static final Color BORDER_CLR   = new Color(220, 223, 228);
    private static final Color TEXT_PRI     = new Color(33, 41, 52);
    private static final Color TEXT_SEC     = new Color(115, 130, 150);
    private static final Color TEXT_LIGHT   = new Color(185, 200, 220);
    private static final Color STAT_BLUE    = new Color(52, 134, 235);
    private static final Color STAT_ORANGE  = new Color(255, 140, 60);
    private static final Color STAT_GREEN   = new Color(46, 176, 120);
    private static final Color PENDING_BG   = new Color(255, 247, 235);
    private static final Color PENDING_FG   = new Color(255, 140, 60);
    private static final Color RESOLVED_BG  = new Color(228, 248, 240);
    private static final Color RESOLVED_FG  = new Color(46, 176, 120);

    private final String currentUsername;
    private final String currentRole; // "secretary", "captain", or "kagawad"
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private final List<Object[]> blotterData = new ArrayList<>();
    private JButton addBtn;
    private JButton searchBtn;
    private JButton historyBtn;

    // ── Stat card value labels ─────────────────────────────────────────────
    private JLabel totalValueLabel;
    private JLabel pendingValueLabel;
    private JLabel resolvedValueLabel;

    // ── Extracted feature handlers ─────────────────────────────────────────
    private ViewBlotterDialog viewBlotterDialog;
    private PrintBlotterFrame printBlotterFrame;

    public dashboard() {
        this("User", "secretary");
    }

    public dashboard(String username) {
        this(username, "secretary");
    }

    /**
     * @return true if the current user is a secretary (full access)
     */
    public boolean isSecretary() {
        return "secretary".equalsIgnoreCase(currentRole);
    }

    public dashboard(String username, String role) {
        this.currentUsername = username;
        this.currentRole = (role != null) ? role.toLowerCase() : "secretary";
        setTitle("Barangay e-Blotter — Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(800, 550));
        setResizable(true);
        setLocationRelativeTo(null);
        loadBlotterData();

        // Initialize extracted feature handlers
        printBlotterFrame = new PrintBlotterFrame(this, blotterData);
        viewBlotterDialog = new ViewBlotterDialog(
            this, blotterData,
            this::showUpdateStatusDialog,
            row -> printBlotterFrame.show(row),
            this::getConnection,
            () -> {
                loadBlotterData();
                refreshTableAndStats();
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
            String sql = "SELECT blotter_id, complainant, Respondent, date, status, " +
                         "Cmplnt_address, complt_type, description " +
                         "FROM blotter ORDER BY date DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs   = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    blotterData.add(new Object[]{
                        rs.getInt("blotter_id"),
                        rs.getString("complainant"),
                        rs.getString("Respondent"),
                        rs.getDate("date") != null ? rs.getDate("date").toString() : "N/A",
                        rs.getString("status"),
                        rs.getString("Cmplnt_address"),
                        rs.getString("complt_type"),
                        rs.getString("description")
                    });
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error loading blotter data: " + e.getMessage());
        }
    }

    private void updateStatCards() {
        long total      = blotterData.size();
        long pending    = blotterData.stream()
                            .filter(r -> "pending".equalsIgnoreCase(r[4].toString())).count();
        long resolved   = blotterData.stream()
                            .filter(r -> "resolved".equalsIgnoreCase(r[4].toString())).count();

        if (totalValueLabel      != null) totalValueLabel.setText(String.valueOf(total));
        if (pendingValueLabel    != null) pendingValueLabel.setText(String.valueOf(pending));
        if (resolvedValueLabel   != null) resolvedValueLabel.setText(String.valueOf(resolved));
    }

    private void refreshTableAndStats() {
        tableModel.setRowCount(0);
        for (Object[] row : blotterData) {
            String statusDisplay = "pending".equalsIgnoreCase(row[4].toString())
                                   ? "Pending" : "Resolved";
            tableModel.addRow(new Object[]{
                row[0], row[1], row[2], row[3], statusDisplay, "View"
            });
        }
        updateStatCards();
        table.repaint();
    }

    private void setAllButtonsEnabled(boolean enabled) {
        if (addBtn    != null) addBtn.setEnabled(enabled);
        if (searchBtn != null) searchBtn.setEnabled(enabled);
        if (historyBtn != null) historyBtn.setEnabled(enabled);
        if (searchField != null) searchField.setEnabled(enabled);
    }

    // ── Root layout ────────────────────────────────────────────────────────

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(),   BorderLayout.CENTER);
        return root;
    }

    // ── Header ─────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BLUE_DARK);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

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
        left.add(badge);
        left.add(titles);

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
                    (getWidth()  - fm.stringWidth(initial)) / 2,
                    (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setOpaque(false);
        avatar.setPreferredSize(new Dimension(40, 40));

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

        header.add(left,  BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(
            this, "Are you sure you want to logout?",
            "Logout", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            new login().setVisible(true);
            dispose();
        }
    }

    // ── Body ───────────────────────────────────────────────────────────────

    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(20, 24, 20, 24));
        body.add(buildStatCards());
        body.add(Box.createVerticalStrut(20));
        body.add(buildTablePanel());
        return body;
    }

    // ── Stat cards ─────────────────────────────────────────────────────────

    private JPanel buildStatCards() {
        JPanel row = new JPanel(new GridLayout(1, 3, 20, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        row.setPreferredSize(new Dimension(0, 130));

        long total      = blotterData.size();
        long pending    = blotterData.stream()
                            .filter(r -> "pending".equalsIgnoreCase(r[4].toString())).count();
        long resolved   = blotterData.stream()
                            .filter(r -> "resolved".equalsIgnoreCase(r[4].toString())).count();

        totalValueLabel      = new JLabel(String.valueOf(total));
        pendingValueLabel    = new JLabel(String.valueOf(pending));
        resolvedValueLabel   = new JLabel(String.valueOf(resolved));

        row.add(statCard("TOTAL RECORDS",   totalValueLabel,    STAT_BLUE));
        row.add(statCard("PENDING CASES",   pendingValueLabel,  STAT_ORANGE));
        row.add(statCard("RESOLVED",        resolvedValueLabel, STAT_GREEN));
        return row;
    }

    private JPanel statCard(String label, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        valueLabel.setForeground(WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        valueLabel.setMinimumSize(new Dimension(60, 40));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(WHITE);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalStrut(10));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(lbl);
        card.add(Box.createVerticalStrut(10));
        return card;
    }

    // ── Table panel ────────────────────────────────────────────────────────

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        addBtn = createStyledButton("+ New Blotter", BLUE, BLUE_HOVER, WHITE);
        addBtn.addActionListener(e -> showAddDialog());

        // History Icon Button
        historyBtn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                int w = getWidth();
                int h = getHeight();
                
                // Button background
                if (getModel().isPressed()) {
                    g2.setColor(BLUE_HOVER.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(BLUE_HOVER);
                } else {
                    g2.setColor(BLUE);
                }
                g2.fillRoundRect(0, 0, w, h, 8, 8);
                
                // Draw clock icon
                g2.setColor(WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                
                int centerX = w / 2;
                int centerY = h / 2;
                int radius = 12;
                
                // Clock circle
                g2.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                
                // Clock hands
                g2.drawLine(centerX, centerY, centerX, centerY - 7);  // Minute hand
                g2.drawLine(centerX, centerY, centerX + 5, centerY - 3);  // Hour hand
                
                g2.dispose();
            }
            
            @Override public Dimension getPreferredSize() {
                return new Dimension(42, 38);
            }
            
            @Override public boolean isOpaque() {
                return false;
            }
        };
        historyBtn.setBorderPainted(false);
        historyBtn.setFocusPainted(false);
        historyBtn.setContentAreaFilled(false);
        historyBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        historyBtn.setToolTipText("View Complete History");
        historyBtn.addActionListener(e -> showHistoryFrame());

        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(WHITE);
        searchBar.setBorder(new CompoundBorder(
            new RoundedBorder(BORDER_CLR, 1, 8),
            new EmptyBorder(0, 12, 0, 0)));

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

        searchBtn = createStyledButton("Search", BLUE, BLUE_HOVER, WHITE);
        searchBtn.addActionListener(e -> filterTable());

        JPanel leftTools  = new JPanel(new FlowLayout(FlowLayout.LEFT,  8, 0));
        leftTools.setOpaque(false);
        // Only secretary can add new blotters
        if (isSecretary()) {
            leftTools.add(addBtn);
        }
        leftTools.add(historyBtn);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);
        rightTools.add(searchBar);
        rightTools.add(searchBtn);

        toolbar.add(leftTools,  BorderLayout.WEST);
        toolbar.add(rightTools, BorderLayout.EAST);

        String[] cols = {"ID", "Complainant", "Respondent", "Date", "Status", "Action"};
        tableModel = new DefaultTableModel(null, cols) {
            @Override public boolean isCellEditable(int row, int col) { return col == 5; }
        };
        for (Object[] row : blotterData) {
            String sd = "pending".equalsIgnoreCase(row[4].toString()) ? "Pending" : "Resolved";
            tableModel.addRow(new Object[]{row[0], row[1], row[2], row[3], sd, "View"});
        }

        table = new JTable(tableModel) {
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

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent evt) {
                int col = table.columnAtPoint(evt.getPoint());
                int row = table.rowAtPoint(evt.getPoint());
                // Only secretary can update status by clicking the status column
                if (col == 4 && row >= 0 && isSecretary()) showUpdateStatusDialog(row);
            }
        });

        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        scroll.setBackground(WHITE);
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(WHITE);
        tableContainer.setBorder(new RoundedBorder(BORDER_CLR, 1, 14));
        tableContainer.add(scroll, BorderLayout.CENTER);

        panel.add(toolbar,         BorderLayout.NORTH);
        panel.add(tableContainer,  BorderLayout.CENTER);
        return panel;
    }

    private void styleTable() {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(48);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(BLUE_LIGHT);
        table.setSelectionForeground(TEXT_PRI);
        table.setCellSelectionEnabled(true);
        table.setRowSelectionAllowed(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(240, 245, 250));
        header.setForeground(TEXT_PRI);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_CLR));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);

        int[] widths = {80, 160, 160, 100, 90, 220};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.getColumnModel().getColumn(0).setCellRenderer(
            (t, val, sel, foc, row, col) -> {
                JLabel l = new JLabel(val == null ? "" : "#" + val);
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setForeground(BLUE);
                l.setBorder(new EmptyBorder(0, 12, 0, 0));
                l.setOpaque(true);
                l.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
                return l;
            });

        table.getColumnModel().getColumn(4).setCellRenderer(
            (t, val, sel, foc, row, col) -> {
                String status    = val == null ? "" : val.toString();
                boolean isPending = "pending".equalsIgnoreCase(status);

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

        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    // ── Button renderer / editor ───────────────────────────────────────────

    class ButtonRenderer extends JPanel implements TableCellRenderer {
        private final JButton viewBtn;
        private final JButton printBtn;

        ButtonRenderer() {
            setOpaque(true);
            setLayout(new FlowLayout(FlowLayout.LEFT, 6, 6));
            
            viewBtn  = createActionButton("View", BLUE, new Color(230, 242, 255));
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
            
            viewBtn  = createActionButton("View", BLUE, new Color(230, 242, 255));
            printBtn = createActionButton("Print", STAT_GREEN, new Color(230, 245, 239));
            
            viewBtn.addActionListener(e -> {
                stopCellEditing();
                SwingUtilities.invokeLater(() -> viewRecord(currentRow));
            });
            
            printBtn.addActionListener(e -> {
                stopCellEditing();
                SwingUtilities.invokeLater(() -> printRecord(currentRow));
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

    // ── Delegated feature methods ──────────────────────────────────────────

    private void viewRecord(int row) {
        viewBlotterDialog.show(row);
    }

    private void printRecord(int row) {
        printBlotterFrame.show(row);
    }

    private void showAddDialog() {
        JDialog dialog = new JDialog(this, "Add New Blotter", true);
        dialog.setSize(820, 720);
        dialog.setMinimumSize(new Dimension(580, 500));
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        AddBlotterPanel addPanel = new AddBlotterPanel(
            dialog,
            this::getConnection,
            blotterData.size() + 1,
            () -> {
                loadBlotterData();
                refreshTableAndStats();
            }
        );

        dialog.setContentPane(addPanel);
        dialog.setVisible(true);
    }

    private void showHistoryFrame() {
        HistoryFrame historyFrame = new HistoryFrame(this, blotterData, this::viewRecord);
        historyFrame.show();
    }

    // ── Status update ──────────────────────────────────────────────────────

    private void showUpdateStatusDialog(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) return;

        String blotterNum   = tableModel.getValueAt(selectedRow, 0).toString();
        String currentStatus = tableModel.getValueAt(selectedRow, 4).toString();

        if ("resolved".equalsIgnoreCase(currentStatus)) {
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

        String displayStatus = "pending".equalsIgnoreCase(currentStatus) ? "Pending" : "Resolved";
        JLabel currentLabel  = new JLabel("Current Status: " + displayStatus);
        currentLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        currentLabel.setForeground(TEXT_SEC);
        currentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel newStatusLabel = new JLabel("New Status:");
        newStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        newStatusLabel.setForeground(TEXT_PRI);
        newStatusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"pending", "resolved"});
        statusCombo.setSelectedItem(currentStatus.toLowerCase());
        statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        statusCombo.setBackground(BLUE_LIGHT);
        statusCombo.setForeground(TEXT_PRI);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(WHITE);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton saveBtn = createStyledButton("Save", BLUE, BLUE_HOVER, WHITE);
        saveBtn.addActionListener(e -> {
            String newStatus = (String) statusCombo.getSelectedItem();
            if (newStatus != null && !newStatus.equalsIgnoreCase(currentStatus)) {
                updateStatus(selectedRow, newStatus, blotterNum);
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
                        pstmt.setString(1, newStatus.toLowerCase());
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
                        String display = "pending".equalsIgnoreCase(newStatus)
                                         ? "Pending" : "Resolved";
                        if (row < blotterData.size()) blotterData.get(row)[4] = newStatus;
                        tableModel.setValueAt(display, row, 4);
                        updateStatCards();
                        JOptionPane.showMessageDialog(dashboard.this,
                            "Status updated to: " + display, "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    String msg = "Error updating status";
                    if (e.getCause() != null && e.getCause().getMessage() != null)
                        msg += ": " + e.getCause().getMessage();
                    else if (e.getMessage() != null)
                        msg += ": " + e.getMessage();
                    JOptionPane.showMessageDialog(dashboard.this, msg,
                        "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setAllButtonsEnabled(true);
                    table.setEnabled(true);
                }
            }
        }.execute();
    }

    // ── Search ─────────────────────────────────────────────────────────────

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

    // ── Styled button factory ──────────────────────────────────────────────

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
                    (getWidth()  - fm.stringWidth(getText())) / 2,
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

    // ── Rounded border ─────────────────────────────────────────────────────

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
}
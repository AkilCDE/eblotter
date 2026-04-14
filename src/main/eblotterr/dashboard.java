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

    // ── Modern color palette (matching Login.java) ─────────────────────────
    private static final Color WHITE = Color.WHITE;
    private static final Color BLUE = new Color(24, 95, 165);
    private static final Color BLUE_HOVER = new Color(14, 76, 136);
    private static final Color BLUE_DARK = new Color(18, 58, 110);
    private static final Color BLUE_LIGHT = new Color(230, 241, 251);
    private static final Color BG = new Color(245, 246, 248);
    private static final Color BORDER_CLR = new Color(215, 220, 228);
    private static final Color TEXT_PRI = new Color(28, 32, 40);
    private static final Color TEXT_SEC = new Color(110, 120, 140);
    private static final Color TEXT_LIGHT = new Color(180, 190, 205);
    private static final Color STAT_BLUE = new Color(24, 95, 165);
    private static final Color STAT_AMBER = new Color(217, 119, 6);
    private static final Color STAT_GREEN = new Color(25, 135, 84);
    private static final Color PENDING_BG = new Color(255, 244, 229);
    private static final Color PENDING_FG = new Color(217, 119, 6);
    private static final Color RESOLVED_BG = new Color(222, 247, 236);
    private static final Color RESOLVED_FG = new Color(25, 135, 84);

    private final String currentUsername;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private final List<Object[]> blotterData = new ArrayList<>();
    private JPanel statRow;
    private JButton addBtn;
    private JButton searchBtn;

    public dashboard() {
        this.currentUsername = "User";
        setTitle("Barangay e-Blotter — Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);
        loadBlotterData();
        setContentPane(buildRoot());
    }

    public dashboard(String username) {
        this.currentUsername = username;
        setTitle("Barangay e-Blotter — Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setMinimumSize(new Dimension(950, 650));
        setLocationRelativeTo(null);
        loadBlotterData();
        setContentPane(buildRoot());
    }

    private void loadBlotterData() {
        blotterData.clear();
        String url = "jdbc:mysql://localhost:3306/ebs";
        String user = "root";
        String pass = "";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet tables = meta.getTables(null, null, "blotters", null)) {
                    if (!tables.next()) {
                        createBlottersTable(conn);
                    }
                }
                
                String sql = "SELECT blotter_id, blotter_number, complainant, respondent, incident_date, status FROM blotters ORDER BY incident_date DESC";
                try (Statement stmt = conn.createStatement()) {
                    try (ResultSet rs = stmt.executeQuery(sql)) {
                        while (rs.next()) {
                            Object[] row = {
                                rs.getString("blotter_number"),
                                rs.getString("complainant"),
                                rs.getString("respondent"),
                                rs.getDate("incident_date") != null ? rs.getDate("incident_date").toString() : "N/A",
                                rs.getString("status"),
                                rs.getInt("blotter_id")
                            };
                            blotterData.add(row);
                        }
                    }
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error loading blotter data: " + e.getMessage());
        }
        
        if (blotterData.isEmpty()) {
            addSampleData();
        }
    }
    
    private void createBlottersTable(Connection conn) throws SQLException {
        String createTable = "CREATE TABLE IF NOT EXISTS blotters (" +
            "blotter_id INT PRIMARY KEY AUTO_INCREMENT," +
            "blotter_number VARCHAR(50) NOT NULL UNIQUE," +
            "complainant VARCHAR(100) NOT NULL," +
            "respondent VARCHAR(100) NOT NULL," +
            "incident_date DATE NOT NULL," +
            "status VARCHAR(20) DEFAULT 'Pending'," +
            "details TEXT," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
        Statement stmt = conn.createStatement();
        stmt.execute(createTable);
        stmt.close();
        
        // Insert sample data
        String insertSample = "INSERT IGNORE INTO blotters (blotter_number, complainant, respondent, incident_date, status) VALUES " +
            "('#2025-001', 'Juan dela Cruz', 'Pedro Reyes', '2025-01-05', 'Pending')," +
            "('#2025-002', 'Ana Lim', 'Jose Santos', '2025-01-08', 'Resolved')," +
            "('#2025-003', 'Maria Garcia', 'Roberto Cruz', '2025-01-11', 'Pending')," +
            "('#2025-004', 'Carlos Bautista', 'Lito Ramos', '2025-01-15', 'Resolved')," +
            "('#2025-005', 'Elena Reyes', 'Noel Pascual', '2025-01-20', 'Pending')";
        stmt = conn.createStatement();
        stmt.execute(insertSample);
        stmt.close();
    }
    
    private void addSampleData() {
        blotterData.clear();
        blotterData.add(new Object[]{"#2025-001", "Juan dela Cruz", "Pedro Reyes", "2025-01-05", "Pending", 1});
        blotterData.add(new Object[]{"#2025-002", "Ana Lim", "Jose Santos", "2025-01-08", "Resolved", 2});
        blotterData.add(new Object[]{"#2025-003", "Maria Garcia", "Roberto Cruz", "2025-01-11", "Pending", 3});
        blotterData.add(new Object[]{"#2025-004", "Carlos Bautista", "Lito Ramos", "2025-01-15", "Resolved", 4});
        blotterData.add(new Object[]{"#2025-005", "Elena Reyes", "Noel Pascual", "2025-01-20", "Pending", 5});
    }

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BLUE_DARK);
        header.setBorder(new EmptyBorder(14, 24, 14, 24));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);

        // Logo/badge
        JPanel badge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(WHITE);
                g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
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
            String[] parts = displayName.split("_");
            displayName = parts[0].substring(0, 1).toUpperCase() + parts[0].substring(1) + " " + 
                         (parts.length > 1 ? parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1) : "");
        } else {
            displayName = displayName.substring(0, 1).toUpperCase() + displayName.substring(1);
        }
        
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(WHITE);
        nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        JLabel dateLabel = new JLabel("Barangay Staff  ·  " + today);
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        dateLabel.setForeground(TEXT_LIGHT);
        dateLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        userInfoPanel.add(nameLabel);
        userInfoPanel.add(dateLabel);

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLUE);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String initial = currentUsername != null && !currentUsername.isEmpty() ? 
                                currentUsername.substring(0, 1).toUpperCase() : "U";
                g2.drawString(initial, (getWidth() - fm.stringWidth(initial)) / 2, 
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

        header.add(left, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }
    
    private void logout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to logout?", "Logout", 
                                                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            new login().setVisible(true);
            dispose();
        }
    }

    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(20, 24, 20, 24));

        statRow = buildStatCards();
        body.add(statRow);
        body.add(Box.createVerticalStrut(20));
        body.add(buildTablePanel());

        return body;
    }

    private JPanel buildStatCards() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        long total = blotterData.size();
        long pending = blotterData.stream().filter(r -> "Pending".equals(r[4])).count();
        long resolved = blotterData.stream().filter(r -> "Resolved".equals(r[4])).count();
        long unresolved = total - resolved;

        row.add(statCard("TOTAL BLOTTERS", String.valueOf(total), STAT_BLUE, "📋"));
        row.add(statCard("PENDING CASES", String.valueOf(pending), STAT_AMBER, "⏳"));
        row.add(statCard("RESOLVED", String.valueOf(resolved), STAT_GREEN, "✓"));
        row.add(statCard("UNRESOLVED", String.valueOf(unresolved), STAT_AMBER, "⚠"));
        return row;
    }

    private JPanel statCard(String label, String value, Color bg, String icon) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 15));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        iconLabel.setForeground(bg);
        
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 36));
        val.setForeground(bg);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        
        topRow.add(iconLabel, BorderLayout.WEST);
        topRow.add(val, BorderLayout.EAST);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_SEC);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(topRow);
        card.add(Box.createVerticalStrut(8));
        card.add(lbl);

        return card;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        JPanel toolbar = new JPanel(new BorderLayout(10, 0));
        toolbar.setOpaque(false);
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        addBtn = createStyledButton("+ New Blotter", BLUE, BLUE_HOVER, WHITE);
        addBtn.addActionListener(e -> showAddDialog());

        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(WHITE);
        searchBar.setBorder(new CompoundBorder(new RoundedBorder(BORDER_CLR, 1, 8), new EmptyBorder(0, 12, 0, 0)));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchIcon.setForeground(TEXT_SEC);

        searchField = new JTextField("Search blotter...");
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setForeground(TEXT_SEC);
        searchField.setBorder(null);
        searchField.setOpaque(false);
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search blotter...")) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_PRI);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search blotter...");
                    searchField.setForeground(TEXT_SEC);
                }
            }
        });
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterTable();
            }
        });

        searchBar.add(searchIcon, BorderLayout.WEST);
        searchBar.add(searchField, BorderLayout.CENTER);

        searchBtn = createStyledButton("Search", BLUE, BLUE_HOVER, WHITE);
        searchBtn.addActionListener(e -> filterTable());

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTools.setOpaque(false);
        leftTools.add(addBtn);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);
        rightTools.add(searchBar);
        rightTools.add(searchBtn);

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(rightTools, BorderLayout.EAST);

        String[] cols = {"Blotter #", "Complainant", "Respondent", "Incident Date", "Status", ""};
        tableModel = new DefaultTableModel(null, cols) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 5;
            }
        };
        
        for (Object[] row : blotterData) {
            tableModel.addRow(new Object[]{row[0], row[1], row[2], row[3], row[4], "View"});
        }

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
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
        
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                int col = table.columnAtPoint(evt.getPoint());
                int row = table.rowAtPoint(evt.getPoint());
                if (col == 4 && row >= 0) {
                    showUpdateStatusDialog(row);
                }
            }
        });
        
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(WHITE);
        scroll.setBackground(WHITE);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(WHITE);
        tableContainer.setBorder(new RoundedBorder(BORDER_CLR, 1, 14));
        tableContainer.add(scroll, BorderLayout.CENTER);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(tableContainer, BorderLayout.CENTER);
        return panel;
    }

    private void styleTable() {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(48);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(BLUE_LIGHT);
        table.setSelectionForeground(TEXT_PRI);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(240, 245, 250));
        header.setForeground(TEXT_PRI);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_CLR));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        int[] widths = {110, 160, 160, 110, 90, 70};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Style the blotter number column
        table.getColumnModel().getColumn(0).setCellRenderer((t, val, sel, foc, row, col) -> {
            JLabel l = new JLabel(val == null ? "" : val.toString());
            l.setFont(new Font("Segoe UI", Font.BOLD, 13));
            l.setForeground(BLUE);
            l.setBorder(new EmptyBorder(0, 12, 0, 0));
            l.setOpaque(true);
            l.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
            return l;
        });

        // Style status column with pill background
        table.getColumnModel().getColumn(4).setCellRenderer((t, val, sel, foc, row, col) -> {
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
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

        // Action button column
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));
    }

    class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton viewBtn = new JButton("View");
        private JButton printBtn = new JButton("Print");
        
        public ButtonRenderer() {
            setOpaque(true);
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 2));
            
            styleButton(viewBtn);
            styleButton(printBtn);
            
            add(viewBtn);
            add(printBtn);
        }
        
        private void styleButton(JButton btn) {
            btn.setBackground(BLUE);
            btn.setForeground(WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setBorder(new EmptyBorder(3, 8, 3, 8));
            btn.setEnabled(false);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
            }
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 2));
        private JButton viewBtn = new JButton("View");
        private JButton printBtn = new JButton("Print");
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            panel.setOpaque(true);
            panel.setBackground(WHITE);
            
            styleButton(viewBtn);
            styleButton(printBtn);
            
            viewBtn.addActionListener(e -> {
                viewRecord(currentRow);
                fireEditingStopped();
            });
            
            printBtn.addActionListener(e -> {
                printRecord(currentRow);
                fireEditingStopped();
            });
            
            panel.add(viewBtn);
            panel.add(printBtn);
        }
        
        private void styleButton(JButton btn) {
            btn.setBackground(BLUE);
            btn.setForeground(WHITE);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 10));
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.setBorder(new EmptyBorder(3, 8, 3, 8));
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = row;
            if (isSelected) {
                panel.setBackground(table.getSelectionBackground());
            } else {
                panel.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
            }
            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "View|Print";
        }
    }

    private void viewRecord(int row) {
        Object blotterNum = tableModel.getValueAt(row, 0);
        Object complainant = tableModel.getValueAt(row, 1);
        Object respondent = tableModel.getValueAt(row, 2);
        Object date = tableModel.getValueAt(row, 3);
        Object status = tableModel.getValueAt(row, 4);
        
        JDialog detailDialog = new JDialog(this, "Blotter Details", true);
        detailDialog.setSize(450, 380);
        detailDialog.setLocationRelativeTo(this);
        
        JPanel content = new JPanel();
        content.setBackground(WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(24, 28, 24, 28));
        
        JLabel title = new JLabel("Blotter Information");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(WHITE);
        details.setAlignmentX(Component.LEFT_ALIGNMENT);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(6, 0, 6, 20);
        
        addDetailRow(details, "Blotter Number:", blotterNum != null ? blotterNum.toString() : "N/A", gbc, 0);
        addDetailRow(details, "Complainant:", complainant != null ? complainant.toString() : "N/A", gbc, 1);
        addDetailRow(details, "Respondent:", respondent != null ? respondent.toString() : "N/A", gbc, 2);
        addDetailRow(details, "Incident Date:", date != null ? date.toString() : "N/A", gbc, 3);
        addDetailRow(details, "Status:", status != null ? status.toString() : "N/A", gbc, 4);
        
        JPanel statusBadge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isPending = "Pending".equals(status);
                g2.setColor(isPending ? PENDING_BG : RESOLVED_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        statusBadge.setLayout(new BorderLayout());
        statusBadge.setOpaque(false);
        JLabel statusLabel = new JLabel(status != null ? status.toString() : "N/A");
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground("Pending".equals(status) ? PENDING_FG : RESOLVED_FG);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(6, 20, 6, 20));
        statusBadge.add(statusLabel, BorderLayout.CENTER);
        
        gbc.gridx = 1;
        gbc.gridy = 4;
        details.add(statusBadge, gbc);
        
        JButton closeBtn = createStyledButton("Close", new Color(108, 117, 125), new Color(90, 98, 104), WHITE);
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> detailDialog.dispose());
        
        JButton editBtn = createStyledButton("Edit Status", BLUE, BLUE_HOVER, WHITE);
        editBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        editBtn.addActionListener(e -> {
            String newStatus = "Pending".equals(status) ? "Resolved" : "Pending";
            updateStatus(row, newStatus, blotterNum != null ? blotterNum.toString() : null);
            detailDialog.dispose();
        });
        
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        buttonRow.setBackground(WHITE);
        buttonRow.add(editBtn);
        buttonRow.add(closeBtn);
        
        content.add(title);
        content.add(Box.createVerticalStrut(20));
        content.add(details);
        content.add(Box.createVerticalStrut(24));
        content.add(buttonRow);
        
        detailDialog.setContentPane(content);
        detailDialog.setVisible(true);
    }
    
    private void addDetailRow(JPanel panel, String label, String value, GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_SEC);
        panel.add(lbl, gbc);
        
        gbc.gridx = 1;
        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        val.setForeground(TEXT_PRI);
        panel.add(val, gbc);
    }
    
    private void updateStatus(int row, String newStatus, String blotterNumber) {
        setAllButtonsEnabled(false);
        table.setEnabled(false);
        
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String url = "jdbc:mysql://localhost:3306/ebs";
                String user = "root";
                String pass = "";
                
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                        String update = "UPDATE blotters SET status = ? WHERE blotter_number = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(update)) {
                            pstmt.setString(1, newStatus);
                            pstmt.setString(2, blotterNumber);
                            pstmt.executeUpdate();
                        }
                    }
                    return true;
                } catch (ClassNotFoundException | SQLException e) {
                    System.err.println("Database error: " + e.getMessage());
                    throw e;
                }
            }
            
            @Override
            protected void done() {
                try {
                    if (get()) {
                        // Update UI on EDT
                        blotterData.get(row)[4] = newStatus;
                        tableModel.setValueAt(newStatus, row, 4);
                        JOptionPane.showMessageDialog(dashboard.this, "Status updated to: " + newStatus, "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (InterruptedException | java.util.concurrent.ExecutionException e) {
                    String message = "Error updating status";
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        message += ": " + e.getCause().getMessage();
                    } else if (e.getMessage() != null) {
                        message += ": " + e.getMessage();
                    }
                    JOptionPane.showMessageDialog(dashboard.this, message, "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setAllButtonsEnabled(true);
                    table.setEnabled(true);
                }
            }
        }.execute();
    }

    private void showUpdateStatusDialog(int selectedRow) {
        if (selectedRow < 0 || selectedRow >= tableModel.getRowCount()) {
            return;
        }

        String blotterNum = tableModel.getValueAt(selectedRow, 0).toString();
        String currentStatus = tableModel.getValueAt(selectedRow, 4).toString();

        JDialog dialog = new JDialog(this, "Update Status", true);
        dialog.setSize(450, 320);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel content = new JPanel();
        content.setBackground(WHITE);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Update Status for " + blotterNum);
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
        statusCombo.setForeground(TEXT_PRI);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        buttonPanel.setBackground(WHITE);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton saveBtn = createStyledButton("Save", BLUE, BLUE_HOVER, WHITE);
        saveBtn.addActionListener(e -> {
            String newStatus = (String) statusCombo.getSelectedItem();
            if (!newStatus.equals(currentStatus)) {
                updateStatus(selectedRow, newStatus, blotterNum);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "No changes made.", "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JButton cancelBtn = createStyledButton("Cancel", new Color(108, 117, 125), new Color(90, 98, 104), WHITE);
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

    private void showAddDialog() {
        JDialog dlg = new JDialog(this, "New Blotter Entry", true);
        dlg.setSize(480, 480);
        dlg.setLocationRelativeTo(this);

        JPanel form = new JPanel();
        form.setBackground(WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("File New Blotter");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRI);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField blotterField = createFormField();
        JTextField complField = createFormField();
        JTextField respField = createFormField();
        JTextField dateField = createFormField();
        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Pending", "Resolved"});
        statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        statusCombo.setBorder(new CompoundBorder(new RoundedBorder(BORDER_CLR, 1, 6), new EmptyBorder(8, 12, 8, 12)));
        
        JTextArea detailsArea = new JTextArea(3, 20);
        detailsArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailsArea.setBorder(new CompoundBorder(new RoundedBorder(BORDER_CLR, 1, 6), new EmptyBorder(8, 12, 8, 12)));
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        detailsScroll.setBorder(null);

        JButton save = createStyledButton("Save Blotter Record", BLUE, BLUE_HOVER, WHITE);
        save.setAlignmentX(Component.LEFT_ALIGNMENT);
        save.addActionListener(e -> {
            String blotter = blotterField.getText().trim();
            String comp = complField.getText().trim();
            String resp = respField.getText().trim();
            String date = dateField.getText().trim();
            String status = (String) statusCombo.getSelectedItem();
            String details = detailsArea.getText().trim();
            
            if (blotter.isEmpty() || comp.isEmpty() || resp.isEmpty() || date.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Please fill all required fields.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Save to database
            saveToDatabase(blotter, comp, resp, date, status, details);
            
            tableModel.addRow(new Object[]{blotter, comp, resp, date, status, "View"});
            blotterData.add(new Object[]{blotter, comp, resp, date, status, blotterData.size() + 1});
            dlg.dispose();
            refreshStats();
        });

        form.add(title);
        form.add(Box.createVerticalStrut(20));
        form.add(createFormRow("Blotter #", blotterField));
        form.add(Box.createVerticalStrut(12));
        form.add(createFormRow("Complainant", complField));
        form.add(Box.createVerticalStrut(12));
        form.add(createFormRow("Respondent", respField));
        form.add(Box.createVerticalStrut(12));
        form.add(createFormRow("Date", dateField));
        form.add(Box.createVerticalStrut(12));
        form.add(createFormRow("Status", statusCombo));
        form.add(Box.createVerticalStrut(12));
        
        JPanel detailsRow = new JPanel(new BorderLayout(8, 0));
        detailsRow.setBackground(WHITE);
        detailsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        JLabel detailsLabel = new JLabel("Details");
        detailsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        detailsLabel.setForeground(TEXT_PRI);
        detailsLabel.setPreferredSize(new Dimension(90, 38));
        detailsRow.add(detailsLabel, BorderLayout.WEST);
        detailsRow.add(detailsScroll, BorderLayout.CENTER);
        form.add(detailsRow);
        
        form.add(Box.createVerticalStrut(24));
        form.add(save);

        dlg.setContentPane(form);
        dlg.setVisible(true);
    }
    
    private void saveToDatabase(String blotter, String complainant, String respondent, String date, String status, String details) {
        String url = "jdbc:mysql://localhost:3306/ebs";
        String user = "root";
        String pass = "";
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                String insert = "INSERT INTO blotters (blotter_number, complainant, respondent, incident_date, status, details) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                    pstmt.setString(1, blotter);
                    pstmt.setString(2, complainant);
                    pstmt.setString(3, respondent);
                    pstmt.setDate(4, java.sql.Date.valueOf(date));
                    pstmt.setString(5, status);
                    pstmt.setString(6, details);
                    pstmt.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Blotter record saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (ClassNotFoundException | SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saving to database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private JTextField createFormField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setForeground(TEXT_PRI);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        f.setBorder(new CompoundBorder(new RoundedBorder(BORDER_CLR, 1, 6), new EmptyBorder(8, 12, 8, 12)));
        return f;
    }
    
    private JPanel createFormRow(String labelTxt, JComponent field) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(labelTxt);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(TEXT_PRI);
        lbl.setPreferredSize(new Dimension(90, 38));
        row.add(lbl, BorderLayout.WEST);
        row.add(field, BorderLayout.CENTER);
        return row;
    }

    private void setAllButtonsEnabled(boolean enabled) {
        if (addBtn != null) addBtn.setEnabled(enabled);
        if (searchBtn != null) searchBtn.setEnabled(enabled);
        searchField.setEnabled(enabled);
    }

    private void refreshStats() {
        Container parent = getContentPane();
        parent.removeAll();
        setContentPane(buildRoot());
        parent.revalidate();
        parent.repaint();
    }
    
    private void filterTable() {
        String query = searchField.getText().toLowerCase().trim();
        if (query.equals("search blotter...") || query.isEmpty()) {
            tableModel.setRowCount(0);
            for (Object[] row : blotterData) {
                tableModel.addRow(new Object[]{row[0], row[1], row[2], row[3], row[4], "View"});
            }
        } else {
            tableModel.setRowCount(0);
            for (Object[] row : blotterData) {
                boolean match = false;
                for (int i = 0; i < 4; i++) {
                    if (row[i] != null && row[i].toString().toLowerCase().contains(query)) {
                        match = true;
                        break;
                    }
                }
                if (match) {
                    tableModel.addRow(new Object[]{row[0], row[1], row[2], row[3], row[4], "View"});
                }
            }
        }
    }

    private JButton createStyledButton(String text, Color bg, Color hover, Color fg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? hover : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(fg);
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, 
                             (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(fg);
        b.setPreferredSize(new Dimension(110, 38));
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static class RoundedBorder extends AbstractBorder {
        private final Color color;
        private final int thickness, radius;

        RoundedBorder(Color c, int t, int r) {
            color = c;
            thickness = t;
            radius = r;
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
            return new Insets(radius / 3, radius / 3, radius / 3, radius / 3);
        }
    }
}
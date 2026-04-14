package main.eblotterr;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
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
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private final List<Object[]> blotterData = new ArrayList<>();
    private JButton addBtn;
    private JButton searchBtn;

    // ── Stat card value labels ─────────────────────────────────────────────
    private JLabel totalValueLabel;
    private JLabel pendingValueLabel;
    private JLabel resolvedValueLabel;

    public dashboard() {
        this("User");
    }

    public dashboard(String username) {
        this.currentUsername = username;
        setTitle("Barangay e-Blotter — Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 750);
        setResizable(false);
        setLocationRelativeTo(null);
        loadBlotterData();
        setContentPane(buildRoot());
    }

    // ── Database helpers ───────────────────────────────────────────────────

    private Connection getConnection() throws ClassNotFoundException, SQLException {
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
        JLabel dateLabel = new JLabel("Barangay Staff  ·  " + today);
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
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

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

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        valueLabel.setForeground(WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

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
        leftTools.add(addBtn);

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
                if (col == 4 && row >= 0) showUpdateStatusDialog(row);
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

        int[] widths = {110, 160, 160, 110, 90, 240};
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

    // ── View / Print ───────────────────────────────────────────────────────

    private void viewRecord(int row) {
        if (row < 0 || row >= blotterData.size()) return;
        
        Object[] data = blotterData.get(row);
        Object blotterNum  = data[0];
        Object complainant = data[1];
        Object respondent  = data[2];
        Object date        = data[3];
        Object status      = data[4];
        Object address     = data[5];
        Object compType    = data[6];
        Object description = data[7];

        JDialog detailDialog = new JDialog(this, "Blotter Details - #" + blotterNum, true);
        detailDialog.setSize(750, 650);
        detailDialog.setMinimumSize(new Dimension(600, 550));
        detailDialog.setLocationRelativeTo(this);
        detailDialog.getContentPane().setBackground(new Color(0xEAF1FB));
        
        // Main panel with background
        JPanel mainPanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0xEAF1FB));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        
        // Header
        mainPanel.add(buildDetailHeader(blotterNum.toString()), BorderLayout.NORTH);
        
        // Content body
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(16, 24, 24, 24));
        
        // Blotter Number Bar
        body.add(buildDetailBlotterBar(blotterNum.toString()));
        body.add(Box.createVerticalStrut(14));
        
        // Parties Involved Card
        body.add(buildDetailPartiesCard(complainant, respondent, address, date));
        body.add(Box.createVerticalStrut(16));
        
        // Incident Details Card
        body.add(buildDetailIncidentCard(compType, description, status));
        body.add(Box.createVerticalStrut(20));
        
        // Button Row
        body.add(buildDetailButtonRow(detailDialog, row, status));
        body.add(Box.createVerticalStrut(10));
        
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        
        mainPanel.add(scroll, BorderLayout.CENTER);
        
        detailDialog.setContentPane(mainPanel);
        detailDialog.setVisible(true);
    }

    // ── Detail View Helper Methods ────────────────────────────────────────────

    private JPanel buildDetailHeader(String blotterNum) {
        JPanel header = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(0x1B3A5C));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));
        
        JButton closeBtn = new JButton("← Back");
        closeBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        closeBtn.setForeground(new Color(0xA8C4E0));
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(header);
            if (window != null) window.dispose();
        });
        
        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        
        JLabel subLabel = new JLabel("CASE DETAILS");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLabel.setForeground(new Color(0xA8C4E0));
        
        JLabel titleLabel = new JLabel("Blotter Information");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        titleBlock.add(subLabel);
        titleBlock.add(titleLabel);
        
        JLabel menuDots = new JLabel("⋯");
        menuDots.setFont(new Font("Segoe UI", Font.BOLD, 20));
        menuDots.setForeground(new Color(0xA8C4E0));
        menuDots.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(closeBtn);
        left.add(titleBlock);
        
        header.add(left, BorderLayout.WEST);
        header.add(menuDots, BorderLayout.EAST);
        
        return header;
    }

    private JPanel buildDetailBlotterBar(String blotterNum) {
        JPanel bar = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x1B3A5C));
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        JLabel lKey = new JLabel("Blotter Number");
        lKey.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lKey.setForeground(new Color(0xA8C4E0));
        
        JLabel lVal = new JLabel("#" + blotterNum);
        lVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lVal.setForeground(Color.WHITE);
        
        bar.add(lKey, BorderLayout.WEST);
        bar.add(lVal, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildDetailPartiesCard(Object complainant, Object respondent, Object address, Object date) {
        JPanel card = createDetailCard();
        
        JLabel sectionLabel = createDetailSectionHeader("PARTIES INVOLVED");
        
        // Row 1: Complainant Name | Respondent Name
        JPanel row1 = new JPanel(new GridLayout(1, 2, 14, 0));
        row1.setOpaque(false);
        row1.add(createDetailField("Complainant's Full Name", complainant != null ? complainant.toString() : "N/A", false));
        row1.add(createDetailField("Respondent's Full Name", respondent != null ? respondent.toString() : "N/A", true));
        
        // Row 2: Complainant's Address | Date of Incident
        JPanel row2 = new JPanel(new GridLayout(1, 2, 14, 0));
        row2.setOpaque(false);
        row2.add(createDetailField("Complainant's Address", address != null ? address.toString() : "N/A", false));
        row2.add(createDetailField("Date of Incident", date != null ? date.toString() : "N/A", false));
        
        card.add(sectionLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(row1);
        card.add(Box.createVerticalStrut(12));
        card.add(row2);
        
        return card;
    }

    private JPanel buildDetailIncidentCard(Object compType, Object description, Object status) {
        JPanel card = createDetailCard();
        
        JLabel sectionLabel = createDetailSectionHeader("INCIDENT DETAILS");
        
        // Type of Complaint field
        JPanel typeField = createDetailField("Type of Complaint", 
            compType != null ? compType.toString() : "N/A", false);
        
        // Description field
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setOpaque(false);
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.Y_AXIS));
        
        JLabel descriptionLabel = new JLabel("Description / Incident Details");
        descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descriptionLabel.setForeground(new Color(0x2C4A6E));
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextArea descriptionArea = new JTextArea(description != null ? description.toString() : "");
        descriptionArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descriptionArea.setForeground(new Color(0x1A1A2E));
        descriptionArea.setBackground(Color.WHITE);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(0xC8D8EC), 1, 8),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        descriptionArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setBorder(null);
        descriptionScroll.setOpaque(false);
        descriptionScroll.getViewport().setOpaque(false);
        descriptionScroll.setPreferredSize(new Dimension(200, 100));
        descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        descriptionScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        descriptionPanel.add(descriptionLabel);
        descriptionPanel.add(Box.createVerticalStrut(4));
        descriptionPanel.add(descriptionScroll);
        
        // Status badge
        String statusStr = status != null ? status.toString() : "pending";
        boolean isPending = "pending".equalsIgnoreCase(statusStr);
        String displayStatus = isPending ? "Pending" : "Resolved";
        
        JPanel statusBadge = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isPending ? PENDING_BG : RESOLVED_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.dispose();
            }
        };
        statusBadge.setLayout(new BorderLayout());
        statusBadge.setOpaque(false);
        statusBadge.setBorder(BorderFactory.createEmptyBorder(4, 16, 4, 16));
        
        JLabel statusLabel = new JLabel("● " + displayStatus);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statusLabel.setForeground(isPending ? PENDING_FG : RESOLVED_FG);
        statusBadge.add(statusLabel, BorderLayout.CENTER);
        
        JPanel statusWrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusWrapper.setOpaque(false);
        statusWrapper.add(statusBadge);
        statusWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        card.add(sectionLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(typeField);
        card.add(Box.createVerticalStrut(16));
        card.add(descriptionPanel);
        card.add(Box.createVerticalStrut(16));
        card.add(new JLabel("Status:"));
        card.add(Box.createVerticalStrut(6));
        card.add(statusWrapper);
        
        return card;
    }

    private JPanel buildDetailButtonRow(JDialog dialog, int row, Object status) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rowPanel.setOpaque(false);
        rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        
        String statusStr = status != null ? status.toString() : "pending";
        boolean isPending = "pending".equalsIgnoreCase(statusStr);
        
        JButton closeBtn = createDetailButton("Close", Color.WHITE, new Color(0x1B3A5C), true);
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JButton printBtn = createDetailButton("Print Report", new Color(0x1B3A5C), Color.WHITE, false);
        printBtn.addActionListener(e -> {
            dialog.dispose();
            printRecord(row);
        });
        
        JButton editBtn = null;
        if (isPending) {
            editBtn = createDetailButton("Update Status", new Color(0xFF8C3C), Color.WHITE, false);
            JButton finalEditBtn = editBtn;
            editBtn.addActionListener(e -> {
                dialog.dispose();
                showUpdateStatusDialog(row);
            });
            rowPanel.add(finalEditBtn);
        }
        
        rowPanel.add(printBtn);
        rowPanel.add(closeBtn);
        
        return rowPanel;
    }

    // ── Detail View Card Helpers ──────────────────────────────────────────────

    private JPanel createDetailCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fill(new RoundRectangle2D.Double(3, 5, getWidth()-6, getHeight()-6, 14, 14));
                // Card body
                g2.setColor(Color.WHITE);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth()-4, getHeight()-4, 12, 12));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return card;
    }

    private JLabel createDetailSectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(0x1B3A5C));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel createDetailField(String labelText, String value, boolean isRespondent) {
        JPanel pnl = new JPanel();
        pnl.setOpaque(false);
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(0x2C4A6E));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JTextField tf = new JTextField(value);
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setForeground(new Color(0x1A1A2E));
        tf.setBackground(isRespondent ? new Color(0xFFF0EE) : Color.WHITE);
        tf.setEditable(false);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(isRespondent ? new Color(0xF5C0BB) : new Color(0xC8D8EC), 1, 8),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        pnl.add(lbl);
        pnl.add(Box.createVerticalStrut(4));
        pnl.add(tf);
        
        return pnl;
    }

    private JButton createDetailButton(String text, Color bg, Color fg, boolean outlined) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isPressed())
                    g2.setColor(bg.darker());
                else if (getModel().isRollover())
                    g2.setColor(bg.brighter());
                else
                    g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                if (outlined) {
                    g2.setColor(new Color(0xC0CFDF));
                    g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 8, 8));
                }
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(text.length() > 10 ? 140 : 100, 40));
        return btn;
    }

    private void printRecord(int row) {
        if (row < 0 || row >= blotterData.size()) return;
        
        Object[] data = blotterData.get(row);
        showPrintPreview(
            data[0].toString(),
            data[1].toString(),
            data[2].toString(),
            data[3].toString(),
            data[4].toString(),
            data[5].toString(),
            data[6].toString(),
            data[7] != null ? data[7].toString() : "");
    }

   private void showPrintPreview(String blotterNum, String complainant,
                              String respondent, String date,
                              String status, String address,
                              String complaintType, String description) {
    JFrame pf = new JFrame("Print Preview - Blotter #" + blotterNum);
    pf.setSize(750, 700);
    pf.setLocationRelativeTo(this);
    pf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    pf.setResizable(true);
    pf.getContentPane().setBackground(new Color(240, 242, 245));

    // Main content panel with clean white background
    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
    content.setBackground(new Color(240, 242, 245));
    content.setBorder(new EmptyBorder(20, 20, 20, 20));

    // === HEADER PANEL (Republic of the Philippines style) ===
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
    headerPanel.setBackground(Color.WHITE);
    headerPanel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(20, 25, 20, 25)));
    headerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    headerPanel.setMaximumSize(new Dimension(680, 120));

    JLabel republicLabel = new JLabel("REPUBLIC OF THE PHILIPPINES");
    republicLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
    republicLabel.setForeground(new Color(45, 118, 200));
    republicLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel barangayLabel = new JLabel("Barangay Mabini, Central Visayas");
    barangayLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    barangayLabel.setForeground(new Color(80, 80, 80));
    barangayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel blotterTitle = new JLabel("BARANGAY BLOTTER REPORT");
    blotterTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
    blotterTitle.setForeground(new Color(25, 60, 110));
    blotterTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

    headerPanel.add(republicLabel);
    headerPanel.add(Box.createVerticalStrut(2));
    headerPanel.add(barangayLabel);
    headerPanel.add(Box.createVerticalStrut(15));
    headerPanel.add(blotterTitle);

    // === BLOTTER INFO PANEL (Blotter No. and Date Filed) ===
    JPanel infoPanel = new JPanel();
    infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
    infoPanel.setBackground(Color.WHITE);
    infoPanel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(15, 25, 15, 25)));
    infoPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    infoPanel.setMaximumSize(new Dimension(680, 80));

    JPanel infoRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    infoRow1.setBackground(Color.WHITE);
    JLabel blotterNoLabel = new JLabel("Blotter No.: ");
    blotterNoLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
    JLabel blotterNoValue = new JLabel("#" + blotterNum);
    blotterNoValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    infoRow1.add(blotterNoLabel);
    infoRow1.add(blotterNoValue);

    JPanel infoRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    infoRow2.setBackground(Color.WHITE);
    JLabel dateFiledLabel = new JLabel("Date Filed: ");
    dateFiledLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    JLabel dateFiledValue = new JLabel(today);
    dateFiledValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    infoRow2.add(dateFiledLabel);
    infoRow2.add(dateFiledValue);

    infoPanel.add(infoRow1);
    infoPanel.add(Box.createVerticalStrut(5));
    infoPanel.add(infoRow2);

    // === COMPLAINANT / RESPONDENT PANEL ===
    JPanel partiesPanel = new JPanel();
    partiesPanel.setLayout(new BoxLayout(partiesPanel, BoxLayout.Y_AXIS));
    partiesPanel.setBackground(Color.WHITE);
    partiesPanel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(15, 25, 15, 25)));
    partiesPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    partiesPanel.setMaximumSize(new Dimension(680, 100));

    JLabel complainantHeader = new JLabel("COMPLAINANT");
    complainantHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
    complainantHeader.setForeground(new Color(100, 100, 100));
    complainantHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    JLabel complainantName = new JLabel(complainant);
    complainantName.setFont(new Font("Segoe UI", Font.BOLD, 13));
    complainantName.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    JLabel complainantAddress = new JLabel(address);
    complainantAddress.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    complainantAddress.setForeground(new Color(100, 100, 100));
    complainantAddress.setAlignmentX(Component.LEFT_ALIGNMENT);

    JLabel respondentHeader = new JLabel("RESPONDENT");
    respondentHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
    respondentHeader.setForeground(new Color(100, 100, 100));
    respondentHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    JLabel respondentName = new JLabel(respondent);
    respondentName.setFont(new Font("Segoe UI", Font.BOLD, 13));
    respondentName.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    JLabel respondentAddress = new JLabel("Purok 5, Barangay Mabini");
    respondentAddress.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    respondentAddress.setForeground(new Color(100, 100, 100));
    respondentAddress.setAlignmentX(Component.LEFT_ALIGNMENT);

    JPanel partiesGrid = new JPanel(new GridLayout(1, 2, 30, 0));
    partiesGrid.setBackground(Color.WHITE);
    
    JPanel leftParty = new JPanel();
    leftParty.setLayout(new BoxLayout(leftParty, BoxLayout.Y_AXIS));
    leftParty.setBackground(Color.WHITE);
    leftParty.add(complainantHeader);
    leftParty.add(Box.createVerticalStrut(3));
    leftParty.add(complainantName);
    leftParty.add(Box.createVerticalStrut(2));
    leftParty.add(complainantAddress);
    
    JPanel rightParty = new JPanel();
    rightParty.setLayout(new BoxLayout(rightParty, BoxLayout.Y_AXIS));
    rightParty.setBackground(Color.WHITE);
    rightParty.add(respondentHeader);
    rightParty.add(Box.createVerticalStrut(3));
    rightParty.add(respondentName);
    rightParty.add(Box.createVerticalStrut(2));
    rightParty.add(respondentAddress);
    
    partiesGrid.add(leftParty);
    partiesGrid.add(rightParty);
    
    partiesPanel.add(partiesGrid);

    // === INCIDENT DETAILS PANEL ===
    JPanel incidentPanel = new JPanel();
    incidentPanel.setLayout(new BoxLayout(incidentPanel, BoxLayout.Y_AXIS));
    incidentPanel.setBackground(Color.WHITE);
    incidentPanel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(15, 25, 15, 25)));
    incidentPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    incidentPanel.setMaximumSize(new Dimension(680, 200));

    JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    dateRow.setBackground(Color.WHITE);
    JLabel dateIncidentLabel = new JLabel("DATE OF INCIDENT: ");
    dateIncidentLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
    JLabel dateIncidentValue = new JLabel(date);
    dateIncidentValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    dateRow.add(dateIncidentLabel);
    dateRow.add(dateIncidentValue);

    JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    typeRow.setBackground(Color.WHITE);
    JLabel typeLabel = new JLabel("TYPE OF COMPLAINT: ");
    typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
    JLabel typeValue = new JLabel(complaintType);
    typeValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    typeRow.add(typeLabel);
    typeRow.add(typeValue);

    JLabel narrativeLabel = new JLabel("INCIDENT NARRATIVE");
    narrativeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
    narrativeLabel.setForeground(new Color(100, 100, 100));
    narrativeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

    JTextArea narrativeArea = new JTextArea(description);
    narrativeArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    narrativeArea.setForeground(new Color(60, 60, 60));
    narrativeArea.setBackground(new Color(248, 249, 250));
    narrativeArea.setEditable(false);
    narrativeArea.setLineWrap(true);
    narrativeArea.setWrapStyleWord(true);
    narrativeArea.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 6),
        new EmptyBorder(10, 12, 10, 12)));
    narrativeArea.setAlignmentX(Component.LEFT_ALIGNMENT);

    JScrollPane narrativeScroll = new JScrollPane(narrativeArea);
    narrativeScroll.setBorder(null);
    narrativeScroll.setOpaque(false);
    narrativeScroll.getViewport().setOpaque(false);
    narrativeScroll.setPreferredSize(new Dimension(630, 80));
    narrativeScroll.setMaximumSize(new Dimension(680, 80));
    narrativeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

    incidentPanel.add(dateRow);
    incidentPanel.add(Box.createVerticalStrut(5));
    incidentPanel.add(typeRow);
    incidentPanel.add(Box.createVerticalStrut(15));
    incidentPanel.add(narrativeLabel);
    incidentPanel.add(Box.createVerticalStrut(5));
    incidentPanel.add(narrativeScroll);

    // === STATUS PANEL ===
    JPanel statusPanel = new JPanel();
    statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
    statusPanel.setBackground(Color.WHITE);
    statusPanel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(12, 25, 12, 25)));
    statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    statusPanel.setMaximumSize(new Dimension(680, 55));

    JPanel caseStatusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    caseStatusRow.setBackground(Color.WHITE);
    JLabel caseStatusLabel = new JLabel("Case Status: ");
    caseStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
    
    boolean isPending = "pending".equalsIgnoreCase(status);
    String displayStatus = isPending ? "Pending — Awaiting hearing" : "Resolved";
    
    JLabel caseStatusValue = new JLabel(displayStatus);
    caseStatusValue.setFont(new Font("Segoe UI", Font.BOLD, 12));
    caseStatusValue.setForeground(isPending ? PENDING_FG : RESOLVED_FG);
    
    caseStatusRow.add(caseStatusLabel);
    caseStatusRow.add(caseStatusValue);
    statusPanel.add(caseStatusRow);

    // === SIGNATURE PANEL ===
    JPanel signaturePanel = new JPanel();
    signaturePanel.setLayout(new BoxLayout(signaturePanel, BoxLayout.Y_AXIS));
    signaturePanel.setBackground(Color.WHITE);
    signaturePanel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(15, 25, 15, 25)));
    signaturePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    signaturePanel.setMaximumSize(new Dimension(680, 100));

    JPanel recordedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    recordedRow.setBackground(Color.WHITE);
    JLabel recordedLabel = new JLabel("Recorded by: ");
    recordedLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
    JLabel recordedValue = new JLabel("Sec. Maria Santos");
    recordedValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    recordedRow.add(recordedLabel);
    recordedRow.add(recordedValue);

    JPanel signatureGrid = new JPanel(new GridLayout(1, 2, 30, 0));
    signatureGrid.setBackground(Color.WHITE);
    
    JPanel leftSig = new JPanel();
    leftSig.setLayout(new BoxLayout(leftSig, BoxLayout.Y_AXIS));
    leftSig.setBackground(Color.WHITE);
    JLabel complainantSigLabel = new JLabel("Complainant's Signature");
    complainantSigLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
    JLabel complainantSigLine = new JLabel("_________________________");
    complainantSigLine.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    JLabel complainantSigName = new JLabel(complainant);
    complainantSigName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    complainantSigName.setForeground(new Color(100, 100, 100));
    leftSig.add(complainantSigLabel);
    leftSig.add(Box.createVerticalStrut(8));
    leftSig.add(complainantSigLine);
    leftSig.add(Box.createVerticalStrut(2));
    leftSig.add(complainantSigName);
    
    JPanel rightSig = new JPanel();
    rightSig.setLayout(new BoxLayout(rightSig, BoxLayout.Y_AXIS));
    rightSig.setBackground(Color.WHITE);
    JLabel captainSigLabel = new JLabel("Barangay Captain");
    captainSigLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
    JLabel captainSigLine = new JLabel("_________________________");
    captainSigLine.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    JLabel captainSigName = new JLabel("Sgd");
    captainSigName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    captainSigName.setForeground(new Color(100, 100, 100));
    rightSig.add(captainSigLabel);
    rightSig.add(Box.createVerticalStrut(8));
    rightSig.add(captainSigLine);
    rightSig.add(Box.createVerticalStrut(2));
    rightSig.add(captainSigName);
    
    signatureGrid.add(leftSig);
    signatureGrid.add(rightSig);
    
    signaturePanel.add(recordedRow);
    signaturePanel.add(Box.createVerticalStrut(15));
    signaturePanel.add(signatureGrid);

    // === BUTTON PANEL ===
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
    buttonPanel.setBackground(new Color(240, 242, 245));
    buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
    buttonPanel.setMaximumSize(new Dimension(680, 50));

    JButton printBtn = createStyledButton("Print Report", BLUE, BLUE_HOVER, WHITE);
    printBtn.setPreferredSize(new Dimension(130, 40));
    printBtn.addActionListener(e -> {
        // Create a printable panel with all content
        JPanel printPanel = new JPanel();
        printPanel.setLayout(new BoxLayout(printPanel, BoxLayout.Y_AXIS));
        printPanel.setBackground(Color.WHITE);
        printPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        
        // Recreate components for printing (since components can only have one parent)
        JPanel printHeader = createPrintHeaderPanel();
        JPanel printInfo = createPrintInfoPanel(blotterNum);
        JPanel printParties = createPrintPartiesPanel(complainant, respondent, address);
        JPanel printIncident = createPrintIncidentPanel(date, complaintType, description);
        JPanel printStatus = createPrintStatusPanel(status);
        JPanel printSignature = createPrintSignaturePanel(complainant);
        
        printPanel.add(printHeader);
        printPanel.add(Box.createVerticalStrut(12));
        printPanel.add(printInfo);
        printPanel.add(Box.createVerticalStrut(12));
        printPanel.add(printParties);
        printPanel.add(Box.createVerticalStrut(12));
        printPanel.add(printIncident);
        printPanel.add(Box.createVerticalStrut(12));
        printPanel.add(printStatus);
        printPanel.add(Box.createVerticalStrut(12));
        printPanel.add(printSignature);
        
        // Create a PrinterJob
        java.awt.print.PrinterJob job = java.awt.print.PrinterJob.getPrinterJob();
        job.setPrintable(new java.awt.print.Printable() {
            @Override
            public int print(Graphics graphics, java.awt.print.PageFormat pageFormat, int pageIndex) 
                    throws java.awt.print.PrinterException {
                if (pageIndex > 0) {
                    return NO_SUCH_PAGE;
                }
                
                Graphics2D g2d = (Graphics2D) graphics;
                g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
                
                // Scale to fit page
                double scaleX = pageFormat.getImageableWidth() / printPanel.getPreferredSize().getWidth();
                double scaleY = pageFormat.getImageableHeight() / printPanel.getPreferredSize().getHeight();
                double scale = Math.min(scaleX, scaleY);
                g2d.scale(scale, scale);
                
                printPanel.printAll(g2d);
                return PAGE_EXISTS;
            }
        });
        
        if (job.printDialog()) {
            try {
                job.print();
            } catch (java.awt.print.PrinterException ex) {
                JOptionPane.showMessageDialog(pf,
                    "Print failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    });

    JButton closeBtn = createStyledButton("Close", 
        new Color(108, 117, 125), new Color(90, 98, 104), WHITE);
    closeBtn.setPreferredSize(new Dimension(100, 40));
    closeBtn.addActionListener(e -> pf.dispose());

    buttonPanel.add(printBtn);
    buttonPanel.add(closeBtn);

    // === ASSEMBLE SCROLL PANE ===
    JPanel mainContainer = new JPanel();
    mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
    mainContainer.setBackground(new Color(240, 242, 245));
    
    mainContainer.add(headerPanel);
    mainContainer.add(Box.createVerticalStrut(12));
    mainContainer.add(infoPanel);
    mainContainer.add(Box.createVerticalStrut(12));
    mainContainer.add(partiesPanel);
    mainContainer.add(Box.createVerticalStrut(12));
    mainContainer.add(incidentPanel);
    mainContainer.add(Box.createVerticalStrut(12));
    mainContainer.add(statusPanel);
    mainContainer.add(Box.createVerticalStrut(12));
    mainContainer.add(signaturePanel);
    mainContainer.add(Box.createVerticalStrut(20));
    mainContainer.add(buttonPanel);

    JScrollPane scrollPane = new JScrollPane(mainContainer);
    scrollPane.setBorder(null);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    scrollPane.setBackground(new Color(240, 242, 245));
    scrollPane.getViewport().setBackground(new Color(240, 242, 245));

    pf.add(scrollPane);
    pf.setVisible(true);
}

// Helper methods to create fresh components for printing
private JPanel createPrintHeaderPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(20, 25, 20, 25)));
    
    JLabel republicLabel = new JLabel("REPUBLIC OF THE PHILIPPINES");
    republicLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
    republicLabel.setForeground(new Color(45, 118, 200));
    republicLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    
    JLabel barangayLabel = new JLabel("Barangay Mabini, Central Visayas");
    barangayLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    barangayLabel.setForeground(new Color(80, 80, 80));
    barangayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
    
    JLabel blotterTitle = new JLabel("BARANGAY BLOTTER REPORT");
    blotterTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
    blotterTitle.setForeground(new Color(25, 60, 110));
    blotterTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
    
    panel.add(republicLabel);
    panel.add(Box.createVerticalStrut(2));
    panel.add(barangayLabel);
    panel.add(Box.createVerticalStrut(15));
    panel.add(blotterTitle);
    
    return panel;
}

private JPanel createPrintInfoPanel(String blotterNum) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(15, 25, 15, 25)));
    
    JPanel infoRow1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    infoRow1.setBackground(Color.WHITE);
    JLabel blotterNoLabel = new JLabel("Blotter No.: ");
    blotterNoLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
    JLabel blotterNoValue = new JLabel("#" + blotterNum);
    blotterNoValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    infoRow1.add(blotterNoLabel);
    infoRow1.add(blotterNoValue);
    
    JPanel infoRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    infoRow2.setBackground(Color.WHITE);
    JLabel dateFiledLabel = new JLabel("Date Filed: ");
    dateFiledLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
    String today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
    JLabel dateFiledValue = new JLabel(today);
    dateFiledValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    infoRow2.add(dateFiledLabel);
    infoRow2.add(dateFiledValue);
    
    panel.add(infoRow1);
    panel.add(Box.createVerticalStrut(5));
    panel.add(infoRow2);
    
    return panel;
}

private JPanel createPrintPartiesPanel(String complainant, String respondent, String address) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(15, 25, 15, 25)));
    
    JLabel complainantHeader = new JLabel("COMPLAINANT");
    complainantHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
    complainantHeader.setForeground(new Color(100, 100, 100));
    
    JLabel complainantName = new JLabel(complainant);
    complainantName.setFont(new Font("Segoe UI", Font.BOLD, 13));
    
    JLabel complainantAddress = new JLabel(address);
    complainantAddress.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    complainantAddress.setForeground(new Color(100, 100, 100));
    
    JLabel respondentHeader = new JLabel("RESPONDENT");
    respondentHeader.setFont(new Font("Segoe UI", Font.BOLD, 11));
    respondentHeader.setForeground(new Color(100, 100, 100));
    
    JLabel respondentName = new JLabel(respondent);
    respondentName.setFont(new Font("Segoe UI", Font.BOLD, 13));
    
    JLabel respondentAddress = new JLabel("Purok 5, Barangay Mabini");
    respondentAddress.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    respondentAddress.setForeground(new Color(100, 100, 100));
    
    JPanel grid = new JPanel(new GridLayout(1, 2, 30, 0));
    grid.setBackground(Color.WHITE);
    
    JPanel left = new JPanel();
    left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
    left.setBackground(Color.WHITE);
    left.add(complainantHeader);
    left.add(Box.createVerticalStrut(3));
    left.add(complainantName);
    left.add(Box.createVerticalStrut(2));
    left.add(complainantAddress);
    
    JPanel right = new JPanel();
    right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
    right.setBackground(Color.WHITE);
    right.add(respondentHeader);
    right.add(Box.createVerticalStrut(3));
    right.add(respondentName);
    right.add(Box.createVerticalStrut(2));
    right.add(respondentAddress);
    
    grid.add(left);
    grid.add(right);
    
    panel.add(grid);
    return panel;
}

private JPanel createPrintIncidentPanel(String date, String complaintType, String description) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(15, 25, 15, 25)));
    
    JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    dateRow.setBackground(Color.WHITE);
    JLabel dateLabel = new JLabel("DATE OF INCIDENT: ");
    dateLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
    JLabel dateValue = new JLabel(date);
    dateValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    dateRow.add(dateLabel);
    dateRow.add(dateValue);
    
    JPanel typeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    typeRow.setBackground(Color.WHITE);
    JLabel typeLabel = new JLabel("TYPE OF COMPLAINT: ");
    typeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
    JLabel typeValue = new JLabel(complaintType);
    typeValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    typeRow.add(typeLabel);
    typeRow.add(typeValue);
    
    JLabel narrativeLabel = new JLabel("INCIDENT NARRATIVE");
    narrativeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
    narrativeLabel.setForeground(new Color(100, 100, 100));
    
    JTextArea narrativeArea = new JTextArea(description);
    narrativeArea.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    narrativeArea.setForeground(new Color(60, 60, 60));
    narrativeArea.setBackground(new Color(248, 249, 250));
    narrativeArea.setEditable(false);
    narrativeArea.setLineWrap(true);
    narrativeArea.setWrapStyleWord(true);
    narrativeArea.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 6),
        new EmptyBorder(10, 12, 10, 12)));
    
    JScrollPane scroll = new JScrollPane(narrativeArea);
    scroll.setBorder(null);
    scroll.setPreferredSize(new Dimension(630, 80));
    
    panel.add(dateRow);
    panel.add(Box.createVerticalStrut(5));
    panel.add(typeRow);
    panel.add(Box.createVerticalStrut(15));
    panel.add(narrativeLabel);
    panel.add(Box.createVerticalStrut(5));
    panel.add(scroll);
    
    return panel;
}

private JPanel createPrintStatusPanel(String status) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(12, 25, 12, 25)));
    
    JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    row.setBackground(Color.WHITE);
    JLabel label = new JLabel("Case Status: ");
    label.setFont(new Font("Segoe UI", Font.BOLD, 12));
    
    boolean isPending = "pending".equalsIgnoreCase(status);
    String displayStatus = isPending ? "Pending — Awaiting hearing" : "Resolved";
    
    JLabel value = new JLabel(displayStatus);
    value.setFont(new Font("Segoe UI", Font.BOLD, 12));
    value.setForeground(isPending ? PENDING_FG : RESOLVED_FG);
    
    row.add(label);
    row.add(value);
    panel.add(row);
    
    return panel;
}

private JPanel createPrintSignaturePanel(String complainant) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        new RoundedBorder(new Color(200, 200, 200), 1, 8),
        new EmptyBorder(15, 25, 15, 25)));
    
    JPanel recordedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    recordedRow.setBackground(Color.WHITE);
    JLabel recordedLabel = new JLabel("Recorded by: ");
    recordedLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
    JLabel recordedValue = new JLabel("Sec. Maria Santos");
    recordedValue.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    recordedRow.add(recordedLabel);
    recordedRow.add(recordedValue);
    
    JPanel grid = new JPanel(new GridLayout(1, 2, 30, 0));
    grid.setBackground(Color.WHITE);
    
    JPanel left = new JPanel();
    left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
    left.setBackground(Color.WHITE);
    JLabel leftLabel = new JLabel("Complainant's Signature");
    leftLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
    JLabel leftLine = new JLabel("_________________________");
    leftLine.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    JLabel leftName = new JLabel(complainant);
    leftName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    leftName.setForeground(new Color(100, 100, 100));
    left.add(leftLabel);
    left.add(Box.createVerticalStrut(8));
    left.add(leftLine);
    left.add(Box.createVerticalStrut(2));
    left.add(leftName);
    
    JPanel right = new JPanel();
    right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
    right.setBackground(Color.WHITE);
    JLabel rightLabel = new JLabel("Barangay Captain");
    rightLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
    JLabel rightLine = new JLabel("_________________________");
    rightLine.setFont(new Font("Segoe UI", Font.PLAIN, 12));
    JLabel rightName = new JLabel("Sgd");
    rightName.setFont(new Font("Segoe UI", Font.PLAIN, 11));
    rightName.setForeground(new Color(100, 100, 100));
    right.add(rightLabel);
    right.add(Box.createVerticalStrut(8));
    right.add(rightLine);
    right.add(Box.createVerticalStrut(2));
    right.add(rightName);
    
    grid.add(left);
    grid.add(right);
    
    panel.add(recordedRow);
    panel.add(Box.createVerticalStrut(15));
    panel.add(grid);
    
    return panel;
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

    // ── Add blotter dialog (launches the full addblotter UI) ───────────────

    private void showAddDialog() {
        JDialog dialog = new JDialog(this, "Add New Blotter", true);
        dialog.setSize(820, 720);
        dialog.setMinimumSize(new Dimension(680, 600));
        dialog.setLocationRelativeTo(this);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        AddBlotterPanel addPanel = new AddBlotterPanel(dialog);
        
        dialog.setContentPane(addPanel);
        dialog.setVisible(true);
    }

    // ── Inner class: AddBlotterPanel ───────────────────────────────────────

    private class AddBlotterPanel extends JPanel {

        // Color palette
        private final Color HEADER_BG      = new Color(0x1B3A5C);
        private final Color BLOTTER_BAR_BG = new Color(0x1B3A5C);
        private final Color PAGE_BG        = new Color(0xEAF1FB);
        private final Color CARD_BG        = Color.WHITE;
        private final Color SECTION_LABEL  = new Color(0x1B3A5C);
        private final Color FIELD_BORDER   = new Color(0xC8D8EC);
        private final Color FIELD_BG       = Color.WHITE;
        private final Color RESPONDENT_BG  = new Color(0xFFF0EE);
        private final Color RESPONDENT_BDR = new Color(0xF5C0BB);
        private final Color BTN_SAVE_BG    = new Color(0x1B3A5C);
        private final Color BTN_SAVE_FG    = Color.WHITE;
        private final Color BTN_CANCEL_BG  = Color.WHITE;
        private final Color BTN_CANCEL_FG  = new Color(0x1B3A5C);
        private final Color LABEL_FG       = new Color(0x2C4A6E);
        private final Color TEXT_FG        = new Color(0x1A1A2E);
        private final Color HEADER_SUBTITLE= new Color(0xA8C4E0);

        // Fonts
        private final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 18);
        private final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 11);
        private final Font FONT_SECTION  = new Font("Segoe UI", Font.BOLD, 11);
        private final Font FONT_LABEL    = new Font("Segoe UI", Font.PLAIN, 11);
        private final Font FONT_INPUT    = new Font("Segoe UI", Font.PLAIN, 13);
        private final Font FONT_BLOTTER  = new Font("Segoe UI", Font.BOLD, 13);
        private final Font FONT_BTN      = new Font("Segoe UI", Font.BOLD, 12);

        // Form fields
        private JTextField tfComplainantName;
        private JTextField tfRespondentName;
        private JTextField tfComplainantAddress;
        private JButton btnDatePicker;
        private JComboBox<String> cbComplaintType;
        private JTextArea taDescription;
        
        private java.util.Date selectedDate = new java.util.Date(); // Default to today
        
        private final JDialog parentDialog;

        AddBlotterPanel(JDialog parent) {
            this.parentDialog = parent;
            setLayout(new BorderLayout());
            setBackground(PAGE_BG);
            
            add(buildHeader(), BorderLayout.NORTH);
            add(buildScrollBody(), BorderLayout.CENTER);
        }

        private JPanel buildHeader() {
            JPanel header = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    g.setColor(HEADER_BG);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            header.setOpaque(false);
            header.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

            JButton backBtn = new JButton("← Cancel");
            backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            backBtn.setForeground(new Color(0xA8C4E0));
            backBtn.setContentAreaFilled(false);
            backBtn.setBorderPainted(false);
            backBtn.setFocusPainted(false);
            backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            backBtn.addActionListener(e -> parentDialog.dispose());

            JPanel titleBlock = new JPanel();
            titleBlock.setOpaque(false);
            titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

            JLabel subLabel = new JLabel("NEW ENTRY");
            subLabel.setFont(FONT_SUBTITLE);
            subLabel.setForeground(HEADER_SUBTITLE);

            JLabel titleLabel = new JLabel("Add New Blotter");
            titleLabel.setFont(FONT_TITLE);
            titleLabel.setForeground(Color.WHITE);

            titleBlock.add(subLabel);
            titleBlock.add(titleLabel);

            JLabel menuDots = new JLabel("⋯");
            menuDots.setFont(new Font("Segoe UI", Font.BOLD, 20));
            menuDots.setForeground(new Color(0xA8C4E0));
            menuDots.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            left.setOpaque(false);
            left.add(backBtn);
            left.add(titleBlock);

            header.add(left, BorderLayout.WEST);
            header.add(menuDots, BorderLayout.EAST);

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
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 10, 10));
                    g2.dispose();
                }
            };
            bar.setOpaque(false);
            bar.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
            bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

            JLabel lKey = new JLabel("Blotter Number");
            lKey.setFont(FONT_BLOTTER);
            lKey.setForeground(new Color(0xA8C4E0));

            // Generate next blotter number
            String nextNumber = "#" + String.format("%04d", blotterData.size() + 1);
            JLabel lVal = new JLabel(nextNumber + " (auto-generated)");
            lVal.setFont(FONT_BLOTTER);
            lVal.setForeground(Color.WHITE);

            bar.add(lKey, BorderLayout.WEST);
            bar.add(lVal, BorderLayout.EAST);
            return bar;
        }

        private JPanel buildPartiesCard() {
            JPanel card = createCard();

            JLabel sectionLabel = sectionHeader("PARTIES INVOLVED");

            JPanel row1 = new JPanel(new GridLayout(1, 2, 14, 0));
            row1.setOpaque(false);
            tfComplainantName = styledField("Enter complainant's full name", false);
            tfRespondentName  = styledField("Enter respondent's full name", true);
            row1.add(labeledField("Complainant's Full Name", tfComplainantName));
            row1.add(labeledField("Respondent's Full Name", tfRespondentName));

            JPanel row2 = new JPanel(new GridLayout(1, 2, 14, 0));
            row2.setOpaque(false);
            tfComplainantAddress = styledField("Enter complainant's address", false);
            
            // Date picker panel
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
            
            btnDatePicker = new JButton(new SimpleDateFormat("MM/dd/yyyy").format(selectedDate));
            btnDatePicker.setFont(FONT_INPUT);
            btnDatePicker.setForeground(TEXT_FG);
            btnDatePicker.setBackground(FIELD_BG);
            btnDatePicker.setHorizontalAlignment(SwingConstants.LEFT);
            btnDatePicker.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorderLocal(FIELD_BORDER, 8),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
            btnDatePicker.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btnDatePicker.addActionListener(e -> showDatePicker());
            
            dateInputPanel.add(btnDatePicker, BorderLayout.CENTER);
            
            datePanel.add(dateLabel);
            datePanel.add(Box.createVerticalStrut(4));
            datePanel.add(dateInputPanel);
            
            row2.add(labeledField("Complainant's Address", tfComplainantAddress));
            row2.add(datePanel);

            card.add(sectionLabel);
            card.add(Box.createVerticalStrut(12));
            card.add(row1);
            card.add(Box.createVerticalStrut(12));
            card.add(row2);

            return card;
        }
        
        private void showDatePicker() {
            // Create date picker dialog
            JDialog dateDialog = new JDialog(parentDialog, "Select Date", true);
            dateDialog.setSize(350, 300);
            dateDialog.setLocationRelativeTo(parentDialog);
            dateDialog.setLayout(new BorderLayout());
            
            // Create calendar panel
            JPanel calendarPanel = new JPanel(new BorderLayout());
            calendarPanel.setBackground(Color.WHITE);
            calendarPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Month/Year selector
            JPanel monthYearPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
            monthYearPanel.setBackground(Color.WHITE);
            
            Calendar cal = Calendar.getInstance();
            cal.setTime(selectedDate);
            
            String[] months = {"January", "February", "March", "April", "May", "June", 
                              "July", "August", "September", "October", "November", "December"};
            JComboBox<String> monthCombo = new JComboBox<>(months);
            monthCombo.setSelectedIndex(cal.get(Calendar.MONTH));
            monthCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            JSpinner yearSpinner = new JSpinner(new SpinnerNumberModel(
                cal.get(Calendar.YEAR), 1900, 2100, 1));
            yearSpinner.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            
            monthYearPanel.add(monthCombo);
            monthYearPanel.add(yearSpinner);
            
            // Day buttons panel
            JPanel daysPanel = new JPanel(new GridLayout(0, 7, 2, 2));
            daysPanel.setBackground(Color.WHITE);
            
            String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            for (String day : dayNames) {
                JLabel label = new JLabel(day, SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 10));
                label.setForeground(TEXT_SEC);
                daysPanel.add(label);
            }
            
            // Create day buttons
            JButton[] dayButtons = new JButton[42];
            for (int i = 0; i < 42; i++) {
                dayButtons[i] = new JButton();
                dayButtons[i].setFont(new Font("Segoe UI", Font.PLAIN, 11));
                dayButtons[i].setBackground(Color.WHITE);
                dayButtons[i].setFocusPainted(false);
                dayButtons[i].setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                dayButtons[i].addActionListener(e -> {
                    JButton source = (JButton) e.getSource();
                    int day = Integer.parseInt(source.getText());
                    
                    Calendar selectedCal = Calendar.getInstance();
                    selectedCal.set(Calendar.YEAR, (Integer) yearSpinner.getValue());
                    selectedCal.set(Calendar.MONTH, monthCombo.getSelectedIndex());
                    selectedCal.set(Calendar.DAY_OF_MONTH, day);
                    
                    selectedDate = selectedCal.getTime();
                    btnDatePicker.setText(new SimpleDateFormat("MM/dd/yyyy").format(selectedDate));
                    dateDialog.dispose();
                });
                daysPanel.add(dayButtons[i]);
            }
            
            // Update day buttons
            Runnable updateDays = () -> {
                Calendar tempCal = Calendar.getInstance();
                tempCal.set(Calendar.YEAR, (Integer) yearSpinner.getValue());
                tempCal.set(Calendar.MONTH, monthCombo.getSelectedIndex());
                tempCal.set(Calendar.DAY_OF_MONTH, 1);
                
                int firstDayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK) - 1;
                int daysInMonth = tempCal.getActualMaximum(Calendar.DAY_OF_MONTH);
                
                // Clear all buttons
                for (int i = 0; i < 42; i++) {
                    dayButtons[i].setText("");
                    dayButtons[i].setEnabled(false);
                }
                
                // Set day numbers
                for (int i = 1; i <= daysInMonth; i++) {
                    int index = firstDayOfWeek + i - 1;
                    dayButtons[index].setText(String.valueOf(i));
                    dayButtons[index].setEnabled(true);
                    
                    // Highlight selected date
                    Calendar checkCal = Calendar.getInstance();
                    checkCal.setTime(selectedDate);
                    if (checkCal.get(Calendar.YEAR) == (Integer) yearSpinner.getValue() &&
                        checkCal.get(Calendar.MONTH) == monthCombo.getSelectedIndex() &&
                        checkCal.get(Calendar.DAY_OF_MONTH) == i) {
                        dayButtons[index].setBackground(BLUE_LIGHT);
                        dayButtons[index].setForeground(BLUE);
                    } else {
                        dayButtons[index].setBackground(Color.WHITE);
                        dayButtons[index].setForeground(TEXT_PRI);
                    }
                }
            };
            
            monthCombo.addActionListener(e -> updateDays.run());
            yearSpinner.addChangeListener(e -> updateDays.run());
            
            updateDays.run();
            
            calendarPanel.add(monthYearPanel, BorderLayout.NORTH);
            calendarPanel.add(daysPanel, BorderLayout.CENTER);
            
            // Buttons panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(Color.WHITE);
            
            JButton todayBtn = new JButton("Today");
            todayBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            todayBtn.addActionListener(e -> {
                selectedDate = new java.util.Date();
                btnDatePicker.setText(new SimpleDateFormat("MM/dd/yyyy").format(selectedDate));
                dateDialog.dispose();
            });
            
            JButton cancelBtn = new JButton("Cancel");
            cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            cancelBtn.addActionListener(e -> dateDialog.dispose());
            
            buttonPanel.add(todayBtn);
            buttonPanel.add(cancelBtn);
            
            dateDialog.add(calendarPanel, BorderLayout.CENTER);
            dateDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            dateDialog.setVisible(true);
        }

        private JPanel buildIncidentDetailsCard() {
            JPanel card = createCard();

            JLabel sectionLabel = sectionHeader("INCIDENT DETAILS");

            String[] complaintTypes = {
                "Noise Disturbance", 
                "Verbal Dispute", 
                "Property Damage", 
                "Theft", 
                "Physical Altercation", 
                "Threats/Harassment",
                "Other"
            };
            cbComplaintType = new JComboBox<>(complaintTypes);
            cbComplaintType.setFont(FONT_INPUT);
            cbComplaintType.setForeground(TEXT_FG);
            cbComplaintType.setBackground(FIELD_BG);
            cbComplaintType.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorderLocal(FIELD_BORDER, 8),
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
                new RoundBorderLocal(FIELD_BORDER, 8),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
            
            JScrollPane descriptionScroll = new JScrollPane(taDescription);
            descriptionScroll.setBorder(null);
            descriptionScroll.setOpaque(false);
            descriptionScroll.getViewport().setOpaque(false);
            descriptionScroll.setPreferredSize(new Dimension(200, 110));
            descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

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

            JButton cancelBtn = roundButton("Cancel", BTN_CANCEL_BG, BTN_CANCEL_FG, true);
            JButton saveBtn   = roundButton("Save Blotter Entry", BTN_SAVE_BG, BTN_SAVE_FG, false);

            cancelBtn.addActionListener(e -> parentDialog.dispose());
            saveBtn.addActionListener(e -> saveBlotterToDB());

            row.add(cancelBtn);
            row.add(saveBtn);
            return row;
        }

        private void saveBlotterToDB() {
            String complainant = tfComplainantName.getText().trim();
            String respondent = tfRespondentName.getText().trim();
            String address = tfComplainantAddress.getText().trim();
            String complaintType = (String) cbComplaintType.getSelectedItem();
            String description = taDescription.getText().trim();

            // Validation
            if (complainant.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter complainant's name.", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                tfComplainantName.requestFocus();
                return;
            }
            
            if (respondent.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter respondent's name.", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                tfRespondentName.requestFocus();
                return;
            }
            
            if (address.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter complainant's address.", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                tfComplainantAddress.requestFocus();
                return;
            }
            
            if (description.isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "Please enter incident description.", 
                    "Validation Error", JOptionPane.ERROR_MESSAGE);
                taDescription.requestFocus();
                return;
            }

            setAllFieldsEnabled(false);

            new SwingWorker<Boolean, Void>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    try (Connection conn = getConnection()) {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        String formattedDate = sdf.format(selectedDate);

                        String insert = "INSERT INTO blotter (complainant, Respondent, Cmplnt_address, " +
                                      "date, complt_type, description, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                            pstmt.setString(1, complainant);
                            pstmt.setString(2, respondent);
                            pstmt.setString(3, address);
                            pstmt.setDate(4, java.sql.Date.valueOf(formattedDate));
                            pstmt.setString(5, complaintType);
                            pstmt.setString(6, description);
                            pstmt.setString(7, "pending");
                            pstmt.executeUpdate();
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
                            loadBlotterData();
                            refreshTableAndStats();
                            parentDialog.dispose();
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
            tfComplainantName.setEnabled(enabled);
            tfRespondentName.setEnabled(enabled);
            tfComplainantAddress.setEnabled(enabled);
            btnDatePicker.setEnabled(enabled);
            cbComplaintType.setEnabled(enabled);
            taDescription.setEnabled(enabled);
        }

        // Helper methods
        private JPanel createCard() {
            JPanel card = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0, 0, 0, 18));
                    g2.fill(new RoundRectangle2D.Double(3, 5, getWidth()-6, getHeight()-6, 14, 14));
                    g2.setColor(CARD_BG);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth()-4, getHeight()-4, 12, 12));
                    g2.dispose();
                }
            };
            card.setOpaque(false);
            card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
            card.setBorder(BorderFactory.createEmptyBorder(18, 20, 20, 20));
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
                new RoundBorderLocal(isRespondent ? RESPONDENT_BDR : FIELD_BORDER, 8),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
            tf.setPreferredSize(new Dimension(200, 38));
            tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
            
            // Add placeholder behavior
            tf.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (tf.getText().equals(placeholder)) {
                        tf.setText("");
                        tf.setForeground(TEXT_FG);
                    }
                }
                
                @Override
                public void focusLost(FocusEvent e) {
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

        private JButton roundButton(String text, Color bg, Color fg, boolean outlined) {
            JButton btn = new JButton(text) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (getModel().isPressed())
                        g2.setColor(bg.darker());
                    else if (getModel().isRollover())
                        g2.setColor(bg.brighter());
                    else
                        g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 8, 8));
                    if (outlined) {
                        g2.setColor(new Color(0xC0CFDF));
                        g2.draw(new RoundRectangle2D.Double(0.5, 0.5, getWidth()-1, getHeight()-1, 8, 8));
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
                @Override public boolean isOpaque() { return false; }
            };
            btn.setFont(FONT_BTN);
            btn.setForeground(fg);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setPreferredSize(new Dimension(text.length() > 10 ? 170 : 100, 40));
            return btn;
        }

        class RoundBorderLocal extends AbstractBorder {
            private final Color color;
            private final int radius;
            RoundBorderLocal(Color c, int r) { this.color = c; this.radius = r; }

            @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.draw(new RoundRectangle2D.Double(x+0.5, y+0.5, w-1, h-1, radius, radius));
                g2.dispose();
            }
            @Override public Insets getBorderInsets(Component c) {
                return new Insets(radius/2, radius/2, radius/2, radius/2);
            }
        }
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

    private static class RoundedBorder extends AbstractBorder {
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
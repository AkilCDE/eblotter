package main.eblotterr;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

/**
 * Frame for viewing the complete blotter history with filtering, time sections, and export.
 * Extracted from dashboard.java to keep the dashboard clean.
 */
public class HistoryFrame {

    // ── Color palette (matches dashboard) ─────────────────────────────────
    private static final Color WHITE        = Color.WHITE;
    private static final Color BLUE         = new Color(45, 118, 200);
    private static final Color BLUE_HOVER   = new Color(35, 95, 160);
    private static final Color BLUE_DARK    = new Color(25, 60, 110);
    private static final Color BLUE_LIGHT   = new Color(230, 241, 251);
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

    private final JFrame parentFrame;
    private final List<Object[]> blotterData;
    private final ViewRecordHandler viewRecordHandler;

    @FunctionalInterface
    public interface ViewRecordHandler {
        void viewRecord(int modelRow);
    }

    /**
     * @param parentFrame       the parent frame (dashboard)
     * @param blotterData       the shared blotter data list
     * @param viewRecordHandler handler for double-click "view record" action
     */
    public HistoryFrame(JFrame parentFrame, List<Object[]> blotterData,
                        ViewRecordHandler viewRecordHandler) {
        this.parentFrame = parentFrame;
        this.blotterData = blotterData;
        this.viewRecordHandler = viewRecordHandler;
    }

    /**
     * Show the history frame.
     */
    public void show() {
        JFrame historyFrame = new JFrame("📋 Blotter History — Complete Records");
        historyFrame.setSize(1100, 750);
        historyFrame.setMinimumSize(new Dimension(900, 600));
        historyFrame.setLocationRelativeTo(parentFrame);
        historyFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Main panel with gradient background
        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(240, 248, 255),
                                                     0, getHeight(), new Color(230, 241, 251));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };

        // ── Modern Header ─────────────────────────────────────────────────────
        JPanel headerPanel = buildHeader(historyFrame);

        // ── Filter Bar ────────────────────────────────────────────────────────
        // Build table + model first so we can wire filter actions to them
        String[] columns = {"ID", "Complainant", "Respondent", "Complaint Type", "Date", "Status", "Description"};
        DefaultTableModel historyModel = new DefaultTableModel(null, columns) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        // Load all data
        for (Object[] row : blotterData) {
            String statusDisplay = "pending".equalsIgnoreCase(row[4].toString()) ? "Pending" : "Resolved";
            String description = row[7] != null ? row[7].toString() : "";
            if (description.length() > 50) {
                description = description.substring(0, 47) + "...";
            }
            historyModel.addRow(new Object[]{
                "#" + row[0],
                row[1],
                row[2],
                row[6] != null ? row[6].toString() : "N/A",
                row[3],
                statusDisplay,
                description
            });
        }

        JTable historyTable = createHistoryTable(historyModel);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(historyModel);
        historyTable.setRowSorter(sorter);

        // Filter bar
        JPanel filterPanel = buildFilterBar(historyFrame, historyTable, sorter, historyModel);

        // ── Table container ───────────────────────────────────────────────────
        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(WHITE);
        scrollPane.setBackground(WHITE);

        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setBackground(WHITE);
        tableContainer.setBorder(new RoundedBorder(BORDER_CLR, 1, 14));
        tableContainer.add(scrollPane, BorderLayout.CENTER);

        // ── Bottom Panel with Record Count ────────────────────────────────────
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(8, 24, 12, 24));

        JLabel recordCount = new JLabel("Showing " + blotterData.size() + " records");
        recordCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        recordCount.setForeground(TEXT_SEC);

        // Summary counts
        long todayCount = countRecordsByDays(0);
        long weekCount  = countRecordsByDays(7);
        long monthCount = countRecordsByDays(30);

        JLabel summaryLabel = new JLabel(
            "Today: " + todayCount + "  |  Last 7 days: " + weekCount + "  |  Last 30 days: " + monthCount);
        summaryLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        summaryLabel.setForeground(TEXT_SEC);

        bottomPanel.add(recordCount, BorderLayout.WEST);
        bottomPanel.add(summaryLabel, BorderLayout.EAST);

        // Double-click to view details
        historyTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = historyTable.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = historyTable.convertRowIndexToModel(row);
                        viewRecordHandler.viewRecord(modelRow);
                        historyFrame.dispose();
                    }
                }
            }
        });

        // Update record count when filter changes
        sorter.addRowSorterListener(e -> {
            recordCount.setText("Showing " + historyTable.getRowCount() + " of " + blotterData.size() + " records");
        });

        // ── Assemble with proper layout ───────────────────────────────────────
        // Use a wrapper for center content to hold both filter + table + bottom
        JPanel centerContent = new JPanel(new BorderLayout(0, 0));
        centerContent.setOpaque(false);

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.setBorder(new EmptyBorder(0, 24, 0, 24));
        tableWrapper.add(tableContainer, BorderLayout.CENTER);

        centerContent.add(filterPanel, BorderLayout.NORTH);
        centerContent.add(tableWrapper, BorderLayout.CENTER);
        centerContent.add(bottomPanel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(centerContent, BorderLayout.CENTER);

        historyFrame.setContentPane(mainPanel);
        historyFrame.setVisible(true);
    }

    // ── Count records within N days ───────────────────────────────────────

    private long countRecordsByDays(int days) {
        LocalDate cutoff = LocalDate.now().minusDays(days);
        return blotterData.stream().filter(row -> {
            try {
                String dateStr = row[3] != null ? row[3].toString() : "";
                if (dateStr.isEmpty() || "N/A".equals(dateStr)) return false;
                LocalDate recordDate = LocalDate.parse(dateStr);
                if (days == 0) {
                    return recordDate.equals(LocalDate.now());
                }
                return !recordDate.isBefore(cutoff);
            } catch (Exception e) {
                return false;
            }
        }).count();
    }

    // ── Build header ──────────────────────────────────────────────────────

    private JPanel buildHeader(JFrame historyFrame) {
        JPanel headerPanel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BLUE_DARK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(18, 24, 18, 24));
        headerPanel.setPreferredSize(new Dimension(0, 80));

        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        headerLeft.setOpaque(false);

        // History icon in header
        JPanel historyIcon = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.setColor(WHITE);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int radius = 14;

                g2.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                g2.drawLine(centerX, centerY, centerX, centerY - 8);
                g2.drawLine(centerX, centerY, centerX + 6, centerY - 4);

                g2.dispose();
            }
        };
        historyIcon.setOpaque(false);
        historyIcon.setPreferredSize(new Dimension(48, 48));

        JPanel titleBlock = new JPanel();
        titleBlock.setOpaque(false);
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

        JLabel subLabel = new JLabel("COMPLETE ARCHIVE");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        subLabel.setForeground(TEXT_LIGHT);

        JLabel titleLabel = new JLabel("Blotter History");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(WHITE);

        titleBlock.add(subLabel);
        titleBlock.add(titleLabel);

        headerLeft.add(historyIcon);
        headerLeft.add(titleBlock);

        // Stats summary in header
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        statsPanel.setOpaque(false);

        long total = blotterData.size();
        long pending = blotterData.stream().filter(r -> "pending".equalsIgnoreCase(r[4].toString())).count();
        long resolved = blotterData.stream().filter(r -> "resolved".equalsIgnoreCase(r[4].toString())).count();

        statsPanel.add(createHeaderStat("Total", String.valueOf(total), STAT_BLUE));
        statsPanel.add(createHeaderStat("Pending", String.valueOf(pending), STAT_ORANGE));
        statsPanel.add(createHeaderStat("Resolved", String.valueOf(resolved), STAT_GREEN));

        JButton closeBtn = new JButton("Close") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isRollover()) {
                    g2.setColor(new Color(255, 255, 255, 40));
                } else {
                    g2.setColor(new Color(255, 255, 255, 20));
                }
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public boolean isOpaque() { return false; }
        };
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeBtn.setForeground(WHITE);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(80, 36));
        closeBtn.addActionListener(e -> historyFrame.dispose());

        headerPanel.add(headerLeft, BorderLayout.WEST);
        headerPanel.add(statsPanel, BorderLayout.CENTER);
        headerPanel.add(closeBtn, BorderLayout.EAST);

        return headerPanel;
    }

    // ── Build filter bar ──────────────────────────────────────────────────

    private JPanel buildFilterBar(JFrame historyFrame, JTable historyTable,
                                  TableRowSorter<DefaultTableModel> sorter,
                                  DefaultTableModel historyModel) {
        JPanel filterPanel = new JPanel(new BorderLayout(12, 0));
        filterPanel.setOpaque(false);
        filterPanel.setBorder(new EmptyBorder(12, 24, 12, 24));

        // Left side: Status filter + Time period filter
        JPanel filterLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterLeft.setOpaque(false);

        JLabel filterLabel = new JLabel("Status:");
        filterLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterLabel.setForeground(TEXT_PRI);

        JComboBox<String> statusFilter = new JComboBox<>(new String[]{"All Records", "Pending", "Resolved"});
        statusFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusFilter.setPreferredSize(new Dimension(130, 34));
        statusFilter.setBackground(WHITE);

        JLabel timeLabel = new JLabel("  Period:");
        timeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        timeLabel.setForeground(TEXT_PRI);

        JComboBox<String> timeFilter = new JComboBox<>(new String[]{
            "All Time", "Today", "Last 3 Days", "Last 7 Days", "Last 30 Days"
        });
        timeFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeFilter.setPreferredSize(new Dimension(130, 34));
        timeFilter.setBackground(WHITE);

        filterLeft.add(filterLabel);
        filterLeft.add(statusFilter);
        filterLeft.add(timeLabel);
        filterLeft.add(timeFilter);

        // Right side: Search + Export
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
        historySearchField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (historySearchField.getText().equals("Search history...")) {
                    historySearchField.setText("");
                    historySearchField.setForeground(TEXT_PRI);
                }
            }
            @Override public void focusLost(FocusEvent e) {
                if (historySearchField.getText().isEmpty()) {
                    historySearchField.setText("Search history...");
                    historySearchField.setForeground(TEXT_SEC);
                }
            }
        });

        searchBar.add(searchIcon, BorderLayout.WEST);
        searchBar.add(historySearchField, BorderLayout.CENTER);

        JButton exportBtn = createStyledButton("Export", STAT_GREEN, new Color(40, 150, 100), WHITE);
        exportBtn.setPreferredSize(new Dimension(90, 36));
        exportBtn.addActionListener(e -> exportHistoryData(historyFrame));

        filterRight.add(searchBar);
        filterRight.add(exportBtn);

        filterPanel.add(filterLeft, BorderLayout.WEST);
        filterPanel.add(filterRight, BorderLayout.EAST);

        // ── Combined filter logic ─────────────────────────────────────────────
        Runnable applyFilters = () -> {
            String statusSel = (String) statusFilter.getSelectedItem();
            String timeSel   = (String) timeFilter.getSelectedItem();
            String searchText = historySearchField.getText().trim();
            boolean hasSearch = !searchText.isEmpty() && !searchText.equals("Search history...");

            java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();

            // Status filter
            if (!"All Records".equals(statusSel)) {
                filters.add(RowFilter.regexFilter("^" + statusSel + "$", 5));
            }

            // Time period filter (column 4 = date)
            if (!"All Time".equals(timeSel)) {
                int days;
                switch (timeSel) {
                    case "Today":        days = 0; break;
                    case "Last 3 Days":  days = 3; break;
                    case "Last 7 Days":  days = 7; break;
                    case "Last 30 Days": days = 30; break;
                    default: days = -1;
                }
                if (days >= 0) {
                    final int filterDays = days;
                    filters.add(new RowFilter<Object, Object>() {
                        @Override
                        public boolean include(Entry<?, ?> entry) {
                            try {
                                String dateStr = entry.getStringValue(4);
                                if (dateStr == null || dateStr.isEmpty() || "N/A".equals(dateStr))
                                    return false;
                                LocalDate recordDate = LocalDate.parse(dateStr);
                                if (filterDays == 0) {
                                    return recordDate.equals(LocalDate.now());
                                }
                                LocalDate cutoff = LocalDate.now().minusDays(filterDays);
                                return !recordDate.isBefore(cutoff);
                            } catch (Exception ex) {
                                return false;
                            }
                        }
                    });
                }
            }

            // Search filter
            if (hasSearch) {
                filters.add(RowFilter.regexFilter("(?i)" + searchText, 0, 1, 2, 3, 6));
            }

            if (filters.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.andFilter(filters));
            }
        };

        statusFilter.addActionListener(e -> applyFilters.run());
        timeFilter.addActionListener(e -> applyFilters.run());
        historySearchField.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) {
                applyFilters.run();
            }
        });

        return filterPanel;
    }

    // ── Create history table ──────────────────────────────────────────────

    private JTable createHistoryTable(DefaultTableModel historyModel) {
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
        historyTable.setSelectionForeground(TEXT_PRI);

        JTableHeader header = historyTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(240, 245, 250));
        header.setForeground(TEXT_PRI);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_CLR));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 42));

        // ID column renderer (blue, bold)
        historyTable.getColumnModel().getColumn(0).setCellRenderer(
            (t, val, sel, foc, row, col) -> {
                JLabel l = new JLabel(val != null ? val.toString() : "");
                l.setFont(new Font("Segoe UI", Font.BOLD, 13));
                l.setForeground(BLUE);
                l.setBorder(new EmptyBorder(0, 12, 0, 0));
                l.setOpaque(true);
                l.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
                return l;
            });

        // Date column renderer with "Today" / "X days ago" hint
        historyTable.getColumnModel().getColumn(4).setCellRenderer(
            (t, val, sel, foc, row, col) -> {
                String dateStr = val != null ? val.toString() : "";
                String display = dateStr;

                try {
                    if (!dateStr.isEmpty() && !"N/A".equals(dateStr)) {
                        LocalDate recordDate = LocalDate.parse(dateStr);
                        long daysAgo = ChronoUnit.DAYS.between(recordDate, LocalDate.now());
                        if (daysAgo == 0) {
                            display = dateStr + "  (Today)";
                        } else if (daysAgo == 1) {
                            display = dateStr + "  (Yesterday)";
                        } else if (daysAgo <= 7) {
                            display = dateStr + "  (" + daysAgo + "d ago)";
                        }
                    }
                } catch (Exception ignored) {}

                JLabel l = new JLabel(display);
                l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                l.setForeground(TEXT_PRI);
                l.setBorder(new EmptyBorder(0, 12, 0, 0));
                l.setOpaque(true);
                l.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));
                return l;
            });

        // Status column renderer (pill badge)
        historyTable.getColumnModel().getColumn(5).setCellRenderer(
            (t, val, sel, foc, row, col) -> {
                String status = val != null ? val.toString() : "";
                boolean isPending = "Pending".equals(status);

                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
                wrapper.setOpaque(true);
                wrapper.setBackground(row % 2 == 0 ? WHITE : new Color(248, 250, 252));

                JPanel pill = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(isPending ? PENDING_BG : RESOLVED_BG);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                        g2.dispose();
                    }
                };
                pill.setLayout(new BorderLayout());
                pill.setOpaque(false);
                pill.setBorder(new EmptyBorder(4, 12, 4, 12));

                JLabel l = new JLabel(status);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setForeground(isPending ? PENDING_FG : RESOLVED_FG);
                pill.add(l, BorderLayout.CENTER);

                wrapper.add(pill);
                return wrapper;
            });

        // Set column widths
        int[] widths = {70, 150, 150, 130, 130, 90, 280};
        for (int i = 0; i < widths.length; i++) {
            historyTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        return historyTable;
    }

    // ── Helper methods ────────────────────────────────────────────────────

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

    private void exportHistoryData(JFrame owner) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export History Data");
        fileChooser.setSelectedFile(new java.io.File("blotter_history_" +
            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv"));

        if (fileChooser.showSaveDialog(owner) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(fileChooser.getSelectedFile())) {
                // Write header
                writer.println("ID,Complainant,Respondent,Complaint Type,Date,Status,Description,Address");

                // Write data
                for (Object[] row : blotterData) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        row[0], row[1], row[2], row[6], row[3], row[4], row[7], row[5]);
                }

                JOptionPane.showMessageDialog(owner,
                    "History data exported successfully!",
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(owner,
                    "Error exporting data: " + ex.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
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

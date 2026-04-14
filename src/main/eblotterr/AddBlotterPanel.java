package main.eblotterr;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Panel for adding a new blotter entry.
 * Extracted from dashboard.java to keep the dashboard clean.
 */
public class AddBlotterPanel extends JPanel {

    // ── Color palette (matches dashboard) ─────────────────────────────────
    private static final Color WHITE        = Color.WHITE;
    private static final Color BLUE         = new Color(45, 118, 200);
    private static final Color BLUE_HOVER   = new Color(35, 95, 160);
    private static final Color BLUE_LIGHT   = new Color(230, 241, 251);
    private static final Color TEXT_PRI     = new Color(33, 41, 52);
    private static final Color TEXT_SEC     = new Color(115, 130, 150);

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
    private final ConnectionProvider connectionProvider;
    private final Runnable onSaveCallback;
    private final int nextBlotterNumber;

    /**
     * Functional interface for obtaining a database connection.
     */
    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws ClassNotFoundException, SQLException;
    }

    /**
     * @param parent             the parent dialog
     * @param connectionProvider supplies DB connections
     * @param nextBlotterNumber  the next auto-generated blotter number
     * @param onSaveCallback     called after a successful save (reload data / refresh UI)
     */
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

    // ── Header ────────────────────────────────────────────────────────────

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

    // ── Scroll body ───────────────────────────────────────────────────────

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

        JButton cancelBtn = roundButton("Cancel", BTN_CANCEL_BG, BTN_CANCEL_FG, true);
        JButton saveBtn   = roundButton("Save Blotter Entry", BTN_SAVE_BG, BTN_SAVE_FG, false);

        cancelBtn.addActionListener(e -> parentDialog.dispose());
        saveBtn.addActionListener(e -> saveBlotterToDB());

        row.add(cancelBtn);
        row.add(saveBtn);
        return row;
    }

    // ── Save to database ──────────────────────────────────────────────────

    private void saveBlotterToDB() {
        String complainant = tfComplainantName.getText().trim();
        String respondent = tfRespondentName.getText().trim();
        String address = tfComplainantAddress.getText().trim();
        String complaintType = (String) cbComplaintType.getSelectedItem();
        String description = taDescription.getText().trim();

        // Validation
        if (complainant.isEmpty() || complainant.equals("Enter complainant's full name")) {
            JOptionPane.showMessageDialog(this,
                "Please enter complainant's name.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfComplainantName.requestFocus();
            return;
        }

        if (respondent.isEmpty() || respondent.equals("Enter respondent's full name")) {
            JOptionPane.showMessageDialog(this,
                "Please enter respondent's name.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfRespondentName.requestFocus();
            return;
        }

        if (address.isEmpty() || address.equals("Enter complainant's address")) {
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
                try (Connection conn = connectionProvider.getConnection()) {
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
                        if (onSaveCallback != null) onSaveCallback.run();
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

    // ── Helper methods ────────────────────────────────────────────────────

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

    // ── Round border helper ───────────────────────────────────────────────

    static class RoundBorderLocal extends AbstractBorder {
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

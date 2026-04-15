package main.eblotterr;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Dialog for viewing and editing the details of a single blotter record.
 * Pending records can be edited; resolved records are read-only.
 * Extracted from dashboard.java to keep the dashboard clean.
 */
public class ViewBlotterDialog {

    // ── Color palette (matches dashboard) ─────────────────────────────────
    private static final Color WHITE        = Color.WHITE;
    private static final Color BLUE         = new Color(45, 118, 200);
    private static final Color BLUE_HOVER   = new Color(35, 95, 160);
    private static final Color PENDING_BG   = new Color(255, 247, 235);
    private static final Color PENDING_FG   = new Color(255, 140, 60);
    private static final Color RESOLVED_BG  = new Color(228, 248, 240);
    private static final Color RESOLVED_FG  = new Color(46, 176, 120);
    private static final Color EDIT_BG      = new Color(255, 252, 240);
    private static final Color EDIT_BORDER  = new Color(255, 200, 100);

    private final JFrame parentFrame;
    private final List<Object[]> blotterData;
    private final StatusUpdateHandler statusUpdateHandler;
    private final PrintHandler printHandler;
    private final ConnectionProvider connectionProvider;
    private final Runnable onDataChanged;
    private final String userRole; // "secretary", "captain", or "kagawad"

    // ── Editable field references — Complainant ───────────────────────────
    private JTextField tfCFirstName;
    private JTextField tfCMiddleName;
    private JTextField tfCLastName;
    private JTextField tfCSuffix;
    private JTextField tfMobileNumber;
    private JTextField tfPurok;

    // ── Editable field references — Respondent ────────────────────────────
    private JTextField tfRFirstName;
    private JTextField tfRMiddleName;
    private JTextField tfRLastName;
    private JTextField tfRSuffix;

    // ── Editable field references — Incident ──────────────────────────────
    private JTextField tfCompType;
    private JTextArea taDescription;
    private boolean editMode = false;

    @FunctionalInterface
    public interface StatusUpdateHandler {
        void showUpdateStatusDialog(int row);
    }

    @FunctionalInterface
    public interface PrintHandler {
        void printRecord(int row);
    }

    @FunctionalInterface
    public interface ConnectionProvider {
        Connection getConnection() throws ClassNotFoundException, SQLException;
    }

    /**
     * @return true if the current user is a secretary (full edit access)
     */
    private boolean isSecretary() {
        return "secretary".equalsIgnoreCase(userRole);
    }

    /**
     * @param parentFrame         the parent frame (dashboard)
     * @param blotterData         the shared blotter data list
     * @param statusUpdateHandler handler for "Update Status" button
     * @param printHandler        handler for "Print Report" button
     * @param connectionProvider  supplies DB connections for saving edits
     * @param onDataChanged       called after a successful save (reload data / refresh UI)
     * @param userRole            the role of the logged-in user
     */
    public ViewBlotterDialog(JFrame parentFrame, List<Object[]> blotterData,
                             StatusUpdateHandler statusUpdateHandler,
                             PrintHandler printHandler,
                             ConnectionProvider connectionProvider,
                             Runnable onDataChanged,
                             String userRole) {
        this.parentFrame = parentFrame;
        this.blotterData = blotterData;
        this.statusUpdateHandler = statusUpdateHandler;
        this.printHandler = printHandler;
        this.connectionProvider = connectionProvider;
        this.onDataChanged = onDataChanged;
        this.userRole = (userRole != null) ? userRole.toLowerCase() : "secretary";
    }

    /**
     * Show the detail view dialog for a given row index.
     */
    public void show(int row) {
        if (row < 0 || row >= blotterData.size()) return;

        // Reset edit mode for each new dialog
        editMode = false;

        Object[] data = blotterData.get(row);
        Object blotterNum  = data[0];
        // data[1] = complainant full name (display)
        // data[2] = respondent full name (display)
        Object date        = data[3];
        Object status      = data[4];
        Object purok       = data[5];
        Object compType    = data[6];
        Object description = data[7];
        // data[8]  = complainant_id
        // data[9]  = c_first_name
        // data[10] = c_middle_name
        // data[11] = c_last_name
        // data[12] = c_suffix
        // data[13] = respondent_id
        // data[14] = r_first_name
        // data[15] = r_middle_name
        // data[16] = r_last_name
        // data[17] = r_suffix
        // data[18] = c_mobile

        boolean isPending = "pending".equalsIgnoreCase(status != null ? status.toString() : "");

        JDialog detailDialog = new JDialog(parentFrame, "Blotter Details - #" + blotterNum, true);
        detailDialog.setSize(750, 750);
        detailDialog.setMinimumSize(new Dimension(480, 500));
        detailDialog.setLocationRelativeTo(parentFrame);
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

        // Parties Involved Card (editable fields)
        body.add(buildDetailPartiesCard(data));
        body.add(Box.createVerticalStrut(16));

        // Incident Details Card (editable fields)
        body.add(buildDetailIncidentCard(compType, description, status));
        body.add(Box.createVerticalStrut(20));

        // Button Row (with Edit / Save buttons)
        body.add(buildDetailButtonRow(detailDialog, row, status, blotterNum));
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

    // ── Toggle edit mode ──────────────────────────────────────────────────

    private void setFieldsEditable(boolean editable) {
        editMode = editable;
        setFieldEditStyle(tfCFirstName, editable, false);
        setFieldEditStyle(tfCMiddleName, editable, false);
        setFieldEditStyle(tfCLastName, editable, false);
        setFieldEditStyle(tfCSuffix, editable, false);
        setFieldEditStyle(tfRFirstName, editable, true);
        setFieldEditStyle(tfRMiddleName, editable, true);
        setFieldEditStyle(tfRLastName, editable, true);
        setFieldEditStyle(tfRSuffix, editable, true);
        setFieldEditStyle(tfMobileNumber, editable, false);
        setFieldEditStyle(tfPurok, editable, false);
        setFieldEditStyle(tfCompType, editable, false);
        if (taDescription != null) {
            taDescription.setEditable(editable);
            taDescription.setBackground(editable ? EDIT_BG : WHITE);
            taDescription.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(editable ? EDIT_BORDER : new Color(0xC8D8EC), 1, 8),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        }
    }

    private void setFieldEditStyle(JTextField tf, boolean editable, boolean isRespondent) {
        if (tf == null) return;
        tf.setEditable(editable);
        Color defaultBg = isRespondent ? new Color(0xFFF0EE) : WHITE;
        Color defaultBorder = isRespondent ? new Color(0xF5C0BB) : new Color(0xC8D8EC);
        tf.setBackground(editable ? EDIT_BG : defaultBg);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(editable ? EDIT_BORDER : defaultBorder, 1, 8),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
    }

    // ── Save edited data ──────────────────────────────────────────────────

    private void saveEdits(JDialog dialog, int row, Object blotterNum) {
        String cFirst = tfCFirstName.getText().trim();
        String cMiddle = tfCMiddleName.getText().trim();
        String cLast = tfCLastName.getText().trim();
        String cSuffix = tfCSuffix.getText().trim();
        String rFirst = tfRFirstName.getText().trim();
        String rMiddle = tfRMiddleName.getText().trim();
        String rLast = tfRLastName.getText().trim();
        String rSuffix = tfRSuffix.getText().trim();
        String newMobileNumber = tfMobileNumber.getText().trim();
        String newPurok = tfPurok.getText().trim();
        String newCompType = tfCompType.getText().trim();
        String newDescription = taDescription.getText().trim();

        // Validation — suffix is optional for both
        if (cFirst.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Complainant's first name cannot be empty.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfCFirstName.requestFocus();
            return;
        }
        if (cMiddle.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Complainant's middle name cannot be empty.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfCMiddleName.requestFocus();
            return;
        }
        if (cLast.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Complainant's last name cannot be empty.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfCLastName.requestFocus();
            return;
        }
        // cSuffix is OPTIONAL

        if (rFirst.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Respondent's first name cannot be empty.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfRFirstName.requestFocus();
            return;
        }
        if (rMiddle.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Respondent's middle name cannot be empty.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfRMiddleName.requestFocus();
            return;
        }
        if (rLast.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Respondent's last name cannot be empty.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfRLastName.requestFocus();
            return;
        }
        // rSuffix is OPTIONAL

        if (newMobileNumber.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Mobile number cannot be empty.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfMobileNumber.requestFocus();
            return;
        }

        if (newPurok.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Purok cannot be empty.",
                "Validation Error", JOptionPane.ERROR_MESSAGE);
            tfPurok.requestFocus();
            return;
        }

        // Confirm save
        int confirm = JOptionPane.showConfirmDialog(dialog,
            "Are you sure you want to save the changes?",
            "Confirm Save", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        // Get foreign key IDs from blotterData
        Object[] data = blotterData.get(row);
        final int complainantId = Integer.parseInt(data[8].toString());
        final int respondentId = Integer.parseInt(data[13].toString());

        // Capture final values for SwingWorker
        final String fCFirst = cFirst, fCMiddle = cMiddle, fCLast = cLast, fCSuffix = cSuffix;
        final String fRFirst = rFirst, fRMiddle = rMiddle, fRLast = rLast, fRSuffix = rSuffix;
        final String fMobileNumber = newMobileNumber, fPurok = newPurok, fCompType = newCompType, fDescription = newDescription;

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                try (Connection conn = connectionProvider.getConnection()) {
                    conn.setAutoCommit(false);
                    try {
                        // Update complainant table
                        String updateComplainant = "UPDATE complainant SET first_name = ?, " +
                            "middle_name = ?, last_name = ?, suffix = ?, mobile_number = ?, purok = ? " +
                            "WHERE complainant_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(updateComplainant)) {
                            pstmt.setString(1, fCFirst);
                            pstmt.setString(2, fCMiddle);
                            pstmt.setString(3, fCLast);
                            pstmt.setString(4, fCSuffix.isEmpty() ? null : fCSuffix);
                            pstmt.setString(5, fMobileNumber);
                            pstmt.setString(6, fPurok);
                            pstmt.setInt(7, complainantId);
                            pstmt.executeUpdate();
                        }

                        // Update respondent table
                        String updateRespondent = "UPDATE respondent SET first_name = ?, " +
                            "middle_name = ?, last_name = ?, suffix = ? " +
                            "WHERE respondent_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(updateRespondent)) {
                            pstmt.setString(1, fRFirst);
                            pstmt.setString(2, fRMiddle);
                            pstmt.setString(3, fRLast);
                            pstmt.setString(4, fRSuffix.isEmpty() ? null : fRSuffix);
                            pstmt.setInt(5, respondentId);
                            pstmt.executeUpdate();
                        }

                        // Update blotter table (complaint type and description only)
                        String updateBlotter = "UPDATE blotter SET complt_type = ?, description = ? " +
                            "WHERE blotter_id = ?";
                        try (PreparedStatement pstmt = conn.prepareStatement(updateBlotter)) {
                            pstmt.setString(1, fCompType);
                            pstmt.setString(2, fDescription);
                            pstmt.setInt(3, Integer.parseInt(blotterNum.toString()));
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
                        JOptionPane.showMessageDialog(dialog,
                            "Blotter details updated successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                        if (onDataChanged != null) onDataChanged.run();
                        dialog.dispose();
                    }
                } catch (Exception e) {
                    String msg = "Error saving changes: ";
                    if (e.getCause() != null && e.getCause().getMessage() != null)
                        msg += e.getCause().getMessage();
                    else if (e.getMessage() != null)
                        msg += e.getMessage();
                    JOptionPane.showMessageDialog(dialog, msg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
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

    private JPanel buildDetailPartiesCard(Object[] data) {
        JPanel card = createDetailCard();

        // ── Complainant Section ──────────────────────────────────────────
        JLabel complainantLabel = createDetailSectionHeader("COMPLAINANT INFORMATION");

        JPanel cRow1 = new JPanel(new GridLayout(1, 2, 14, 0));
        cRow1.setOpaque(false);

        tfCFirstName = new JTextField(data[9] != null ? data[9].toString() : "");
        styleDetailTextField(tfCFirstName, false);
        tfCMiddleName = new JTextField(data[10] != null ? data[10].toString() : "");
        styleDetailTextField(tfCMiddleName, false);

        cRow1.add(createLabeledEditableField("First Name", tfCFirstName));
        cRow1.add(createLabeledEditableField("Middle Name", tfCMiddleName));

        JPanel cRow2 = new JPanel(new GridLayout(1, 2, 14, 0));
        cRow2.setOpaque(false);

        tfCLastName = new JTextField(data[11] != null ? data[11].toString() : "");
        styleDetailTextField(tfCLastName, false);
        tfCSuffix = new JTextField(data[12] != null ? data[12].toString() : "");
        styleDetailTextField(tfCSuffix, false);

        cRow2.add(createLabeledEditableField("Last Name", tfCLastName));
        cRow2.add(createLabeledEditableField("Suffix (Optional)", tfCSuffix));

        // Mobile Number & Purok
        JPanel cRow3 = new JPanel(new GridLayout(1, 2, 14, 0));
        cRow3.setOpaque(false);

        tfMobileNumber = new JTextField(data.length > 18 && data[18] != null ? data[18].toString() : "");
        styleDetailTextField(tfMobileNumber, false);
        tfPurok = new JTextField(data[5] != null ? data[5].toString() : "");
        styleDetailTextField(tfPurok, false);

        cRow3.add(createLabeledEditableField("Mobile Number", tfMobileNumber));
        cRow3.add(createLabeledEditableField("Purok", tfPurok));

        // ── Respondent Section ───────────────────────────────────────────
        JLabel respondentLabel = createDetailSectionHeader("RESPONDENT INFORMATION");

        JPanel rRow1 = new JPanel(new GridLayout(1, 2, 14, 0));
        rRow1.setOpaque(false);

        tfRFirstName = new JTextField(data[14] != null ? data[14].toString() : "");
        styleDetailTextField(tfRFirstName, true);
        tfRMiddleName = new JTextField(data[15] != null ? data[15].toString() : "");
        styleDetailTextField(tfRMiddleName, true);

        rRow1.add(createLabeledEditableField("First Name", tfRFirstName));
        rRow1.add(createLabeledEditableField("Middle Name", tfRMiddleName));

        JPanel rRow2 = new JPanel(new GridLayout(1, 2, 14, 0));
        rRow2.setOpaque(false);

        tfRLastName = new JTextField(data[16] != null ? data[16].toString() : "");
        styleDetailTextField(tfRLastName, true);
        tfRSuffix = new JTextField(data[17] != null ? data[17].toString() : "");
        styleDetailTextField(tfRSuffix, true);

        rRow2.add(createLabeledEditableField("Last Name", tfRLastName));
        rRow2.add(createLabeledEditableField("Suffix (Optional)", tfRSuffix));

        // Date row
        JPanel dateRow = new JPanel(new GridLayout(1, 2, 14, 0));
        dateRow.setOpaque(false);
        dateRow.add(createDetailField("Date of Incident",
            data[3] != null ? data[3].toString() : "N/A", false));
        dateRow.add(new JPanel()); // Empty spacer for formatting

        // ── Assemble Card ────────────────────────────────────────────────
        card.add(complainantLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(cRow1);
        card.add(Box.createVerticalStrut(10));
        card.add(cRow2);
        card.add(Box.createVerticalStrut(10));
        card.add(cRow3);
        card.add(Box.createVerticalStrut(16));
        card.add(respondentLabel);
        card.add(Box.createVerticalStrut(10));
        card.add(rRow1);
        card.add(Box.createVerticalStrut(10));
        card.add(rRow2);
        card.add(Box.createVerticalStrut(12));
        card.add(dateRow);

        return card;
    }

    private JPanel buildDetailIncidentCard(Object compType, Object description, Object status) {
        JPanel card = createDetailCard();

        JLabel sectionLabel = createDetailSectionHeader("INCIDENT DETAILS");

        // Type of Complaint field (editable)
        JPanel typePanel = new JPanel();
        typePanel.setOpaque(false);
        typePanel.setLayout(new BoxLayout(typePanel, BoxLayout.Y_AXIS));
        JLabel lblType = new JLabel("Type of Complaint");
        lblType.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblType.setForeground(new Color(0x2C4A6E));
        lblType.setAlignmentX(Component.LEFT_ALIGNMENT);
        tfCompType = new JTextField(compType != null ? compType.toString() : "N/A");
        styleDetailTextField(tfCompType, false);
        typePanel.add(lblType);
        typePanel.add(Box.createVerticalStrut(4));
        typePanel.add(tfCompType);

        // Description field (editable)
        JPanel descriptionPanel = new JPanel();
        descriptionPanel.setOpaque(false);
        descriptionPanel.setLayout(new BoxLayout(descriptionPanel, BoxLayout.Y_AXIS));

        JLabel descriptionLabel = new JLabel("Description / Incident Details");
        descriptionLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        descriptionLabel.setForeground(new Color(0x2C4A6E));
        descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        taDescription = new JTextArea(description != null ? description.toString() : "");
        taDescription.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        taDescription.setForeground(new Color(0x1A1A2E));
        taDescription.setBackground(Color.WHITE);
        taDescription.setEditable(false);
        taDescription.setLineWrap(true);
        taDescription.setWrapStyleWord(true);
        taDescription.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(new Color(0xC8D8EC), 1, 8),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        taDescription.setAlignmentX(Component.LEFT_ALIGNMENT);

        JScrollPane descriptionScroll = new JScrollPane(taDescription);
        descriptionScroll.setBorder(null);
        descriptionScroll.setOpaque(false);
        descriptionScroll.getViewport().setOpaque(false);
        descriptionScroll.setPreferredSize(new Dimension(0, 100));
        descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
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
        card.add(typePanel);
        card.add(Box.createVerticalStrut(16));
        card.add(descriptionPanel);
        card.add(Box.createVerticalStrut(16));
        card.add(new JLabel("Status:"));
        card.add(Box.createVerticalStrut(6));
        card.add(statusWrapper);

        return card;
    }

    private JPanel buildDetailButtonRow(JDialog dialog, int row, Object status, Object blotterNum) {
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
            printHandler.printRecord(row);
        });

        // Only secretary can edit or update status on pending blotters
        if (isPending && isSecretary()) {
            // Edit / Save button
            JButton editBtn = createDetailButton("✏ Edit Details", new Color(0x2D76C8), Color.WHITE, false);
            JButton saveBtn = createDetailButton("💾 Save Changes", new Color(0x2EB078), Color.WHITE, false);
            JButton cancelEditBtn = createDetailButton("Cancel Edit", new Color(0x6C757D), Color.WHITE, false);

            // Save button is hidden initially
            saveBtn.setVisible(false);
            cancelEditBtn.setVisible(false);

            editBtn.addActionListener(e -> {
                setFieldsEditable(true);
                editBtn.setVisible(false);
                saveBtn.setVisible(true);
                cancelEditBtn.setVisible(true);
                printBtn.setVisible(false);
                closeBtn.setVisible(false);
                rowPanel.revalidate();
                rowPanel.repaint();
            });

            saveBtn.addActionListener(e -> {
                saveEdits(dialog, row, blotterNum);
            });

            cancelEditBtn.addActionListener(e -> {
                setFieldsEditable(false);
                // Reset field values from blotterData
                Object[] data = blotterData.get(row);
                tfCFirstName.setText(data[9] != null ? data[9].toString() : "");
                tfCMiddleName.setText(data[10] != null ? data[10].toString() : "");
                tfCLastName.setText(data[11] != null ? data[11].toString() : "");
                tfCSuffix.setText(data[12] != null ? data[12].toString() : "");
                tfRFirstName.setText(data[14] != null ? data[14].toString() : "");
                tfRMiddleName.setText(data[15] != null ? data[15].toString() : "");
                tfRLastName.setText(data[16] != null ? data[16].toString() : "");
                tfRSuffix.setText(data[17] != null ? data[17].toString() : "");
                tfMobileNumber.setText(data.length > 18 && data[18] != null ? data[18].toString() : "");
                tfPurok.setText(data[5] != null ? data[5].toString() : "");
                tfCompType.setText(data[6] != null ? data[6].toString() : "N/A");
                taDescription.setText(data[7] != null ? data[7].toString() : "");
                editBtn.setVisible(true);
                saveBtn.setVisible(false);
                cancelEditBtn.setVisible(false);
                printBtn.setVisible(true);
                closeBtn.setVisible(true);
                rowPanel.revalidate();
                rowPanel.repaint();
            });

            // Update Status button
            JButton statusBtn = createDetailButton("Update Status", new Color(0xFF8C3C), Color.WHITE, false);
            statusBtn.addActionListener(e -> {
                dialog.dispose();
                statusUpdateHandler.showUpdateStatusDialog(row);
            });

            rowPanel.add(editBtn);
            rowPanel.add(saveBtn);
            rowPanel.add(cancelEditBtn);
            rowPanel.add(statusBtn);
        }

        rowPanel.add(printBtn);
        rowPanel.add(closeBtn);

        return rowPanel;
    }

    // ── Detail View Card Helpers ──────────────────────────────────────────────

    private void styleDetailTextField(JTextField tf, boolean isRespondent) {
        tf.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tf.setForeground(new Color(0x1A1A2E));
        tf.setBackground(isRespondent ? new Color(0xFFF0EE) : Color.WHITE);
        tf.setEditable(false);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(isRespondent ? new Color(0xF5C0BB) : new Color(0xC8D8EC), 1, 8),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
    }

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

    private JPanel createLabeledEditableField(String labelText, JTextField tf) {
        JPanel pnl = new JPanel();
        pnl.setOpaque(false);
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(new Color(0x2C4A6E));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        tf.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnl.add(lbl);
        pnl.add(Box.createVerticalStrut(4));
        pnl.add(tf);

        return pnl;
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
        btn.setPreferredSize(new Dimension(text.length() > 10 ? 150 : 100, 40));
        return btn;
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

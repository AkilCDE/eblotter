package main.eblotterr;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.*;

public class addblotter extends JFrame {

    // ── Color palette (matches the screenshot) ──────────────────────────
    private static final Color HEADER_BG      = new Color(0x1B3A5C);
    private static final Color BLOTTER_BAR_BG = new Color(0x1B3A5C);
    private static final Color PAGE_BG        = new Color(0xEAF1FB);
    private static final Color CARD_BG        = Color.WHITE;
    private static final Color SECTION_LABEL  = new Color(0x1B3A5C);
    private static final Color FIELD_BORDER   = new Color(0xC8D8EC);
    private static final Color FIELD_BG       = Color.WHITE;
    private static final Color RESPONDENT_BG  = new Color(0xFFF0EE);
    private static final Color RESPONDENT_BDR = new Color(0xF5C0BB);
    private static final Color BTN_SAVE_BG    = new Color(0x1B3A5C);
    private static final Color BTN_SAVE_FG    = Color.WHITE;
    private static final Color BTN_CANCEL_BG  = Color.WHITE;
    private static final Color BTN_CANCEL_FG  = new Color(0x1B3A5C);
    private static final Color LABEL_FG       = new Color(0x2C4A6E);
    private static final Color TEXT_FG        = new Color(0x1A1A2E);
    private static final Color HEADER_SUBTITLE= new Color(0xA8C4E0);

    // ── Fonts ────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE    = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_SECTION  = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_LABEL    = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_INPUT    = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BLOTTER  = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_BTN      = new Font("Segoe UI", Font.BOLD, 12);

    // ── Form fields ──────────────────────────────────────────────────────
    private JTextField tfComplainantName;
    private JTextField tfRespondentName;
    private JTextField tfComplainantAddress;
    private JTextField tfDate;
    private JComboBox<String> cbComplaintType;
    private JTextArea taNarrative;

    public addblotter() {
        setTitle("E-Blotter – Add New Blotter");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(820, 750);
        setMinimumSize(new Dimension(680, 680));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(buildHeader(),      BorderLayout.NORTH);
        add(buildScrollBody(),  BorderLayout.CENTER);

        setVisible(true);
    }

    // ════════════════════════════════════════════════════════════════════
    //  HEADER
    // ════════════════════════════════════════════════════════════════════
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

        // ← Back button
        JButton backBtn = new JButton("← Back");
        backBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backBtn.setForeground(new Color(0xA8C4E0));
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> dispose());

        // Title block (centre-left)
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

        // ⋮ menu dots
        JLabel menuDots = new JLabel("⋯");
        menuDots.setFont(new Font("Segoe UI", Font.BOLD, 20));
        menuDots.setForeground(new Color(0xA8C4E0));
        menuDots.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        left.add(backBtn);
        left.add(titleBlock);

        header.add(left,     BorderLayout.WEST);
        header.add(menuDots, BorderLayout.EAST);

        return header;
    }

    // ════════════════════════════════════════════════════════════════════
    //  SCROLL BODY
    // ════════════════════════════════════════════════════════════════════
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

    // ════════════════════════════════════════════════════════════════════
    //  BLOTTER NUMBER BAR
    // ════════════════════════════════════════════════════════════════════
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

        JLabel lVal = new JLabel("#2025-004 (auto-generated)");
        lVal.setFont(FONT_BLOTTER);
        lVal.setForeground(Color.WHITE);

        bar.add(lKey, BorderLayout.WEST);
        bar.add(lVal, BorderLayout.EAST);
        return bar;
    }

    // ════════════════════════════════════════════════════════════════════
    //  PARTIES INVOLVED CARD
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildPartiesCard() {
        JPanel card = createCard();

        JLabel sectionLabel = sectionHeader("PARTIES INVOLVED");

        // Row 1: Complainant Name | Respondent Name
        JPanel row1 = new JPanel(new GridLayout(1, 2, 14, 0));
        row1.setOpaque(false);
        tfComplainantName = styledField("Juan dela Cruz", false);
        tfRespondentName  = styledField("Pedro Reyes",    true);
        row1.add(labeledField("Complainant's Full Name", tfComplainantName));
        row1.add(labeledField("Respondent's Full Name",  tfRespondentName));

        // Row 2: Complainant's Address | Date of Incident
        JPanel row2 = new JPanel(new GridLayout(1, 2, 14, 0));
        row2.setOpaque(false);
        tfComplainantAddress = styledField("Purok 3, Brgy. Mabini", false);
        tfDate = styledField("01/05/2025", false);
        row2.add(labeledField("Complainant's Address", tfComplainantAddress));
        row2.add(labeledField("Date of Incident", tfDate));

        card.add(sectionLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(row1);
        card.add(Box.createVerticalStrut(12));
        card.add(row2);

        return card;
    }

    // ════════════════════════════════════════════════════════════════════
    //  INCIDENT DETAILS CARD
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildIncidentDetailsCard() {
        JPanel card = createCard();

        JLabel sectionLabel = sectionHeader("INCIDENT DETAILS");

        // Complaint Type
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
            new RoundBorder(FIELD_BORDER, 8),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        cbComplaintType.setPreferredSize(new Dimension(200, 38));
        cbComplaintType.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        // Narrative
        taNarrative = new JTextArea("requests to lower the volume...", 4, 20);
        taNarrative.setFont(FONT_INPUT);
        taNarrative.setForeground(TEXT_FG);
        taNarrative.setBackground(FIELD_BG);
        taNarrative.setLineWrap(true);
        taNarrative.setWrapStyleWord(true);
        taNarrative.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(FIELD_BORDER, 8),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        
        JScrollPane narrativeScroll = new JScrollPane(taNarrative);
        narrativeScroll.setBorder(null);
        narrativeScroll.setOpaque(false);
        narrativeScroll.getViewport().setOpaque(false);
        narrativeScroll.setPreferredSize(new Dimension(200, 110));
        narrativeScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        card.add(sectionLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(labeledField("Type of Complaint", cbComplaintType));
        card.add(Box.createVerticalStrut(16));
        card.add(labeledField("Narrative / Incident Description", narrativeScroll));

        return card;
    }

    // ════════════════════════════════════════════════════════════════════
    //  BUTTON ROW
    // ════════════════════════════════════════════════════════════════════
    private JPanel buildButtonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton cancelBtn = roundButton("Cancel",           BTN_CANCEL_BG, BTN_CANCEL_FG, true);
        JButton saveBtn   = roundButton("Save Blotter Entry", BTN_SAVE_BG,  BTN_SAVE_FG,  false);

        cancelBtn.addActionListener(e -> dispose());
        saveBtn.addActionListener(e -> saveBlotterToDB());

        row.add(cancelBtn);
        row.add(saveBtn);
        return row;
    }

    private void saveBlotterToDB() {
        String complainant = tfComplainantName.getText().trim();
        String respondent = tfRespondentName.getText().trim();
        String address = tfComplainantAddress.getText().trim();
        String dateStr = tfDate.getText().trim();
        String complaintType = (String) cbComplaintType.getSelectedItem();
        String narrative = taNarrative.getText().trim();

        // Validation
        if (complainant.isEmpty() || respondent.isEmpty() || 
            address.isEmpty() || dateStr.isEmpty() || narrative.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Please fill in all required fields.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                String url = "jdbc:mysql://localhost:3306/ebs";
                String user = "root";
                String pass = "";

                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                    try (Connection conn = DriverManager.getConnection(url, user, pass)) {
                        // Parse and convert date from MM/dd/yyyy to yyyy-MM-dd format
                        String[] parts = dateStr.split("/");
                        if (parts.length != 3) {
                            throw new SQLException("Invalid date format. Use MM/DD/YYYY");
                        }
                        String formattedDate = parts[2] + "-" + parts[0] + "-" + parts[1];

                        // Insert into database
                        String insert = "INSERT INTO blotter (complainant, respondent, address, " +
                                      "date, complaint_type, narrative, status) VALUES (?, ?, ?, ?, ?, ?, ?)";
                        try (PreparedStatement pstmt = conn.prepareStatement(insert)) {
                            pstmt.setString(1, complainant);
                            pstmt.setString(2, respondent);
                            pstmt.setString(3, address);
                            pstmt.setDate(4, java.sql.Date.valueOf(formattedDate));
                            pstmt.setString(5, complaintType);
                            pstmt.setString(6, narrative);
                            pstmt.setString(7, "pending");
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
                        JOptionPane.showMessageDialog(addblotter.this,
                            "Blotter entry saved successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    }
                } catch (Exception e) {
                    String message = "Error saving blotter: ";
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        message += e.getCause().getMessage();
                    } else if (e.getMessage() != null) {
                        message += e.getMessage();
                    }
                    JOptionPane.showMessageDialog(addblotter.this, message, 
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ════════════════════════════════════════════════════════════════════

    /** White rounded card panel */
    private JPanel createCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Shadow
                g2.setColor(new Color(0, 0, 0, 18));
                g2.fill(new RoundRectangle2D.Double(3, 5, getWidth()-6, getHeight()-6, 14, 14));
                // Card body
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

    /** Blue section title */
    private JLabel sectionHeader(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(SECTION_LABEL);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    /** Vertical pair: small label + component */
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

    /** Styled text field – respondent variant gets a pink tint */
    private JTextField styledField(String placeholder, boolean isRespondent) {
        JTextField tf = new JTextField(placeholder);
        tf.setFont(FONT_INPUT);
        tf.setForeground(TEXT_FG);
        tf.setBackground(isRespondent ? RESPONDENT_BG : FIELD_BG);
        tf.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(isRespondent ? RESPONDENT_BDR : FIELD_BORDER, 8),
            BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        tf.setPreferredSize(new Dimension(200, 38));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return tf;
    }

    /** Rounded button factory */
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

    // ════════════════════════════════════════════════════════════════════
    //  ROUND BORDER (reusable)
    // ════════════════════════════════════════════════════════════════════
    static class RoundBorder extends AbstractBorder {
        private final Color color;
        private final int radius;
        RoundBorder(Color c, int r) { this.color = c; this.radius = r; }

        @Override public void paintBorder(Component c, Graphics g,
                int x, int y, int w, int h) {
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

    // ════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new addblotter());
    }
}
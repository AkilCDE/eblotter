package main.eblotterr;

import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;

/**
 * Frame for print-previewing and printing a blotter record.
 * Extracted from dashboard.java to keep the dashboard clean.
 */
public class PrintBlotterFrame {

    // ── Color palette (matches dashboard) ─────────────────────────────────
    private static final Color WHITE        = Color.WHITE;
    private static final Color BLUE         = new Color(45, 118, 200);
    private static final Color BLUE_HOVER   = new Color(35, 95, 160);
    private static final Color BLUE_DARK    = new Color(25, 60, 110);
    private static final Color PENDING_FG   = new Color(255, 140, 60);
    private static final Color RESOLVED_FG  = new Color(46, 176, 120);

    private final JFrame parentFrame;
    private final List<Object[]> blotterData;

    /**
     * @param parentFrame  the parent frame (dashboard)
     * @param blotterData  the shared blotter data list
     */
    public PrintBlotterFrame(JFrame parentFrame, List<Object[]> blotterData) {
        this.parentFrame = parentFrame;
        this.blotterData = blotterData;
    }

    /**
     * Show the print preview for the given row index.
     */
    public void show(int row) {
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
    JDialog pf = new JDialog(parentFrame, "Print Preview - Blotter #" + blotterNum, true);
    pf.setSize(750, 700);
    pf.setMinimumSize(new Dimension(500, 500));
    pf.setLocationRelativeTo(parentFrame);
    pf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    pf.setResizable(true);
    pf.setUndecorated(true);
    pf.getContentPane().setBackground(new Color(240, 242, 245));

    // Custom header with close button and dragging
    JPanel customHeader = new JPanel(new BorderLayout());
    customHeader.setBackground(BLUE_DARK);
    customHeader.setPreferredSize(new Dimension(0, 50));
    customHeader.setBorder(new EmptyBorder(0, 16, 0, 16));

    JLabel titleLabel = new JLabel("Print Preview - Blotter #" + blotterNum);
    titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
    titleLabel.setForeground(WHITE);
    titleLabel.setBorder(new EmptyBorder(0, 0, 0, 0));

    JButton closeHeaderBtn = new JButton("x");
    closeHeaderBtn.setFont(new Font("Segoe UI", Font.BOLD, 18));
    closeHeaderBtn.setForeground(WHITE);
    closeHeaderBtn.setBackground(new Color(0, 0, 0, 0));
    closeHeaderBtn.setBorderPainted(false);
    closeHeaderBtn.setContentAreaFilled(false);
    closeHeaderBtn.setFocusPainted(false);
    closeHeaderBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    closeHeaderBtn.setPreferredSize(new Dimension(40, 40));
    closeHeaderBtn.addActionListener(e -> pf.dispose());

    customHeader.add(titleLabel, BorderLayout.WEST);
    customHeader.add(closeHeaderBtn, BorderLayout.EAST);

    // Window dragging
    final int[] dragOffset = new int[2];
    customHeader.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            dragOffset[0] = e.getX();
            dragOffset[1] = e.getY();
        }
    });
    customHeader.addMouseMotionListener(new MouseMotionAdapter() {
        public void mouseDragged(MouseEvent e) {
            Point p = pf.getLocation();
            pf.setLocation(p.x + e.getX() - dragOffset[0], p.y + e.getY() - dragOffset[1]);
        }
    });

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
    headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

    JLabel republicLabel = new JLabel("REPUBLIC OF THE PHILIPPINES");
    republicLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
    republicLabel.setForeground(new Color(45, 118, 200));
    republicLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel barangayLabel = new JLabel("Central Visayas");
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
    infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

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
    partiesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

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
    
    JLabel respondentAddress = new JLabel("Purok 5");
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
    incidentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));

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
    narrativeScroll.setPreferredSize(new Dimension(0, 80));
    narrativeScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
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
    statusPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 55));

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
    signaturePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

    JPanel recordedRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    recordedRow.setBackground(Color.WHITE);
    JLabel recordedLabel = new JLabel("Recorded by: ");
    recordedLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
    JLabel recordedValue = new JLabel("Sec. Deogracias Balili");
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
    JLabel captainSigName = new JLabel("Antonette B. Libut");
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
    buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

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

    // Main panel with custom header
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBackground(new Color(240, 242, 245));
    mainPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
    mainPanel.add(customHeader, BorderLayout.NORTH);
    mainPanel.add(scrollPane, BorderLayout.CENTER);

    pf.setContentPane(mainPanel);
    pf.setVisible(true);
}

// ── Helper methods to create fresh components for printing ────────────

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
    
    JLabel barangayLabel = new JLabel("Central Visayas");
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
    
    JLabel respondentAddress = new JLabel("Purok 5");
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

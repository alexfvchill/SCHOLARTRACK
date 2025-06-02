import com.itextpdf.text.BaseColor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.JTable;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPageEventHelper;
import com.itextpdf.text.pdf.PdfWriter;
import com.toedter.calendar.JDateChooser;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.pdf.ColumnText;
import java.awt.Color;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;



/**
 * Applicants management interface for scholarship tracking system
 * @author alexa
 */
public class Applicants extends javax.swing.JInternalFrame {

    // Database configuration - consider moving to external config file
    private static final String DB_URL = "jdbc:mysql://localhost:3306/scholartrack_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "12345";

    private JDateChooser startdate;
private JDateChooser enddate;
    
    /**
     * Creates new form Applicants
     */
   public Applicants() {
        initComponents();
        setupUI();
        setupEventListeners();
        
        
        startdate = jDateChooser1;
enddate = jDateChooser2;

        try {
            loadScholarshipOptions();
            loadApplicants();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Mouse listener to open details dialog on double-click
        ApplicantsTbl.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int selectedRow = ApplicantsTbl.getSelectedRow();
                    if (selectedRow != -1) {
                        int applicationId = (int) ApplicantsTbl.getValueAt(selectedRow, 0);
                        openApplicationDetailsDialog(applicationId);
                    }
                }
            }
        });
    }

    private void setupUI() {
        this.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,0));
        BasicInternalFrameUI ui = (BasicInternalFrameUI)this.getUI();
        ui.setNorthPane(null);
        
        ApplicantsTbl.getTableHeader().setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
    }

    /**
     * Setup event listeners for interactive components
     */
    
    private void setupEventListeners() {
    searchbtn.addActionListener(e -> loadApplicants());
    statuscombo.addActionListener(e -> loadApplicants());
    scholarshipcombo.addActionListener(e -> loadApplicants()); 
    
    clearbtn.addActionListener(e -> clearFilters());
    JButton exportBtn = new JButton("Export");
    exportBtn.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
    exportBtn.setBackground(new Color(76, 175, 80));
    exportBtn.setForeground(Color.WHITE); 
    jPanel1.add(exportBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(860, 160, 90, 25));  // Adjust position as needed
    exportBtn.addActionListener(e -> exportApplicants());
    
    JButton deleteBtn = new JButton("Delete");
    deleteBtn.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
    deleteBtn.setBackground(new Color(244, 67, 54)); // Red color
    deleteBtn.setForeground(Color.WHITE); // White text
    jPanel1.add(deleteBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(960, 160, 90, 25));
    deleteBtn.addActionListener(e -> deleteApplicant());
    
    Timer searchTimer = new Timer(300, e -> loadApplicants()); // 300ms delay
    searchTimer.setRepeats(false);
    
    // Add document listener to SearchField for auto-search
    SearchField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            // Restart timer when user types
            searchTimer.restart();
        }
        
        @Override
        public void removeUpdate(DocumentEvent e) {
            // Restart timer when user deletes text
            searchTimer.restart();
        }
        
        @Override
        public void changedUpdate(DocumentEvent e) {
            // Restart timer for attribute changes (rarely used in plain text)
            searchTimer.restart();
        }
    });
    
    // Optional: Make Enter key trigger immediate search
    SearchField.addActionListener(e -> {
        searchTimer.stop(); // Stop the timer
        loadApplicants();   // Execute search immediately
    });
    
}
    private void setupEventListenersWithMinChars() {
    // Existing listeners
    searchbtn.addActionListener(e -> loadApplicants());
    statuscombo.addActionListener(e -> loadApplicants());
    scholarshipcombo.addActionListener(e -> loadApplicants()); 
    clearbtn.addActionListener(e -> clearFilters());
    
    // AUTO-SEARCH WITH MINIMUM CHARACTER REQUIREMENT
    Timer searchTimer = new Timer(500, e -> {
        String searchText = SearchField.getText().trim();
        // Only search if there are at least 2 characters or field is empty (to show all)
        if (searchText.length() >= 2 || searchText.isEmpty()) {
            loadApplicants();
        }
    });
    searchTimer.setRepeats(false);
    
    SearchField.getDocument().addDocumentListener(new DocumentListener() {
        private void triggerSearch() {
            searchTimer.restart();
        }
        
        @Override
        public void insertUpdate(DocumentEvent e) { triggerSearch(); }
        
        @Override
        public void removeUpdate(DocumentEvent e) { triggerSearch(); }
        
        @Override
        public void changedUpdate(DocumentEvent e) { triggerSearch(); }
    });
    
    // Enter key for immediate search
    SearchField.addActionListener(e -> {
        searchTimer.stop();
        loadApplicants();
    });
    
    // Rest of your existing button setup...
}
    

    private void deleteApplicant() {
    int selectedRow = ApplicantsTbl.getSelectedRow();
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select an applicant to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    int confirm = JOptionPane.showConfirmDialog(this, 
        "Are you sure you want to delete this applicant?", 
        "Confirm Delete", 
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE);
    
    if (confirm == JOptionPane.YES_OPTION) {
        try {
            // Get the applicant ID from the selected row (assuming it's in the first column)
            int applicantId = (int) ApplicantsTbl.getValueAt(selectedRow, 0);
            
            // Delete from database
            String sql = "DELETE FROM scholarship_applications WHERE id = ?";
            try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setInt(1, applicantId);
                int rowsDeleted = pstmt.executeUpdate();
                
                if (rowsDeleted > 0) {
                    JOptionPane.showMessageDialog(this, "Applicant deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadApplicants(); // Refresh the table
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete applicant.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
    
    private String getFilterSummary() {
    StringBuilder info = new StringBuilder();

    String scholarship = scholarshipcombo.getSelectedItem().toString();
    if (!"All".equalsIgnoreCase(scholarship)) {
        info.append("Scholarship: ").append(scholarship).append(" | ");
    }

    String status = statuscombo.getSelectedItem().toString();
    if (!"All".equalsIgnoreCase(status)) {
        info.append("Status: ").append(status).append(" | ");
    }

    String startDate = ((JTextField) startdate.getDateEditor().getUiComponent()).getText();
    String endDate = ((JTextField) enddate.getDateEditor().getUiComponent()).getText();
    if (!startDate.isEmpty() && !endDate.isEmpty()) {
        info.append("Date Range: ").append(startDate).append(" to ").append(endDate).append(" | ");
    }

    info.append("Total Applicants: ").append(ApplicantsTbl.getRowCount());
    return info.toString();
}
    
   private void exportApplicants() {
    String status = statuscombo.getSelectedItem().toString();
    String scholarship = scholarshipcombo.getSelectedItem().toString();

    Date dateFrom = startdate.getDate();
    Date dateTo = enddate.getDate();

    exportFilteredApplicants(status, scholarship, dateFrom, dateTo);
}
    
private void exportFilteredApplicants(String status, String scholarship, Date dateFrom, Date dateTo) {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setDialogTitle("Export Applicants");
    fileChooser.setAcceptAllFileFilterUsed(false);
    fileChooser.addChoosableFileFilter(new FileNameExtensionFilter("PDF File (*.pdf)", "pdf"));

    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".pdf")) {
            file = new File(file.getAbsolutePath() + ".pdf");
        }

        String[] columns = {
            "Id", "Scholarship", "Student Name", "Course", 
            "Year Level", "GWA", "Application Date", "Status"
        };
        List<Object[]> rows = new ArrayList<>();
        StringBuilder query = new StringBuilder(
            "SELECT id, scholarship_name, student_name, course, year_level, gwa, application_date, status " +
            "FROM scholarship_applications WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (!"All".equalsIgnoreCase(status)) {
            query.append(" AND status = ?");
            params.add(status);
        }
        if (!"All".equalsIgnoreCase(scholarship)) {
            query.append(" AND scholarship_name = ?");
            params.add(scholarship);
        }
        if (dateFrom != null && dateTo != null) {
            query.append(" AND application_date BETWEEN ? AND ?");
            params.add(new java.sql.Date(dateFrom.getTime()));
            params.add(new java.sql.Date(dateTo.getTime()));
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pst = conn.prepareStatement(query.toString())) {

            for (int i = 0; i < params.size(); i++) {
                pst.setObject(i + 1, params.get(i));
            }

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                rows.add(new Object[] {
                    String.valueOf(rs.getInt("id")), // This ensures it is a string, not a double
    rs.getString("scholarship_name"),
    rs.getString("student_name"),
    rs.getString("course"),
    rs.getString("year_level"),
    rs.getDouble("gwa"),
    rs.getDate("application_date"),
    rs.getString("status")
                });
            }

            StringBuilder filterInfo = new StringBuilder("Filters: ");
            if (!"All".equalsIgnoreCase(status)) filterInfo.append("Status - ").append(status).append("; ");
            if (!"All".equalsIgnoreCase(scholarship)) filterInfo.append("Scholarship - ").append(scholarship).append("; ");
            if (dateFrom != null && dateTo != null) {
    query.append(" AND application_date BETWEEN ? AND ?");
    params.add(new java.sql.Date(dateFrom.getTime()));
    params.add(new java.sql.Date(dateTo.getTime()));
} else if (dateFrom != null) {
    query.append(" AND application_date >= ?");
    params.add(new java.sql.Date(dateFrom.getTime()));
} else if (dateTo != null) {
    query.append(" AND application_date <= ?");
    params.add(new java.sql.Date(dateTo.getTime()));
}

            String cleanFilterInfo = filterInfo.toString().replaceAll("\\s*\\|\\s*$", "");
            String headerText = "LIST OF APPLICANTS" + System.lineSeparator() + cleanFilterInfo + System.lineSeparator() + "Total Applicants: " + rows.size();
            exportToPDF(file, headerText, columns, rows);
            JOptionPane.showMessageDialog(this, "List of Applicants exported to PDF successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting filtered applicants: " + e.getMessage());
        }
    }
}
// Shared PDF export method for table or filtered rows
private void exportToPDF(File file, String headerInfo, String[] columns, List<Object[]> rows) throws Exception {
    if (isFileLocked(file)) {
        JOptionPane.showMessageDialog(null, "Please close the PDF file before exporting.");
        return;
    }

    Document document = new Document(PageSize.A4.rotate());
    PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(file));

    writer.setPageEvent(new PdfPageEventHelper() {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, BaseColor.GRAY);
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            Rectangle pageSize = document.getPageSize();
            Phrase footer = new Phrase("Page " + writer.getPageNumber(), footerFont);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    footer, (pageSize.getLeft() + pageSize.getRight()) / 2, pageSize.getBottom() + 15, 0);
        }
    });

    document.open();;

    // Logo (optional)
    try {
    File logoFile = new File("resources/scholartrack_logo.png");
    if (logoFile.exists()) {
        Image logo = Image.getInstance(logoFile.getAbsolutePath());
        logo.scaleToFit(100, 100);
        logo.setAlignment(Image.ALIGN_CENTER);
        document.add(logo);
    }
} catch (Exception e) {
    System.err.println("Logo not loaded: " + e.getMessage());
}

    // Header title
    Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    Paragraph title = new Paragraph("SCHOLARTRACK", titleFont);
    title.setAlignment(Element.ALIGN_CENTER);
    document.add(title);

    // Meta information
    Paragraph meta = new Paragraph(headerInfo, FontFactory.getFont(FontFactory.HELVETICA, 12));
    meta.setSpacingBefore(10);
    meta.setSpacingAfter(20);
    document.add(meta);

    
    // Table
    PdfPTable pdfTable = new PdfPTable(columns.length);
    pdfTable.setWidthPercentage(100);
    Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, BaseColor.WHITE);
    Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
    BaseColor headerBg = new BaseColor(0, 121, 182); // blue

    
    
    for (String col : columns) {
        PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
        cell.setBackgroundColor(headerBg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(8);
        pdfTable.addCell(cell);
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy");

    for (Object[] row : rows) {
    for (Object value : row) {
        String cellText = "";
        if (value instanceof java.util.Date) {
            cellText = dateFormat.format(value);
        } else {
            try {
                cellText = String.format("%,.2f", Double.parseDouble(value.toString()));
            } catch (Exception ex) {
                cellText = value != null ? value.toString() : "";
            }
        }

        PdfPCell cell = new PdfPCell(new Phrase(cellText, cellFont));
        cell.setPadding(5);
        cell.setNoWrap(false);
        cell.setBorderWidth(1);
        cell.setBorderColor(BaseColor.GRAY);

        // Align numbers right, text left
        if (value instanceof Number) {
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        } else {
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        }

        pdfTable.addCell(cell);
    }
    }
    document.add(pdfTable);
    document.close();
}
    
    private boolean isFileLocked(File file) {
    if (!file.exists()) return false;
    File sameFile = new File(file.getAbsolutePath() + ".tmp");
    boolean locked = !file.renameTo(sameFile);
    if (!locked) {
        sameFile.renameTo(file);
    }
    return locked;
}

    private void clearFilters() {
        SearchField.setText("");
        statuscombo.setSelectedItem("All");
        scholarshipcombo.setSelectedItem("All");
        jDateChooser1.setDate(null);
        jDateChooser2.setDate(null);
        loadApplicants();
    }

    /**
     * Load available scholarship options into combo box
     */
    private void loadScholarshipOptions() {
        System.out.println("Loading scholarship options...");
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT DISTINCT scholarship_name FROM scholarship_applications ORDER BY scholarship_name");
             ResultSet rs = stmt.executeQuery()) {

            scholarshipcombo.removeAllItems();
            scholarshipcombo.addItem("All");
            
            int scholarshipCount = 0;
            while (rs.next()) {
                String scholarshipName = rs.getString("scholarship_name");
                if (scholarshipName != null && !scholarshipName.trim().isEmpty()) {
                    scholarshipcombo.addItem(scholarshipName);
                    scholarshipCount++;
                }
            }
            
            System.out.println("Loaded " + scholarshipCount + " scholarship options");

        } catch (SQLException e) {
            System.err.println("Error loading scholarship options: " + e.getMessage());
            e.printStackTrace();
            // Don't show dialog for this error as it's not critical
        }
    }

    /**
     * Get database connection
     */
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    /**
     * Load applicants data based on current filters
     */
    private void loadApplicants() {
        System.out.println("loadApplicants() called");
        
        DefaultTableModel model = (DefaultTableModel) ApplicantsTbl.getModel();
        model.setRowCount(0); // Clear existing rows

        // Handle potential null values during initialization
        String keyword = "";
        String status = "All";
        String scholarship = "All";
        java.util.Date fromDate = null;
        java.util.Date toDate = null;
        
        try {
            keyword = SearchField.getText() != null ? SearchField.getText().trim() : "";
            status = statuscombo.getSelectedItem() != null ? statuscombo.getSelectedItem().toString() : "All";
            scholarship = scholarshipcombo.getSelectedItem() != null ? scholarshipcombo.getSelectedItem().toString() : "All";
            fromDate = jDateChooser1.getDate();
            toDate = jDateChooser2.getDate();
        } catch (Exception e) {
            System.err.println("Error getting filter values: " + e.getMessage());
        }

        StringBuilder query = new StringBuilder("SELECT * FROM scholarship_applications WHERE 1=1");

        // Build dynamic query based on filters - using correct column names
        if (!keyword.isEmpty()) {
            query.append(" AND (student_name LIKE ? OR course LIKE ? OR student_number LIKE ?)");
        }
        if (!status.equals("All")) {
            query.append(" AND status = ?");
        }
        if (!scholarship.equals("All")) {
            query.append(" AND scholarship_name = ?");
        }
        if (fromDate != null && toDate != null) {
            query.append(" AND DATE(application_date) BETWEEN ? AND ?");
        }

        query.append(" ORDER BY application_date DESC"); // Add ordering
        
        System.out.println("Executing query: " + query.toString());

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {

            System.out.println("Database connection established");
            
            int paramIndex = 1;
            
            // Set parameters based on active filters
            if (!keyword.isEmpty()) {
                String searchPattern = "%" + keyword + "%";
                stmt.setString(paramIndex++, searchPattern); // student_name
                stmt.setString(paramIndex++, searchPattern); // course
                stmt.setString(paramIndex++, searchPattern); // student_number
            }
            if (!status.equals("All")) {
                stmt.setString(paramIndex++, status);
            }
            if (!scholarship.equals("All")) {
                stmt.setString(paramIndex++, scholarship);
            }
            if (fromDate != null && toDate != null) {
                stmt.setDate(paramIndex++, new java.sql.Date(fromDate.getTime()));
                stmt.setDate(paramIndex++, new java.sql.Date(toDate.getTime()));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                int rowCount = 0;
                while (rs.next()) {
                    // Parse GWA safely since it's stored as VARCHAR
                    String gwaStr = rs.getString("gwa");
                    double gwaValue = 0.0;
                    try {
                        gwaValue = Double.parseDouble(gwaStr);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid GWA value: " + gwaStr);
                    }
                    
                    model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("scholarship_name"),
                        rs.getString("student_name"), // Using student_name instead of first_name + last_name
                        rs.getString("course"),
                        rs.getString("year_level"),
                        gwaValue, // Safely parsed GWA
                        rs.getTimestamp("application_date"), // Using timestamp instead of date
                        rs.getString("status")
                    });
                    rowCount++;
                }
                System.out.println("Loaded " + rowCount + " records into table");
                
                if (rowCount == 0) {
                    System.out.println("No records found in database - check if table exists and has data");
                }
            }

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error loading applicants data: " + e.getMessage() + 
                "\n\nPlease check:\n1. Database is running\n2. Database name is correct\n3. Table 'scholarship_applications' exists\n4. Login credentials are correct", 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unexpected error: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    
    
    private void openApplicationDetailsDialog(int applicationId) {
    ApplicationDetailsDialog dialog = new ApplicationDetailsDialog(null, applicationId);
    dialog.setVisible(true);
    loadApplicants(); // Refresh the table after closing dialog

    }
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        ApplicantsTbl = new javax.swing.JTable();
        FilterByLbl = new javax.swing.JLabel();
        statuscombo = new javax.swing.JComboBox<>();
        statuslbl = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        jLabel2 = new javax.swing.JLabel();
        jDateChooser2 = new com.toedter.calendar.JDateChooser();
        jLabel3 = new javax.swing.JLabel();
        scholarshipcombo = new javax.swing.JComboBox<>();
        clearbtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        searchbtn = new javax.swing.JButton();
        SearchField = new javax.swing.JTextField();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        ApplicantsTbl.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Id", "Scholarship", "Student_Name", "Course", "Year Level", "GWA", "Application Date", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(ApplicantsTbl);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 190, 1010, 450));

        FilterByLbl.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        FilterByLbl.setText("Filter by:");
        jPanel1.add(FilterByLbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 90, -1, -1));

        statuscombo.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        statuscombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", "Pending", "Approved", "Rejected" }));
        statuscombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                statuscomboActionPerformed(evt);
            }
        });
        jPanel1.add(statuscombo, new org.netbeans.lib.awtextra.AbsoluteConstraints(140, 120, 110, -1));

        statuslbl.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        statuslbl.setText("Status:");
        jPanel1.add(statuslbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(90, 120, -1, 20));

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        jLabel1.setText("Date:");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(260, 120, -1, 20));
        jPanel1.add(jDateChooser1, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 120, -1, -1));

        jLabel2.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        jLabel2.setText("To");
        jPanel1.add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(410, 120, -1, 20));
        jPanel1.add(jDateChooser2, new org.netbeans.lib.awtextra.AbsoluteConstraints(440, 120, -1, -1));

        jLabel3.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        jLabel3.setText("Scholarship:");
        jPanel1.add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(560, 120, -1, 20));

        scholarshipcombo.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        scholarshipcombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "All", " " }));
        scholarshipcombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                scholarshipcomboActionPerformed(evt);
            }
        });
        jPanel1.add(scholarshipcombo, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 120, 90, -1));

        clearbtn.setBackground(new java.awt.Color(255, 102, 102));
        clearbtn.setFont(new java.awt.Font("Dialog", 1, 12)); // NOI18N
        clearbtn.setForeground(new java.awt.Color(255, 255, 255));
        clearbtn.setText("Clear");
        clearbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearbtnActionPerformed(evt);
            }
        });
        jPanel1.add(clearbtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(740, 120, 80, -1));

        jPanel2.setBackground(new java.awt.Color(153, 153, 0));

        lblTitle.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-applicants-40.png")); // NOI18N
        lblTitle.setText("Applicants");

        searchbtn.setBackground(new java.awt.Color(255, 234, 0));
        searchbtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        searchbtn.setText("Search");
        searchbtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchbtnActionPerformed(evt);
            }
        });

        SearchField.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        SearchField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                SearchFieldActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(lblTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 307, Short.MAX_VALUE)
                .addComponent(SearchField, javax.swing.GroupLayout.PREFERRED_SIZE, 392, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(searchbtn)
                .addGap(49, 49, 49))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, 58, Short.MAX_VALUE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(searchbtn)
                            .addComponent(SearchField, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1090, 70));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void statuscomboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_statuscomboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_statuscomboActionPerformed

    private void scholarshipcomboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_scholarshipcomboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_scholarshipcomboActionPerformed

    private void clearbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearbtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_clearbtnActionPerformed

    private void SearchFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_SearchFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_SearchFieldActionPerformed

    private void searchbtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchbtnActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_searchbtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable ApplicantsTbl;
    private javax.swing.JLabel FilterByLbl;
    private javax.swing.JTextField SearchField;
    private javax.swing.JButton clearbtn;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private com.toedter.calendar.JDateChooser jDateChooser2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JComboBox<String> scholarshipcombo;
    private javax.swing.JButton searchbtn;
    private javax.swing.JComboBox<String> statuscombo;
    private javax.swing.JLabel statuslbl;
    // End of variables declaration//GEN-END:variables
}

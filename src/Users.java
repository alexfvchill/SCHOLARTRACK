import com.itextpdf.text.BaseColor;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.RowFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Font;
import java.awt.BorderLayout;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Enhanced Users Management System with Export Functionality
 * Features: Real-time filtering, PDF export, user deletion, dynamic UI
 * @author alexa
 */
public class Users extends javax.swing.JInternalFrame {
    
    // Constants for database connection
    private static final String DB_URL = "jdbc:mysql://localhost:3306/scholartrack_db";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "12345";
    
    // UI Components
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField nameFilter;
    private JTextField emailFilter;
    private JTextField addressFilter;
    private JComboBox<String> schoolFilter;
    private JButton clearFiltersBtn;
    private JButton exportBtn;
    private JButton deleteUserBtn;
    private JLabel statusLabel;
    
    // Data storage
    private ArrayList<Object[]> allUserData;
    private JTable userTable;
    private DefaultTableModel userTableModel;

    public Users() {
        initComponents();
        initializeCustomComponents();
        setupUI();
        loadUserData();
    }
    
    private void initializeCustomComponents() {
        // Remove frame border and title bar
        this.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,0));
        BasicInternalFrameUI ui = (BasicInternalFrameUI)this.getUI();
        ui.setNorthPane(null);
        
        // Configure table
        jTable1.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        jTable1.getTableHeader().setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
        jTable1.clearSelection();
        
        // Initialize data storage
        allUserData = new ArrayList<>();
    }
    
    private void setupUI() {
        createFiltersPanel();
        setupTableSorter();
    }
    
    private void createFiltersPanel() {
        // Main filters panel
        JPanel filtersPanel = new JPanel();
        filtersPanel.setBackground(Color.WHITE);
        filtersPanel.setPreferredSize(new Dimension(850, 120));
        filtersPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Filters & Actions"));
        filtersPanel.setLayout(new BorderLayout());
        
        // Create filters section
        JPanel filtersSection = createFiltersSection();
        JPanel actionsSection = createActionsSection();
        
        filtersPanel.add(filtersSection, BorderLayout.NORTH);
        filtersPanel.add(actionsSection, BorderLayout.SOUTH);
        
        // Create status label
        statusLabel = new JLabel("Loading users...");
        statusLabel.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 11));
        statusLabel.setForeground(Color.DARK_GRAY);
        
        // Add to layout
        addFiltersToMainLayout(filtersPanel);
    }
    
    private JPanel createFiltersSection() {
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        leftPanel.setBackground(Color.WHITE);
        
        // Initialize filter components
        initializeFilterComponents();
        
        // Add components to panel
        leftPanel.add(new JLabel("Name:"));
        leftPanel.add(nameFilter);
        leftPanel.add(new JLabel("Email:"));
        leftPanel.add(emailFilter);
        leftPanel.add(new JLabel("Address:"));
        leftPanel.add(addressFilter);
        leftPanel.add(new JLabel("School:"));
        leftPanel.add(schoolFilter);
        leftPanel.add(clearFiltersBtn);
        
        return leftPanel;
    }
    
    private void initializeFilterComponents() {
        // Text filters
        nameFilter = createStandardTextField();
        emailFilter = createStandardTextField();
        addressFilter = createStandardTextField();
        
        // School dropdown
        schoolFilter = new JComboBox<>();
        schoolFilter.setPreferredSize(new Dimension(140, 25));
        schoolFilter.addItem("All Schools");
        
        // Clear button
        clearFiltersBtn = new JButton("Clear All");
        clearFiltersBtn.setPreferredSize(new Dimension(80, 25));
        clearFiltersBtn.setBackground(new Color(240, 240, 240));
        clearFiltersBtn.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
        
        // Add listeners
        addFilterListeners();
    }
    
    private JTextField createStandardTextField() {
        JTextField textField = new JTextField(12);
        textField.setPreferredSize(new Dimension(120, 25));
        return textField;
    }
    
    private void addFilterListeners() {
        DocumentListener filterListener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilters(); }
            public void removeUpdate(DocumentEvent e) { applyFilters(); }
            public void changedUpdate(DocumentEvent e) { applyFilters(); }
        };
        
        nameFilter.getDocument().addDocumentListener(filterListener);
        emailFilter.getDocument().addDocumentListener(filterListener);
        addressFilter.getDocument().addDocumentListener(filterListener);
        
        schoolFilter.addActionListener(e -> applyFilters());
        clearFiltersBtn.addActionListener(e -> clearAllFilters());
    }
    
    private JPanel createActionsSection() {
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        rightPanel.setBackground(Color.WHITE);
        
        // Export button
        exportBtn = createActionButton("Export", new Color(76, 175, 80), 
            "Export all users in PDF Format");
        exportBtn.addActionListener(e -> exportToPDF());
        
        // Delete button
        deleteUserBtn = createActionButton("Delete", new Color(244, 67, 54), 
            "Delete selected user");
        deleteUserBtn.addActionListener(this::handleDeleteUser);
        
        rightPanel.add(exportBtn);
        rightPanel.add(deleteUserBtn);
        
        return rightPanel;
    }
    
    private JButton createActionButton(String text, Color bgColor, String tooltip) {
        JButton button = new JButton(text);
        button.setPreferredSize(new Dimension(70, 30));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 12));
        button.setToolTipText(tooltip);
        return button;
    }
    
    private void handleDeleteUser(ActionEvent e) {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            showMessage("Please select a user to delete.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            int modelRow = jTable1.convertRowIndexToModel(selectedRow);
            String idStr = jTable1.getModel().getValueAt(modelRow, 0).toString();
            int studentId = Integer.parseInt(idStr);
            
            int confirm = JOptionPane.showConfirmDialog(null, 
                "Are you sure you want to delete this user?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                deleteUserFromDatabase(studentId);
            }
        } catch (NumberFormatException ex) {
            showMessage("Invalid user ID format.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addFiltersToMainLayout(JPanel filtersPanel) {
        // Get the existing GroupLayout and update it
        javax.swing.GroupLayout jPanel1Layout = (javax.swing.GroupLayout) jPanel1.getLayout();
        
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(52, 52, 52)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(filtersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 968, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 968, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(ViewRequestBtn))
                            .addComponent(statusLabel))))
                .addContainerGap(58, Short.MAX_VALUE))
        );
        
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(20, 20, 20)
                .addComponent(filtersPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(ViewRequestBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 420, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(statusLabel)
                .addContainerGap(30, Short.MAX_VALUE))
        );
    }
    
    private void setupTableSorter() {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        sorter = new TableRowSorter<>(model);
        jTable1.setRowSorter(sorter);
        
        // Apply initial filter state
        applyFilters();
    }
    
    private void applyFilters() {
        String name = nameFilter.getText().trim();
        String email = emailFilter.getText().trim();
        String address = addressFilter.getText().trim();
        String school = (String) schoolFilter.getSelectedItem();

        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        // Based on your table structure: student_id, student_number, name, school, email, address
        // Column indices: 0=student_id, 1=student_number, 2=name, 3=school, 4=email, 5=address
        
        if (!name.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(name), 2)); // Name column (index 2)
        }
        if (!email.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(email), 4)); // Email column (index 4)
        }
        if (!address.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(address), 5)); // Address column (index 5)
        }
        if (school != null && !school.equals("All Schools")) {
            filters.add(RowFilter.regexFilter("(?i)^" + Pattern.quote(school) + "$", 3)); // School column (index 3)
        }

        // Apply combined filter or clear if no filters
        if (filters.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters));
        }

        updateFilterStatus();
    }
    
    private void clearAllFilters() {
        // Clear all filter inputs
        nameFilter.setText("");
        emailFilter.setText("");
        addressFilter.setText("");
        schoolFilter.setSelectedIndex(0); // Reset to "All Schools"
        
        // Clear the table filter
        sorter.setRowFilter(null);
        
        // Update status
        statusLabel.setText("Filters cleared. Showing all users.");
    }
    
    private void updateFilterStatus() {
        int totalRows = jTable1.getModel().getRowCount();
        int visibleRows = jTable1.getRowCount();
        
        if (visibleRows == totalRows) {
            statusLabel.setText("Showing all " + totalRows + " users");
        } else {
            statusLabel.setText("Showing " + visibleRows + " of " + totalRows + " users (filtered)");
        }
    }
    
    private void deleteUserFromDatabase(int studentId) {
        String query = "DELETE FROM students WHERE student_id = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, studentId);
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                showMessage("User deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadUserData(); // Refresh table
            } else {
                showMessage("User not found or could not be deleted.", "Delete Failed", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException ex) {
            showMessage("Database error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void exportToPDF() {
        File selectedFile = selectExportFile();
        if (selectedFile == null) return;
        
        if (selectedFile.exists() && !confirmOverwrite()) {
            return;
        }
        
        try {
            generatePDFReport(selectedFile);
            showMessage("PDF file exported successfully!", "Export Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            showMessage("Error exporting to PDF: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    private File selectExportFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save PDF File");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF Files", "pdf"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getAbsolutePath().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            return file;
        }
        return null;
    }
    
    private boolean confirmOverwrite() {
        return JOptionPane.showConfirmDialog(this,
                "File already exists. Overwrite?",
                "Confirm Overwrite",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }
    
    private void generatePDFReport(File file) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();
        
        try {
            // Add title
            addPDFTitle(document);
            
            // Add timestamp
            addPDFTimestamp(document);
            
            // Add table
            addPDFTable(document);
            
        } finally {
            document.close();
        }
    }
    
    private void addPDFTitle(Document document) throws Exception {
        Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
        Paragraph title = new Paragraph("SCHOLARTRACK - Users List", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20f);
        document.add(title);
    }
    
    private void addPDFTimestamp(Document document) throws Exception {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        Font infoFont = new Font(Font.FontFamily.HELVETICA, 10);
        Paragraph info = new Paragraph("Exported on: " + timestamp, infoFont);
        info.setAlignment(Element.ALIGN_RIGHT);
        info.setSpacingAfter(10f);
        document.add(info);
    }
    
    private void addPDFTable(Document document) throws Exception {
        int columnCount = jTable1.getColumnCount();
        PdfPTable table = new PdfPTable(columnCount);
        table.setWidthPercentage(100f);
        
        // Set equal column widths
        float[] widths = new float[columnCount];
        Arrays.fill(widths, 100f / columnCount);
        table.setWidths(widths);
        
        // Add headers
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        for (int i = 0; i < columnCount; i++) {
            PdfPCell headerCell = new PdfPCell(new Phrase(jTable1.getColumnName(i), headerFont));
            headerCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            headerCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(headerCell);
        }
        
        // Add data rows
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 10);
        for (int row = 0; row < jTable1.getRowCount(); row++) {
            for (int col = 0; col < columnCount; col++) {
                Object value = jTable1.getValueAt(row, col);
                PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value.toString(), cellFont));
                table.addCell(cell);
            }
        }
        
        document.add(table);
    }
    
    public void loadUserData() {
        String sql = "SELECT student_id, student_number, name, school, email, address FROM students ORDER BY name";
        
        try (Connection con = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            allUserData.clear();
            
            Set<String> schools = new HashSet<>();
            
            while (rs.next()) {
                Object[] rowData = {
                    rs.getInt("student_id"),
                    rs.getString("student_number"),
                    rs.getString("name"),
                    rs.getString("school"),
                    rs.getString("email"),
                    rs.getString("address")
                };
                
                model.addRow(rowData);
                allUserData.add(rowData);
                
                String school = rs.getString("school");
                if (school != null && !school.trim().isEmpty()) {
                    schools.add(school);
                }
            }
            
            updateSchoolFilter(schools);
            
            // Reset filters and update status
            statusLabel.setText("Loaded " + model.getRowCount() + " users successfully");
            
            // Apply any existing filters
            applyFilters();
            
        } catch (SQLException e) {
            showMessage("Failed to load user data: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void updateSchoolFilter(Set<String> schools) {
        String currentSelection = (String) schoolFilter.getSelectedItem();
        schoolFilter.removeAllItems();
        schoolFilter.addItem("All Schools");
        
        for (String school : schools) {
            schoolFilter.addItem(school);
        }
        
        // Restore selection if it still exists
        if (currentSelection != null && schools.contains(currentSelection)) {
            schoolFilter.setSelectedItem(currentSelection);
        }
    }
    
    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }
    
    /**
     * Debug method to print current filter states
     * Useful for troubleshooting filter issues
     */
    private void debugFilterState() {
        System.out.println("=== Filter Debug Info ===");
        System.out.println("Name filter: '" + nameFilter.getText() + "'");
        System.out.println("Email filter: '" + emailFilter.getText() + "'");
        System.out.println("Address filter: '" + addressFilter.getText() + "'");
        System.out.println("School filter: '" + schoolFilter.getSelectedItem() + "'");
        System.out.println("Total rows in model: " + jTable1.getModel().getRowCount());
        System.out.println("Visible rows after filter: " + jTable1.getRowCount());
        System.out.println("========================");
    }
    
    /**
     * Test method to verify that filters work with sample data
     */
    public void testFilters() {
        System.out.println("Testing filter functionality...");
        
        // Test name filter
        nameFilter.setText("John");
        applyFilters();
        System.out.println("Name filter 'John': " + jTable1.getRowCount() + " results");
        
        // Clear and test email filter
        clearAllFilters();
        emailFilter.setText("@gmail");
        applyFilters();
        System.out.println("Email filter '@gmail': " + jTable1.getRowCount() + " results");
        
        // Clear and test school filter
        clearAllFilters();
        if (schoolFilter.getItemCount() > 1) {
            schoolFilter.setSelectedIndex(1); // Select first non-"All Schools" option
            applyFilters();
            System.out.println("School filter '" + schoolFilter.getSelectedItem() + "': " + jTable1.getRowCount() + " results");
        }
        
        // Clear all filters
        clearAllFilters();
        System.out.println("All filters cleared: " + jTable1.getRowCount() + " results");
    }
    

/**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jLabel2 = new javax.swing.JLabel();
        ViewRequestBtn = new javax.swing.JButton();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        jLabel1.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-user-groups-40.png")); // NOI18N
        jLabel1.setText("Users Management");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Student_id", "Student number", "Name", "School", "Email", "Address"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jLabel2.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jLabel2.setText("Registered Users:");

        ViewRequestBtn.setBackground(new java.awt.Color(255, 234, 0));
        ViewRequestBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        ViewRequestBtn.setText("View Request");
        ViewRequestBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ViewRequestBtnMouseClicked(evt);
            }
        });
        ViewRequestBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ViewRequestBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(52, 52, 52)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 968, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(ViewRequestBtn)))
                .addContainerGap(88, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel1)
                .addGap(53, 53, 53)
                .addComponent(ViewRequestBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(19, 19, 19)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 417, Short.MAX_VALUE)
                .addGap(72, 72, 72))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents



    private void ViewRequestBtnMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ViewRequestBtnMouseClicked
       
    }//GEN-LAST:event_ViewRequestBtnMouseClicked

    private void ViewRequestBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ViewRequestBtnActionPerformed
    AccountRequests requestForm = new AccountRequests();
    this.getDesktopPane().add(requestForm); // Only works if 'Users' is in a JDesktopPane
    requestForm.setVisible(true);
    }//GEN-LAST:event_ViewRequestBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ViewRequestBtn;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}

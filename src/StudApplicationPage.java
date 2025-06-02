import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.sql.*;
import java.text.SimpleDateFormat;
import UserSession.UserSession;
import java.util.function.BiConsumer;

public class StudApplicationPage extends javax.swing.JInternalFrame {
    
    // Database connection details
    private static final String DB_URL = "jdbc:mysql://localhost:3306/scholartrack_db";
    private static final String DB_USER = "root"; // Update with your username
    private static final String DB_PASSWORD = "12345"; // Update with your password
    
    // Application data model based on actual database schema
    public static class Application {
        private int id;
        private String scholarshipName;
        private String studentName;
        private String studentNumber;
        private String email;
        private String phone;
        private String birthDate;
        private String gender;
        private String address;
        private String school;
        private String course;
        private String yearLevel;
        private String gwa;
        private String totalFamilyIncome;
        private String applicationDate;
        private String status;
        private String uploadedFiles;
        private String rejectionReason;
        
       public Application(int id, String scholarshipName, String studentName, String studentNumber,
                   String email, String phone, String birthDate, String gender, String address,
                   String school, String course, String yearLevel, String gwa, String totalFamilyIncome,
                   String applicationDate, String status, String uploadedFiles, String rejectionReason) {
    this.id = id;
    this.scholarshipName = scholarshipName;
    this.studentName = studentName;
    this.studentNumber = studentNumber;
    this.email = email;
    this.phone = phone;
    this.birthDate = birthDate;
    this.gender = gender;
    this.address = address;
    this.school = school;
    this.course = course;
    this.yearLevel = yearLevel;
    this.gwa = gwa;
    this.totalFamilyIncome = totalFamilyIncome;
    this.applicationDate = applicationDate;
    this.status = status;
    this.uploadedFiles = uploadedFiles;
    this.rejectionReason = rejectionReason;
}

        
        // Getters
        public int getId() { return id; }
        public String getScholarshipName() { return scholarshipName; }
        public String getStudentName() { return studentName; }
        public String getStudentNumber() { return studentNumber; }
        public String getEmail() { return email; }
        public String getPhone() { return phone; }
        public String getBirthDate() { return birthDate; }
        public String getGender() { return gender; }
        public String getAddress() { return address; }
        public String getSchool() { return school; }
        public String getCourse() { return course; }
        public String getYearLevel() { return yearLevel; }
        public String getGwa() { return gwa; }
        public String getTotalFamilyIncome() { return totalFamilyIncome; }
        public String getApplicationDate() { return applicationDate; }
        public String getStatus() { return status; }
        public String getUploadedFiles() { return uploadedFiles; }
        public String getRejectionReason() { return rejectionReason; }
       
        
        // Setters
        public void setStatus(String status) { this.status = status; }
    }
    
    private DefaultTableModel tableModel;
    private JTable applicationsTable;
    private List<Application> applications;
    private JComboBox<String> statusFilterCombo;
    private JButton viewDetailsButton;
private int currentStudentId;
private String rejectionReason;

public StudApplicationPage() {
    this(UserSession.getStudentId()); // get student ID from session
}

public StudApplicationPage(int studentId) {
    this.currentStudentId = studentId;

    initComponents();
    setupCustomUI();
    initializeCustomComponents();
    loadStudentApplications();
}

    
    /**
     * Custom UI setup - removes title bar and borders
     */
    private void setupCustomUI() {
        this.setBorder(javax.swing.BorderFactory.createEmptyBorder(0,0,0,0));
        BasicInternalFrameUI ui = (BasicInternalFrameUI)this.getUI();
        ui.setNorthPane(null);
    }
    
    /**
     * Initialize custom components that weren't created by the form designer
     */
    private void initializeCustomComponents() {
        // Initialize the applications table
        applicationsTable = new JTable();
        applicationsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Add double-click listener for viewing details
        applicationsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Application selectedApp = getSelectedApplication();
                    if (selectedApp != null) {
                        showApplicationSummary(selectedApp);
                    }
                }
            }
        });
        
        jScrollPane1.setViewportView(applicationsTable);
        
        // Create and setup filter panel
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.setBackground(Color.WHITE);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel filterLabel = new JLabel("Filter by Status:");
        filterLabel.setFont(new Font("Dialog", Font.BOLD, 12));
        
        statusFilterCombo = new JComboBox<>(new String[]{"All", "PENDING", "APPROVED", "REJECTED"});
        statusFilterCombo.addActionListener(e -> filterApplicationsByStatus());
        
        
        viewDetailsButton = new JButton("View Details");
        viewDetailsButton.addActionListener(e -> {
            Application selectedApp = getSelectedApplication();
            if (selectedApp != null) {
                showApplicationSummary(selectedApp);
            } else {
                JOptionPane.showMessageDialog(this, "Please select an application to view details.", 
                    "No Selection", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        
        
        filterPanel.add(filterLabel);
        filterPanel.add(statusFilterCombo);
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(viewDetailsButton);
        
        
        // Create a container panel for the header section
        JPanel headerPanel = new JPanel(new BorderLayout());
headerPanel.setBackground(Color.WHITE);
headerPanel.add(jLabel1, BorderLayout.WEST);
headerPanel.add(filterPanel, BorderLayout.EAST);
        
     JPanel tableWrapperPanel = new JPanel(new BorderLayout());
tableWrapperPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 30)); // top, left, bottom, right
tableWrapperPanel.add(jScrollPane1, BorderLayout.CENTER);


        // Update the main panel layout
        jPanel1.removeAll();
jPanel1.setLayout(new BorderLayout());
jPanel1.add(headerPanel, BorderLayout.NORTH);
jPanel1.add(tableWrapperPanel, BorderLayout.CENTER); // Add padded wrapper
jPanel1.revalidate();
jPanel1.repaint();
    }
    
    /**
     * Load applications for the current student from database
     */
    private void loadStudentApplications() {
        applications = new ArrayList<>();
        
      String query = """
SELECT id, student_id, scholarship_name, student_name, student_number, email, phone, 
       birth_date, gender, address, school, course, year_level, gwa, 
       total_family_income, application_date, status, uploaded_files, rejection_reason
FROM scholarship_applications 
WHERE student_id = ?
ORDER BY application_date DESC
""";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            
            pstmt.setInt(1, currentStudentId);
            
            ResultSet rs = pstmt.executeQuery();
            
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy");
            
            while (rs.next()) {
                String formattedDate;
                try {
                    Timestamp timestamp = rs.getTimestamp("application_date");
                    formattedDate = timestamp != null ? outputFormat.format(timestamp) : "N/A";
                } catch (Exception e) {
                    formattedDate = "N/A";
                }
                
                String formattedBirthDate;
                try {
                    Date birthDate = rs.getDate("birth_date");
                    formattedBirthDate = birthDate != null ? outputFormat.format(birthDate) : "N/A";
                } catch (Exception e) {
                    formattedBirthDate = "N/A";
                }
                
               Application app = new Application(
    rs.getInt("id"),
    rs.getString("scholarship_name") != null ? rs.getString("scholarship_name") : "N/A",
    rs.getString("student_name") != null ? rs.getString("student_name") : "N/A",
    rs.getString("student_number") != null ? rs.getString("student_number") : "N/A",
    rs.getString("email") != null ? rs.getString("email") : "N/A",
    rs.getString("phone") != null ? rs.getString("phone") : "N/A",
    formattedBirthDate,
    rs.getString("gender") != null ? rs.getString("gender") : "N/A",
    rs.getString("address") != null ? rs.getString("address") : "N/A",
    rs.getString("school") != null ? rs.getString("school") : "N/A",
    rs.getString("course") != null ? rs.getString("course") : "N/A",
    rs.getString("year_level") != null ? rs.getString("year_level") : "N/A",
    rs.getString("gwa") != null ? rs.getString("gwa") : "0",
    rs.getString("total_family_income") != null ? rs.getString("total_family_income") : "N/A",
    formattedDate,
    rs.getString("status") != null ? rs.getString("status") : "PENDING",
    rs.getString("uploaded_files") != null ? rs.getString("uploaded_files") : "N/A",
    rs.getString("rejection_reason") != null ? rs.getString("rejection_reason") : ""
);
                applications.add(app);
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Error loading applications from database: " + e.getMessage(),
                "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        
        // Setup table model
        String[] columnNames = {"ID", "Scholarship", "Student Name", "Course", 
                               "Year Level", "GWA", "Status", "Application Date"};
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table read-only
            }
        };
        
        // Populate table with data
        for (Application app : applications) {
            Object[] rowData = {
                app.getId(),
                app.getScholarshipName(),
                app.getStudentName(),
                app.getCourse(),
                app.getYearLevel(),
                app.getGwa() + "%",
                app.getStatus(),
                app.getApplicationDate()
            };
            tableModel.addRow(rowData);
        }
        
        applicationsTable.setModel(tableModel);
        setupTableAppearance();
    }
    
    private void setupTableAppearance() {
    applicationsTable.setRowHeight(35);
    applicationsTable.setFont(new Font("Dialog", Font.PLAIN, 12));
    applicationsTable.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 12));
    applicationsTable.getTableHeader().setBackground(new Color(70, 130, 180));
    applicationsTable.getTableHeader().setForeground(Color.BLACK);
    applicationsTable.setGridColor(new Color(220, 220, 220));
    applicationsTable.setSelectionBackground(new Color(184, 207, 229));

    TableColumnModel columnModel = applicationsTable.getColumnModel();
    columnModel.getColumn(0).setPreferredWidth(60);
    columnModel.getColumn(1).setPreferredWidth(150);
    columnModel.getColumn(2).setPreferredWidth(150);
    columnModel.getColumn(3).setPreferredWidth(100);
    columnModel.getColumn(4).setPreferredWidth(80);
    columnModel.getColumn(5).setPreferredWidth(50);
    columnModel.getColumn(6).setPreferredWidth(80);
    columnModel.getColumn(7).setPreferredWidth(120);

    applicationsTable.getColumnModel().getColumn(6).setCellRenderer(new StatusCellRenderer());
    
}
    
    /**
     * Custom cell renderer for status column with color coding
     */
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                String status = value != null ? value.toString() : "";
                switch (status.toUpperCase()) {
                    case "APPROVED":
                        c.setBackground(new Color(220, 255, 220)); // Light green
                        setForeground(new Color(0, 100, 0)); // Dark green
                        break;
                    case "REJECTED":
                        c.setBackground(new Color(255, 220, 220)); // Light red
                        setForeground(new Color(150, 0, 0)); // Dark red
                        break;
                    case "UNDER REVIEW":
                        c.setBackground(new Color(255, 255, 220)); // Light yellow
                        setForeground(new Color(150, 100, 0)); // Dark yellow
                        break;
                    case "PENDING":
                        c.setBackground(new Color(220, 220, 255)); // Light blue
                        setForeground(new Color(0, 0, 150)); // Dark blue
                        break;
                    default:
                        c.setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                }
            }
            
            setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }
    }
    
    /**
     * Get selected application details
     */
    public Application getSelectedApplication() {
        int selectedRow = applicationsTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < applications.size()) {
            // Get the application ID from the table model
            int appId = (Integer) tableModel.getValueAt(selectedRow, 0);
            // Find the corresponding application in the list
            return applications.stream()
                    .filter(app -> app.getId() == appId)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
    
    /**
     * Refresh applications data
     */
    public void refreshApplications() {
        loadStudentApplications();
        statusFilterCombo.setSelectedIndex(0); // Reset filter to "All"
    }
    
    private void filterApplicationsByStatus() {
        String selectedStatus = (String) statusFilterCombo.getSelectedItem();
        
        // Clear current table
        tableModel.setRowCount(0);
        
        // Add filtered data
        for (Application app : applications) {
            if ("All".equals(selectedStatus) || app.getStatus().equalsIgnoreCase(selectedStatus)) {
                Object[] rowData = {
                    app.getId(),
                    app.getScholarshipName(),
                    app.getStudentName(),
                    app.getCourse(),
                    app.getYearLevel(),
                    app.getGwa() + "%",
                    app.getStatus(),
                    app.getApplicationDate()
                };
                tableModel.addRow(rowData);
            }
        }
    }
    
    /**
     * Show detailed view of selected application
     */
class RoundedPanel extends JPanel {
    private int cornerRadius;

    public RoundedPanel(int radius) {
        this.cornerRadius = radius;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Dimension arcs = new Dimension(cornerRadius, cornerRadius);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arcs.width, arcs.height);
    }
}

    private void showApplicationSummary(Application app) {
    JDialog dialog = new JDialog((Frame) null, "Application Summary", true);
    dialog.setSize(600, 600);
    dialog.setLocationRelativeTo(null);
    dialog.setUndecorated(true); // Frameless
    dialog.setBackground(new Color(0, 0, 0, 0)); // Transparent background

    Color primary = new Color(52, 120, 246);

    // Rounded white panel to simulate modern card
    RoundedPanel outerPanel = new RoundedPanel(20);
    outerPanel.setLayout(new BorderLayout());
    outerPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
    outerPanel.setBackground(Color.WHITE);

    JLabel title = new JLabel("Scholarship Application Summary");
    title.setFont(new Font("Dialog", Font.BOLD, 20));
    title.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
    outerPanel.add(title, BorderLayout.NORTH);

    JPanel contentPanel = new JPanel(new GridLayout(0, 2, 20, 10));
    contentPanel.setBackground(Color.WHITE);

    String[] labels = {
        "Application ID", "Scholarship Name", "Student Name", "Student Number",
        "Email", "Phone", "Birth Date", "Gender", "Address", "School",
        "Course", "Year Level", "GWA", "Family Income", "Status",
        "Application Date", "Uploaded Files"
    };

    String[] values = {
        String.valueOf(app.getId()), app.getScholarshipName(), app.getStudentName(),
        app.getStudentNumber(), app.getEmail(), app.getPhone(), app.getBirthDate(),
        app.getGender(), app.getAddress(), app.getSchool(), app.getCourse(),
        app.getYearLevel(), app.getGwa() + "%", app.getTotalFamilyIncome(),
        app.getStatus(), app.getApplicationDate(), app.getUploadedFiles()
    };

    for (int i = 0; i < labels.length; i++) {
        JPanel fieldPanel = new JPanel(new BorderLayout());
        fieldPanel.setBackground(Color.WHITE);

        JLabel label = new JLabel(labels[i] + ":");
        label.setFont(new Font("Dialog", Font.BOLD, 13));

        JLabel value = new JLabel(values[i]);
        value.setFont(new Font("Dialog", Font.PLAIN, 13));
        value.setForeground(new Color(60, 60, 60));

        fieldPanel.add(label, BorderLayout.NORTH);
        fieldPanel.add(value, BorderLayout.CENTER);
        contentPanel.add(fieldPanel);
    }

    if ("REJECTED".equalsIgnoreCase(app.getStatus())
            && app.getRejectionReason() != null
            && !app.getRejectionReason().isEmpty()) {

        JPanel rejectionPanel = new JPanel(new BorderLayout());
        rejectionPanel.setBackground(Color.WHITE);

        JLabel rejLabel = new JLabel("Rejection Reason:");
        rejLabel.setFont(new Font("Dialog", Font.BOLD, 13));
        rejLabel.setForeground(Color.RED);

        JLabel rejValue = new JLabel("<html><p style='width:520px;'>" + app.getRejectionReason() + "</p></html>");
        rejValue.setFont(new Font("Dialog", Font.PLAIN, 13));
        rejValue.setForeground(Color.RED);

        rejectionPanel.add(rejLabel, BorderLayout.NORTH);
        rejectionPanel.add(rejValue, BorderLayout.CENTER);

        JPanel fullRowPanel = new JPanel(new BorderLayout());
        fullRowPanel.setBackground(Color.WHITE);
        fullRowPanel.add(rejectionPanel, BorderLayout.CENTER);

        contentPanel.add(fullRowPanel);
        contentPanel.add(new JLabel()); // fill second column
    }

    outerPanel.add(contentPanel, BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
    buttonPanel.setBackground(Color.WHITE);

    JButton okButton = new JButton("OK");
    okButton.setFocusPainted(false);
    okButton.setBackground(primary);
    okButton.setForeground(Color.WHITE);
    okButton.setFont(new Font("Dialog", Font.BOLD, 14));
    okButton.setPreferredSize(new Dimension(100, 30));
    okButton.addActionListener(e -> dialog.dispose());

    buttonPanel.add(okButton);
    outerPanel.add(buttonPanel, BorderLayout.SOUTH);

    dialog.setContentPane(outerPanel);
    dialog.setVisible(true);
}


/**
 * Extracts file names from the uploadedFiles string and adds emoji/icon indicators.
 */
private String[] getFileNamesWithIcons(String uploadedFiles) {
    List<String> fileList = new ArrayList<>();
    if (uploadedFiles != null && !uploadedFiles.trim().isEmpty()) {
        String[] paths = uploadedFiles.split(";");
        for (String path : paths) {
            path = path.trim();
            if (!path.isEmpty()) {
                int lastSlash = path.lastIndexOf('/');
                String fileName = (lastSlash >= 0) ? path.substring(lastSlash + 1) : path;
                String icon = getIconForFile(fileName);
                fileList.add(icon + " " + fileName);
            }
        }
    }
    return fileList.toArray(new String[0]);
}

/**
 * Returns an emoji icon based on file extension.
 */
private String getIconForFile(String fileName) {
    String file = fileName.toLowerCase();
    if (file.endsWith(".pdf")) return "📄";
    if (file.endsWith(".doc") || file.endsWith(".docx")) return "📝";
    if (file.endsWith(".jpg") || file.endsWith(".jpeg") || file.endsWith(".png")) return "🖼️";
    if (file.endsWith(".xls") || file.endsWith(".xlsx")) return "📊";
    if (file.endsWith(".zip") || file.endsWith(".rar")) return "🗜️";
    return "📁"; // Default
}

    
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();

        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(1080, 730));

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        jLabel1.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-applications-40.png")); // NOI18N
        jLabel1.setText("Applications");

        jScrollPane1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(55, 55, 55)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 948, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jLabel1)))
                .addContainerGap(87, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 76, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 496, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(94, 94, 94))
        );

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1090, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}

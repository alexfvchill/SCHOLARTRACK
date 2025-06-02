import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.DefaultTableModel;
import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class AccountRequests extends javax.swing.JInternalFrame {
private javax.swing.JLabel searchTitleLabel;
private javax.swing.JTextField searchNameField;
private javax.swing.JTextField searchSchoolField;
private javax.swing.JTextField searchAddressField;
    public AccountRequests() {
        initComponents();
        this.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        BasicInternalFrameUI ui = (BasicInternalFrameUI) this.getUI();
        ui.setNorthPane(null);
        loadPendingRequests(null, null, null, null); // Initial load with no filters
        
        approveBtn.addActionListener(e -> handleApproval("approved"));
        rejectBtn.addActionListener(e -> handleApproval("rejected"));
        

searchTitleLabel = new javax.swing.JLabel();
searchNameField = new javax.swing.JTextField();
searchSchoolField = new javax.swing.JTextField();
searchAddressField = new javax.swing.JTextField();

// Set up title label
searchTitleLabel.setFont(new java.awt.Font("Dialog", 1, 12));
searchTitleLabel.setForeground(new java.awt.Color(0, 0, 0));
searchTitleLabel.setText("Search & Filter by(Name, School, Address)");
jPanel1.add(searchTitleLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 120, -1, -1));

// Search Fields
searchNameField.setToolTipText("Search by Name");
jPanel1.add(searchNameField, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 120, 160, 25));

searchSchoolField.setToolTipText("Search by School");
jPanel1.add(searchSchoolField, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 120, 160, 25));

searchAddressField.setToolTipText("Search by Address");
jPanel1.add(searchAddressField, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 120, 160, 25));
jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 170, 960, 470));
        // Add search functionality with real-time filtering
        setupSearchListeners();
    }
    
    private void setupSearchListeners() {
        // Real-time search as user types
        KeyAdapter searchListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performSearch();
            }
        };
        
        searchNameField.addKeyListener(searchListener);
        searchSchoolField.addKeyListener(searchListener);
        searchAddressField.addKeyListener(searchListener);
    }
    
    private void performSearch() {
        String nameFilter = searchNameField.getText().trim();
        String schoolFilter = searchSchoolField.getText().trim();
        String addressFilter = searchAddressField.getText().trim();
        
        loadPendingRequestsWithFilters(nameFilter, schoolFilter, addressFilter);
    }
   
    private void loadPendingRequests(String schoolFilter, java.util.Date dateFrom, java.util.Date dateTo, String keyword) {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");

            StringBuilder sql = new StringBuilder("SELECT student_number, name, school, email, status, address, created_at FROM student_requests WHERE status = 'pending'");
            if (schoolFilter != null && !schoolFilter.equals("Please select your School")) {
                sql.append(" AND school = ?");
            }
            if (dateFrom != null && dateTo != null) {
                sql.append(" AND created_at BETWEEN ? AND ?");
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                sql.append(" AND (name LIKE ? OR email LIKE ?)");
            }

            PreparedStatement pst = con.prepareStatement(sql.toString());

            int index = 1;
            if (schoolFilter != null && !schoolFilter.equals("Please select your School")) {
                pst.setString(index++, schoolFilter);
            }
            if (dateFrom != null && dateTo != null) {
                pst.setDate(index++, new java.sql.Date(dateFrom.getTime()));
                pst.setDate(index++, new java.sql.Date(dateTo.getTime()));
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                String likeKeyword = "%" + keyword + "%";
                pst.setString(index++, likeKeyword);
                pst.setString(index++, likeKeyword);
            }

            ResultSet rs = pst.executeQuery();
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("student_number"),
                    rs.getString("name"),
                    rs.getString("school"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("status")
                });
            }

            rs.close();
            pst.close();
            con.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading requests:\n" + e.getMessage());
        }
    }
    
    private void loadPendingRequestsWithFilters(String nameFilter, String schoolFilter, String addressFilter) {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");

            StringBuilder sql = new StringBuilder("SELECT student_number, name, school, email, status, address, created_at FROM student_requests WHERE status = 'pending'");
            
            // Add filters if they are not empty
            if (nameFilter != null && !nameFilter.isEmpty()) {
                sql.append(" AND name LIKE ?");
            }
            if (schoolFilter != null && !schoolFilter.isEmpty()) {
                sql.append(" AND school LIKE ?");
            }
            if (addressFilter != null && !addressFilter.isEmpty()) {
                sql.append(" AND address LIKE ?");
            }

            PreparedStatement pst = con.prepareStatement(sql.toString());

            int index = 1;
            if (nameFilter != null && !nameFilter.isEmpty()) {
                pst.setString(index++, "%" + nameFilter + "%");
            }
            if (schoolFilter != null && !schoolFilter.isEmpty()) {
                pst.setString(index++, "%" + schoolFilter + "%");
            }
            if (addressFilter != null && !addressFilter.isEmpty()) {
                pst.setString(index++, "%" + addressFilter + "%");
            }

            ResultSet rs = pst.executeQuery();
            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("student_number"),
                    rs.getString("name"),
                    rs.getString("school"),
                    rs.getString("email"),
                    rs.getString("address"),
                    rs.getString("status")
                });
            }

            rs.close();
            pst.close();
            con.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading requests:\n" + e.getMessage());
        }
    }

    private void updateStatus(String studentNumber, String newStatus) {
        try {
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
            
            // Step 1: Update status in student_requests
            String updateSql = "UPDATE student_requests SET status = ? WHERE student_number = ?";
            PreparedStatement updatePst = con.prepareStatement(updateSql);
            updatePst.setString(1, newStatus);
            updatePst.setString(2, studentNumber);
            updatePst.executeUpdate();

            if ("approved".equalsIgnoreCase(newStatus)) {
                // Check if email already exists in students table
                String emailCheckSql = "SELECT email FROM students WHERE email = (SELECT email FROM student_requests WHERE student_number = ?)";
                PreparedStatement emailCheckPst = con.prepareStatement(emailCheckSql);
                emailCheckPst.setString(1, studentNumber);
                ResultSet emailRs = emailCheckPst.executeQuery();

                if (emailRs.next()) {
                    JOptionPane.showMessageDialog(this, "Email already exists in the system.");
                    emailRs.close();
                    emailCheckPst.close();
                    return; // Stop here, don't approve
                }
                emailRs.close();
                emailCheckPst.close();

                // Fetch student data from student_requests
                String selectSql = "SELECT * FROM student_requests WHERE student_number = ?";
                PreparedStatement selectPst = con.prepareStatement(selectSql);
                selectPst.setString(1, studentNumber);
                ResultSet rs = selectPst.executeQuery();

                if (rs.next()) {
                    String name = rs.getString("name");
                    String school = rs.getString("school");
                    String email = rs.getString("email");
                    String address = rs.getString("address");
                    String password = rs.getString("password");

                    // Insert into students table
                    String insertSql = "INSERT INTO students (student_number, name, school, email, address, password, profile_pic) VALUES (?, ?, ?, ?, ?, ?, ?)";
                    PreparedStatement insertPst = con.prepareStatement(insertSql);
                    insertPst.setString(1, studentNumber);
                    insertPst.setString(2, name);
                    insertPst.setString(3, school);
                    insertPst.setString(4, email);
                    insertPst.setString(5, address);
                    insertPst.setString(6, password);
                    insertPst.setString(7, "null"); // You can change this later to a default picture path

                    insertPst.executeUpdate();
                    insertPst.close();
                }

                rs.close();
                selectPst.close();

                // Delete from student_requests
                String deleteSql = "DELETE FROM student_requests WHERE student_number = ?";
                PreparedStatement deletePst = con.prepareStatement(deleteSql);
                deletePst.setString(1, studentNumber);
                deletePst.executeUpdate();
                deletePst.close();
            }

            updatePst.close();
            con.close();

            // Refresh with current filters
            performSearch();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating status:\n" + e.getMessage());
        }
    }
    
    private void handleApproval(String status) {
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow != -1) {
            String studentNumber = jTable1.getValueAt(selectedRow, 0).toString();
            String action = status.equalsIgnoreCase("approved") ? "approve" : "reject";

            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to " + action + " this request?",
                "Confirm " + action.substring(0, 1).toUpperCase() + action.substring(1),
                JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                updateStatus(studentNumber, status);
                JOptionPane.showMessageDialog(this, "Request " + status + " successfully.");
            }
       
        }

    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        approveBtn = new javax.swing.JButton();
        rejectBtn = new javax.swing.JButton();
        backBtn = new javax.swing.JButton();
        jLabel8 = new javax.swing.JLabel();

        setMaximumSize(new java.awt.Dimension(1084, 720));
        setMinimumSize(new java.awt.Dimension(1084, 720));
        setPreferredSize(new java.awt.Dimension(1084, 720));

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setMaximumSize(new java.awt.Dimension(1084, 720));
        jPanel1.setMinimumSize(new java.awt.Dimension(1084, 720));
        jPanel1.setPreferredSize(new java.awt.Dimension(1084, 720));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jTable1.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "Student Number", "Name", "School", "Email", "Address", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 180, 960, 450));

        approveBtn.setBackground(new java.awt.Color(255, 234, 0));
        approveBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        approveBtn.setText("Approve");
        approveBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                approveBtnActionPerformed(evt);
            }
        });
        jPanel1.add(approveBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(820, 640, -1, -1));

        rejectBtn.setBackground(new java.awt.Color(255, 51, 51));
        rejectBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        rejectBtn.setForeground(new java.awt.Color(255, 255, 255));
        rejectBtn.setText("Reject");
        rejectBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rejectBtnActionPerformed(evt);
            }
        });
        jPanel1.add(rejectBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(930, 640, 90, -1));

        backBtn.setBackground(new java.awt.Color(204, 204, 204));
        backBtn.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        backBtn.setText("Back");
        backBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backBtnActionPerformed(evt);
            }
        });
        jPanel1.add(backBtn, new org.netbeans.lib.awtextra.AbsoluteConstraints(60, 640, -1, -1));

        jLabel8.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(0, 0, 0));
        jLabel8.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-user-groups-40 (1).png")); // NOI18N
        jLabel8.setText("User Request");
        jPanel1.add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 20, -1, 60));

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

    private void rejectBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rejectBtnActionPerformed
         handleApproval("rejected");
    }//GEN-LAST:event_rejectBtnActionPerformed

    private void backBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backBtnActionPerformed
       this.dispose(); // Close the current StudProfilePage

    Users usersPage = new Users(); // Create a new instance of Users.java
    getParent().add(usersPage); // Add it to the JDesktopPane
    usersPage.setVisible(true); // Show the Users frame
    }//GEN-LAST:event_backBtnActionPerformed

    private void approveBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_approveBtnActionPerformed
        handleApproval("approved");
    }//GEN-LAST:event_approveBtnActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton approveBtn;
    private javax.swing.JButton backBtn;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton rejectBtn;
    // End of variables declaration//GEN-END:variables
}

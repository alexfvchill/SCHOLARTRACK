import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.SwingUtilities;
import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.DefaultTableModel;
import java.sql.*;

public class StudDashPage extends javax.swing.JInternalFrame {
    
    private String studentId;
    private Connection conn;

    private JButton notificationButton;
    private DefaultListModel<String> unreadModel = new DefaultListModel<>();
    private DefaultListModel<String> readModel = new DefaultListModel<>();
    private JList<String> unreadList = new JList<>(unreadModel);
    private JList<String> readList = new JList<>(readModel);

    public StudDashPage(String studentId) {
        this.studentId = studentId;
    initComponents();
    setupUI();
    connectDatabase(); // Ensure connection is created
    setupNotificationButton();
    refreshNotificationCount();

    displayScholarshipCount();
    displaySubmittedApplicationsCount();
    displayAwardedScholarshipsCount();
    loadAvailableScholarships();
    
     int delay = 5000; // 30 seconds
    Timer notificationTimer = new Timer(delay, e -> refreshNotificationCount());
    notificationTimer.setInitialDelay(0); // optional: trigger immediately
    notificationTimer.start();
    }

    private void setupUI() {
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        ((BasicInternalFrameUI) this.getUI()).setNorthPane(null);
        jTable1.getTableHeader().setFont(new Font("Dialog", Font.BOLD, 14));
    }

    private void connectDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Database connection failed:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void displayScholarshipCount() {
        String query = "SELECT COUNT(*) AS total FROM scholarships WHERE status = 'Open'";
        try (PreparedStatement pst = conn.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {
            if (rs.next()) {
                jLabel5.setText(String.valueOf(rs.getInt("total")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displaySubmittedApplicationsCount() {
        String query = "SELECT COUNT(*) AS total FROM scholarship_applications WHERE student_id = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, studentId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    jLabel7.setText(String.valueOf(rs.getInt("total")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayAwardedScholarshipsCount() {
        String query = "SELECT COUNT(*) AS total FROM scholarship_applications WHERE student_id = ? AND status = 'Approved'";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, studentId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    jLabel9.setText(String.valueOf(rs.getInt("total")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadAvailableScholarships() {
        String query = "SELECT name, description, deadline, status FROM scholarships WHERE status = 'Open'";
        try (PreparedStatement pst = conn.prepareStatement(query);
             ResultSet rs = pst.executeQuery()) {

            DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("deadline"),
                        rs.getString("status")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupNotificationButton() {
        notificationButton = new JButton();
        notificationButton.setFocusPainted(false);
        notificationButton.setContentAreaFilled(false);
        notificationButton.setBorderPainted(false);
        notificationButton.setFont(new Font("Dialog", Font.BOLD, 25));
        notificationButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        notificationButton.addActionListener(e -> showNotificationDialog());

        if (jPanel1.getLayout() instanceof org.netbeans.lib.awtextra.AbsoluteLayout) {
            jPanel1.add(notificationButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(950, 20, 100, 30));
        } else {
            jPanel1.setLayout(new FlowLayout(FlowLayout.RIGHT));
            jPanel1.add(notificationButton);
        }
    }
private void refreshNotificationCount() {
    if (conn == null || studentId == null || studentId.isEmpty()) return;

    String query = "SELECT COUNT(*) AS total FROM student_notifications WHERE student_id = ? AND is_read = FALSE";

    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
        pstmt.setString(1, studentId); // Set this BEFORE executing

        try (ResultSet rs = pstmt.executeQuery()) {
            int count = rs.next() ? rs.getInt("total") : 0;

            String text = count > 0 ? "🔔 (" + count + ")" : "🔔";
            notificationButton.setText(text);
            Font currentFont = notificationButton.getFont();
            notificationButton.setFont(currentFont.deriveFont(20f));
            notificationButton.setToolTipText(count > 0 ? count + " unread notifications" : "No unread notifications");
        }
    } catch (SQLException e) {
        notificationButton.setText("🔔 ⚠️");
        notificationButton.setToolTipText("Error fetching notifications");
        e.printStackTrace(); // Optional for debugging
    }
}

    private void showNotificationDialog() {
        NotificationDialog dialog = new NotificationDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        refreshNotificationCount();
    }

    private class NotificationDialog extends JDialog {
        private JLabel statusLabel = new JLabel("Loading notifications...");

        public NotificationDialog(Window parent) {
            super(parent, "Notifications", Dialog.ModalityType.APPLICATION_MODAL);
            setupDialog();
            loadNotifications();
        }

        private void setupDialog() {
            setSize(600, 500);
            setLocationRelativeTo(getParent());
            setLayout(new BorderLayout());

            add(statusLabel, BorderLayout.NORTH);

            JTabbedPane tabs = new JTabbedPane();
            unreadList.setCellRenderer(new NotificationListCellRenderer());
            readList.setCellRenderer(new NotificationListCellRenderer());

            tabs.addTab("Unread", new JScrollPane(unreadList));
            tabs.addTab("Read", new JScrollPane(readList));
            add(tabs, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton markAllBtn = new JButton("Mark All as Read");
            JButton deleteBtn = new JButton("Delete Selected");
            JButton refreshBtn = new JButton("Refresh");
            JButton closeBtn = new JButton("Close");

            markAllBtn.addActionListener(e -> {
                markNotificationsAsRead();
                loadNotifications();
            });

            deleteBtn.addActionListener(e -> {
                JList<String> currentList = tabs.getSelectedIndex() == 0 ? unreadList : readList;
                String selected = currentList.getSelectedValue();
                if (selected != null) {
                    if (JOptionPane.showConfirmDialog(this, "Delete this notification?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        deleteNotificationByContent(selected);
                        loadNotifications();
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Select a notification to delete.");
                }
            });

            refreshBtn.addActionListener(e -> loadNotifications());
            closeBtn.addActionListener(e -> dispose());

            buttonPanel.add(markAllBtn);
            buttonPanel.add(deleteBtn);
            buttonPanel.add(refreshBtn);
            buttonPanel.add(closeBtn);
            add(buttonPanel, BorderLayout.SOUTH);
        }

   private void loadNotifications() {
    unreadModel.clear();
    readModel.clear();
    statusLabel.setText("Loading...");

    String query = "SELECT id, message, timestamp, is_read FROM student_notifications WHERE student_id = ? ORDER BY timestamp DESC";
    try (PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, studentId);
        try (ResultSet rs = stmt.executeQuery()) {
            int unreadCount = 0, readCount = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                String msg = rs.getString("message");
                String timestamp = rs.getTimestamp("timestamp").toString();
                String formatted = "[" + timestamp + "] " + msg + "||" + id;

                boolean isRead = rs.getBoolean("is_read");
                if (!isRead) {
                    unreadModel.addElement(formatted);
                    unreadCount++;
                } else {
                    readModel.addElement(formatted);
                    readCount++;
                }
            }

            statusLabel.setText("Unread: " + unreadCount + " | Read: " + readCount);
        }
    } catch (SQLException e) {
        statusLabel.setText("Error loading notifications");
        JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
    }
}

  private void markNotificationsAsRead() {
    try (PreparedStatement stmt = conn.prepareStatement(
            "UPDATE student_notifications SET is_read = TRUE WHERE student_id = ? AND is_read = FALSE")) {
        stmt.setString(1, studentId);
        int updated = stmt.executeUpdate();
        if (updated > 0) {
            JOptionPane.showMessageDialog(this, "All notifications marked as read.");
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Mark as read failed:\n" + e.getMessage());
    }
}
  private void deleteNotificationByContent(String raw) {
        if (raw == null || !raw.contains("||")) return;
        String[] parts = raw.split("\\|\\|");
        try {
            int id = Integer.parseInt(parts[1]);
            try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM student_notifications WHERE id = ?")) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Notification deleted.");
            }
        } catch (SQLException | NumberFormatException e) {
            System.err.println("Delete error: " + e.getMessage());
        }
    }

        private class NotificationListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String val = value.toString();
            if (val.contains("||")) val = val.split("\\|\\|")[0];
            super.getListCellRendererComponent(list, val, index, isSelected, cellHasFocus);
            setFont(getFont().deriveFont(list == unreadList ? Font.BOLD : Font.PLAIN));
            setForeground(list == unreadList ? Color.BLUE : Color.GRAY);
            return this;
        }
    }
}

    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jPanel3 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();

        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setPreferredSize(new java.awt.Dimension(1080, 730));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        jLabel1.setText("Dashboard");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 20, -1, -1));

        jPanel2.setBackground(new java.awt.Color(255, 234, 0));

        jPanel5.setBackground(new java.awt.Color(153, 153, 0));

        jLabel3.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-scholarship-40.png")); // NOI18N
        jLabel3.setText("Scholarships");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel3)
                .addContainerGap(79, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
        );

        jLabel5.setFont(new java.awt.Font("Dialog", 1, 48)); // NOI18N
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jLabel6.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Total Available Scholarships");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel6)
                .addGap(40, 40, 40))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel6)
                .addGap(0, 30, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 80, 280, 200));

        jPanel3.setBackground(new java.awt.Color(255, 234, 0));

        jPanel6.setBackground(new java.awt.Color(153, 153, 0));

        jLabel2.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-applications-40.png")); // NOI18N
        jLabel2.setText("Applications");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addContainerGap(85, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 54, Short.MAX_VALUE)
        );

        jLabel7.setFont(new java.awt.Font("Dialog", 1, 48)); // NOI18N
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jLabel8.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Total Application Submitted");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(36, 36, 36))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel8)
                .addGap(29, 29, 29))
        );

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 80, 280, 200));

        jPanel4.setBackground(new java.awt.Color(255, 234, 0));

        jPanel7.setBackground(new java.awt.Color(153, 153, 0));

        jLabel4.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-award-40.png")); // NOI18N
        jLabel4.setText("Awarded");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addContainerGap(129, Short.MAX_VALUE))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, 56, Short.MAX_VALUE)
        );

        jLabel9.setFont(new java.awt.Font("Dialog", 1, 48)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jLabel10.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("Scholarship Awarded");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel10)
                .addGap(58, 58, 58))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 67, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel10)
                .addGap(0, 28, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(730, 80, 280, 200));

        jLabel11.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jLabel11.setText("Available Scholarship");
        jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 310, -1, -1));

        jTable1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jTable1.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Name", "Description", "Deadline", "Status"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(jTable1);

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(50, 350, 960, 310));

        getContentPane().add(jPanel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1100, 700));

        pack();
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}

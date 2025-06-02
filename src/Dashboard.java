import java.sql.*;
import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.table.DefaultTableModel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Dashboard extends javax.swing.JInternalFrame {

    
    private JButton notificationButton;
    private Connection conn;
    private ExecutorService executorService;
    private DefaultListModel<String> unreadModel = new DefaultListModel<>();
private DefaultListModel<String> readModel = new DefaultListModel<>();
private JList<String> unreadList = new JList<>(unreadModel);
private JList<String> readList = new JList<>(readModel);
    // Connection pool or connection management
    private static final String DB_URL = "jdbc:mysql://localhost:3306/scholartrack_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "12345";

    public Dashboard() {
        initComponents();
        setupUI();
        initializeDatabase();
        setupNotificationButton();
        loadInitialData();
    }

    private void setupUI() {
        jTable2.getTableHeader().setFont(new java.awt.Font("Dialog", java.awt.Font.BOLD, 14));
        this.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        BasicInternalFrameUI ui = (BasicInternalFrameUI) this.getUI();
        ui.setNorthPane(null);
        
        // Initialize thread pool for background tasks
        executorService = Executors.newFixedThreadPool(3);
    }

    private void initializeDatabase() {
        try {
            // Consider using connection pooling for production applications
            conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            
            // Test connection
            if (!conn.isValid(5)) {
                throw new SQLException("Database connection is not valid");
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, 
                "Failed to connect to database: " + e.getMessage(),
                "Database Connection Error", 
                JOptionPane.ERROR_MESSAGE);
            
            // Consider implementing retry logic or graceful degradation
            handleDatabaseConnectionFailure();
        }
    }

    private void handleDatabaseConnectionFailure() {
        // Disable database-dependent features or show offline mode
        SwingUtilities.invokeLater(() -> {
            lblTotalScholarships.setText("N/A");
            lblTotalUsers.setText("N/A");
            jLabelTotalApplicants.setText("N/A");
            
            // Show a warning message
            JLabel warningLabel = new JLabel("⚠️ Database connection failed - some features disabled");
            warningLabel.setForeground(Color.RED);
            jPanel1.add(warningLabel, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 60, 500, 20));
        });
    }

    private void setupNotificationButton() {
        notificationButton = new JButton();
        notificationButton.setFocusPainted(false);
        notificationButton.setContentAreaFilled(false);
        notificationButton.setBorderPainted(false);
        notificationButton.setFont(new Font("Dialog", Font.BOLD, 13));
        notificationButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        notificationButton.addActionListener(e -> showNotificationDialog());

        // Ensure jPanel1 has proper layout
        if (jPanel1.getLayout() instanceof org.netbeans.lib.awtextra.AbsoluteLayout) {
            // Add notification button with absolute positioning
            jPanel1.add(notificationButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(950, 20, 100, 30));
        } else {
            jPanel1.setLayout(new FlowLayout(FlowLayout.RIGHT));
            jPanel1.add(notificationButton);
        }
    }

    private void loadInitialData() {
        // Load data asynchronously to prevent UI blocking
        CompletableFuture.runAsync(this::updateTotalScholarships, executorService)
            .thenRun(() -> SwingUtilities.invokeLater(this::updateTotalUsers))
            .thenRun(() -> SwingUtilities.invokeLater(this::updateTotalApplicants))
            .thenRun(() -> SwingUtilities.invokeLater(this::loadScholarshipTable))
            .thenRun(() -> SwingUtilities.invokeLater(this::refreshNotificationCount))
            .exceptionally(throwable -> {
                SwingUtilities.invokeLater(() -> 
                    JOptionPane.showMessageDialog(this, 
                        "Error loading dashboard data: " + throwable.getMessage(),
                        "Loading Error", 
                        JOptionPane.WARNING_MESSAGE));
                return null;
            });
    }

    private void showNotificationDialog() {
        NotificationDialog dialog = new NotificationDialog(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
        refreshNotificationCount();
    }
private void refreshNotificationCount() {
    if (conn == null) return;

    String query = "SELECT COUNT(*) AS total FROM notifications WHERE recipient_type = 'admin' AND status = 'unread'";
    
    try (PreparedStatement pstmt = conn.prepareStatement(query);
         ResultSet rs = pstmt.executeQuery()) {

        int unreadCount = 0;
        if (rs.next()) {
            unreadCount = rs.getInt("total");
        }

        final int count = unreadCount;
        SwingUtilities.invokeLater(() -> {
            String text = count > 0 ? "🔔 (" + count + ")" : "🔔";
            notificationButton.setText(text);
            Font currentFont = notificationButton.getFont();
            notificationButton.setFont(currentFont.deriveFont(20f));
            notificationButton.setToolTipText(count > 0 ?
                count + " unread notifications" : "No unread notifications");
        });

    } catch (SQLException e) {
        System.err.println("Error fetching notifications: " + e.getMessage());
        SwingUtilities.invokeLater(() -> {
            notificationButton.setText("🔔 ⚠️");
            notificationButton.setToolTipText("Error fetching notification count");
        });
    }
}


    private int getUserCountFromDB() throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM students";
        try (PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    private int getPendingApplicationCountFromDB() throws SQLException {
        String query = "SELECT COUNT(*) AS total FROM scholarship_applications WHERE status = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "Pending");
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    public void updateTotalScholarships() {
        if (conn == null) return;
        
        String query = "SELECT COUNT(*) AS total FROM scholarships WHERE status = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "Open");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    final int count = rs.getInt("total");
                    SwingUtilities.invokeLater(() -> lblTotalScholarships.setText(String.valueOf(count)));
                }
            }
        } catch (SQLException e) {
            handleDatabaseError("updating scholarship count", e);
        }
    }

    public void updateTotalUsers() {
        if (conn == null) return;
        
        try {
            int count = getUserCountFromDB();
            SwingUtilities.invokeLater(() -> lblTotalUsers.setText(String.valueOf(count)));
        } catch (SQLException e) {
            handleDatabaseError("updating user count", e);
        }
    }

    public void updateTotalApplicants() {
        if (conn == null) return;
        
        try {
            int count = getPendingApplicationCountFromDB();
            SwingUtilities.invokeLater(() -> jLabelTotalApplicants.setText(String.valueOf(count)));
        } catch (SQLException e) {
            handleDatabaseError("updating applicant count", e);
        }
    }

    public void loadScholarshipTable() {
        if (conn == null) return;
        
        DefaultTableModel model = (DefaultTableModel) jTable2.getModel();
        
        String query = "SELECT name, description, requirements, deadline FROM scholarships WHERE status = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, "Open");
            try (ResultSet rs = pstmt.executeQuery()) {
                
                SwingUtilities.invokeLater(() -> model.setRowCount(0));
                
                while (rs.next()) {
                    Object[] row = {
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("requirements"),
                        rs.getString("deadline")
                    };
                    
                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }
            }
        } catch (SQLException e) {
            handleDatabaseError("loading scholarship table", e);
        }
    }

    private void handleDatabaseError(String operation, SQLException e) {
        String message = "Database error while " + operation + ": " + e.getMessage();
        System.err.println(message);
        
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, message, "Database Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    // Add refresh functionality
    public void refreshDashboard() {
        loadInitialData();
    }

    // Cleanup method - call this when closing the dashboard
    public void cleanup() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }

    // Enhanced NotificationDialog with better error handling
    private class NotificationDialog extends JDialog {
        private DefaultListModel<String> notificationModel = new DefaultListModel<>();
        private JList<String> notificationList = new JList<>(notificationModel);
        private JLabel statusLabel = new JLabel("Loading notifications...");

        public NotificationDialog(Window parent) {
            super(parent, "Notifications", ModalityType.APPLICATION_MODAL);
            setupDialog();
            loadNotifications();
        }

        private void setupDialog() {
    setSize(600, 500);
    setLocationRelativeTo(getParent());
    setLayout(new BorderLayout());

    // Status
    add(statusLabel, BorderLayout.NORTH);

    // Tabbed view for unread/read
    JTabbedPane tabs = new JTabbedPane();

    unreadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    unreadList.setCellRenderer(new NotificationListCellRenderer());

    readList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    readList.setCellRenderer(new NotificationListCellRenderer());

    tabs.addTab("Unread", new JScrollPane(unreadList));
    tabs.addTab("Read", new JScrollPane(readList));
    add(tabs, BorderLayout.CENTER);

    // Buttons
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
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this notification?",
            "Confirm Deletion",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            deleteNotificationByContent(selected);
            loadNotifications();
        } else if (confirm == JOptionPane.NO_OPTION) {
            JOptionPane.showMessageDialog(this, "Notification was not deleted.");
        }
        // If Cancel is clicked, do nothing silently
    } else {
        JOptionPane.showMessageDialog(this, "Please select a notification to delete.");
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
    if (conn == null) {
        statusLabel.setText("No DB connection");
        return;
    }

    unreadModel.clear();
    readModel.clear();
    statusLabel.setText("Loading...");

    String sql = "SELECT id, message, created_at, status FROM notifications ORDER BY created_at DESC";

    try (PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        int unreadCount = 0, readCount = 0;

        while (rs.next()) {
            int id = rs.getInt("id");
            String message = rs.getString("message");
            Timestamp time = rs.getTimestamp("created_at");
            String status = rs.getString("status");

            String formatted = String.format("[%s] %s", time.toString(), message);
            formatted += "||" + id;  // For internal deletion tracking

            if ("unread".equalsIgnoreCase(status)) {
                unreadModel.addElement(formatted);
                unreadCount++;
            } else {
                readModel.addElement(formatted);
                readCount++;
            }
        }

        statusLabel.setText("Unread: " + unreadCount + " | Read: " + readCount);

    } catch (SQLException e) {
        statusLabel.setText("Error loading notifications");
        JOptionPane.showMessageDialog(this, "Failed to load notifications:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}

        private void markNotificationsAsRead() {
            if (conn == null) return;
            
            String query = "UPDATE notifications SET status = 'read' WHERE status = 'unread'";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                int updatedRows = pstmt.executeUpdate();
                System.out.println("Marked " + updatedRows + " notifications as read");
            } catch (SQLException e) {
                System.err.println("Error marking notifications as read: " + e.getMessage());
            }
        }
    }

    private void deleteNotificationByContent(String raw) {
    if (conn == null || raw == null || !raw.contains("||")) return;

    String[] parts = raw.split("\\|\\|");
    if (parts.length < 2) return;

    int id = Integer.parseInt(parts[1]);

    String sql = "DELETE FROM notifications WHERE id = ?";
    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, id);
        stmt.executeUpdate();
        System.out.println("Deleted notification ID: " + id);
    } catch (SQLException e) {
        System.err.println("Delete error: " + e.getMessage());
    }
}
    
    
    private class NotificationListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {
        String val = value.toString();
        if (val.contains("||")) val = val.split("\\|\\|")[0];

        super.getListCellRendererComponent(list, val, index, isSelected, cellHasFocus);

        if (list == unreadList) {
            setFont(getFont().deriveFont(Font.BOLD));
            setForeground(Color.BLUE);
        } else {
            setFont(getFont().deriveFont(Font.PLAIN));
            setForeground(Color.DARK_GRAY);
        }

        return this;
    }
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
        jPanel3 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblTotalScholarships = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabelTotalApplicants = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        lblTotalUsers = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();

        setBackground(new java.awt.Color(255, 255, 255));
        setMaximizable(true);
        setResizable(true);
        setAutoscrolls(true);
        setMaximumSize(new java.awt.Dimension(1084, 720));
        setMinimumSize(new java.awt.Dimension(1084, 720));
        setPreferredSize(new java.awt.Dimension(1084, 720));
        setVisible(true);

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setMaximumSize(new java.awt.Dimension(1097, 640));
        jPanel1.setMinimumSize(new java.awt.Dimension(1097, 640));
        jPanel1.setName(""); // NOI18N
        jPanel1.setPreferredSize(new java.awt.Dimension(1097, 640));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        jLabel1.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-dashboard-40.png")); // NOI18N
        jLabel1.setText("Dashboard");
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 20, 240, -1));

        jPanel3.setBackground(new java.awt.Color(255, 234, 0));
        jPanel3.setForeground(new java.awt.Color(255, 255, 255));

        jPanel5.setBackground(new java.awt.Color(153, 153, 0));

        jLabel2.setFont(new java.awt.Font("Microsoft YaHei UI", 1, 24)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-scholarship-40.png")); // NOI18N
        jLabel2.setText("Scholarships");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblTotalScholarships.setFont(new java.awt.Font("Nirmala UI", 1, 48)); // NOI18N
        lblTotalScholarships.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jLabel3.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("total numbers of scholarships");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap(53, Short.MAX_VALUE)
                .addComponent(jLabel3)
                .addGap(48, 48, 48))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTotalScholarships, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(lblTotalScholarships, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addGap(0, 27, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 80, 310, 200));

        jPanel6.setBackground(new java.awt.Color(255, 234, 0));

        jPanel8.setBackground(new java.awt.Color(153, 153, 0));

        jLabel4.setFont(new java.awt.Font("Microsoft YaHei UI", 1, 24)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-applicants-40.png")); // NOI18N
        jLabel4.setText("Applications");

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel4)
                .addContainerGap(99, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabelTotalApplicants.setFont(new java.awt.Font("Nirmala UI", 1, 48)); // NOI18N
        jLabelTotalApplicants.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jLabel9.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel9.setText("total number of applicantions");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelTotalApplicants, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel9)
                .addGap(49, 49, 49))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21)
                .addComponent(jLabelTotalApplicants, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel9)
                .addGap(0, 26, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(390, 80, 310, 200));

        jPanel9.setBackground(new java.awt.Color(255, 234, 0));

        jPanel10.setBackground(new java.awt.Color(153, 153, 0));

        jLabel5.setFont(new java.awt.Font("Microsoft YaHei UI", 1, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-user-groups-40.png")); // NOI18N
        jLabel5.setText("Users");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel5)
                .addContainerGap(185, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel5)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblTotalUsers.setFont(new java.awt.Font("Nirmala UI", 1, 48)); // NOI18N
        lblTotalUsers.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

        jLabel10.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel10.setText("total number of users");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTotalUsers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel10)
                .addGap(75, 75, 75))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(21, 21, 21)
                .addComponent(lblTotalUsers, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel10)
                .addGap(0, 20, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(750, 80, 310, 200));

        jLabel11.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jLabel11.setText("Scholarships Available");
        jPanel1.add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 300, 440, -1));

        jScrollPane2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jScrollPane2.setFont(new java.awt.Font("Dialog", 0, 14)); // NOI18N

        jTable2.setAutoCreateRowSorter(true);
        jTable2.setBorder(javax.swing.BorderFactory.createTitledBorder(""));
        jTable2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Name", "Description", "Requirements", "Deadline"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jTable2.setToolTipText("");
        jTable2.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        jTable2.setRowHeight(30);
        jTable2.setShowGrid(true);
        jScrollPane2.setViewportView(jTable2);
        if (jTable2.getColumnModel().getColumnCount() > 0) {
            jTable2.getColumnModel().getColumn(0).setHeaderValue("Name");
            jTable2.getColumnModel().getColumn(1).setHeaderValue("Description");
            jTable2.getColumnModel().getColumn(2).setHeaderValue("Requirements");
            jTable2.getColumnModel().getColumn(3).setHeaderValue("Deadline");
        }

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(40, 340, 1020, 300));

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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelTotalApplicants;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable2;
    private javax.swing.JLabel lblTotalScholarships;
    private javax.swing.JLabel lblTotalUsers;
    // End of variables declaration//GEN-END:variables
}

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import javax.swing.border.TitledBorder;

public class ApplicationDetailsDialog extends JDialog {
    private int applicationId;
    private JPanel infoPanel, docPanel;
    private JTextArea specialCircumstancesArea;
    private JTextArea historyArea;
    private int studentId;
    private static final String DB_URL = "jdbc:mysql://localhost:3306/scholartrack_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "12345";
    
    private static final String FONT_NAME = "Dialog";
private static final int FONT_SIZE = 14;
private static final int FONT_STYLE_LABEL = Font.BOLD;
private static final int FONT_STYLE_VALUE = Font.PLAIN;

    public ApplicationDetailsDialog(Frame parent, int applicationId) {
        super(parent, "Application Details", true);
        this.applicationId = applicationId;
        setSize(800, 700);
        setLocationRelativeTo(parent);
        initComponents();
        loadApplicationDetails();
        loadRejectionHistory(); 
    }

    private void initComponents() {
    setLayout(new BorderLayout(15, 15));

    JPanel content = new JPanel();
    content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS)); // Stack vertically
    content.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

    // === Applicant Info Panel ===
    infoPanel = new JPanel(new GridBagLayout());
    TitledBorder border = BorderFactory.createTitledBorder("Applicant Information");
    border.setTitleFont(new Font("Dialog", Font.BOLD, 16));
    infoPanel.setBorder(border);
    JScrollPane infoScroll = new JScrollPane(infoPanel);
    infoScroll.setPreferredSize(new Dimension(750, 300)); // Optional: adjust as needed
    content.add(infoScroll);

    // === Uploaded Documents Panel ===
    docPanel = new JPanel();
    docPanel.setLayout(new BoxLayout(docPanel, BoxLayout.Y_AXIS));
    TitledBorder docBorder = BorderFactory.createTitledBorder("Uploaded Documents");
    docBorder.setTitleFont(new Font("Dialog", Font.BOLD, 16));
    docPanel.setBorder(docBorder);
    JScrollPane docScroll = new JScrollPane(docPanel);
    docScroll.setPreferredSize(new Dimension(750, 150));
    content.add(docScroll);

    // === Rejection History Panel ===
    JPanel historyPanel = new JPanel(new BorderLayout());
    TitledBorder historyBorder = BorderFactory.createTitledBorder("Rejection Reason History");
    historyBorder.setTitleFont(new Font("Dialog", Font.BOLD, 16));
    historyPanel.setBorder(historyBorder);

    JTextArea historyArea = new JTextArea(6, 50);
    historyArea.setEditable(false);
    historyArea.setFont(new Font("Dialog", Font.PLAIN, 13));
    JScrollPane historyScroll = new JScrollPane(historyArea);
    historyPanel.add(historyScroll, BorderLayout.CENTER);
    historyPanel.setPreferredSize(new Dimension(750, 120));
    content.add(historyPanel);

    // Store for later use
    this.historyArea = historyArea;

    // === Bottom Buttons ===
    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    JButton approveBtn = new JButton("Approve");
    JButton rejectBtn = new JButton("Reject");
    JButton cancelBtn = new JButton("Cancel"); 
    approveBtn.setBackground(new Color(0, 153, 76));
    approveBtn.setForeground(Color.WHITE);
    rejectBtn.setBackground(new Color(204, 0, 0));
    rejectBtn.setForeground(Color.WHITE);
    cancelBtn.setBackground(Color.GRAY); // Optional: style the cancel button
cancelBtn.setForeground(Color.WHITE);

    bottomPanel.add(cancelBtn);
    bottomPanel.add(approveBtn);
    bottomPanel.add(rejectBtn);

    approveBtn.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        int confirm = JOptionPane.showConfirmDialog(
            ApplicationDetailsDialog.this,
            "Are you sure you want to approve this application?",
            "Confirm Approval",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            updateStatus("Approved", null);
        }
    }
});
    rejectBtn.addActionListener(e -> {
    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Are you sure you want to reject this application?",
        "Confirm Rejection",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );

    if (confirm == JOptionPane.YES_OPTION) {
        JTextArea reasonArea = new JTextArea(5, 30);
        reasonArea.setLineWrap(true);
        reasonArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(reasonArea);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Enter reason for rejection:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(this, panel, "Rejection Reason", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String reason = reasonArea.getText().trim();
            if (!reason.isEmpty()) {
                updateStatus("Rejected", reason);
            } else {
                JOptionPane.showMessageDialog(this, "Rejection reason is required.", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
});

    cancelBtn.addActionListener(e -> {
    int confirm = JOptionPane.showConfirmDialog(
        this,
        "Are you sure you want to cancel and close this dialog?",
        "Cancel Confirmation",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE
    );
    if (confirm == JOptionPane.YES_OPTION) {
        dispose();
    }
    });
    add(content, BorderLayout.CENTER);
    add(bottomPanel, BorderLayout.SOUTH);
}

    private void addField(String label, String value, int y) {
         GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = y;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.insets = new Insets(4, 4, 4, 4);

    JLabel labelComponent = new JLabel(label + ":");
    labelComponent.setFont(new Font(FONT_NAME, FONT_STYLE_LABEL, FONT_SIZE));
    infoPanel.add(labelComponent, gbc);
    

    gbc.gridx = 1;
    JLabel valueComponent = new JLabel(value != null ? value : "");
    valueComponent.setFont(new Font(FONT_NAME, FONT_STYLE_VALUE, FONT_SIZE));
    infoPanel.add(valueComponent, gbc);
    }

    private void loadApplicationDetails() {
        String sql = "SELECT * FROM scholarship_applications WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, applicationId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int y = 0;
                addField("Name", rs.getString("student_name"), y++);
                addField("Student Number", rs.getString("student_number"), y++);
                addField("Email", rs.getString("email"), y++);
                addField("Phone", rs.getString("phone"), y++);
                addField("Gender", rs.getString("gender"), y++);
                addField("Birth Date", rs.getString("birth_date"), y++);
                addField("Address", rs.getString("address"), y++);
                addField("Province", rs.getString("province"), y++);
                addField("Municipality", rs.getString("municipality"), y++);
                addField("Barangay", rs.getString("barangay"), y++);
                addField("Zip Code", rs.getString("zip_code"), y++);
                addField("School", rs.getString("school"), y++);
                addField("Course", rs.getString("course"), y++);
                addField("Year Level", rs.getString("year_level"), y++);
                addField("GWA", rs.getString("gwa"), y++);
                addField("Father Name", rs.getString("father_name"), y++);
                addField("Father Age", rs.getString("father_age"), y++);
                addField("Father Occupation", rs.getString("father_occupation"), y++);
                addField("Father Income", rs.getString("father_income"), y++);
                addField("Father Education", rs.getString("father_education"), y++);
                addField("Mother Name", rs.getString("mother_name"), y++);
                addField("Mother Age", rs.getString("mother_age"), y++);
                addField("Mother Occupation", rs.getString("mother_occupation"), y++);
                addField("Mother Income", rs.getString("mother_income"), y++);
                addField("Mother Education", rs.getString("mother_education"), y++);
                addField("Total Family Income", rs.getString("total_family_income"), y++);
                addField("Number of Siblings", rs.getString("number_of_siblings"), y++);
                addField("Number of Dependents", rs.getString("number_of_dependents"), y++);
                addField("Housing Status", rs.getString("housing_status"), y++);
                addField("Special Circumstances", rs.getString("special_circumstances"), y++);
                addField("Application Date", rs.getString("application_date"), y++);
                
              
                String status = rs.getString("status");
                addField("Status", status, y++);
                if ("Rejected".equalsIgnoreCase(status)) {
                addField("Rejection Reason", rs.getString("rejection_reason") != null ? rs.getString("rejection_reason") : "No reason provided", y++);
           }
                
                
                
                List<String> paths = parseDocumentPaths(rs.getString("uploaded_files"));
                for (String path : paths) {
                    addDocumentRow(path);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading application details: " + e.getMessage());
        }
    }
    
    private void loadRejectionHistory() {
    String sql = "SELECT reason, rejected_at FROM rejection_history WHERE application_id = ? ORDER BY rejected_at DESC";
    StringBuilder historyBuilder = new StringBuilder();

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setInt(1, applicationId);
        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            Timestamp timestamp = rs.getTimestamp("rejected_at");
            String reason = rs.getString("reason");

            historyBuilder.append("• ")
                    .append(timestamp.toLocalDateTime())
                    .append(" - ")
                    .append(reason)
                    .append("\n\n");
        }

        if (historyArea != null) {
            historyArea.setText(historyBuilder.toString().isEmpty() ?
                "No previous rejection records." : historyBuilder.toString());
        }

    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to load rejection history: " + e.getMessage());
    }
}

    private void addDocumentRow(String path) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row.add(new JLabel(new File(path).getName()));

        JButton previewBtn = new JButton("Preview");
        JButton downloadBtn = new JButton("Download");

        previewBtn.addActionListener(e -> previewFile(path));
        downloadBtn.addActionListener(e -> downloadFile(path));

        row.add(previewBtn);
        row.add(downloadBtn);
        docPanel.add(row);
    }

    private List<String> parseDocumentPaths(String uploadedFilesJson) {
        List<String> paths = new ArrayList<>();
        if (uploadedFilesJson == null || uploadedFilesJson.isEmpty()) return paths;

        String[] entries = uploadedFilesJson.split("\\{");
        for (String entry : entries) {
            if (entry.contains("originalPath")) {
                int start = entry.indexOf("\"originalPath\":\"") + 16;
                int end = entry.indexOf("\"", start);
                if (start > 0 && end > start) {
                    String path = entry.substring(start, end).replaceAll("\\\\", "/");
                    paths.add(path);
                }
            }
        }
        return paths;
    }

    private void previewFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this, "File not found: " + path);
            return;
        }
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Unable to preview file.");
        }
    }

    private void downloadFile(String path) {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File(new File(path).getName()));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.copy(Paths.get(path), chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
                JOptionPane.showMessageDialog(this, "Downloaded successfully to: " + chooser.getSelectedFile().getPath());
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Download failed: " + e.getMessage());
            }
        }
    }

private void updateStatus(String newStatus, String reason) {
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
        conn.setAutoCommit(false); // Begin transaction

        int studentId = -1;

        // Step 1: Get student_id from the application
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT student_id FROM scholarship_applications WHERE id = ?")) {
            ps.setInt(1, applicationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    studentId = rs.getInt("student_id");
                } else {
                    throw new SQLException("Student ID not found for application ID: " + applicationId);
                }
            }
        }

        // Step 2: Update application status
        try (PreparedStatement stmt = conn.prepareStatement(
                "UPDATE scholarship_applications SET status = ?, rejection_reason = ?, is_read = ? WHERE id = ?")) {
            stmt.setString(1, newStatus);
            stmt.setString(2, reason);
            stmt.setBoolean(3, !"Approved".equalsIgnoreCase(newStatus)); // false if approved = triggers admin notification
            stmt.setInt(4, applicationId);
            stmt.executeUpdate();
        }

        // Step 3: Save rejection reason to history (optional)
        if ("Rejected".equalsIgnoreCase(newStatus)) {
            try (PreparedStatement insertHistory = conn.prepareStatement(
                    "INSERT INTO rejection_history(application_id, reason, rejected_at) VALUES (?, ?, NOW())")) {
                insertHistory.setInt(1, applicationId);
                insertHistory.setString(2, reason);
                insertHistory.executeUpdate();
            }
        }

        // Step 4: Insert student notification
        String message;
if ("Approved".equalsIgnoreCase(newStatus)) {
    message = "CONGRATULATIONS! Your scholarship application has been approved. Proceed to Applications for more details.";
} else if ("Rejected".equalsIgnoreCase(newStatus)) {
    message = "We're sorry. Your scholarship application has been rejected. Proceed to Applications for more details.";
} else {
    message = "Your scholarship application status has been updated to " + newStatus + ". Proceed to Applications for more details.";
}

try (PreparedStatement insertNotif = conn.prepareStatement(
        "INSERT INTO student_notifications (student_id, application_id, message) VALUES (?, ?, ?)")) {
    insertNotif.setInt(1, studentId);
    insertNotif.setInt(2, applicationId);
    insertNotif.setString(3, message);
    insertNotif.executeUpdate();
}

        conn.commit(); // ✅ Commit everything

        JOptionPane.showMessageDialog(this, "Application status updated to " + newStatus);
        dispose();

    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Failed to update status: " + e.getMessage());
    }
}
}

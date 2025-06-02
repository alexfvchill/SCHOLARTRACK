
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.toedter.calendar.JDateChooser;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import javax.swing.Timer;


public class Scholarships extends javax.swing.JInternalFrame {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_database_name";
    private static final String DB_USER = "your_username";
    private static final String DB_PASS = "your_password";
    
     private javax.swing.JTextField searchField;
     private javax.swing.JButton searchButton;
     private Timer searchTimer;
     private javax.swing.JComboBox<String> statusFilter;
     private javax.swing.JTextField minSlotsField;
     private javax.swing.JTextField maxSlotsField;
     private com.toedter.calendar.JDateChooser fromDateChooser;
     private com.toedter.calendar.JDateChooser toDateChooser;
     private javax.swing.JButton clearFiltersButton; 
     
     
     
    public Scholarships() {
    initComponents();
    this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    BasicInternalFrameUI ui = (BasicInternalFrameUI) this.getUI();
    ui.setNorthPane(null);
   
   
    initSearchComponents();
    loadScholarships(); // initial load

   

}
    
private void initSearchComponents() {
    // Existing search field
    searchField = new JTextField();
    searchField.setFont(new Font("Dialog", Font.PLAIN, 12));
    searchField.setToolTipText("Type to search scholarships...");
    jPanel1.add(searchField, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 100, 300, 30));
    
    // Existing search button
    searchButton = new JButton("Search");
    searchButton.setFont(new Font("Dialog", Font.BOLD, 14));
    searchButton.setBackground(new Color(255, 204, 0));
    jPanel1.add(searchButton, new org.netbeans.lib.awtextra.AbsoluteConstraints(790, 100, 90, 30));

    // Create filter panel
    JPanel filterPanel = new JPanel();
    filterPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
    filterPanel.setBackground(new Color(255,255,255));
    filterPanel.setBorder(BorderFactory.createTitledBorder(
        BorderFactory.createLineBorder(Color.GRAY, 1), 
        "Filter by:", 
        0, 
        0, 
        new Font("Dialog", Font.BOLD, 14)
    ));

    // Status filter
    JLabel statusLabel = new JLabel("Status:");
    statusLabel.setFont(new Font("Dialog", Font.BOLD, 12));
    statusFilter = new JComboBox<>(new String[]{"All Status", "Open", "Closed"});
    statusFilter.setPreferredSize(new Dimension(120, 25));
    
    // Slots range filter
    JLabel slotsLabel = new JLabel("Slots:");
    slotsLabel.setFont(new Font("Dialog", Font.BOLD, 12));
    minSlotsField = new JTextField(3);
    minSlotsField.setToolTipText("Min slots");
    JLabel toLabel = new JLabel("to");
    maxSlotsField = new JTextField(3);
    maxSlotsField.setToolTipText("Max slots");
    
    // Date range filter
    JLabel dateLabel = new JLabel("Deadline:");
    dateLabel.setFont(new Font("Dialog", Font.BOLD, 12));
    fromDateChooser = new com.toedter.calendar.JDateChooser();
    fromDateChooser.setPreferredSize(new Dimension(120, 25));
    fromDateChooser.setToolTipText("From date");
    JLabel dateToLabel = new JLabel("to");
    toDateChooser = new com.toedter.calendar.JDateChooser();
    toDateChooser.setPreferredSize(new Dimension(120, 25));
    toDateChooser.setToolTipText("To date");
    
    // Clear filters button
    clearFiltersButton = new JButton("Clear All");
    clearFiltersButton.setFont(new Font("Dialog", Font.BOLD, 12));
    clearFiltersButton.setBackground(new Color(255,102,102));
    clearFiltersButton.setPreferredSize(new Dimension(80, 25));

    jLabel7.setFont(new Font("Dialog", Font.BOLD, 14)); // or 20, 24, etc.
    
    
    // Add components to filter panel
    filterPanel.add(statusLabel);
    filterPanel.add(statusFilter);
    filterPanel.add(Box.createHorizontalStrut(10));
    filterPanel.add(slotsLabel);
    filterPanel.add(minSlotsField);
    filterPanel.add(toLabel);
    filterPanel.add(maxSlotsField);
    filterPanel.add(Box.createHorizontalStrut(10));
    filterPanel.add(dateLabel);
    filterPanel.add(fromDateChooser);
    filterPanel.add(dateToLabel);
    filterPanel.add(toDateChooser);
    filterPanel.add(Box.createHorizontalStrut(10));
    filterPanel.add(clearFiltersButton);

    // Add filter panel to main panel
    jPanel1.add(filterPanel, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 130, 1040, 60));
    
    // Adjust the scroll pane position to make room for filters
    jPanel1.remove(jScrollPane1);
    jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 200, 1040, 450));

    // Initialize search timer (300ms delay for auto-search)
    searchTimer = new Timer(300, e -> performSearch());
    searchTimer.setRepeats(false);

    // Add document listener for auto-search as user types
    searchField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            scheduleSearch();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            scheduleSearch();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            scheduleSearch();
        }
    });

    // Add listeners for filter components
    statusFilter.addActionListener(e -> performSearch());
    
    // Add document listeners for slot fields
    minSlotsField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) { scheduleSearch(); }
        @Override
        public void removeUpdate(DocumentEvent e) { scheduleSearch(); }
        @Override
        public void changedUpdate(DocumentEvent e) { scheduleSearch(); }
    });
    
    maxSlotsField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) { scheduleSearch(); }
        @Override
        public void removeUpdate(DocumentEvent e) { scheduleSearch(); }
        @Override
        public void changedUpdate(DocumentEvent e) { scheduleSearch(); }
    });

    // Add property change listeners for date choosers
    fromDateChooser.addPropertyChangeListener("date", e -> performSearch());
    toDateChooser.addPropertyChangeListener("date", e -> performSearch());

    // Keep manual search button functionality
    searchButton.addActionListener(e -> performSearch());
    
    // Clear filters button functionality
    clearFiltersButton.addActionListener(e -> clearAllFilters());
}

    private void scheduleSearch() {
        // Restart timer on each keystroke
        searchTimer.restart();
    }

    private void performSearch() {
        String searchQuery = searchField.getText().trim();
    String selectedStatus = (String) statusFilter.getSelectedItem();
    String minSlotsText = minSlotsField.getText().trim();
    String maxSlotsText = maxSlotsField.getText().trim();
    java.util.Date fromDate = fromDateChooser.getDate();
    java.util.Date toDate = toDateChooser.getDate();
    
    // If no filters are applied, load all scholarships
    if (searchQuery.isEmpty() && 
        "All Status".equals(selectedStatus) && 
        minSlotsText.isEmpty() && 
        maxSlotsText.isEmpty() && 
        fromDate == null && 
        toDate == null) {
        loadScholarships();
        return;
    }
    
    loadScholarshipsWithFilters(searchQuery, selectedStatus, minSlotsText, maxSlotsText, fromDate, toDate);
}
    
   public void loadScholarshipsWithFilters(String searchQuery, String selectedStatus, 
                                       String minSlotsText, String maxSlotsText,
                                       java.util.Date fromDate, java.util.Date toDate) {
    jPanel3.removeAll();
    
    // Build dynamic SQL query
    StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM scholarships WHERE 1=1");
    java.util.List<Object> parameters = new java.util.ArrayList<>();
    
    // Text search filter
    if (!searchQuery.isEmpty()) {
        sqlBuilder.append(" AND (name LIKE ? OR description LIKE ? OR requirements LIKE ? OR benefits LIKE ?)");
        String wildcardQuery = "%" + searchQuery + "%";
        parameters.add(wildcardQuery);
        parameters.add(wildcardQuery);
        parameters.add(wildcardQuery);
        parameters.add(wildcardQuery);
    }
    
    // Status filter
    if (!"All Status".equals(selectedStatus)) {
        sqlBuilder.append(" AND status = ?");
        parameters.add(selectedStatus);
    }
    
    // Slots range filter
    if (!minSlotsText.isEmpty() && minSlotsText.matches("\\d+")) {
        sqlBuilder.append(" AND slot >= ?");
        parameters.add(Integer.parseInt(minSlotsText));
    }
    
    if (!maxSlotsText.isEmpty() && maxSlotsText.matches("\\d+")) {
        sqlBuilder.append(" AND slot <= ?");
        parameters.add(Integer.parseInt(maxSlotsText));
    }
    
    // Date range filter
    if (fromDate != null) {
        sqlBuilder.append(" AND deadline >= ?");
        parameters.add(new java.text.SimpleDateFormat("yyyy-MM-dd").format(fromDate));
    }
    
    if (toDate != null) {
        sqlBuilder.append(" AND deadline <= ?");
        parameters.add(new java.text.SimpleDateFormat("yyyy-MM-dd").format(toDate));
    }
    
    sqlBuilder.append(" ORDER BY name");
    
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
         PreparedStatement stmt = conn.prepareStatement(sqlBuilder.toString())) {
        
        // Set parameters
        for (int i = 0; i < parameters.size(); i++) {
            stmt.setObject(i + 1, parameters.get(i));
        }
        
        ResultSet rs = stmt.executeQuery();
        int count = 0;
        
        while (rs.next()) {
            ScholarshipCardPanel card = new ScholarshipCardPanel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("requirements"),
                rs.getString("benefits"),
                rs.getInt("slot"),
                rs.getString("status"),
                rs.getString("deadline")
            );
            
            jPanel3.add(card);
            count++;
        }
        
        jLabel7.setText("Total Scholarships: " + count);
        
        // Adjust preferred size based on number of cards
        int panelHeight = Math.max(count * 250, jScrollPane1.getHeight());
        jPanel3.setPreferredSize(new Dimension(jScrollPane1.getWidth() - 20, panelHeight));
        
        jPanel3.revalidate();
        jPanel3.repaint();
        
        // Update the title to show filter results
        if (count == 0) {
            jLabel7.setText("No scholarships found");
        } else {
            jLabel7.setText("Total Scholarships: " + count);
        }
        
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
    
   // Method to clear all filters
private void clearAllFilters() {
    searchField.setText("");
    statusFilter.setSelectedIndex(0); // "All Status"
    minSlotsField.setText("");
    maxSlotsField.setText("");
    fromDateChooser.setDate(null);
    toDateChooser.setDate(null);
    jLabel7.setText("List of Scholarships");
    loadScholarships(); // Reload all scholarships
}
   
    
public class ScholarshipCardPanel extends JPanel {
        private int scholarshipId;
      
public ScholarshipCardPanel(int id, String name, String description, String requirements,
                            String benefits, int slot, String status, String deadline) {
    this.scholarshipId = id;

        setLayout(new BorderLayout(10, 10));
        setPreferredSize(new Dimension(290, 350));
        setBackground(Color.WHITE); // Changed from new Color(255, 255, 255) to Color.WHITE for clarity
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

          // Title with proper text wrapping that fits the panel
    JTextArea titleArea = new JTextArea(name);
    titleArea.setFont(new Font("Dialog", Font.BOLD, 16));
    titleArea.setForeground(new Color(102, 102, 0));
    titleArea.setBackground(Color.WHITE);
    titleArea.setEditable(false);
    titleArea.setLineWrap(true);
    titleArea.setWrapStyleWord(true);
    titleArea.setRows(2); // Allow up to 2 lines for title
    titleArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 10, 5));
    titleArea.setOpaque(true);
    
    // Center the text
    titleArea.setAlignmentX(Component.CENTER_ALIGNMENT);
    
         // Create formatted content with better spacing and bold labels
            StringBuilder content = new StringBuilder();
            content.append("<html><b>Status:</b> ").append(status).append("<br><br>");
            content.append("<b>Deadline:</b> ").append(formatDate(deadline)).append("<br><br>");
            content.append("<b>Available Slots:</b> ").append(slot).append("<br><br>");
            content.append("<b>Description:</b><br>").append(description).append("<br><br>");
            content.append("<b>Requirements:</b><br>").append(requirements).append("<br><br>");
            content.append("<b>Benefits:</b><br>").append(benefits).append("</html>");
 
// Text area with improved formatting
            JLabel detailsLabel = new JLabel(content.toString());
            detailsLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
            detailsLabel.setVerticalAlignment(JLabel.TOP);
            detailsLabel.setOpaque(false);

            JScrollPane scrollPane = new JScrollPane(detailsLabel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
            scrollPane.setPreferredSize(new Dimension(300, 180));
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
              scrollPane.getViewport().setBackground(Color.WHITE); // Ensure scroll pane viewport is also white
            
            
// Buttons with spacing
            JButton editBtn = new JButton("Edit");
            JButton deleteBtn = new JButton("Delete");
            styleButton(editBtn, new Color(0, 123, 255));
            styleButton(deleteBtn, new Color(220, 53, 69));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
            buttonPanel.setOpaque(false);
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
            buttonPanel.add(editBtn);
            buttonPanel.add(deleteBtn);

        // Add components
       add(titleArea, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Event Listeners
        editBtn.addActionListener(e -> showEditDialog(id, name, status, description, deadline, requirements, benefits, slot));
            deleteBtn.addActionListener(e -> deleteScholarship(id));
        }



private void styleButton(JButton button, Color color) {
        button.setBackground(color);
        button.setForeground(Color.BLACK);
        button.setFont(new Font("Dialog", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
}
    
private String formatDate(String dateStr) {
    try {
        java.text.SimpleDateFormat inputFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.text.SimpleDateFormat outputFormat = new java.text.SimpleDateFormat("MMM dd, yyyy");
        java.util.Date date = inputFormat.parse(dateStr);
        return outputFormat.format(date);
    } catch (Exception e) {
        return dateStr; // fallback to original format if parsing fails
    }
}
    


public void loadScholarships() {
     jPanel3.removeAll();
    jPanel3.setLayout(new FlowLayout(FlowLayout.LEFT, 40, 40));
    jScrollPane1.setViewportView(jPanel3);
    jScrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    jPanel3.setPreferredSize(new Dimension(jScrollPane1.getWidth() - 20, jPanel3.getComponentCount() * 250));

    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
         Statement stmt = conn.createStatement();
         ResultSet rs = stmt.executeQuery("SELECT * FROM scholarships ORDER BY name")) {

        int count = 0;
        while (rs.next()) {
            ScholarshipCardPanel card = new ScholarshipCardPanel(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("requirements"),
                rs.getString("benefits"),
                rs.getInt("slot"),
                rs.getString("status"),
                rs.getString("deadline")
            );    
                  
            jPanel3.add(card);
            count++;
        }

        // Set preferred size based on number of cards
        int panelHeight = Math.max(count * 250, jScrollPane1.getHeight());
        jPanel3.setPreferredSize(new Dimension(jScrollPane1.getWidth() - 20, panelHeight));
        
        jPanel3.revalidate();
        jPanel3.repaint();
        
        // Update title with total count
        jLabel7.setText("List of Scholarships (" + count + " total)");

    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
}
     
     
// Add this method to create a filter summary display (optional)
private String getActiveFiltersString() {
    java.util.List<String> activeFilters = new java.util.ArrayList<>();
    
    if (!searchField.getText().trim().isEmpty()) {
        activeFilters.add("Search: \"" + searchField.getText().trim() + "\"");
    }
    
    if (!"All Status".equals(statusFilter.getSelectedItem())) {
        activeFilters.add("Status: " + statusFilter.getSelectedItem());
    }
    
    if (!minSlotsField.getText().trim().isEmpty() || !maxSlotsField.getText().trim().isEmpty()) {
        String slotsFilter = "Slots: ";
        if (!minSlotsField.getText().trim().isEmpty()) {
            slotsFilter += minSlotsField.getText().trim() + "+";
        }
        if (!maxSlotsField.getText().trim().isEmpty()) {
            if (!minSlotsField.getText().trim().isEmpty()) {
                slotsFilter = slotsFilter.replace("+", "-" + maxSlotsField.getText().trim());
            } else {
                slotsFilter += "≤" + maxSlotsField.getText().trim();
            }
        }
        activeFilters.add(slotsFilter);
    }
    
    if (fromDateChooser.getDate() != null || toDateChooser.getDate() != null) {
        String dateFilter = "Deadline: ";
        if (fromDateChooser.getDate() != null) {
            dateFilter += formatDate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(fromDateChooser.getDate()));
        }
        if (toDateChooser.getDate() != null) {
            if (fromDateChooser.getDate() != null) {
                dateFilter += " to " + formatDate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(toDateChooser.getDate()));
            } else {
                dateFilter += "before " + formatDate(new java.text.SimpleDateFormat("yyyy-MM-dd").format(toDateChooser.getDate()));
            }
        }
        activeFilters.add(dateFilter);
    }
    
    return activeFilters.isEmpty() ? "No filters active" : String.join(", ", activeFilters);
}

   
public void loadScholarships(String searchQuery) {
    jPanel3.removeAll(); // Clear previous results

        String sql = "SELECT * FROM scholarships WHERE name LIKE ? OR description LIKE ? OR requirements LIKE ?";
        
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String wildcardQuery = "%" + searchQuery.trim() + "%";
            stmt.setString(1, wildcardQuery);
            stmt.setString(2, wildcardQuery);
            stmt.setString(3, wildcardQuery);

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                ScholarshipCardPanel card = new ScholarshipCardPanel(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getString("requirements"),
                    rs.getString("benefits"),
                    rs.getInt("slot"),
                    rs.getString("status"),
                    rs.getString("deadline")
                );

                jPanel3.add(card);
                count++;
            }

            // Adjust preferred size based on number of cards
            int panelHeight = Math.max(count * 250, jScrollPane1.getHeight());
            jPanel3.setPreferredSize(new Dimension(jScrollPane1.getWidth() - 20, panelHeight));

            jPanel3.revalidate();
            jPanel3.repaint();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
   
   
   
private void deleteScholarship(int id) {
    int confirm = JOptionPane.showConfirmDialog(
            this,
            "Are you sure you want to delete this scholarship?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
    if (confirm != JOptionPane.YES_OPTION) {
        JOptionPane.showMessageDialog(this, "Deleted Cancelled");
        return;
    }
    
    String sql = "DELETE FROM scholarships WHERE id = ?";
    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        pstmt.setInt(1, id);
        int rowsAffected = pstmt.executeUpdate();
        if (rowsAffected > 0) {
            JOptionPane.showMessageDialog(this, "Scholarship deleted successfully!");
        } else {
            JOptionPane.showMessageDialog(this, "Scholarship not found or already deleted.");
        }
        loadScholarships();
    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error deleting scholarship:\n" + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
    }
}


private void showEditDialog(int id, String name, String status, String description,
                            String deadline, String requirements, String benefits, int slot) {
    JDialog dialog = new JDialog((Frame) null, "Edit Scholarship", true);
    dialog.setSize(600, 600);
    dialog.setLocationRelativeTo(null);
    dialog.setLayout(new BorderLayout());
    dialog.setBackground(Color.WHITE);
    
    // Modern header panel
    JPanel headerPanel = new JPanel();
    headerPanel.setBackground(new Color(41, 128, 185)); // Modern blue
    headerPanel.setPreferredSize(new Dimension(600, 60));
    headerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 15));
    
    JLabel titleLabel = new JLabel("Edit Scholarship");
    titleLabel.setFont(new Font("Dialog", Font.BOLD, 24));
    titleLabel.setForeground(Color.WHITE);
    headerPanel.add(titleLabel);
    
    // Main content panel with modern styling
    JPanel mainPanel = new JPanel();
    mainPanel.setBackground(new Color(248, 249, 250));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    
    // Create modern input fields
    JTextArea nameArea = createModernTextArea(name, 3);
    JTextField statusField = createModernTextField(status);
    JDateChooser deadlineChooser = createModernDateChooser(deadline);
    JTextArea descriptionArea = createModernTextArea(description, 4);
    JTextArea requirementsArea = createModernTextArea(requirements, 4);
    JTextArea benefitsArea = createModernTextArea(benefits, 4);
    JTextField slotField = createModernTextField(String.valueOf(slot));
    
    // Add form sections
    mainPanel.add(createFormSection("Scholarship Name", nameArea));
    mainPanel.add(Box.createVerticalStrut(20));
    
    mainPanel.add(createFormSection("Status", statusField));
    mainPanel.add(Box.createVerticalStrut(20));
    
    mainPanel.add(createFormSection("Deadline", deadlineChooser));
    mainPanel.add(Box.createVerticalStrut(20));
    
    mainPanel.add(createFormSection("Description", descriptionArea));
    mainPanel.add(Box.createVerticalStrut(20));
    
    mainPanel.add(createFormSection("Requirements", requirementsArea));
    mainPanel.add(Box.createVerticalStrut(20));
    
    mainPanel.add(createFormSection("Benefits", benefitsArea));
    mainPanel.add(Box.createVerticalStrut(20));
    
    mainPanel.add(createFormSection("Available Slots", slotField));
    
    // Modern button panel
    JPanel buttonPanel = new JPanel();
    buttonPanel.setBackground(Color.WHITE);
    buttonPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(220, 220, 220)),
        BorderFactory.createEmptyBorder(20, 40, 20, 40)
    ));
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 0));
    
    // Create modern buttons
    JButton cancelBtn = createModernButton("Cancel", new Color(108, 117, 125), Color.WHITE);
    JButton updateBtn = createModernButton("Update Scholarship", new Color(40, 167, 69), Color.WHITE);
    
    // Button actions
    cancelBtn.addActionListener(e -> {
        int confirm = JOptionPane.showConfirmDialog(dialog, 
            "Are you sure you want to cancel?\nAll changes will be lost.", 
            "Confirm Cancel", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            dialog.dispose();
        }
    });
    
    updateBtn.addActionListener(e -> {
    // Validation
    String newName = nameArea.getText().trim();
    String newStatus = statusField.getText().trim();
    String newDescription = descriptionArea.getText().trim();
    java.util.Date selectedDate = deadlineChooser.getDate();
    String newRequirements = requirementsArea.getText().trim();
    String newBenefits = benefitsArea.getText().trim();
    String slotText = slotField.getText().trim();
    
    // Updated validation message to "Please fill all fields"
    if (newName.isEmpty() || newStatus.isEmpty() || selectedDate == null ||
        newRequirements.isEmpty() || newBenefits.isEmpty() || newDescription.isEmpty()) {
        showModernErrorDialog(dialog, "Please fill all fields", "Missing Information");
        return;
    }
    
    if (slotText.isEmpty() || !slotText.matches("\\d+")) {
        showModernErrorDialog(dialog, "Please enter a valid number for slots.", "Invalid Input");
        return;
    }
    
    int newSlot = Integer.parseInt(slotText);
    String newDeadline = new java.text.SimpleDateFormat("yyyy-MM-dd").format(selectedDate);
    
    int confirm = JOptionPane.showConfirmDialog(dialog,
        "Are you sure you want to update this scholarship?",
        "Confirm Update",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE);
    
    if (confirm == JOptionPane.YES_OPTION) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
             PreparedStatement pstmt = conn.prepareStatement(
                 "UPDATE scholarships SET name=?, description=?, requirements=?, benefits=?, slot=?, status=?, deadline=? WHERE id=?"
             )) {
            
            pstmt.setString(1, newName);
            pstmt.setString(2, newDescription);
            pstmt.setString(3, newRequirements);
            pstmt.setString(4, newBenefits);
            pstmt.setInt(5, newSlot);
            pstmt.setString(6, newStatus);
            pstmt.setString(7, newDeadline);
            pstmt.setInt(8, id);
            
            pstmt.executeUpdate();
            
            showModernSuccessDialog(dialog, "Scholarship updated successfully!");
            dialog.dispose();
            loadScholarships();
            
        } catch (SQLException ex) {
            ex.printStackTrace();
            showModernErrorDialog(dialog, "Error updating scholarship:\n" + ex.getMessage(), "Database Error");
        }
    }
});
    buttonPanel.add(cancelBtn);
    buttonPanel.add(updateBtn);
    
    // Assemble dialog
    dialog.add(headerPanel, BorderLayout.NORTH);
    dialog.add(new JScrollPane(mainPanel), BorderLayout.CENTER);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    
    dialog.setVisible(true);
}


// Helper methods for modern styling
private JPanel createFormSection(String labelText, JComponent component) {
    JPanel section = new JPanel();
    section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
    section.setBackground(new Color(248, 249, 250));
    section.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    JLabel label = new JLabel(labelText);
    label.setFont(new Font("Dialog", Font.BOLD, 14));
    label.setForeground(new Color(52, 58, 64));
    label.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    component.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    section.add(label);
    section.add(component);
    
    return section;
}

private JTextArea createModernTextArea(String text, int rows) {
    JTextArea textArea = new JTextArea(text, rows, 0);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setFont(new Font("Dialog", Font.PLAIN, 14));
    textArea.setBackground(Color.WHITE);
    textArea.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
        BorderFactory.createEmptyBorder(12, 12, 12, 12)
    ));
    
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218), 1));
    scrollPane.setBackground(Color.WHITE);
    scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, rows * 25 + 30));
    
    return textArea;
}

private JTextField createModernTextField(String text) {
    JTextField textField = new JTextField(text);
    textField.setFont(new Font("Dialog", Font.PLAIN, 14));
    textField.setBackground(Color.WHITE);
    textField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
        BorderFactory.createEmptyBorder(12, 12, 12, 12)
    ));
    textField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
    textField.setPreferredSize(new Dimension(0, 45));
    
    return textField;
}

private JDateChooser createModernDateChooser(String deadline) {
    JDateChooser dateChooser = new JDateChooser();
    try {
        java.util.Date date = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(deadline);
        dateChooser.setDate(date);
    } catch (Exception e) {
        dateChooser.setDate(null);
    }
    
    dateChooser.setFont(new Font("Dialog", Font.PLAIN, 14));
    dateChooser.setBackground(Color.WHITE);
    dateChooser.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218), 1));
    dateChooser.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
    dateChooser.setPreferredSize(new Dimension(0, 45));
    
    return dateChooser;
}

private JButton createModernButton(String text, Color bgColor, Color textColor) {
    JButton button = new JButton(text);
    button.setFont(new Font("Dialog", Font.BOLD, 14));
    button.setBackground(bgColor);
    button.setForeground(textColor);
    button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    button.setPreferredSize(new Dimension(button.getPreferredSize().width, 30));
    
    // Add hover effect
    button.addMouseListener(new java.awt.event.MouseAdapter() {
        Color originalColor = bgColor;
        public void mouseEntered(java.awt.event.MouseEvent evt) {
            button.setBackground(bgColor.darker());
        }
        public void mouseExited(java.awt.event.MouseEvent evt) {
            button.setBackground(originalColor);
        }
    });
    
    return button;
}

private void showModernErrorDialog(JDialog parent, String message, String title) {
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
}

private void showModernSuccessDialog(JDialog parent, String message) {
    JOptionPane.showMessageDialog(parent, message, "Success", JOptionPane.INFORMATION_MESSAGE);
}

private void showAddScholarshipDialog() {
    JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Scholarship", true);
    dialog.setSize(600, 600);
    
    // Center the dialog on screen
    dialog.setLocationRelativeTo(null);
    
    dialog.setLayout(new BorderLayout());
    dialog.setBackground(Color.WHITE);
    
    // Modern header panel with gradient effect
    JPanel headerPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Create gradient background
            GradientPaint gradient = new GradientPaint(0, 0, new Color(52, 152, 219), 
                                                     getWidth(), getHeight(), new Color(41, 128, 185));
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    };
    headerPanel.setPreferredSize(new Dimension(650, 80));
    headerPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 30, 20));
    
    // Header content
    JPanel headerContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
    headerContent.setOpaque(false);
    
    // Add icon
    JLabel iconLabel = new JLabel("📚");
    iconLabel.setFont(new Font("Dialog", Font.PLAIN, 32));
    
    JLabel titleLabel = new JLabel("Add New Scholarship");
    titleLabel.setFont(new Font("Dialog", Font.BOLD, 28));
    titleLabel.setForeground(Color.WHITE);
    
    JLabel subtitleLabel = new JLabel("Create opportunities for students");
    subtitleLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
    subtitleLabel.setForeground(new Color(255, 255, 255, 180));
    
    JPanel titlePanel = new JPanel();
    titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
    titlePanel.setOpaque(false);
    titlePanel.add(titleLabel);
    titlePanel.add(Box.createVerticalStrut(5));
    titlePanel.add(subtitleLabel);
    
    headerContent.add(iconLabel);
    headerContent.add(titlePanel);
    headerPanel.add(headerContent);
    
    // Main scrollable content panel
    JPanel mainPanel = new JPanel();
    mainPanel.setBackground(new Color(248, 250, 252));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    
    // Create modern form fields
    JTextArea nameArea = createModernTextArea("", 3);
    JTextArea descriptionArea = createModernTextArea("", 4);
    JTextArea requirementsArea = createModernTextArea("", 4);
    JTextArea benefitsArea = createModernTextArea("", 4);
    JTextField slotField = createModernTextField("");
    
    // Modern combo box for status
    String[] statuses = {"Open", "Closed"};
    JComboBox<String> statusCombo = new JComboBox<>(statuses);
    styleModernComboBox(statusCombo);
    
    // Modern date chooser
    com.toedter.calendar.JDateChooser deadlineChooser = createModernDateChooser("");
    
    // Add form sections with modern styling
    mainPanel.add(createFormSection("Scholarship Name *", nameArea, "Enter the full name of the scholarship"));
    mainPanel.add(Box.createVerticalStrut(25));
    
    mainPanel.add(createFormSection("Description *", descriptionArea, "Provide a detailed description of the scholarship"));
    mainPanel.add(Box.createVerticalStrut(25));
    
    mainPanel.add(createFormSection("Requirements *", requirementsArea, "List all eligibility requirements"));
    mainPanel.add(Box.createVerticalStrut(25));
    
    mainPanel.add(createFormSection("Benefits *", benefitsArea, "Describe what the scholarship offers"));
    mainPanel.add(Box.createVerticalStrut(25));
    
    mainPanel.add(createFormSection("Available Slots *", slotField, "Number of scholarships available"));
    mainPanel.add(Box.createVerticalStrut(25));
    
    mainPanel.add(createFormSection("Status", statusCombo, "Current status of the scholarship"));
    mainPanel.add(Box.createVerticalStrut(25));
    
    mainPanel.add(createFormSection("Application Deadline *", deadlineChooser, "Last date for applications"));
    
    // Modern button panel with glass effect
    JPanel buttonPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Glass effect background
            g2d.setColor(new Color(255, 255, 255, 250));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            // Top border with subtle shadow
            g2d.setColor(new Color(0, 0, 0, 20));
            g2d.fillRect(0, 0, getWidth(), 1);
        }
    };
    buttonPanel.setPreferredSize(new Dimension(650, 80));
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
    buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 0));
    
    // Create modern buttons with enhanced styling
    JButton cancelBtn = createModernButton("Cancel", new Color(108, 117, 125), Color.WHITE, false);
    JButton saveBtn = createModernButton("Create Scholarship", new Color(40, 167, 69), Color.WHITE, true);
    
    // Enhanced button actions
    cancelBtn.addActionListener(e -> {
        // Always show confirmation dialog when canceling
        int confirm = JOptionPane.showConfirmDialog(dialog, 
            "Are you sure you want to cancel?\nAll entered information will be lost.", 
            "Confirm Cancel", 
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            dialog.dispose();
        }
    });
    
    saveBtn.addActionListener(e -> {
        // Get and validate input
        String name = nameArea.getText().trim();
        String description = descriptionArea.getText().trim();
        String requirements = requirementsArea.getText().trim();
        String benefits = benefitsArea.getText().trim();
        String slotText = slotField.getText().trim();
        String status = (String) statusCombo.getSelectedItem();
        java.util.Date selectedDate = deadlineChooser.getDate();

        // Enhanced validation with modern error dialog
        if (name.isEmpty() || description.isEmpty() || requirements.isEmpty() || 
            benefits.isEmpty() || slotText.isEmpty() || selectedDate == null) {
            showModernErrorDialog(dialog, "Please fill all fields", "Missing Required Information");
            return;
        }

        if (!slotText.matches("\\d+")) {
            showModernErrorDialog(dialog, "Please enter a valid number for slots.", "Invalid Input");
            return;
        }

        int slot = Integer.parseInt(slotText);
        String deadline = new SimpleDateFormat("yyyy-MM-dd").format(selectedDate);

        // Modern confirmation dialog
        int confirm = showModernConfirmDialog(dialog,
                "Create this scholarship with the provided information?",
                "Confirm Scholarship Creation");

        if (confirm == JOptionPane.YES_OPTION) {
            // Show loading state
            saveBtn.setText("Creating...");
            saveBtn.setEnabled(false);
            
            SwingUtilities.invokeLater(() -> {
                String insertSQL = "INSERT INTO scholarships (name, description, requirements, benefits, slot, status, deadline) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
                     PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

                    pstmt.setString(1, name);
                    pstmt.setString(2, description);
                    pstmt.setString(3, requirements);
                    pstmt.setString(4, benefits);
                    pstmt.setInt(5, slot);
                    pstmt.setString(6, status);
                    pstmt.setString(7, deadline);
                    pstmt.executeUpdate();

                    // Notify students
                    String message = "🎓 New scholarship opportunity: '" + name + "' is now available! Apply through SCHOLARTRACK.";
                    String selectStudentsSQL = "SELECT student_id FROM students WHERE student_id NOT IN (SELECT DISTINCT student_id FROM scholarship_applications WHERE status = 'approved')";
                    try (PreparedStatement selectStmt = conn.prepareStatement(selectStudentsSQL);
                         ResultSet rs = selectStmt.executeQuery()) {

                        String notifSQL = "INSERT INTO student_notifications (student_id, message, is_read) VALUES (?, ?, ?)";
                        try (PreparedStatement notifStmt = conn.prepareStatement(notifSQL)) {
                            while (rs.next()) {
                                int studentId = rs.getInt("student_id");
                                notifStmt.setInt(1, studentId);
                                notifStmt.setString(2, message);
                                notifStmt.setBoolean(3, false);
                                notifStmt.addBatch();
                            }
                            notifStmt.executeBatch();
                        }
                    }

                    SwingUtilities.invokeLater(() -> {
                        showModernSuccessDialog(dialog, "Scholarship created successfully!\nStudents have been notified about this new opportunity.");
                        dialog.dispose();
                        loadScholarships();
                    });

                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        saveBtn.setText("Create Scholarship");
                        saveBtn.setEnabled(true);
                        ex.printStackTrace();
                        showModernErrorDialog(dialog, "Failed to create scholarship:\n" + ex.getMessage(), "Database Error");
                    });
                }
            });
        }
    });

    buttonPanel.add(cancelBtn);
    buttonPanel.add(saveBtn);
    
    // Assemble dialog
    dialog.add(headerPanel, BorderLayout.NORTH);
    dialog.add(new JScrollPane(mainPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
    dialog.add(buttonPanel, BorderLayout.SOUTH);
    
    // Make dialog resizable and add window listener
    dialog.setResizable(true);
    dialog.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosing(java.awt.event.WindowEvent windowEvent) {
            cancelBtn.doClick(); // Trigger cancel logic
        }
    });
    
    dialog.setVisible(true);
}


// Enhanced helper methods for modern styling
private JPanel createFormSection(String labelText, JComponent component, String helpText) {
    JPanel section = new JPanel();
    section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
    section.setBackground(new Color(248, 250, 252));
    section.setAlignmentX(Component.LEFT_ALIGNMENT);
    
    // Modern label with help text
    JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    labelPanel.setBackground(new Color(248, 250, 252));
    
    JLabel label = new JLabel(labelText);
    label.setFont(new Font("Dialog", Font.BOLD, 15));
    label.setForeground(new Color(33, 37, 41));
    
    labelPanel.add(label);
    
    if (helpText != null && !helpText.isEmpty()) {
        JLabel helpLabel = new JLabel(" - " + helpText);
        helpLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
        helpLabel.setForeground(new Color(108, 117, 125));
        labelPanel.add(helpLabel);
    }
    
    section.add(labelPanel);
    section.add(Box.createVerticalStrut(8));
    
    component.setAlignmentX(Component.LEFT_ALIGNMENT);
    section.add(component);
    
    return section;
}

private JButton createModernButton(String text, Color bgColor, Color textColor, boolean isPrimary) {
    JButton button = new JButton(text);
    button.setFont(new Font("Dialog", Font.BOLD, 14));
    button.setBackground(bgColor);
    button.setForeground(textColor);
    button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    button.setPreferredSize(new Dimension(button.getPreferredSize().width, 45));
    
    // Add modern hover and click effects
    button.addMouseListener(new java.awt.event.MouseAdapter() {
        Color originalColor = bgColor;
        Color hoverColor = isPrimary ? bgColor.brighter() : bgColor.darker();
        
        public void mouseEntered(java.awt.event.MouseEvent evt) {
            if (button.isEnabled()) {
                button.setBackground(hoverColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1),
                    BorderFactory.createEmptyBorder(11, 23, 11, 23)
                ));
            }
        }
        
        public void mouseExited(java.awt.event.MouseEvent evt) {
            if (button.isEnabled()) {
                button.setBackground(originalColor);
                button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
            }
        }
        
        public void mousePressed(java.awt.event.MouseEvent evt) {
            if (button.isEnabled()) {
                button.setBackground(originalColor.darker());
            }
        }
        
        public void mouseReleased(java.awt.event.MouseEvent evt) {
            if (button.isEnabled()) {
                button.setBackground(hoverColor);
            }
        }
    });
    
    return button;
}

private void styleModernComboBox(JComboBox<String> comboBox) {
    comboBox.setFont(new Font("Dialog", Font.PLAIN, 14));
    comboBox.setBackground(Color.WHITE);
    comboBox.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(206, 212, 218), 1),
        BorderFactory.createEmptyBorder(12, 12, 12, 12)
    ));
    comboBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
    comboBox.setPreferredSize(new Dimension(0, 45));
}

private int showModernConfirmDialog(JDialog parent, String message, String title) {
    return JOptionPane.showConfirmDialog(parent, message, title, 
        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
}

   private void styleDialogButton(JButton btn) {
   btn.setBackground(new Color(0, 123, 255));
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Dialog", Font.BOLD, 14));
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jPanel3 = new javax.swing.JPanel();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel2.setBackground(new java.awt.Color(153, 153, 0));

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Scholarship Management");

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-scholarship-40.png")); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 48, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addContainerGap(569, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 58, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel1.add(jPanel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1090, 70));

        jButton1.setBackground(new java.awt.Color(255, 234, 0));
        jButton1.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        jButton1.setText("Add new");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel1.add(jButton1, new org.netbeans.lib.awtextra.AbsoluteConstraints(963, 100, 100, -1));

        jLabel7.setFont(new java.awt.Font("Dialog", 1, 24)); // NOI18N
        jLabel7.setText("List of Scholarships");
        jPanel1.add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 90, 240, -1));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1038, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 508, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(jPanel3);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(30, 140, 1040, 510));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    
    
    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
         showAddScholarshipDialog();
    }//GEN-LAST:event_jButton1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}

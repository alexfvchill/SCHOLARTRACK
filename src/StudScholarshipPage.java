import UserSession.UserSession;
import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.sql.*;
import javax.swing.Timer;
import java.util.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.io.File;
import java.sql.SQLException;
import java.util.List;
import com.toedter.calendar.JDateChooser;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutionException;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.ui.FlatRoundBorder;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.border.EmptyBorder;

public class StudScholarshipPage extends JInternalFrame {

    // Database configuration - Consider moving to properties file
    private static final String DB_URL = "jdbc:mysql://localhost:3306/scholartrack_db";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "12345";
    
    // UI Components
    private JTextField searchField;
    private JButton searchButton;
    private Timer searchTimer;
    private JPanel scholarshipPanel;
    private JComboBox<String> statusFilter;
    private JComboBox<String> deadlineFilter;
    private JButton clearFiltersButton;
    private JLabel statusLabel;
    private Connection conn;
    
    // Data management
    private List<ScholarshipData> allScholarships;
    private List<ScholarshipData> filteredScholarships;
    
    private final Map<String, JDateChooser> dateChoosers = new HashMap<>();
    private final Map<String, JComboBox<String>> comboBoxes = new HashMap<>();
    private final Map<String, String[]> schoolPrograms = new HashMap<>();
    private Map<String, Map<String, String[]>> locationData;

    private JPanel filesListPanel;
    private JScrollPane filesScrollPane;
    private JButton clearAllBtn;
   private int currentStudentId;
    private List<File> selectedFiles = new ArrayList<>();
    

    public StudScholarshipPage(int studentId) {
    this.currentStudentId = studentId;  // ✅ Set student ID first
    initComponents();
    setupInternalFrame();
    initializeUI();
    loadScholarshipsAsync();  // ✅ Now this uses the correct student ID
}
    
    private void setupInternalFrame() {
        this.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        BasicInternalFrameUI ui = (BasicInternalFrameUI) this.getUI();
        ui.setNorthPane(null);
    }
    
    private void initializeUI() {
        initializeScholarshipDisplay();
        initSearchAndFilterComponents();
        setupLayout();
        setupEventListeners();
    }
    
    private void initializeScholarshipDisplay() {
        scholarshipPanel = new JPanel();
        scholarshipPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 20));
        scholarshipPanel.setBackground(new Color(248, 249, 250));
        
        scholarshipPanel.setBorder(new EmptyBorder(0, 20, 20, 20));
        
        JPanel paddedPanel = new JPanel(new BorderLayout());
paddedPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 20, 20)); // top, left, bottom, right
paddedPanel.setBackground(new Color(248, 249, 250)); // Match background
 paddedPanel.add(scholarshipPanel, BorderLayout.CENTER); 
        
        
         jScrollPane1.setViewportView(paddedPanel);
    jScrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    jScrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    jScrollPane1.getVerticalScrollBar().setUnitIncrement(18);
    }
    
   private void initSearchAndFilterComponents() {
    // Search field with modern styling - REDUCED WIDTH FOR HORIZONTAL LAYOUT
    searchField = new JTextField(20);
    searchField.setFont(new Font("Dialog", Font.PLAIN, 14));
    searchField.setToolTipText("Search scholarships by name, description, or requirements...");
    searchField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(206, 212, 218)),
        BorderFactory.createEmptyBorder(8, 12, 8, 12)
    ));
    
    searchButton = new JButton("Search");
    styleButton(searchButton, new Color(0, 123, 255), Color.WHITE);
    
    // Filter components - REMOVE "Pending" from statusFilter
    statusFilter = new JComboBox<>(new String[]{"All Status", "Open", "Closed"}); // REMOVED "Pending"
    deadlineFilter = new JComboBox<>(new String[]{
        "All Deadlines", "This Week", "This Month", "Next 3 Months", "Next 6 Months"
    });
    // REMOVE benefitsFilter initialization completely
    
    clearFiltersButton = new JButton("Clear Filters");
    clearFiltersButton.setFont(new Font("Dialog", Font.BOLD, 14));
    styleButton(clearFiltersButton, new Color(204,0,0), Color.WHITE);
    
    // Status label for showing results count
    statusLabel = new JLabel("Loading scholarships...");
    statusLabel.setFont(new Font("Dialog", Font.ITALIC, 14));
    statusLabel.setForeground(new Color(108, 117, 125));
    
    // Style combo boxes - REMOVE benefitsFilter from array, SET PREFERRED SIZE FOR HORIZONTAL LAYOUT
    JComboBox<?>[] comboBoxes = {statusFilter, deadlineFilter}; // REMOVED benefitsFilter
    for (JComboBox<?> combo : comboBoxes) {
        combo.setFont(new Font("Dialog", Font.PLAIN, 12));
        combo.setBackground(Color.WHITE);
        combo.setBorder(BorderFactory.createLineBorder(new Color(206, 212, 218)));
        combo.setPreferredSize(new Dimension(140, 35)); // SET SIZE FOR HORIZONTAL LAYOUT
    }
}
    
    private void styleButton(JButton button, Color bgColor, Color fgColor) {
        button.setFont(new Font("Dialog", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    private void setupLayout() {
        // Create search and filter panel with better organization
        JPanel searchAndFilterPanel = createSearchAndFilterPanel();
        
        // Update main layout
        jPanel1.setLayout(new BorderLayout(0, 10));
        
        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout(0, 15));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Title panel
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titlePanel.setBackground(Color.WHITE);
        titlePanel.add(jLabel1);
        
        headerPanel.add(titlePanel, BorderLayout.NORTH);
        headerPanel.add(searchAndFilterPanel, BorderLayout.CENTER);
        headerPanel.add(statusLabel, BorderLayout.SOUTH);
        
        jPanel1.add(headerPanel, BorderLayout.NORTH);
        jPanel1.add(jScrollPane1, BorderLayout.CENTER);
    }
    
   private JPanel createSearchAndFilterPanel() {
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
    panel.setBackground(Color.WHITE);
    
    
    // Search components
    JLabel searchLabel = new JLabel("Search:");
    searchLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(searchLabel);
    panel.add(searchField);
    panel.add(searchButton);
    
    // Add separator
    JLabel separator1 = new JLabel("|");
    separator1.setFont(new Font("Dialog", Font.BOLD, 14));
    separator1.setForeground(new Color(206, 212, 218));
    panel.add(separator1);
    
    // Status filter
    JLabel statusLabel = new JLabel("Status:");
    statusLabel.setFont(new Font("Dialog", Font.BOLD, 12));
    panel.add(statusLabel);
    panel.add(statusFilter);
    
    // Deadline filter
    JLabel deadlineLabel = new JLabel("Deadline:");
    deadlineLabel.setFont(new Font("Dialog", Font.BOLD, 12));
    panel.add(deadlineLabel);
    panel.add(deadlineFilter);
    
    // Add separator
    JLabel separator2 = new JLabel("|");
    separator2.setFont(new Font("Dialog", Font.BOLD, 14));
    separator2.setForeground(new Color(206, 212, 218));
    panel.add(separator2);
    
    // Clear filters button
    panel.add(clearFiltersButton);
    
    return panel;
}
    
    
   private void setupEventListeners() {
    // Initialize search timer
    searchTimer = new Timer(300, e -> performFilteredSearch());
    searchTimer.setRepeats(false);

    // Search field listeners
    searchField.getDocument().addDocumentListener(new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) { scheduleSearch(); }
        @Override
        public void removeUpdate(DocumentEvent e) { scheduleSearch(); }
        @Override
        public void changedUpdate(DocumentEvent e) { scheduleSearch(); }
    });

    // Button and filter listeners - REMOVE benefitsFilter listener
    searchButton.addActionListener(e -> performFilteredSearch());
    statusFilter.addActionListener(e -> performFilteredSearch());
    deadlineFilter.addActionListener(e -> performFilteredSearch());
    
    clearFiltersButton.addActionListener(e -> clearAllFilters());
}
    
   private void clearAllFilters() {
    searchField.setText("");
    statusFilter.setSelectedIndex(0);
    deadlineFilter.setSelectedIndex(0);
    performFilteredSearch();
}
    
    private void scheduleSearch() {
        searchTimer.restart();
    }
    
  private void performFilteredSearch() {
    if (allScholarships == null) return;
    
    String searchQuery = searchField.getText().trim().toLowerCase();
    String statusValue = (String) statusFilter.getSelectedItem();
    String deadlineValue = (String) deadlineFilter.getSelectedItem();

    
    filteredScholarships = new ArrayList<>();
    
    for (ScholarshipData scholarship : allScholarships) {
        if (matchesFilters(scholarship, searchQuery, statusValue, deadlineValue)) { // REMOVE benefitsValue parameter
            filteredScholarships.add(scholarship);
        }
    }
    
    displayScholarships(filteredScholarships);
    updateStatusLabel();
}
    
    private boolean matchesFilters(ScholarshipData scholarship, String searchQuery, 
                             String statusFilter, String deadlineFilter) { // REMOVE benefitsFilter parameter
    // Search query filter
    if (!searchQuery.isEmpty()) {
        String searchableText = (scholarship.name + " " + scholarship.description + " " + scholarship.requirements).toLowerCase();
        if (!searchableText.contains(searchQuery)) {
            return false;
        }
    }
        // Status filter
    
    return !(!"All Status".equals(statusFilter) && !statusFilter.equals(scholarship.status));
}
    
   private boolean hasApprovedApplication(int studentId) {
    boolean hasApproved = false;

   String query = "SELECT 1 FROM scholarship_applications WHERE student_id = ? AND status = 'Approved' LIMIT 1";

    try (
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/scholartrack_db", "root", "12345");
        PreparedStatement pst = conn.prepareStatement(query)
    ) {
        pst.setInt(1, studentId);
        ResultSet rs = pst.executeQuery();
        hasApproved = rs.next(); // If any row exists, student has approved app
    } catch (SQLException ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(null,
            "Error connecting to database: " + ex.getMessage(),
            "Database Error",
            JOptionPane.ERROR_MESSAGE);
    }

    return hasApproved;
}
    
    private void loadScholarshipsAsync() {
    SwingWorker<List<ScholarshipData>, Void> worker = new SwingWorker<List<ScholarshipData>, Void>() {
        @Override
        protected List<ScholarshipData> doInBackground() throws Exception {
            return loadScholarshipsFromDatabase();
        }

        @Override
        protected void done() {
            try {
                allScholarships = get();
                
                if (hasApprovedApplication(currentStudentId)) {
                    JOptionPane.showMessageDialog(null, 
                        "You already have an approved application.\nNo more scholarships available.");
                    // Optionally clear UI
                     clearScholarshipsDisplay(); // hide cards
                     disableSearchAndFilters();  // <-- Disable all controls
                    return;
                }

                filteredScholarships = new ArrayList<>(allScholarships);
                displayScholarships(filteredScholarships);
                updateStatusLabel();
            } catch (Exception e) {
                handleDatabaseError(e);
            }
        }
    };
    worker.execute();
}
    
    private void clearScholarshipsDisplay() {
    scholarshipPanel.removeAll(); // or the container where scholarships are shown
    scholarshipPanel.revalidate();
    scholarshipPanel.repaint();
}
    
    private void disableSearchAndFilters() {
    if (searchField != null) searchField.setEnabled(false);
    if (searchButton != null) searchButton.setEnabled(false);
    if (statusFilter != null) statusFilter.setEnabled(false);
    if (deadlineFilter != null) deadlineFilter.setEnabled(false);
    if (clearFiltersButton != null) clearFiltersButton.setEnabled(false);
    
    statusLabel.setText("You already have an exixting scholarship. No more listings available.");
}
    
   private List<ScholarshipData> loadScholarshipsFromDatabase() throws SQLException {
    List<ScholarshipData> scholarships = new ArrayList<>();

    String query = "SELECT * FROM scholarships ORDER BY deadline ASC";

    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/scholartrack_db", "root", "12345");
         PreparedStatement stmt = conn.prepareStatement(query);
         ResultSet rs = stmt.executeQuery()) {

        while (rs.next()) {
            scholarships.add(new ScholarshipData(
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("requirements"),
                rs.getString("benefits"),
                rs.getInt("slot"),
                rs.getString("status"),
                rs.getString("deadline") // Consider parsing to LocalDate if applicable
            ));
        }
    }

    return scholarships;
}
    
    private void displayScholarships(List<ScholarshipData> scholarships) {
        scholarshipPanel.removeAll();
        
        if (scholarships.isEmpty()) {
            displayNoResultsMessage();
        } else {
            for (ScholarshipData scholarship : scholarships) {
                StudentScholarshipCard card = new StudentScholarshipCard(scholarship);
                scholarshipPanel.add(card);
            }
        }
        
        updatePanelSize(scholarships.size());
        scholarshipPanel.revalidate();
        scholarshipPanel.repaint();
    }
    
    private void displayNoResultsMessage() {
        JLabel noResultsLabel = new JLabel("No scholarships found matching your criteria.");
        noResultsLabel.setFont(new Font("Dialog", Font.BOLD, 16));
        noResultsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        noResultsLabel.setForeground(new Color(108, 117, 125));
        scholarshipPanel.add(noResultsLabel);
    }
    
    private void updatePanelSize(int scholarshipCount) {
        int panelHeight = Math.max(scholarshipCount * 250, jScrollPane1.getHeight());
        scholarshipPanel.setPreferredSize(new Dimension(jScrollPane1.getWidth() - 20, panelHeight));
    }
    
    private void updateStatusLabel() {
        int total = allScholarships != null ? allScholarships.size() : 0;
        int filtered = filteredScholarships != null ? filteredScholarships.size() : 0;
        
        if (total == filtered) {
            statusLabel.setText(String.format("Showing %d scholarships", total));
        } else {
            statusLabel.setText(String.format("Showing %d of %d scholarships", filtered, total));
        }
    }
    
    private void handleDatabaseError(Exception e) {
        e.printStackTrace();
        statusLabel.setText("Error loading scholarships. Please try again.");
        JOptionPane.showMessageDialog(this, 
            "Error connecting to database: " + e.getMessage(), 
            "Database Error", 
            JOptionPane.ERROR_MESSAGE);
    }
    
    // Data class for scholarship information
    private static class ScholarshipData {
        final String name, description, requirements, benefits, status, deadline;
        final int slot;
        
        ScholarshipData(String name, String description, String requirements, 
                       String benefits, int slot, String status, String deadline) {
            this.name = name;
            this.description = description;
            this.requirements = requirements;
            this.benefits = benefits;
            this.slot = slot;
            this.status = status;
            this.deadline = deadline;
        }
    }
    
    // Enhanced scholarship card with modern design
    public class StudentScholarshipCard extends JPanel {
        private final ScholarshipData scholarship;
        
        public StudentScholarshipCard(ScholarshipData scholarship) {
            this.scholarship = scholarship;
            setupCard();
        }
        
        private void setupCard() {
            setLayout(new BorderLayout(0, 15));
            setPreferredSize(new Dimension(400, 400));
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(204,204,204), 1, true),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
            ));
            
            // Add hover effect
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0, 123, 255), 2, true),
                        BorderFactory.createEmptyBorder(19, 19, 19, 19)
                    ));
                }
                
                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(222, 226, 230), 1, true),
                        BorderFactory.createEmptyBorder(20, 20, 20, 20)
                    ));
                }
            });
            
            add(createHeaderPanel(), BorderLayout.NORTH);
            add(createContentPanel(), BorderLayout.CENTER);
            add(createButtonPanel(), BorderLayout.SOUTH);
        }
        
        private JPanel createHeaderPanel() {
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(Color.WHITE);
            
            // Title
            JLabel titleLabel = new JLabel("<html>" + scholarship.name + "</html>");
            titleLabel.setFont(new Font("Dialog", Font.BOLD, 18));
            titleLabel.setForeground(new Color(153,153,0));
            
            // Status badge
            JLabel statusBadge = createStatusBadge(scholarship.status);
            
            headerPanel.add(titleLabel, BorderLayout.CENTER);
            headerPanel.add(statusBadge, BorderLayout.EAST);
            
            return headerPanel;
        }
        
        private JLabel createStatusBadge(String status) {
            JLabel badge = new JLabel(status);
            badge.setFont(new Font("Dialog", Font.BOLD, 11));
            badge.setOpaque(true);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
            
            Color bgColor, fgColor;
            switch (status.toLowerCase()) {
                case "open":
                    bgColor = new Color(220, 248, 198);
                    fgColor = new Color(56, 142, 60);
                    break;
                case "closed":
                    bgColor = new Color(255, 235, 238);
                    fgColor = new Color(211, 47, 47);
                    break;
                default:
                    bgColor = new Color(255, 248, 225);
                    fgColor = new Color(245, 124, 0);
            }
            
            badge.setBackground(bgColor);
            badge.setForeground(fgColor);
            
            return badge;
        }
        
  private JScrollPane createContentPanel() {
    String content = String.format(
        "<html><div style='width:250px; font-family: Dialog; font-size: 12px; line-height: 1.4;'>"
        + "<p><b>Deadline:</b> %s</p>"
        + "<p><b>Available Slots:</b> %d</p>"
        + "<p><b>Description:</b><br/>%s</p>"
        + "<p><b>Requirements:</b><br/>%s</p>"
        + "<p><b>Benefits:</b><br/>%s</p>"
        + "</div></html>",
        formatDate(scholarship.deadline),
        scholarship.slot,
        scholarship.description,
        scholarship.requirements,
        scholarship.benefits
    );

    JLabel contentLabel = new JLabel(content);
    contentLabel.setVerticalAlignment(JLabel.TOP);

    JScrollPane scrollPane = new JScrollPane(contentLabel);
    scrollPane.setPreferredSize(new Dimension(280, 160)); // 🔧 Set visible scrollable area
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollPane.getVerticalScrollBar().setUnitIncrement(10); // Smooth scrolling
    scrollPane.getViewport().setBackground(Color.WHITE);

    return scrollPane;
}
        
        private JPanel createButtonPanel() {
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
    buttonPanel.setBackground(Color.WHITE);

    boolean isOpen = "Open".equalsIgnoreCase(scholarship.status);

    // Apply Button
    JButton applyBtn = new JButton(isOpen ? "Apply Now" : "Not Available");
    applyBtn.setEnabled(isOpen);
    styleButton(applyBtn, isOpen ? new Color(40, 167, 69) : new Color(173, 181, 189), Color.WHITE);
    if (isOpen) {
        applyBtn.addActionListener(e -> showApplicationDialog());
    }

    // Optional: Add Read More button if needed
    JButton readMoreBtn = new JButton("Read More");
    styleButton(readMoreBtn, new Color(0, 123, 255), Color.WHITE);
    readMoreBtn.addActionListener(e -> showReadMoreDialog());

    buttonPanel.add(applyBtn);
    buttonPanel.add(readMoreBtn); // Comment this line if you don’t want the extra button

    return buttonPanel;
}
        
 private void showReadMoreDialog() {
    // Main title (scholarship name)
    JLabel titleLabel = new JLabel(scholarship.name);
    titleLabel.setFont(new Font("Dialog", Font.BOLD, 18));
    titleLabel.setForeground(Color.BLACK);
    titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    titleLabel.setBorder(BorderFactory.createEmptyBorder(15, 15, 5, 15));

    // Subtitle
    JLabel subtitleLabel = new JLabel("Details");
    subtitleLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
    subtitleLabel.setForeground(Color.DARK_GRAY);
    subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
    subtitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

    // HTML content
    JTextPane textPane = new JTextPane();
    textPane.setEditable(false);
    textPane.setContentType("text/html");

    String htmlContent = String.format(
        "<html><body style='"
        + "font-family:Dialog; "
        + "font-size:12px; "
        + "line-height:1.6em; "
        + "color:#000000;'>"
        + "<div style='margin-bottom:10px;'><b>Deadline:</b> %s</div>"
        + "<div style='margin-bottom:10px;'><b>Available Slots:</b> %d</div>"
        + "<div style='margin-bottom:10px;'><b>Description:</b><br>%s</div>"
        + "<div style='margin-bottom:10px;'><b>Requirements:</b><br>%s</div>"
        + "<div style='margin-bottom:10px;'><b>Benefits:</b><br>%s</div>"
        + "</body></html>",
        formatDate(scholarship.deadline),
        scholarship.slot,
        scholarship.description.replace("\n", "<br>"),
        scholarship.requirements.replace("\n", "<br>"),
        scholarship.benefits.replace("\n", "<br>")
    );

    textPane.setText(htmlContent);
    textPane.setCaretPosition(0);

    // Scrollable panel for long content
    JScrollPane scrollPane = new JScrollPane(textPane);
    scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 15));
    scrollPane.getViewport().setBackground(Color.WHITE);
    scrollPane.setPreferredSize(new Dimension(500, 350));
    scrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

    // Close button
    JButton closeButton = new JButton("Close");
    closeButton.setFocusPainted(false);
    closeButton.setBackground(Color.BLACK);
    closeButton.setForeground(Color.WHITE);
    closeButton.setFont(new Font("Dialog", Font.BOLD, 13));
    closeButton.setPreferredSize(new Dimension(100, 35));
    closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    closeButton.setAlignmentX(Component.RIGHT_ALIGNMENT);

    // Button panel (right-aligned)
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
    buttonPanel.setOpaque(false);
    buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 15, 15));
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(closeButton);
    buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    // Header panel (title + subtitle stacked)
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
    headerPanel.setOpaque(false);
    headerPanel.add(titleLabel);
    headerPanel.add(subtitleLabel);
    headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

    // Rounded main panel
    JPanel contentPanel = new JPanel() {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.setColor(new Color(220, 220, 220));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    };
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.setOpaque(false);
    contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    contentPanel.add(headerPanel);
    contentPanel.add(scrollPane);
    contentPanel.add(buttonPanel);

    // Dialog setup
    JDialog dialog = new JDialog((Frame) null, "Scholarship Details", true);
    closeButton.addActionListener(e -> dialog.dispose());
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.getContentPane().setBackground(new Color(245, 245, 245));
    dialog.getContentPane().add(contentPanel);
    dialog.pack();
    dialog.setMinimumSize(new Dimension(520, 470));
    dialog.setLocationRelativeTo(null);
    dialog.setResizable(true);
    dialog.setVisible(true);
}
        
        private void showApplicationDialog() {
            EnhancedApplicationFormDialog dialog = new EnhancedApplicationFormDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), 
                scholarship.name
            );
            dialog.setVisible(true);
        }
    }
    
    
   
    // Enhanced application form dialog
 public class EnhancedApplicationFormDialog extends JDialog {
    private final Map<String, JTextField> textFields = new HashMap<>();
    private final Map<String, JTextArea> textAreas = new HashMap<>();
    private final List<File> selectedFiles = new ArrayList<>();
    private JLabel fileLabel;
    private final String scholarshipName;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private int currentStep = 0;
    private JButton nextButton, prevButton, submitButton;
    private JLabel stepIndicator;
    
    // Step names
    private final String[] stepNames = {"Personal Information", "Family Background", "Required Documents"};
    
    public EnhancedApplicationFormDialog(JFrame parent, String scholarshipName) {
        super(parent, "Apply for " + scholarshipName, true);
        this.scholarshipName = scholarshipName;
        setupDialog();
    }
    
    private void setupDialog() {
        setLayout(new BorderLayout());
        setSize(800, 700);
        setLocationRelativeTo(getParent());
        setResizable(false);
        
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainPanel(), BorderLayout.CENTER);
        add(createNavigationPanel(), BorderLayout.SOUTH);
        
        updateNavigationButtons();
        updateStepIndicator();
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 123, 255));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(25, 30, 25, 30));
        
        // Title section
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(0, 123, 255));
        
        JLabel titleLabel = new JLabel("Scholarship Application");
        titleLabel.setFont(new Font("Dialog", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel(scholarshipName);
        subtitleLabel.setFont(new Font("Dialog", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(220, 220, 220));
        
        titlePanel.add(titleLabel, BorderLayout.NORTH);
        titlePanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        // Step indicator
        stepIndicator = new JLabel();
        stepIndicator.setFont(new Font("Dialog", Font.BOLD, 14));
        stepIndicator.setForeground(Color.WHITE);
        stepIndicator.setHorizontalAlignment(SwingConstants.RIGHT);
        
        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(stepIndicator, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JPanel createMainPanel() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(Color.WHITE);
        
        // Add all three panels
        mainPanel.add(createPersonalInfoPanel(), "step0");
        mainPanel.add(createFamilyBackgroundPanel(), "step1");
        mainPanel.add(createFileUploadPanel(), "step2");
        
        return mainPanel;
    }
    
  private JScrollPane createPersonalInfoPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
    
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 0, 20, 0);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    
    // Initialize school programs and location data mapping
    initializeSchoolPrograms();
    initializeLocationData(); // NEW METHOD
    
    // Section title
    JLabel sectionTitle = new JLabel("Personal Information");
    sectionTitle.setFont(new Font("Dialog", Font.BOLD, 22));
    sectionTitle.setForeground(new Color(52, 58, 64));
    gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
    panel.add(sectionTitle, gbc);
    
    // Personal info fields - MODIFIED to include school and course as combo
    Object[][] personalFields = {
        {"firstName", "First Name", "text"},
        {"middleName", "Middle Name", "text"},
        {"lastName", "Last Name", "text"},
        {"studentNumber", "Student Number", "text"},
        {"email", "Email Address", "text"},
        {"phone", "Phone Number", "text"},
        {"birthDate", "Date of Birth", "date"},
        {"gender", "Gender", "combo"},
        {"address", "Home Address", "textarea"},
        {"province", "Province", "combo"},
        {"municipality", "Municipality", "combo"},
        {"barangay", "Barangay", "combo"},
        {"zipCode", "ZIP/Postal Code", "text"},
        {"school", "School", "combo"},
        {"course", "Course/Program", "combo"},
        {"yearLevel", "Year Level", "combo"},
        {"gwa", "Current GWA/Grade", "text"}
    };
    
    // Gender options for the combo box
    String[] genderOptions = {
        "Select Gender",
        "Male",
        "Female",
        "Non-binary",
        "Prefer not to say",
        
    };
    
    // Year Level options for the combo box
    String[] yearLevelOptions = {
        "Select Year Level",
        "First Year",
        "Second Year",
        "Third Year",
        "Fourth Year",
        "Fifth Year",
    };
    
    // Province options - Aurora only
    String[] provinceOptions = {
        "Select Province",
        "Aurora"
    };
    
    // School options for the combo box
    String[] schoolOptions = {
        "Select School",
        "Aurora State College of Technology",
        
    };
    
    gbc.gridwidth = 1;
    for (int i = 0; i < personalFields.length; i++) {
        Object[] field = personalFields[i];
        gbc.gridx = 0; gbc.gridy = i + 1;
        
        JLabel label = new JLabel(field[1] + ":");
        label.setFont(new Font("Dialog", Font.BOLD, 14));
        panel.add(label, gbc);
        
        gbc.gridx = 1;
        String fieldType = (String) field[2];
        
        if ("textarea".equals(fieldType)) {
            JTextArea textArea = new JTextArea(3, 30);
            textArea.setFont(new Font("Dialog", Font.PLAIN, 14));
            textArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
            ));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(350, 80));
            textAreas.put((String) field[0], textArea);
            panel.add(scrollPane, gbc);
            
        } else if ("date".equals(fieldType)) {
            JDateChooser dateChooser = new JDateChooser();
            dateChooser.setPreferredSize(new Dimension(350, 35));
            dateChooser.setFont(new Font("Dialog", Font.PLAIN, 14));
            
            dateChooser.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
            
            dateChooser.setDateFormatString("yyyy-MM-dd");
            
            dateChoosers.put((String) field[0], dateChooser);
            panel.add(dateChooser, gbc);
            
        } else if ("combo".equals(fieldType)) {
            // Create JComboBox for different field types
            String[] options;
            String defaultOption;
            boolean enableLocationCascading = false;
            boolean enableSchoolCascading = false;
            
            if ("gender".equals(field[0])) {
                options = genderOptions;
                defaultOption = "Select Gender";
            } else if ("yearLevel".equals(field[0])) {
                options = yearLevelOptions;
                defaultOption = "Select Year Level";
            } else if ("province".equals(field[0])) {
                options = provinceOptions;
                defaultOption = "Select Province";
                enableLocationCascading = true;
            } else if ("municipality".equals(field[0])) {
                options = new String[]{"Select Province First"};
                defaultOption = "Select Province First";
            } else if ("barangay".equals(field[0])) {
                options = new String[]{"Select Municipality First"};
                defaultOption = "Select Municipality First";
            } else if ("school".equals(field[0])) {
                options = schoolOptions;
                defaultOption = "Select School";
                enableSchoolCascading = true;
            } else if ("course".equals(field[0])) {
                options = new String[]{"Please select a school first"};
                defaultOption = "Please select a school first";
            } else {
                options = new String[]{"Select Option"};
                defaultOption = "Select Option";
            }
            
            JComboBox<String> comboBox = new JComboBox<>(options);
            comboBox.setPreferredSize(new Dimension(350, 35));
            comboBox.setFont(new Font("Dialog", Font.PLAIN, 14));
            
            comboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218)),
                BorderFactory.createEmptyBorder(2, 8, 2, 8)
            ));
            
            comboBox.setBackground(Color.WHITE);
            
            // Special handling for dependent combos - initially disabled
            if ("municipality".equals(field[0]) || "barangay".equals(field[0]) || "course".equals(field[0])) {
                comboBox.setEnabled(false);
            }
            
            // Add action listener for selection changes
            final String fieldName = (String) field[0];
            final String defOption = defaultOption;
            final boolean locationCascade = enableLocationCascading;
            final boolean schoolCascade = enableSchoolCascading;
            
            comboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String selected = (String) comboBox.getSelectedItem();
                    if (!defOption.equals(selected)) {
                        System.out.println(fieldName + " selected: " + selected);
                        
                        // Handle cascading for location selection
                        if (locationCascade && "province".equals(fieldName)) {
                            updateMunicipalityComboBox(selected);
                        }
                        
                        // Handle cascading for school selection
                        if (schoolCascade && "school".equals(fieldName)) {
                            updateCourseComboBox(selected);
                        }
                    }
                }
            });
            
            // Special handling for municipality selection
            if ("municipality".equals(field[0])) {
                comboBox.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        String selected = (String) comboBox.getSelectedItem();
                        if (selected != null && !selected.equals("Select Municipality") && 
                            !selected.equals("Select Province First")) {
                            updateBarangayComboBox(selected);
                        }
                    }
                });
            }
            
            comboBoxes.put((String) field[0], comboBox);
            panel.add(comboBox, gbc);
            
        } else {
            // Regular text field
            JTextField textField = createStyledTextField();
            textFields.put((String) field[0], textField);
            panel.add(textField, gbc);
        }
    }
    
    JScrollPane scrollPane = new JScrollPane(panel);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    return scrollPane;
}
  
  private void initializeLocationData() {
    locationData = new HashMap<>();
    
    // Aurora Province only
    Map<String, String[]> auroraData = new HashMap<>();
    auroraData.put("Baler", new String[]{
        "Select Barangay",
        "Barangay I (Poblacion)", "Barangay II (Poblacion)", "Barangay III (Poblacion)", "Barangay IV (Poblacion)",
        "Buhangin", "Calabuanan", "Obligacion", "Pingit", "Reserva", "Sabang", "Suklayin", "Zabali"
    });
    auroraData.put("Casiguran", new String[]{
        "Select Barangay",
        "Barangay 1 (Poblacion)", "Barangay 2 (Poblacion)", "Barangay 3 (Poblacion)", "Barangay 4 (Poblacion)", "Barangay 5 (Poblacion)", "Barangay 6 (Poblacion)", "Barangay 7 (Poblacion)",
        "Barangay 8 (Poblacion)", "Bianuan", "Calabgan", "Calangcuasan", "Calantas", "Cozo", "Culat", "Dibacong", "Dibet", "Ditinagyan", "Esperanza",  "Esteves", "Lual", "Marikit", "San Ildefonso", "Tabas",  "Tinib"
    });
    auroraData.put("Dilasag", new String[]{
        "Select Barangay",
        "Diagyan", "Dicabasan", "Dilaguidi", "Dimaseset", "Diniog", "Esperanza", "Lawang", "Maligaya (Poblacion)", "Manggitahan", "Masagana", "	Ura"
    });
    auroraData.put("Dinalungan", new String[]{
        "Select Barangay",
        "Abuleg", "Dibaraybay", "Ditawini", "Mapalad", "Nipoo (Bulo)", "Paleg", "Simbahan", "ZONE I (Poblacion)", "Zone II (Poblacion)"
    });
    auroraData.put("Dingalan", new String[]{
        "Select Barangay",
        "Aplaya", "Butas Na Bato", "Caragsacan", "Davildavilan", "Ibona", "Matawe (Cabog)", "Paltic", "Poblacion", "Tanawan", "Umiray (Malamig)"
    });
    auroraData.put("Dipaculao", new String[]{
        "Select Barangay",
        "Bayabas", "Borlongan", "Calaocan", "Diamanen", "Dianed", "Diarabasin", "Dibutunan", "Dimabuno", "Dinadiawan", "Ditale", "Gupa", "Ipil", "Laboy", "Lipit",
        "Lobbot", "Maligaya", "Mijares", "Mucdol", "North Poblacion", "Puangi", "Salay", "Sapangkawayan", "South Poblacion", "Toytoyan"
    });
    auroraData.put("Maria Aurora", new String[]{
        "Select Barangay",
        "Alcala", "Bagtu", "Bangco", "Balete", "Bannawag", "Barangay I (Poblacion)", "Barangay II (Poblacion)", "Barangay III (Poblacion)", "Barangay IV (Poblacion)", "Baubo", "Bayanihan", "Bazal", 
        "Cabituculan East", "Cabituculan West", "Cadayacan", "Debucao", "Decoliat", "Detailen", "Diaat", "Dialatman", "Diaman", "Dianawan", "Dikildit", "Dimanpudso", "Diome", "Estonilo", "Florida", 
        "Galintuja", "Malasin", "Ponglo", "Quirino", "Ramada", "San Joaquin", "San Jose", "San Juan", "San Leonardo", "Santa Lucia", "Santo Tomas", "Suguit", "Villa Aurora", "Wenceslao"
    });
    auroraData.put("San Luis", new String[]{
        "Select Barangay",
        "Bacong", "Barangay I (Poblacion)", "Barangay II (Poblacion)", "Barangay III (Poblacion)", "Barangay IV (Poblacion)", "Dibalo", "Dibayabay", "Dibut", "Dikapinisan", "Dimanayat", "Diteki", "Ditumabo", "L. Pimintel", "Nonong Senior",
        "Real", "San Isidro", "San Jose", "Zarah"
    });
    locationData.put("Aurora", auroraData);
}

  private void updateMunicipalityComboBox(String selectedProvince) {
    JComboBox<String> municipalityCombo = comboBoxes.get("municipality");
    JComboBox<String> barangayCombo = comboBoxes.get("barangay");
    
    if (municipalityCombo != null) {
        municipalityCombo.removeAllItems();
        
        if (selectedProvince != null && !selectedProvince.equals("Select Province")) {
            Map<String, String[]> provinceData = locationData.get(selectedProvince);
            if (provinceData != null) {
                municipalityCombo.addItem("Select Municipality");
                for (String municipality : provinceData.keySet()) {
                    municipalityCombo.addItem(municipality);
                }
                municipalityCombo.setEnabled(true);
            } else {
                municipalityCombo.addItem("No municipalities available");
                municipalityCombo.setEnabled(false);
            }
        } else {
            municipalityCombo.addItem("Select Province First");
            municipalityCombo.setEnabled(false);
        }
        
        // Reset barangay dropdown
        if (barangayCombo != null) {
            barangayCombo.removeAllItems();
            barangayCombo.addItem("Select Municipality First");
            barangayCombo.setEnabled(false);
        }
    }
}

// NEW METHOD: Update barangay dropdown based on selected municipality
private void updateBarangayComboBox(String selectedMunicipality) {
    JComboBox<String> barangayCombo = comboBoxes.get("barangay");
    JComboBox<String> provinceCombo = comboBoxes.get("province");
    
    if (barangayCombo != null && provinceCombo != null) {
        String selectedProvince = (String) provinceCombo.getSelectedItem();
        barangayCombo.removeAllItems();
        
        if (selectedProvince != null && !selectedProvince.equals("Select Province") &&
            selectedMunicipality != null && !selectedMunicipality.equals("Select Municipality")) {
            
            Map<String, String[]> provinceData = locationData.get(selectedProvince);
            if (provinceData != null && provinceData.containsKey(selectedMunicipality)) {
                String[] barangays = provinceData.get(selectedMunicipality);
                for (String barangay : barangays) {
                    barangayCombo.addItem(barangay);
                }
                barangayCombo.setEnabled(true);
            } else {
                barangayCombo.addItem("No barangays available");
                barangayCombo.setEnabled(false);
            }
        } else {
            barangayCombo.addItem("Select Municipality First");
            barangayCombo.setEnabled(false);
        }
    }
}
  
     public String getSelectedMunicipality() {
    JComboBox<String> municipalityCombo = comboBoxes.get("municipality");
    if (municipalityCombo != null) {
        String selected = (String) municipalityCombo.getSelectedItem();
        return ("Select Municipality".equals(selected) || "Select Province First".equals(selected)) ? null : selected;
    }
    return null;
}

public String getSelectedBarangay() {
    JComboBox<String> barangayCombo = comboBoxes.get("barangay");
    if (barangayCombo != null) {
        String selected = (String) barangayCombo.getSelectedItem();
        return ("Select Barangay".equals(selected) || "Select Municipality First".equals(selected)) ? null : selected;
    }
    return null;
}
 
    private void initializeSchoolPrograms() {
    schoolPrograms.put("Aurora State College of Technology", new String[]{
        "Select Course/Program",
        "Bachelor of Science in Agriculture(BSA ANIMAL SCIENCE)",
        "Bachelor of Science in Agriculture(BSA CROP SCIENCE)",
        "Bachelor of Science in Fisheries (BSFi)",
        "Bachelor of Elementary Education (BEEd)",
        "Bachelor of Secondary Education (BSEd ENGLISH)",
        "Bachelor of Secondary Education (BSEd FILIPINO)",
        "Bachelor of Secondary Education (BSEd MATHEMATICS)",
        "Bachelor of Secondary Education (BSEd SCIENCE)",
        "Bachelor of Secondary Education (BSEd SOCIAL STUDIES)",
        "Bachelor of Technology and Livelihood Education (BTLED HOME ECONOMICS)",
        "Bachelor of Technology and Livelihood Education (BTLED ICT)",
        "Bachelor of Physical Education (BPEd)",
        "Bachelor of Science in Civil Engineering (BSCE)",
        "Bachelor of Science in Electrical Engineering (BSEE)", 
        "Bachelor of Science in Mechanical Engineering(BSME)",
        "Bachelor of Science in Forestry",
        "Bachelor of Science in Information Technology (BSIT)",
        "Bachelor in Industrial Technology (BIT)",
        "Bachelor of Science in Hospitality Management (BSHM)",
        "Bachelor of Science in Tourism Management (BSTM)",
        "Bachelor of Science in Marine Biology (BSMB)",
        "Bachelor of Science in Environmental Sciences (BSES)",
        "Bachelor of Science in Accountancy (BSAc)",
        "Bachelor of Arts in Political Science (BAPolSci)",
    });
    }
    private void updateCourseComboBox(String selectedSchool) {
    JComboBox<String> courseCombo = comboBoxes.get("course");
    
    if (courseCombo != null) {
        courseCombo.removeAllItems();
        
        if (selectedSchool != null && !selectedSchool.equals("Select School")) {
            String[] programs = schoolPrograms.get(selectedSchool);
            if (programs != null) {
                for (String program : programs) {
                    courseCombo.addItem(program);
                }
                courseCombo.setEnabled(true);
            } else {
                courseCombo.addItem("No programs available");
                courseCombo.setEnabled(false);
            }
        } else {
            courseCombo.addItem("Please select a school first");
            courseCombo.setEnabled(false);
        }
    }
}
   
 public java.util.Date getBirthDate() {
    JDateChooser dateChooser = dateChoosers.get("birthDate");
    return dateChooser != null ? dateChooser.getDate() : null;
}

public String getSelectedGender() {
    JComboBox<String> genderCombo = comboBoxes.get("gender");
    if (genderCombo != null) {
        String selected = (String) genderCombo.getSelectedItem();
        return "Select Gender".equals(selected) ? null : selected;
    }
    return null;
}

public String getSelectedYearLevel() {
    JComboBox<String> yearLevelCombo = comboBoxes.get("yearLevel");
    if (yearLevelCombo != null) {
        String selected = (String) yearLevelCombo.getSelectedItem();
        return "Select Year Level".equals(selected) ? null : selected;
    }
    return null;
}

public String getSelectedProvince() {
    JComboBox<String> provinceCombo = comboBoxes.get("province");
    if (provinceCombo != null) {
        String selected = (String) provinceCombo.getSelectedItem();
        return "Select Province".equals(selected) ? null : selected;
    }
    return null;
}

public String getSelectedCity() {
    JComboBox<String> cityCombo = comboBoxes.get("city");
    if (cityCombo != null) {
        String selected = (String) cityCombo.getSelectedItem();
        return "Select Municipality/City".equals(selected) ? null : selected;
    }
    return null;
}


// Method to validate the form
public boolean validatePersonalInfo() {
    // Check if birth date is selected
    if (getBirthDate() == null) {
        JOptionPane.showMessageDialog(null, "Please select your date of birth.");
        return false;
    }
    
    // Check if gender is selected
    if (getSelectedGender() == null) {
        JOptionPane.showMessageDialog(null, "Please select your gender.");
        return false;
    }

 
    if (getSelectedYearLevel() == null) {
        JOptionPane.showMessageDialog(null, "Please select your year level.");
        return false;
    }
     // Check if province is selected
    if (getSelectedProvince() == null) {
        JOptionPane.showMessageDialog(null, "Please select your province.");
        return false;
    }
    
    // Check if city is selected
    if (getSelectedCity() == null) {
        JOptionPane.showMessageDialog(null, "Please select your municipality.");
        return false;
    }
    
    // Check if barangay is selected
    if (getSelectedBarangay() == null) {
        JOptionPane.showMessageDialog(null, "Please select your barangay.");
        return false;
    }
    
    // Add other validation logic as needed
    return true;
}
    
    private JScrollPane createFamilyBackgroundPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
    
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, 0, 20, 0);
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    
    // Section title
    JLabel sectionTitle = new JLabel("Family Background");
    sectionTitle.setFont(new Font("Dialog", Font.BOLD, 20));
    sectionTitle.setForeground(new Color(52, 58, 64));
    gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
    panel.add(sectionTitle, gbc);
    
    // Father's Information
    JLabel fatherTitle = new JLabel("Father's Information");
    fatherTitle.setFont(new Font("Dialog", Font.BOLD, 16));
    fatherTitle.setForeground(new Color(0, 123, 255));
    gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
    panel.add(fatherTitle, gbc);
    
    // Father's fields with combo boxes for income and education
    gbc.gridwidth = 1;
    int currentRow = 2;
    
    // Father's Name
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel fatherNameLabel = new JLabel("Father's Full Name:");
    fatherNameLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(fatherNameLabel, gbc);
    gbc.gridx = 1;
    JTextField fatherNameField = createStyledTextField();
    textFields.put("fatherName", fatherNameField);
    panel.add(fatherNameField, gbc);
    currentRow++;
    
    // Father's Age
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel fatherAgeLabel = new JLabel("Father's Age:");
    fatherAgeLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(fatherAgeLabel, gbc);
    gbc.gridx = 1;
    JTextField fatherAgeField = createStyledTextField();
    textFields.put("fatherAge", fatherAgeField);
    panel.add(fatherAgeField, gbc);
    currentRow++;
    
    // Father's Occupation
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel fatherOccupationLabel = new JLabel("Father's Occupation:");
    fatherOccupationLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(fatherOccupationLabel, gbc);
    gbc.gridx = 1;
    JTextField fatherOccupationField = createStyledTextField();
    textFields.put("fatherOccupation", fatherOccupationField);
    panel.add(fatherOccupationField, gbc);
    currentRow++;
    
    // Father's Monthly Income (ComboBox)
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel fatherIncomeLabel = new JLabel("Father's Monthly Income:");
    fatherIncomeLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(fatherIncomeLabel, gbc);
    gbc.gridx = 1;
    JComboBox<String> fatherIncomeCombo = createIncomeComboBox();
    comboBoxes.put("fatherIncome", fatherIncomeCombo);
    panel.add(fatherIncomeCombo, gbc);
    currentRow++;
    
    // Father's Educational Attainment (ComboBox)
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel fatherEducationLabel = new JLabel("Father's Educational Attainment:");
    fatherEducationLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(fatherEducationLabel, gbc);
    gbc.gridx = 1;
    JComboBox<String> fatherEducationCombo = createEducationComboBox();
    comboBoxes.put("fatherEducation", fatherEducationCombo);
    panel.add(fatherEducationCombo, gbc);
    currentRow++;
    
    // Mother's Information
    JLabel motherTitle = new JLabel("Mother's Information");
    motherTitle.setFont(new Font("Dialog", Font.BOLD, 16));
    motherTitle.setForeground(new Color(0, 123, 255));
    gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 2;
    gbc.insets = new Insets(30, 0, 20, 0);
    panel.add(motherTitle, gbc);
    currentRow++;
    
    gbc.insets = new Insets(0, 0, 20, 0);
    gbc.gridwidth = 1;
    
    // Mother's Name
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel motherNameLabel = new JLabel("Mother's Full Name:");
    motherNameLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(motherNameLabel, gbc);
    gbc.gridx = 1;
    JTextField motherNameField = createStyledTextField();
    textFields.put("motherName", motherNameField);
    panel.add(motherNameField, gbc);
    currentRow++;
    
    // Mother's Age
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel motherAgeLabel = new JLabel("Mother's Age:");
    motherAgeLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(motherAgeLabel, gbc);
    gbc.gridx = 1;
    JTextField motherAgeField = createStyledTextField();
    textFields.put("motherAge", motherAgeField);
    panel.add(motherAgeField, gbc);
    currentRow++;
    
    // Mother's Occupation
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel motherOccupationLabel = new JLabel("Mother's Occupation:");
    motherOccupationLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(motherOccupationLabel, gbc);
    gbc.gridx = 1;
    JTextField motherOccupationField = createStyledTextField();
    textFields.put("motherOccupation", motherOccupationField);
    panel.add(motherOccupationField, gbc);
    currentRow++;
    
    // Mother's Monthly Income (ComboBox)
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel motherIncomeLabel = new JLabel("Mother's Monthly Income:");
    motherIncomeLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(motherIncomeLabel, gbc);
    gbc.gridx = 1;
    JComboBox<String> motherIncomeCombo = createIncomeComboBox();
    comboBoxes.put("motherIncome", motherIncomeCombo);
    panel.add(motherIncomeCombo, gbc);
    currentRow++;
    
    // Mother's Educational Attainment (ComboBox)
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel motherEducationLabel = new JLabel("Mother's Educational Attainment:");
    motherEducationLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(motherEducationLabel, gbc);
    gbc.gridx = 1;
    JComboBox<String> motherEducationCombo = createEducationComboBox();
    comboBoxes.put("motherEducation", motherEducationCombo);
    panel.add(motherEducationCombo, gbc);
    currentRow++;
    
    // Additional Family Information
    JLabel additionalTitle = new JLabel("Additional Family Information");
    additionalTitle.setFont(new Font("Dialog", Font.BOLD, 16));
    additionalTitle.setForeground(new Color(0, 123, 255));
    gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 2;
    gbc.insets = new Insets(30, 0, 20, 0);
    panel.add(additionalTitle, gbc);
    currentRow++;
    
    gbc.insets = new Insets(0, 0, 20, 0);
    gbc.gridwidth = 1;
    
    // Total Family Monthly Income (ComboBox)
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel totalIncomeLabel = new JLabel("Total Family Monthly Income:");
    totalIncomeLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(totalIncomeLabel, gbc);
    gbc.gridx = 1;
    JComboBox<String> totalIncomeCombo = createIncomeComboBox();
    comboBoxes.put("totalFamilyIncome", totalIncomeCombo);
    panel.add(totalIncomeCombo, gbc);
    currentRow++;
    
    // Number of Siblings
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel siblingsLabel = new JLabel("Number of Siblings:");
    siblingsLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(siblingsLabel, gbc);
    gbc.gridx = 1;
    JTextField siblingsField = createStyledTextField();
    textFields.put("numberOfSiblings", siblingsField);
    panel.add(siblingsField, gbc);
    currentRow++;
    
    // Total Number of Dependents
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel dependentsLabel = new JLabel("Total Number of Dependents:");
    dependentsLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(dependentsLabel, gbc);
    gbc.gridx = 1;
    JTextField dependentsField = createStyledTextField();
    textFields.put("numberOfDependents", dependentsField);
    panel.add(dependentsField, gbc);
    currentRow++;
    
    // Housing Status
    gbc.gridx = 0; gbc.gridy = currentRow;
    JLabel housingLabel = new JLabel("Housing Status:");
    housingLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(housingLabel, gbc);
    gbc.gridx = 1;
    String[] housingOptions = {"Select Housing Status", "Own", "Rent", "Family-owned", "Others"};
    JComboBox<String> housingCombo = new JComboBox<>(housingOptions);
    housingCombo.setFont(new Font("Dialog", Font.PLAIN, 14));
    housingCombo.setPreferredSize(new Dimension(200, 35));
    comboBoxes.put("housingStatus", housingCombo);
    panel.add(housingCombo, gbc);
    currentRow++;
    
    // Special Circumstances
    gbc.gridx = 0; gbc.gridy = currentRow; gbc.gridwidth = 2;
    gbc.insets = new Insets(10, 0, 10, 0);
    JLabel circumstancesLabel = new JLabel("Special Family Circumstances (if any):");
    circumstancesLabel.setFont(new Font("Dialog", Font.BOLD, 14));
    panel.add(circumstancesLabel, gbc);
    
    gbc.gridy = currentRow + 1;
    JTextArea circumstancesArea = new JTextArea(4, 30);
    circumstancesArea.setFont(new Font("Dialog", Font.PLAIN, 14));
    circumstancesArea.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(206, 212, 218)),
        BorderFactory.createEmptyBorder(8, 12, 8, 12)
    ));
    circumstancesArea.setLineWrap(true);
    circumstancesArea.setWrapStyleWord(true);
    
    JScrollPane circumstancesScroll = new JScrollPane(circumstancesArea);
    circumstancesScroll.setPreferredSize(new Dimension(400, 100));
    textAreas.put("specialCircumstances", circumstancesArea);
    panel.add(circumstancesScroll, gbc);
    
    JScrollPane scrollPane = new JScrollPane(panel);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    return scrollPane;
}
    
    private JComboBox<String> createIncomeComboBox() {
    String[] incomeOptions = {
        "Select Income Range",
        "Under ₱10,000",
        "₱10,000 - ₱19,999",
        "₱20,000 - ₱29,999",
        "₱30,000 - ₱39,999",
        "₱40,000 - ₱49,999",
        "₱50,000 - ₱59,999",
        "₱60,000 - ₱79,999",
        "₱80,000 - ₱99,999",
        "₱100,000 - ₱149,999",
        "₱150,000 - ₱199,999",
        "₱200,000 - ₱299,999",
        "₱300,000 and above",
    
    };
    
    JComboBox<String> comboBox = new JComboBox<>(incomeOptions);
    comboBox.setFont(new Font("Dialog", Font.PLAIN, 14));
    comboBox.setPreferredSize(new Dimension(200, 35));
    return comboBox;
}

    
    
    private JComboBox<String> createEducationComboBox() {
    String[] educationOptions = {
        "Select Education Level",
        "No formal education",
        "Elementary (Undergraduate)",
        "Elementary Graduate",
        "High School (Undergraduate)",
        "High School Graduate",
        "Vocational/Technical Course",
        "College (Undergraduate)",
        "College Graduate",

        
    };
    
    JComboBox<String> comboBox = new JComboBox<>(educationOptions);
    comboBox.setFont(new Font("Dialog", Font.PLAIN, 14));
    comboBox.setPreferredSize(new Dimension(200, 35));
    return comboBox;
}
    
    
  private JPanel createFileUploadPanel() {
    JPanel panel = new JPanel(new BorderLayout(0, 20));
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(40, 167, 69), 2),
            "Upload Your Documents",
            0, 0, new Font("Dialog", Font.BOLD, 14), new Color(40, 167, 69)
        ),
        BorderFactory.createEmptyBorder(20, 20, 20, 20)
    ));
    
    // Upload button and info
    JPanel uploadControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    uploadControlPanel.setBackground(Color.WHITE);
    
    JButton selectFilesBtn = new JButton("📎 Select Files");
    styleButton(selectFilesBtn, new Color(0, 123, 255), Color.WHITE);
    selectFilesBtn.setFont(new Font("Dialog", Font.BOLD, 14));
    selectFilesBtn.addActionListener(e -> selectFiles());
    
    clearAllBtn = new JButton("🗑️ Clear All");
    styleButton(clearAllBtn, new Color(220, 53, 69), Color.WHITE);
    clearAllBtn.setFont(new Font("Dialog", Font.BOLD, 12));
    clearAllBtn.addActionListener(e -> clearAllFiles());
    clearAllBtn.setEnabled(false);
    
    uploadControlPanel.add(selectFilesBtn);
    uploadControlPanel.add(Box.createHorizontalStrut(10));
    uploadControlPanel.add(clearAllBtn);
    
    // File status display
    fileLabel = new JLabel("No files selected");
    fileLabel.setFont(new Font("Dialog", Font.ITALIC, 14));
    fileLabel.setForeground(new Color(108, 117, 125));
    fileLabel.setBorder(BorderFactory.createEmptyBorder(15, 0, 10, 0));
    
    // Create files list panel
    filesListPanel = new JPanel();
    filesListPanel.setLayout(new BoxLayout(filesListPanel, BoxLayout.Y_AXIS));
    filesListPanel.setBackground(Color.WHITE);
    
    filesScrollPane = new JScrollPane(filesListPanel);
    filesScrollPane.setPreferredSize(new Dimension(0, 150));
    filesScrollPane.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
        BorderFactory.createEmptyBorder(5, 5, 5, 5)
    ));
    filesScrollPane.setVisible(false);
    
    // Center panel to hold file label and list
    JPanel centerPanel = new JPanel(new BorderLayout());
    centerPanel.setBackground(Color.WHITE);
    centerPanel.add(fileLabel, BorderLayout.NORTH);
    centerPanel.add(filesScrollPane, BorderLayout.CENTER);
    
    panel.add(uploadControlPanel, BorderLayout.NORTH);
    panel.add(centerPanel, BorderLayout.CENTER);
    
    // Accepted formats
    JLabel formatLabel = new JLabel("<html><b>Accepted formats:</b> PDF, DOC, DOCX, JPG, JPEG, PNG<br/>" +
        "<b>Maximum file size:</b> 10MB per file<br/>" +
        "<b>Note:</b> You can select multiple files at once</html>");
    formatLabel.setFont(new Font("Dialog", Font.PLAIN, 12));
    formatLabel.setForeground(new Color(108, 117, 125));
    panel.add(formatLabel, BorderLayout.SOUTH);
    
    return panel;
}

private void selectFiles() {
    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setMultiSelectionEnabled(true);
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    
    // Set file filter to match your accepted formats
    fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            String name = f.getName().toLowerCase();
            return name.endsWith(".pdf") || name.endsWith(".doc") || 
                   name.endsWith(".docx") || name.endsWith(".jpg") || 
                   name.endsWith(".jpeg") || name.endsWith(".png");
        }
        
        @Override
        public String getDescription() {
            return "Supported files (PDF, DOC, DOCX, JPG, JPEG, PNG)";
        }
    });
    
    int result = fileChooser.showOpenDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File[] files = fileChooser.getSelectedFiles();
        addFiles(Arrays.asList(files));
    }
}

private void addFiles(List<File> newFiles) {
    for (File file : newFiles) {
        // Check file size (10MB limit)
        if (file.length() > 10 * 1024 * 1024) {
            JOptionPane.showMessageDialog(this, 
                "File '" + file.getName() + "' exceeds 10MB limit and will be skipped.",
                "File Size Warning", JOptionPane.WARNING_MESSAGE);
            continue;
        }
        
        // Avoid duplicates
        if (!selectedFiles.contains(file)) {
            selectedFiles.add(file);
        }
    }
    updateFileDisplay();
}

private void updateFileDisplay() {
    filesListPanel.removeAll();
    
    if (selectedFiles.isEmpty()) {
        fileLabel.setText("No files selected");
        filesScrollPane.setVisible(false);
        clearAllBtn.setEnabled(false);
    } else {
        // Update main label
        String totalSizeText = " (Total: " + formatFileSize(getTotalSize()) + ")";
        fileLabel.setText(selectedFiles.size() + " file(s) selected" + totalSizeText);
        clearAllBtn.setEnabled(true);
        
        // Show individual files
        for (int i = 0; i < selectedFiles.size(); i++) {
            File file = selectedFiles.get(i);
            JPanel fileItemPanel = createFileItemPanel(file, i);
            filesListPanel.add(fileItemPanel);
            if (i < selectedFiles.size() - 1) {
                filesListPanel.add(Box.createVerticalStrut(3));
            }
        }
        
        filesScrollPane.setVisible(true);
    }
    
    filesListPanel.revalidate();
    filesListPanel.repaint();
}

private JPanel createFileItemPanel(File file, int index) {
    JPanel itemPanel = new JPanel(new BorderLayout(8, 0));
    itemPanel.setBackground(new Color(248, 249, 250));
    itemPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
        BorderFactory.createEmptyBorder(6, 10, 6, 10)
    ));
    
    // File info
    JPanel infoPanel = new JPanel(new BorderLayout(8, 0));
    infoPanel.setBackground(new Color(248, 249, 250));
    
    JLabel iconLabel = new JLabel(getFileIcon(file));
    iconLabel.setFont(new Font("Dialog", Font.PLAIN, 16));
    
    JLabel nameLabel = new JLabel("<html><b>" + file.getName() + "</b><br/>" +
        "<small style='color: #6c757d;'>" + formatFileSize(file.length()) + 
        " • " + getFileExtension(file).toUpperCase() + "</small></html>");
    nameLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
    
    infoPanel.add(iconLabel, BorderLayout.WEST);
    infoPanel.add(nameLabel, BorderLayout.CENTER);
    
    // Remove button
    JButton removeBtn = new JButton("✕");
    removeBtn.setFont(new Font("Dialog", Font.BOLD, 14));
    removeBtn.setForeground(new Color(220, 53, 69));
    removeBtn.setBackground(Color.WHITE);
    removeBtn.setBorder(BorderFactory.createLineBorder(new Color(220, 53, 69), 1));
    removeBtn.setPreferredSize(new Dimension(24, 24));
    removeBtn.setFocusPainted(false);
    removeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
    removeBtn.addActionListener(e -> removeFile(index));
    
    itemPanel.add(infoPanel, BorderLayout.CENTER);
    itemPanel.add(removeBtn, BorderLayout.EAST);
    
    return itemPanel;
}

private void removeFile(int index) {
    if (index >= 0 && index < selectedFiles.size()) {
        selectedFiles.remove(index);
        updateFileDisplay();
    }
}

private void clearAllFiles() {
    int result = JOptionPane.showConfirmDialog(this,
        "Are you sure you want to remove all selected files?",
        "Clear All Files", JOptionPane.YES_NO_OPTION);
    
    if (result == JOptionPane.YES_OPTION) {
        selectedFiles.clear();
        updateFileDisplay();
    }
}

private String getFileIcon(File file) {
    String extension = getFileExtension(file).toLowerCase();
        return switch (extension) {
            case "pdf" -> "📄";
            case "doc", "docx" -> "📝";
            case "jpg", "jpeg", "png" -> "🖼️";
            default -> "📁";
        };
}

private String getFileExtension(File file) {
    String name = file.getName();
    int lastDot = name.lastIndexOf('.');
    return lastDot > 0 ? name.substring(lastDot + 1) : "";
}

private String formatFileSize(long bytes) {
    if (bytes == 0) return "0 B";
    String[] units = {"B", "KB", "MB", "GB"};
    int unitIndex = 0;
    double size = bytes;
    
    while (size >= 1024 && unitIndex < units.length - 1) {
        size /= 1024;
        unitIndex++;
    }
    
    java.text.DecimalFormat df = new java.text.DecimalFormat("#.##");
    return df.format(size) + " " + units[unitIndex];
}

private long getTotalSize() {
    return selectedFiles.stream().mapToLong(File::length).sum();
}

  public List<File> getSelectedFiles() {
    return new ArrayList<>(selectedFiles);
}

public boolean hasFiles() {
    return !selectedFiles.isEmpty();
}

    
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Dialog", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(350, 35));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(206, 212, 218)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return field;
    }
    
    private JPanel createNavigationPanel() {
        JPanel navPanel = new JPanel(new BorderLayout());
        navPanel.setBackground(Color.WHITE);
        navPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(20, 30, 20, 30)
        ));
        
        // Left side - Previous button
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(Color.WHITE);
        
        prevButton = new JButton("← Previous");
        styleButton(prevButton, new Color(108, 117, 125), Color.WHITE);
        prevButton.addActionListener(e -> previousStep());
        leftPanel.add(prevButton);
        
        // Right side - Next/Submit buttons
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(Color.WHITE);
        
        JButton cancelBtn = new JButton("Cancel");
        styleButton(cancelBtn, new Color(220, 53, 69), Color.WHITE);
        cancelBtn.addActionListener(e -> dispose());
        
        nextButton = new JButton("Next →");
        styleButton(nextButton, new Color(0, 123, 255), Color.WHITE);
        nextButton.addActionListener(e -> nextStep());
        
        submitButton = new JButton("Submit Application");
        styleButton(submitButton, new Color(40, 167, 69), Color.WHITE);
        submitButton.addActionListener(e -> submitApplication());
        submitButton.setVisible(false);
        
        rightPanel.add(cancelBtn);
        rightPanel.add(nextButton);
        rightPanel.add(submitButton);
        
        navPanel.add(leftPanel, BorderLayout.WEST);
        navPanel.add(rightPanel, BorderLayout.EAST);
        
        
        return navPanel;
    }
    
    private void styleButton(JButton button, Color bgColor, Color fgColor) {
        button.setFont(new Font("Dialog", Font.BOLD, 14));
        button.setBackground(bgColor);
        button.setForeground(fgColor);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    
    private void nextStep() {
        if (validateCurrentStep()) {
            currentStep++;
            cardLayout.show(mainPanel, "step" + currentStep);
            updateNavigationButtons();
            updateStepIndicator();
        }
    }
    
    private void previousStep() {
        currentStep--;
        cardLayout.show(mainPanel, "step" + currentStep);
        updateNavigationButtons();
        updateStepIndicator();
    }
    
    private void updateNavigationButtons() {
        prevButton.setVisible(currentStep > 0);
        nextButton.setVisible(currentStep < 2);
        submitButton.setVisible(currentStep == 2);
    }
    
    private void updateStepIndicator() {
        stepIndicator.setText(String.format("Step %d of 3: %s", 
            currentStep + 1, stepNames[currentStep]));
    }
    
    private boolean validateCurrentStep() {
        List<String> errors = new ArrayList<>();
        
        switch (currentStep) {
            case 0 -> // Personal Information
                validatePersonalInfo(errors);
            case 1 -> // Family Background
                validateFamilyBackground(errors);
            case 2 -> // Documents
                validateDocuments(errors);
        }
        
         
    
        
        if (!errors.isEmpty()) {
            showValidationErrors(errors);
            return false;
        }
        
        return true;
    }
    
    private void validatePersonalInfo(List<String> errors) {
        String[] requiredFields = {"firstName", "lastName", "studentNumber", "email", "phone", 
            "birthDate", "school", "course", "yearLevel"};
        
        for (String fieldKey : requiredFields) {
            JTextField field = textFields.get(fieldKey);
            if (field != null && field.getText().trim().isEmpty()) {
                errors.add(getFieldDisplayName(fieldKey) + " is required");
            }
        }
        
        // Validate email
        String email = textFields.get("email").getText().trim();
        if (!email.isEmpty() && !isValidEmail(email)) {
            errors.add("Please enter a valid email address");
        }
        
        // Check address textarea
        JTextArea addressArea = textAreas.get("address");
        if (addressArea != null && addressArea.getText().trim().isEmpty()) {
            errors.add("Home Address is required");
        }
        
    }
    
    private void validateFamilyBackground(List<String> errors) {
        String[] requiredFields = {"fatherName", "fatherOccupation", "motherName", 
            "motherOccupation", "totalFamilyIncome", "numberOfSiblings"};
        
        for (String fieldKey : requiredFields) {
            JTextField field = textFields.get(fieldKey);
            if (field != null && field.getText().trim().isEmpty()) {
                errors.add(getFieldDisplayName(fieldKey) + " is required");
            }
        }
    }
    
    private void validateDocuments(List<String> errors) {
        if (selectedFiles.isEmpty()) {
            errors.add("Please upload at least one required document");
        }
    }
    
    private String getFieldDisplayName(String fieldKey) {
        Map<String, String> displayNames = new HashMap<>();
        displayNames.put("firstName", "First Name");
        displayNames.put("lastName", "Last Name");
        displayNames.put("studentNumber", "Student Number");
        displayNames.put("email", "Email Address");
        displayNames.put("phone", "Phone Number");
        displayNames.put("birthDate", "Date of Birth");
        displayNames.put("school", "School/University");
        displayNames.put("course", "Course/Program");
        displayNames.put("yearLevel", "Year Level");
        displayNames.put("fatherName", "Father's Name");
        displayNames.put("fatherOccupation", "Father's Occupation");
        displayNames.put("motherName", "Mother's Name");
        displayNames.put("motherOccupation", "Mother's Occupation");
        displayNames.put("totalFamilyIncome", "Total Family Income");
        displayNames.put("numberOfSiblings", "Number of Siblings");
        
        return displayNames.getOrDefault(fieldKey, fieldKey);
    }
    
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    

    
    
    private void showValidationErrors(List<String> errors) {
        StringBuilder message = new StringBuilder("Please fill the following fields:\n\n");
        for (String error : errors) {
            message.append("• ").append(error).append("\n");
        }
        
        JOptionPane.showMessageDialog(this, message.toString(), 
            "Validation Error", JOptionPane.WARNING_MESSAGE);
    }
    
  private void submitApplication() {
    // Step 1: Validate current form step
    if (!validateCurrentStep()) {
        return;
    }

    // Step 2: Ask for confirmation
    int confirm = JOptionPane.showConfirmDialog(
        this,  // Use 'null' if this is not inside a JFrame/JDialog
        "Are you sure you want to submit this application?",
        "Confirm Submission",
        JOptionPane.YES_NO_OPTION
    );

    // Step 3: Cancel if user selects "No"
    if (confirm != JOptionPane.YES_OPTION) {
        JOptionPane.showMessageDialog(this, "Submission cancelled.");
        return;
    }

    // Step 4: Show loading state
    submitButton.setEnabled(false);
    submitButton.setText("Submitting...");

    // Step 5: Run submission in background thread
    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
        @Override
        protected Boolean doInBackground() throws Exception {
            return saveApplicationToDatabase(); // Your method to save the data
        }

        @Override
        protected void done() {
            try {
                boolean success = get();
                if (success) {
                    showSuccessDialog(); // Your method to show success
                    dispose(); // Close the window/form
                } else {
                    showErrorDialog("Failed to submit application. Please try again.");
                }
            } catch (InterruptedException | ExecutionException e) {
                showErrorDialog("Error submitting application: " + e.getMessage());
            } finally {
                submitButton.setEnabled(true);
                submitButton.setText("Submit Application");
            }
        }
    };

    // Step 6: Start background task
    worker.execute();
}
    private boolean saveApplicationToDatabase() throws SQLException {
    String sql = "INSERT INTO scholarship_applications (" +
        "student_id, scholarship_name, student_name, student_number, email, phone, birth_date, " +
        "gender, address, province, municipality, barangay, zip_code, school, course, year_level, gwa, " +
        "father_name, father_age, father_occupation, father_income, father_education, " +
        "mother_name, mother_age, mother_occupation, mother_income, mother_education, " +
        "total_family_income, number_of_siblings, number_of_dependents, housing_status, " +
        "special_circumstances, uploaded_files" +
        ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        int index = 1;

        // ✅ Set student_id from session
        stmt.setInt(index++, UserSession.getStudentId());

        stmt.setString(index++, scholarshipName);
        stmt.setString(index++, getFullName());
        stmt.setString(index++, textFields.get("studentNumber").getText().trim());
        stmt.setString(index++, textFields.get("email").getText().trim());
        stmt.setString(index++, textFields.get("phone").getText().trim());

        JDateChooser dateChooser = dateChoosers.get("birthDate");
        String birthDateStr = "";
        if (dateChooser != null && dateChooser.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            birthDateStr = sdf.format(dateChooser.getDate());
        }
        stmt.setString(index++, birthDateStr);

        JComboBox<String> genderCombo = comboBoxes.get("gender");
        stmt.setString(index++, genderCombo != null && genderCombo.getSelectedItem() != null
                ? genderCombo.getSelectedItem().toString() : "");

        stmt.setString(index++, textAreas.get("address").getText().trim());

        JComboBox<String> provinceCombo = comboBoxes.get("province");
        stmt.setString(index++, provinceCombo != null && provinceCombo.getSelectedItem() != null
                ? provinceCombo.getSelectedItem().toString() : "");

        JComboBox<String> municipalityCombo = comboBoxes.get("municipality");
        stmt.setString(index++, municipalityCombo != null && municipalityCombo.getSelectedItem() != null
                ? municipalityCombo.getSelectedItem().toString() : "");

        JComboBox<String> barangayCombo = comboBoxes.get("barangay");
        stmt.setString(index++, barangayCombo != null && barangayCombo.getSelectedItem() != null
                ? barangayCombo.getSelectedItem().toString() : "");

        stmt.setString(index++, textFields.get("zipCode").getText().trim());

        JComboBox<String> schoolCombo = comboBoxes.get("school");
        stmt.setString(index++, schoolCombo != null && schoolCombo.getSelectedItem() != null
                ? schoolCombo.getSelectedItem().toString() : "");

        JComboBox<String> courseCombo = comboBoxes.get("course");
        stmt.setString(index++, courseCombo != null && courseCombo.getSelectedItem() != null
                ? courseCombo.getSelectedItem().toString() : "");

        JComboBox<String> yearLevelCombo = comboBoxes.get("yearLevel");
        stmt.setString(index++, yearLevelCombo != null && yearLevelCombo.getSelectedItem() != null
                ? yearLevelCombo.getSelectedItem().toString() : "");

        stmt.setString(index++, textFields.get("gwa").getText().trim());

        stmt.setString(index++, textFields.get("fatherName").getText().trim());
        stmt.setString(index++, textFields.get("fatherAge").getText().trim());
        stmt.setString(index++, textFields.get("fatherOccupation").getText().trim());

        JComboBox<String> fatherIncomeCombo = comboBoxes.get("fatherIncome");
        stmt.setString(index++, fatherIncomeCombo != null && fatherIncomeCombo.getSelectedItem() != null
                ? fatherIncomeCombo.getSelectedItem().toString() : "");

        JComboBox<String> fatherEducationCombo = comboBoxes.get("fatherEducation");
        stmt.setString(index++, fatherEducationCombo != null && fatherEducationCombo.getSelectedItem() != null
                ? fatherEducationCombo.getSelectedItem().toString() : "");

        stmt.setString(index++, textFields.get("motherName").getText().trim());
        stmt.setString(index++, textFields.get("motherAge").getText().trim());
        stmt.setString(index++, textFields.get("motherOccupation").getText().trim());

        JComboBox<String> motherIncomeCombo = comboBoxes.get("motherIncome");
        stmt.setString(index++, motherIncomeCombo != null && motherIncomeCombo.getSelectedItem() != null
                ? motherIncomeCombo.getSelectedItem().toString() : "");

        JComboBox<String> motherEducationCombo = comboBoxes.get("motherEducation");
        stmt.setString(index++, motherEducationCombo != null && motherEducationCombo.getSelectedItem() != null
                ? motherEducationCombo.getSelectedItem().toString() : "");

        String totalIncome = comboBoxes.get("totalFamilyIncome").getSelectedItem().toString();
        stmt.setString(index++, totalIncome);

        stmt.setString(index++, textFields.get("numberOfSiblings").getText().trim());
        stmt.setString(index++, textFields.get("numberOfDependents").getText().trim());

        JComboBox<String> housingStatusCombo = comboBoxes.get("housingStatus");
        stmt.setString(index++, housingStatusCombo != null && housingStatusCombo.getSelectedItem() != null
                ? housingStatusCombo.getSelectedItem().toString() : "");

        JTextArea specialCircumstancesArea = textAreas.get("specialCircumstances");
        stmt.setString(index++, specialCircumstancesArea != null
                ? specialCircumstancesArea.getText().trim() : "");

        String filesJson = prepareFilesForDatabase();
        stmt.setString(index++, filesJson);

       int rowsInserted = stmt.executeUpdate();
        if (rowsInserted > 0) {
            // ✅ Insert notification for admin
            String notificationSQL = "INSERT INTO notifications (title, message, recipient_type, status) VALUES (?, ?, 'admin', 'unread')";
            try (PreparedStatement notifyStmt = conn.prepareStatement(notificationSQL)) {
                String studentName = getFullName();
                notifyStmt.setString(1, "New Scholarship Application");
                notifyStmt.setString(2, "Student " + studentName + " has applied for the scholarship: " + scholarshipName);
                notifyStmt.executeUpdate();
            }
            return true;
        } else {
            return false;
        }
    }
}

// Add this method to properly format files for database storage
private String prepareFilesForDatabase() {
    if (selectedFiles == null || selectedFiles.isEmpty()) {
        return "[]"; // Return empty JSON array instead of empty string
    }
    
    try {
        StringBuilder json = new StringBuilder("[");
        
        for (int i = 0; i < selectedFiles.size(); i++) {
            File file = selectedFiles.get(i);
            
            if (i > 0) {
                json.append(",");
            }
            
            // Create a unique identifier for the file
            String fileId = "file_" + System.currentTimeMillis() + "_" + i;
            String timestamp = java.time.Instant.now().toString();
            
            json.append("{");
            json.append("\"id\":\"").append(fileId).append("\",");
            json.append("\"name\":\"").append(escapeJsonString(file.getName())).append("\",");
            json.append("\"originalPath\":\"").append(escapeJsonString(file.getAbsolutePath())).append("\",");
            json.append("\"size\":").append(file.length()).append(",");
            json.append("\"type\":\"").append(getFileExtension(file).toLowerCase()).append("\",");
            json.append("\"mimeType\":\"").append(getMimeType(file)).append("\",");
            json.append("\"uploadDate\":\"").append(timestamp).append("\"");
            json.append("}");
        }
        
        json.append("]");
        return json.toString();
        
    } catch (Exception e) {
        e.printStackTrace();
        // Return empty array on error to avoid null constraint violation
        return "[]";
    }
}

// Helper method to escape JSON strings
private String escapeJsonString(String str) {
    if (str == null) return "";
    return str.replace("\\", "\\\\")
              .replace("\"", "\\\"")
              .replace("\n", "\\n")
              .replace("\r", "\\r")
              .replace("\t", "\\t");
}

// Helper method to get MIME type
private String getMimeType(File file) {
    String extension = getFileExtension(file).toLowerCase();
    switch (extension) {
        case "pdf":
            return "application/pdf";
        case "doc":
            return "application/msword";
        case "docx":
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        case "jpg":
        case "jpeg":
            return "image/jpeg";
        case "png":
            return "image/png";
        default:
            return "application/octet-stream";
    }
}
   private void showSuccessDialog() {
    JDialog successDialog = new JDialog(this, "", true);
    successDialog.setSize(400, 300);
    successDialog.setLocationRelativeTo(this);
    successDialog.setResizable(false);

    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 20, 30));
    contentPanel.setBackground(Color.WHITE);

    JLabel iconLabel = new JLabel("🎉");
    iconLabel.setFont(new Font("Dialog", Font.BOLD, 48));
    iconLabel.setForeground(new Color(40, 167, 69));
    iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel messageLabel = new JLabel("<html><div style='text-align: center;'>Your application has been submitted successfully!</div></html>");
    messageLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
    messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

    JButton okButton = new JButton("OK");
    styleButton(okButton, new Color(0, 123, 255), Color.WHITE);
    okButton.addActionListener(e -> successDialog.dispose());

    JPanel buttonPanel = new JPanel(new FlowLayout());
    buttonPanel.setBackground(Color.WHITE);
    buttonPanel.add(okButton);

    contentPanel.add(iconLabel);
    contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
    contentPanel.add(messageLabel);

    successDialog.add(contentPanel, BorderLayout.CENTER);
    successDialog.add(buttonPanel, BorderLayout.SOUTH);
    successDialog.setVisible(true);
}

private void styleModernButton(JButton button) {
    button.setFont(new Font("Dialog", Font.BOLD, 14));
    button.setForeground(Color.WHITE);
    button.setBackground(new Color(59, 130, 246)); // Modern blue
    button.setBorder(BorderFactory.createEmptyBorder(12, 32, 12, 32));
    button.setFocusPainted(false);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    
    // Hover effects
    button.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            button.setBackground(new Color(37, 99, 235)); // Darker blue on hover
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            button.setBackground(new Color(59, 130, 246)); // Original blue
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            button.setBackground(new Color(29, 78, 216)); // Even darker when pressed
        }
        
        @Override
        public void mouseReleased(MouseEvent e) {
            button.setBackground(new Color(37, 99, 235)); // Back to hover state
        }
    });
}
        
        private void showErrorDialog(String message) {
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        }

        private String getFullName() {
    String firstName = textFields.get("firstName").getText().trim();
    String middleName = textFields.get("middleName").getText().trim();
    String lastName = textFields.get("lastName").getText().trim();
    return String.join(" ", firstName, middleName, lastName).replaceAll("\\s+", " ").trim();
}
    }
    
    private String formatDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));
        } catch (Exception e) {
            return dateStr;
        }
    }
    


    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));

        jLabel1.setFont(new java.awt.Font("Dialog", 1, 36)); // NOI18N
        jLabel1.setIcon(new javax.swing.ImageIcon("C:\\Users\\alexa\\Downloads\\icons8-scholarship-40.png")); // NOI18N
        jLabel1.setText("Scholarship");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 270, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(94, 94, 94)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 906, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(68, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(17, 17, 17)
                .addComponent(jLabel1)
                .addGap(68, 68, 68)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 512, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(50, Short.MAX_VALUE))
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


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}

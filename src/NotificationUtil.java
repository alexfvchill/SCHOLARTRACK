import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;

public class NotificationUtil {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/scholartrack_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "12345";

    public static void notifyStudentsWithNoScholarship(String message) {
    String query = "SELECT student_id FROM students WHERE student_id NOT IN (SELECT student_id FROM scholarship_applications WHERE status = 'Approved')";
    String insert = "INSERT INTO student_notifications (student_id, message, is_read) VALUES (?, ?, FALSE)";

    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345");
         PreparedStatement selectStmt = conn.prepareStatement(query);
         ResultSet rs = selectStmt.executeQuery();
         PreparedStatement insertStmt = conn.prepareStatement(insert)) {

        while (rs.next()) {
            int studentId = rs.getInt("studentId");
            insertStmt.setInt(1, studentId);
            insertStmt.setString(2, message);
            insertStmt.addBatch();
        }

        insertStmt.executeBatch();
        System.out.println("Notification sent to students with no existing scholarships.");

    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(null, "Failed to notify students: " + e.getMessage(), "Notification Error", JOptionPane.ERROR_MESSAGE);
    }
}

}
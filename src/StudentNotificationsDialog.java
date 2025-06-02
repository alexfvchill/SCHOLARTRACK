import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.swing.JScrollPane; // for Swing
import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class StudentNotificationsDialog extends JDialog {

    public StudentNotificationsDialog(Frame parent, int studentId) {
        super(parent, "Notifications", true);  // modal dialog
        setSize(500, 400);
        setLocationRelativeTo(parent);  // center on parent

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/scholartrack_db", "root", "12345")) {
            String sql = "SELECT * FROM notifications WHERE user_id IS NULL OR user_id = ? ORDER BY created_at DESC";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, studentId);

            ResultSet rs = pst.executeQuery();
            boolean hasNotifs = false;

            while (rs.next()) {
                hasNotifs = true;
                String title = rs.getString("title");
                String message = rs.getString("message");
                Timestamp time = rs.getTimestamp("created_at");

                JTextArea notifArea = new JTextArea(title + "\n" + message + "\n— " + time.toString());
                notifArea.setWrapStyleWord(true);
                notifArea.setLineWrap(true);
                notifArea.setEditable(false);
                notifArea.setOpaque(true);
                notifArea.setBackground(new Color(245, 245, 245));
                notifArea.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(Color.GRAY),
                        BorderFactory.createEmptyBorder(10, 10, 10, 10)
                ));
                panel.add(notifArea);
                panel.add(Box.createRigidArea(new Dimension(0, 10)));  // spacing
            }

            if (!hasNotifs) {
                JLabel noNotifLabel = new JLabel("No notifications available.");
                noNotifLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(Box.createVerticalGlue());
                panel.add(noNotifLabel);
                panel.add(Box.createVerticalGlue());
            }

        } catch (SQLException e) {
            e.printStackTrace();
            panel.add(new JLabel("Error loading notifications."));
        }

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane);
    }
}


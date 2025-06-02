/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author alexa
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/scholartrack_db";
    private static final String USER = "root"; // your MySQL username
    private static final String PASS = "12345";     // your MySQL password

    public static Connection getConnection() throws SQLException {
    try {
        Class.forName("com.mysql.cj.jdbc.Driver"); // still needed for now
    } catch (ClassNotFoundException e) {
        throw new SQLException("MySQL JDBC Driver not found", e);
    }
    return DriverManager.getConnection(URL, USER, PASS);
}
    }

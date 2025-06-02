/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package UserSession;

/**
 *
 * @author alexa
 */
public class UserSession {
    private static int studentId;

    public static void setStudentId(int id) {
        studentId = id;
    }

    public static int getStudentId() {
        return studentId;
    }

    public static void clearSession() {
        studentId = 0; // or -1 if you prefer
    }
}

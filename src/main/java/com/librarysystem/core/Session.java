package com.librarysystem.core;

/**
 * Singleton holding the current user's session information.
 * After login, call Session.login(...) to populate.
 * Panels can access Session.getInstance() to get user role, name, etc.
 */
public class Session {

    private static Session instance;

    private int userId;          // 0 if member
    private int memberId;        // 0 if staff
    private String username;     // staff username or member email
    private String role;         // "Admin", "Librarian", "Student"
    private String fullName;     // e.g., "John Doe"

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void login(int userId, int memberId, String username, String role, String fullName) {
        this.userId = userId;
        this.memberId = memberId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }

    public void logout() {
        userId = 0;
        memberId = 0;
        username = null;
        role = null;
        fullName = null;
    }

    public boolean isLoggedIn() {
        return role != null;
    }

    // --- getters ---
    public int getUserId() { return userId; }
    public int getMemberId() { return memberId; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getFullName() { return fullName; }
}
package com.cinebook.model;

import java.util.Objects;

/**
 * Model representing a CineBook User.
 * Corresponds to table 'users' (user_id, full_name, email, password_hash, role).
 */
public class User {

    private int userId;
    private String fullName;
    private String email;
    private String passwordHash;
    private String role; // 'ADMIN', 'CUSTOMER', 'STAFF'

    public User() {
    }

    public User(String fullName, String email, String passwordHash, String role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = (role == null || role.trim().isEmpty()) ? "CUSTOMER" : role.toUpperCase();
    }

    public User(int userId, String fullName, String email, String passwordHash, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = (role == null || role.trim().isEmpty()) ? "CUSTOMER" : role.toUpperCase();
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = (role == null || role.trim().isEmpty()) ? "CUSTOMER" : role.toUpperCase();
    }

    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(this.role);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userId == user.userId && Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }

    @Override
    public String toString() {
        return String.format("User [ID: %d | Name: %s | Email: %s | Role: %s]",
                userId, fullName, email, role);
    }
}

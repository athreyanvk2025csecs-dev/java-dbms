package com.cinebook.service;

import com.cinebook.dao.UserDAO;
import com.cinebook.model.User;
import com.cinebook.util.PasswordUtil;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Service managing user authentication, customer registration, and profile querying.
 * Enforces business rules such as email deduplication and secure password hashing.
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Registers a new customer in CineBook.
     * Prevents duplicate email registrations and hashes passwords using SHA-256.
     *
     * @param fullName      customer full name
     * @param email         customer email
     * @param plainPassword customer password
     * @return the registered User entity with generated userId
     * @throws IllegalArgumentException if validation fails or email is already in use
     * @throws SQLException             if database error occurs
     */
    public User registerCustomer(String fullName, String email, String plainPassword) throws SQLException {
        return registerUser(fullName, email, plainPassword, "CUSTOMER");
    }

    /**
     * Registers a new administrator.
     *
     * @param fullName      admin full name
     * @param email         admin email
     * @param plainPassword admin password
     * @return registered User entity
     * @throws SQLException on database error
     */
    public User registerAdmin(String fullName, String email, String plainPassword) throws SQLException {
        return registerUser(fullName, email, plainPassword, "ADMIN");
    }

    private User registerUser(String fullName, String email, String plainPassword, String role) throws SQLException {
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be blank.");
        }
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new IllegalArgumentException("Invalid email format: '" + email + "'");
        }
        if (plainPassword == null || plainPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Prevent duplicate email registrations
        if (userDAO.emailExists(normalizedEmail)) {
            throw new IllegalArgumentException("Email is already registered: " + normalizedEmail);
        }

        String passwordHash = PasswordUtil.hashPassword(plainPassword);
        User user = new User(fullName.trim(), normalizedEmail, passwordHash, role);
        int generatedId = userDAO.createUser(user);
        user.setUserId(generatedId);
        return user;
    }

    /**
     * Authenticates a user with email and plaintext password.
     *
     * @param email         user email
     * @param plainPassword plaintext password
     * @return authenticated User entity
     * @throws IllegalArgumentException if credentials are invalid or user not found
     * @throws SQLException             on database error
     */
    public User login(String email, String plainPassword) throws SQLException {
        if (email == null || plainPassword == null) {
            throw new IllegalArgumentException("Email and password are required.");
        }

        Optional<User> userOpt = userDAO.getUserByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        User user = userOpt.get();
        if (!PasswordUtil.verifyPassword(plainPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        return user;
    }

    /**
     * Retrieves a user by ID.
     *
     * @param userId user identifier
     * @return Optional containing User if present
     * @throws SQLException on database error
     */
    public Optional<User> getUserById(int userId) throws SQLException {
        return userDAO.getUserById(userId);
    }

    /**
     * Retrieves all registered users.
     *
     * @return list of User entities
     * @throws SQLException on database error
     */
    public List<User> getAllUsers() throws SQLException {
        return userDAO.getAllUsers();
    }
}

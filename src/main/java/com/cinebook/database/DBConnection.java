package com.cinebook.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection provides centralized JDBC connection management for CineBook.
 * Reads database credentials dynamically from db.properties configuration file.
 * Prevents hardcoding of database credentials in Java code.
 */
public final class DBConnection {

    private static final String DEFAULT_CONFIG_FILE = "db.properties";
    private static Properties properties;

    private DBConnection() {
        // Utility class: prevent instantiation
    }

    /**
     * Loads database configuration properties from classpath or project directory.
     *
     * @return loaded Properties object
     * @throws SQLException if configuration cannot be loaded
     */
    private static synchronized Properties loadProperties() throws SQLException {
        if (properties != null) {
            return properties;
        }

        properties = new Properties();

        // 1. Attempt loading from thread context / classloader classpath
        InputStream inStream = DBConnection.class.getClassLoader().getResourceAsStream(DEFAULT_CONFIG_FILE);

        // 2. Fallback to file system paths if classpath stream is not found
        if (inStream == null) {
            File localFile = new File("src/main/resources/" + DEFAULT_CONFIG_FILE);
            if (!localFile.exists()) {
                localFile = new File(DEFAULT_CONFIG_FILE);
            }
            if (localFile.exists()) {
                try {
                    inStream = new FileInputStream(localFile);
                } catch (IOException e) {
                    throw new SQLException("Failed to open database configuration file: " + localFile.getAbsolutePath(), e);
                }
            }
        }

        if (inStream == null) {
            throw new SQLException(
                    "Database configuration error: '" + DEFAULT_CONFIG_FILE + "' not found! "
                    + "Please ensure db.properties exists in classpath or src/main/resources/."
            );
        }

        try (InputStream streamToRead = inStream) {
            properties.load(streamToRead);
        } catch (IOException e) {
            throw new SQLException("Failed to parse database configuration from " + DEFAULT_CONFIG_FILE, e);
        }

        // Validate essential property keys
        validateProperty("db.url");
        validateProperty("db.user");

        // Register JDBC driver if explicitly provided
        String driver = properties.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver class not found: " + driver, e);
        }

        return properties;
    }

    private static void validateProperty(String key) throws SQLException {
        String val = properties.getProperty(key);
        if (val == null || val.trim().isEmpty()) {
            throw new SQLException("Database configuration missing required property: '" + key + "'");
        }
    }

    /**
     * Establishes and returns a new JDBC Connection using configured credentials.
     *
     * @return an active java.sql.Connection
     * @throws SQLException with clear context-rich error message if connection fails
     */
    public static Connection getConnection() throws SQLException {
        Properties props = loadProperties();

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.user");
        String password = props.getProperty("db.password", "");

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            String errorMsg = String.format(
                    "Database Connection Failed! [URL: %s, User: %s]. "
                    + "Verify that MySQL server is running, the database exists, and the credentials in db.properties are correct. "
                    + "Underlying error: %s (SQLState: %s, ErrorCode: %d)",
                    url, user, e.getMessage(), e.getSQLState(), e.getErrorCode()
            );
            throw new SQLException(errorMsg, e.getSQLState(), e.getErrorCode(), e);
        }
    }

    /**
     * Explicitly resets cached properties (useful for testing or reloading config).
     */
    public static synchronized void resetConfig() {
        properties = null;
    }

    /**
     * Helper to test database connectivity.
     *
     * @return true if connection is successful and valid, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(2);
        } catch (SQLException e) {
            System.err.println("Database connectivity test failed: " + e.getMessage());
            return false;
        }
    }
}

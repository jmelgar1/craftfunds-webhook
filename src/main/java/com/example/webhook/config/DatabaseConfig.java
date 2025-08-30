package com.example.webhook.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final String DB_URL = System.getenv("DATABASE_URL");
    private static final String DB_USERNAME = System.getenv("DATABASE_USERNAME");
    private static final String DB_PASSWORD = System.getenv("DATABASE_PASSWORD");
    
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        String connectionUrl = getConnectionUrl();
        return DriverManager.getConnection(connectionUrl, DB_USERNAME, DB_PASSWORD);
    }
    
    private static String getConnectionUrl() {
        return DB_URL;
    }
    
    public static void testConnection() {
        try (Connection conn = getConnection()) {
            System.out.println("Database connection successful!");
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
        }
    }
}
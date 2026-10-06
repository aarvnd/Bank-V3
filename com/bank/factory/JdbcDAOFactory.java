package com.bank.factory;

import com.bank.dao.AccountDAO;
import com.bank.dao.TransactionDAO;
import com.bank.dao.UserDAO;
import com.bank.dao.impl.JdbcAccountDAO;
import com.bank.dao.impl.JdbcTransactionDAO;
import com.bank.dao.impl.JdbcUserDAO;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class JdbcDAOFactory extends DAOFactory {
    // Database config comes from the environment so no credentials live in source:
    // BANK_DB_URL, BANK_DB_USER, BANK_DB_PASSWORD
    private static final String URL = env("BANK_DB_URL",
            "jdbc:mysql://localhost:3306/bank_db?createDatabaseIfNotExist=true");
    private static final String USER = env("BANK_DB_USER", "root");
    private static final String PASS = env("BANK_DB_PASSWORD", "");

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    @Override
    public UserDAO getUserDAO() { return new JdbcUserDAO(); }

    @Override
    public AccountDAO getAccountDAO() { return new JdbcAccountDAO(); }

    @Override
    public TransactionDAO getTransactionDAO() { return new JdbcTransactionDAO(); }

    @Override
    public void initialize() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(50) UNIQUE NOT NULL, " +
                    "password VARCHAR(255) NOT NULL, " +
                    "full_name VARCHAR(100), " +
                    "email VARCHAR(100), " +
                    "phone VARCHAR(20), " +
                    "user_type VARCHAR(20))");

            // Create Accounts Table
            stmt.execute("CREATE TABLE IF NOT EXISTS accounts (" +
                    "account_number VARCHAR(20) PRIMARY KEY, " +
                    "user_id INT, " +
                    "name VARCHAR(100), " +
                    "email VARCHAR(100), " +
                    "phone VARCHAR(20), " +
                    "balance DOUBLE, " +
                    "account_type VARCHAR(20), " +
                    "created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(user_id))");

            // Create Transactions Table
            stmt.execute("CREATE TABLE IF NOT EXISTS transactions (" +
                    "transaction_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "account_number VARCHAR(20), " +
                    "type VARCHAR(20), " +
                    "amount DOUBLE, " +
                    "balance_after DOUBLE, " +
                    "description VARCHAR(200), " +
                    "transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (account_number) REFERENCES accounts(account_number))");

            // Create Default Admin
            try {
                // Simple check if admin exists, in real app use DAO
                stmt.execute("INSERT IGNORE INTO users (username, password, full_name, user_type) " +
                        "VALUES ('admin', 'admin123', 'System Administrator', 'ADMIN')");
            } catch (Exception ignored) {}

        } catch (Exception e) {
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    @Override
    public void cleanup() {
        // Close connection pool if used
    }

    @Override
    public String getFactoryName() { return "JDBC Factory (MySQL)"; }
}
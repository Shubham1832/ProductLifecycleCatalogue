package edu.niet.plcm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates PostgreSQL connections using environment variables.
 * Set DB_URL, DB_USER, and DB_PASSWORD before running the application.
 */
public final class Database {
    private Database() {}

    public static Connection getConnection() throws SQLException {
        String url = env("DB_URL", "jdbc:postgresql://localhost:5432/plcm_db");
        String user = env("DB_USER", "postgres");
        String password = env("DB_PASSWORD", "");
        return DriverManager.getConnection(url, user, password);
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null ? fallback : value;
    }
}

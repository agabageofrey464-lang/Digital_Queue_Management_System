package com.digiq.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Single pooled {@link DataSource} for the whole application.
 *
 * <p>Settings come from {@code db.properties} on the classpath; any of them can be
 * overridden at deploy time with a matching {@code -Ddigiq.*} system property, which
 * is how you point a marking copy at a different MySQL instance without a rebuild.</p>
 */
public final class Database {

    private static volatile HikariDataSource dataSource;

    private Database() {
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (Database.class) {
                if (dataSource == null) {
                    dataSource = build();
                }
            }
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static HikariDataSource build() {
        Properties props = load();

        String url = resolve("digiq.db.url", props.getProperty("db.url"));
        String user = resolve("digiq.db.username", props.getProperty("db.username"));
        String password = resolve("digiq.db.password", props.getProperty("db.password"));

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(url);
        cfg.setUsername(user);
        cfg.setPassword(password);
        cfg.setDriverClassName(props.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        cfg.setPoolName("digiq-pool");
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "12")));
        cfg.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.min", "2")));
        cfg.setConnectionTimeout(10_000);
        cfg.setPoolName("digiq");
        // Keeps prepared statements fast on MySQL.
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        return new HikariDataSource(cfg);
    }

    private static String resolve(String systemProperty, String fallback) {
        String override = System.getProperty(systemProperty);
        return (override != null && !override.isBlank()) ? override : fallback;
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = Database.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "db.properties not found on the classpath (expected in src/main/resources).");
            }
            props.load(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read db.properties", ex);
        }
        return props;
    }

    /** Called by {@link AppContextListener} when the web application stops. */
    public static void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }
}

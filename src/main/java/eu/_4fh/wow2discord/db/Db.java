package eu._4fh.wow2discord.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Db implements AutoCloseable {
    private static Db instance = null;

    public static Db i() {
        if (instance == null) {
            throw new IllegalStateException("Db not initialized");
        }
        return instance;
    }

    private final HikariDataSource dataSource;

    public Db() {
        HikariConfig config = new HikariConfig("hikari.properties");
        config.setAutoCommit(false);
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        dataSource = new HikariDataSource(config);
        instance = this;
    }

    Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        dataSource.close();
        instance = null;
    }
}

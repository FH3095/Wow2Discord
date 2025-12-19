package eu._4fh.wow2discord.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import eu._4fh.wow2discord.util.Singleton;

import java.sql.Connection;
import java.sql.SQLException;

public class Db implements AutoCloseable {

    private static final Singleton<Db> instance = new Singleton<>(Db.class);

    public static Db i() {
        return instance.get();
    }

    private final HikariDataSource dataSource;

    public Db() {
        instance.set(this);
        HikariConfig config = new HikariConfig("hikari.properties");
        config.setAutoCommit(false);
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");
        dataSource = new HikariDataSource(config);
    }

    Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public synchronized void close() {
        dataSource.close();
        instance.unset(this);
    }
}

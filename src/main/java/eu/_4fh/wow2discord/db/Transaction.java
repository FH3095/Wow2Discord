package eu._4fh.wow2discord.db;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Transaction implements AutoCloseable {
    private static final Map<Class<? extends Record>, Map<Byte, Constructor<?>>> createMethods = new ConcurrentHashMap<>();
    private final Connection con;
    public final DbOnlineUsers onlineUsers;

    public Transaction() {
        try {
            con = Db.i().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        onlineUsers = new DbOnlineUsers(this);
    }

    public void commit() {
        try {
            con.commit();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            con.rollback();
            con.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static record ExistsRecord(int i) {
    }

    private Map<Byte, Constructor<?>> computeCreateMethods(Class<? extends Record> recordClass) {
        Map<Byte, Constructor<?>> result = new HashMap<>(1);
        Constructor<?>[] constructors = recordClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            byte parameterCount = (byte) constructor.getParameterCount();
            if (!result.containsKey(parameterCount)) {
                result.put(parameterCount, constructor);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private Constructor<?> getCreateMethod(Class<? extends Record> recordClass, byte numParameters) {
        Map<Byte, Constructor<?>> methods = createMethods.computeIfAbsent(recordClass, this::computeCreateMethods);
        Constructor<?> result = methods.get(numParameters);
        if (result == null) {
            throw new IllegalArgumentException(
                    "Cant find constructor for " + recordClass.getName() + " with number of parameters " + numParameters);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private <T extends Record> List<T> createResults(ResultSet rs, Class<T> recordClass) throws SQLException {
        List<T> result = new ArrayList<>(1);
        byte numColumns = (byte) rs.getMetaData().getColumnCount();
        Constructor<?> constructor = getCreateMethod(recordClass, numColumns);
        Class<?>[] parameterClasses = constructor.getParameterTypes();
        while (rs.next()) {
            Object[] parameters = new Object[parameterClasses.length];
            for (int i = 0; i < parameterClasses.length; i++) {
                parameters[i] = rs.getObject(i + 1, parameterClasses[i]);
            }
            try {
                result.add((T) constructor.newInstance(parameters));
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    <T extends Record> List<T> query(Class<T> recordClass, String sql, Object... parameters) {
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return createResults(rs, recordClass);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    long update(String sql, Object... parameters) {
        try (PreparedStatement stmt = con.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                stmt.setObject(i + 1, parameters[i]);
            }
            return stmt.executeLargeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}

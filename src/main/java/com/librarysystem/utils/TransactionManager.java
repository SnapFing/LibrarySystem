package com.librarysystem.utils;

import com.librarysystem.db.DBHelper;
import java.sql.*;
import java.util.logging.*;

public class TransactionManager {
    private static final Logger LOG = Logger.getLogger(TransactionManager.class.getName());
    private static final int MAX_RETRIES = 2;

    public static <T> T executeTransaction(TransactionOperation<T> op) throws SQLException {
        return executeWithRetry(op, Connection.TRANSACTION_READ_COMMITTED, false);
    }

    public static void executeTransactionVoid(TransactionOperationVoid op) throws SQLException {
        executeTransaction(conn -> { op.execute(conn); return null; });
    }

    public static <T> T executeTransactionWithIsolation(TransactionOperation<T> op, int level) throws SQLException {
        return executeWithRetry(op, level, true);
    }

    private static <T> T executeWithRetry(TransactionOperation<T> op, int isolationLevel, boolean customIsolation) throws SQLException {
        for (int attempt = 1; attempt <= MAX_RETRIES + 1; attempt++) {
            Connection conn = null;
            boolean autoCommitOriginal = true;
            int isolationOriginal = Connection.TRANSACTION_READ_COMMITTED;
            try {
                conn = DBHelper.getConnection();
                autoCommitOriginal = conn.getAutoCommit();
                if (customIsolation) {
                    isolationOriginal = conn.getTransactionIsolation();
                    conn.setTransactionIsolation(isolationLevel);
                }
                conn.setAutoCommit(false);
                T result = op.execute(conn);
                conn.commit();
                return result;
            } catch (Throwable e) {
                if (conn != null) {
                    try { conn.rollback(); } catch (SQLException ex) { LOG.log(Level.WARNING, "Rollback failed", ex); }
                }
                if (attempt <= MAX_RETRIES && e instanceof SQLException && ((SQLException)e).getSQLState().equals("40001")) {
                    LOG.log(Level.WARNING, "Deadlock detected, retrying (attempt {0})", attempt);
                    try { Thread.sleep(100 * attempt); } catch (InterruptedException ignored) {}
                    continue;
                }
                throw e instanceof SQLException ? (SQLException) e : new SQLException(e);
            } finally {
                if (conn != null) {
                    try {
                        if (customIsolation) conn.setTransactionIsolation(isolationOriginal);
                        if (!autoCommitOriginal) conn.setAutoCommit(true);
                    } catch (SQLException e) { LOG.log(Level.WARNING, "Error resetting connection", e); }
                    finally {
                        try { conn.close(); } catch (SQLException e) { LOG.warning("Error closing connection"); }
                    }
                }
            }
        }
        throw new SQLException("Transaction failed after retries");
    }

    public static <T> TransactionResult<T> executeSafe(TransactionOperation<T> op) {
        try { return TransactionResult.success(executeTransaction(op)); }
        catch (SQLException e) { return TransactionResult.failure(e.getMessage()); }
    }

    @FunctionalInterface public interface TransactionOperation<T> { T execute(Connection conn) throws Exception; }
    @FunctionalInterface public interface TransactionOperationVoid { void execute(Connection conn) throws Exception; }
    public static class TransactionResult<T> {
        private boolean success; private T data; private String error;
        private TransactionResult(boolean s, T d, String e) { success=s; data=d; error=e; }
        public static <T> TransactionResult<T> success(T data) { return new TransactionResult<>(true, data, null); }
        public static <T> TransactionResult<T> failure(String error) { return new TransactionResult<>(false, null, error); }
        public boolean isSuccess() { return success; }
        public T getData() { return data; }
        public String getErrorMessage() { return error; }
    }
}
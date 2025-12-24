package utils;

import db.DBHelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Transaction Manager for handling database transactions safely.
 * Ensures ACID properties (Atomicity, Consistency, Isolation, Durability).
 *
 * Usage:
 * <pre>
 * TransactionManager.executeTransaction(conn -> {
 *     // Your database operations here
 *     stmt1.executeUpdate();
 *     stmt2.executeUpdate();
 *     return true; // Success
 * });
 * </pre>
 */
public class TransactionManager {

    /**
     * Executes a database operation within a transaction.
     * Automatically commits on success or rolls back on failure.
     *
     * @param operation The operation to execute (receives a Connection)
     * @param <T> Return type of the operation
     * @return Result of the operation
     * @throws SQLException if transaction fails
     */
    public static <T> T executeTransaction(TransactionOperation<T> operation) throws SQLException {
        Connection conn = null;
        boolean autoCommitOriginal = true;

        try {
            // Get connection from pool
            conn = DBHelper.getConnection();

            // Save original auto-commit state
            autoCommitOriginal = conn.getAutoCommit();

            // Disable auto-commit to start transaction
            conn.setAutoCommit(false);

            // Execute the operation
            T result = operation.execute(conn);

            // If we got here, operation succeeded - commit
            conn.commit();

            return result;

        } catch (Exception e) {
            // Operation failed - rollback all changes
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("⚠️ Transaction rolled back due to: " + e.getMessage());
                } catch (SQLException rollbackEx) {
                    System.err.println("❌ Failed to rollback transaction: " + rollbackEx.getMessage());
                    rollbackEx.printStackTrace();
                }
            }

            // Re-throw as SQLException
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new SQLException("Transaction failed: " + e.getMessage(), e);
            }

        } finally {
            // Restore original auto-commit state and close connection
            if (conn != null) {
                try {
                    conn.setAutoCommit(autoCommitOriginal);
                    conn.close(); // Returns connection to pool
                } catch (SQLException e) {
                    System.err.println("⚠️ Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Executes a transaction with no return value.
     *
     * @param operation The operation to execute
     * @throws SQLException if transaction fails
     */
    public static void executeTransactionVoid(TransactionOperationVoid operation) throws SQLException {
        executeTransaction(conn -> {
            operation.execute(conn);
            return null;
        });
    }

    /**
     * Executes a transaction with custom isolation level.
     *
     * @param operation The operation to execute
     * @param isolationLevel Transaction isolation level (e.g., Connection.TRANSACTION_SERIALIZABLE)
     * @param <T> Return type
     * @return Result of the operation
     * @throws SQLException if transaction fails
     */
    public static <T> T executeTransactionWithIsolation(
            TransactionOperation<T> operation,
            int isolationLevel) throws SQLException {

        Connection conn = null;
        boolean autoCommitOriginal = true;
        int isolationOriginal = Connection.TRANSACTION_READ_COMMITTED;

        try {
            conn = DBHelper.getConnection();
            autoCommitOriginal = conn.getAutoCommit();
            isolationOriginal = conn.getTransactionIsolation();

            // Set custom isolation level
            conn.setTransactionIsolation(isolationLevel);
            conn.setAutoCommit(false);

            T result = operation.execute(conn);
            conn.commit();

            return result;

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    rollbackEx.printStackTrace();
                }
            }

            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new SQLException("Transaction failed: " + e.getMessage(), e);
            }

        } finally {
            if (conn != null) {
                try {
                    conn.setTransactionIsolation(isolationOriginal);
                    conn.setAutoCommit(autoCommitOriginal);
                    conn.close();
                } catch (SQLException e) {
                    System.err.println("⚠️ Error closing connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Functional interface for transaction operations that return a value.
     */
    @FunctionalInterface
    public interface TransactionOperation<T> {
        T execute(Connection conn) throws Exception;
    }

    /**
     * Functional interface for transaction operations with no return value.
     */
    @FunctionalInterface
    public interface TransactionOperationVoid {
        void execute(Connection conn) throws Exception;
    }

    /**
     * Transaction result wrapper for operations that may fail.
     */
    public static class TransactionResult<T> {
        private final boolean success;
        private final T data;
        private final String errorMessage;

        private TransactionResult(boolean success, T data, String errorMessage) {
            this.success = success;
            this.data = data;
            this.errorMessage = errorMessage;
        }

        public static <T> TransactionResult<T> success(T data) {
            return new TransactionResult<>(true, data, null);
        }

        public static <T> TransactionResult<T> failure(String errorMessage) {
            return new TransactionResult<>(false, null, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public T getData() {
            return data;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Safe transaction executor that catches exceptions and returns a result object.
     * Useful for UI operations where you don't want to throw exceptions.
     */
    public static <T> TransactionResult<T> executeSafe(TransactionOperation<T> operation) {
        try {
            T result = executeTransaction(operation);
            return TransactionResult.success(result);
        } catch (SQLException e) {
            e.printStackTrace();
            return TransactionResult.failure(e.getMessage());
        }
    }
}
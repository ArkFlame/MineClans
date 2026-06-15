package com.arkflame.mineclans.providers;

public class DatabaseException extends RuntimeException {
    private final String operation;
    private final String sqlState;
    private final int vendorCode;

    public DatabaseException(String operation, String message) {
        super(message);
        this.operation = operation;
        this.sqlState = null;
        this.vendorCode = 0;
    }

    public DatabaseException(String operation, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.sqlState = cause instanceof java.sql.SQLException
                ? ((java.sql.SQLException) cause).getSQLState() : null;
        this.vendorCode = cause instanceof java.sql.SQLException
                ? ((java.sql.SQLException) cause).getErrorCode() : 0;
    }

    public String getOperation() {
        return operation;
    }

    public String getSqlState() {
        return sqlState;
    }

    public int getVendorCode() {
        return vendorCode;
    }
}

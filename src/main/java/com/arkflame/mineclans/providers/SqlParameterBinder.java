package com.arkflame.mineclans.providers;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class SqlParameterBinder {

    public static void bind(PreparedStatement statement, String sql, Object... parameters) throws SQLException {
        int expectedCount = getParameterCount(statement, sql);
        if (expectedCount != parameters.length) {
            throw new IllegalArgumentException(
                    "SQL parameter mismatch: expected=" + expectedCount + " provided=" + parameters.length);
        }
        for (int i = 0; i < parameters.length; i++) {
            Object param = parameters[i];
            if (param instanceof UUID) {
                statement.setString(i + 1, param.toString());
            } else if (param == null) {
                statement.setNull(i + 1, java.sql.Types.NULL);
            } else {
                statement.setObject(i + 1, param);
            }
        }
    }

    private static int getParameterCount(PreparedStatement statement, String sql) {
        try {
            int count = statement.getParameterMetaData().getParameterCount();
            if (count > 0) {
                return count;
            }
        } catch (SQLException ignored) {
        }
        return countPlaceholders(sql);
    }

    static int countPlaceholders(String sql) {
        int count = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean escaped = false;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '\'' && !inDoubleQuote && !inBacktick) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (c == '"' && !inSingleQuote && !inBacktick) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (c == '`' && !inSingleQuote && !inDoubleQuote) {
                inBacktick = !inBacktick;
                continue;
            }

            if (!inSingleQuote && !inDoubleQuote && !inBacktick && c == '?') {
                count++;
            }
        }

        return count;
    }
}

package com.arkflame.mineclans.providers;

import java.sql.Connection;
import java.sql.SQLException;

public interface DatabaseTransaction<T> {
    T execute(Connection connection) throws SQLException;
}

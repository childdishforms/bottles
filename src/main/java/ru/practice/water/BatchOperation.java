package ru.practice.water;

import java.sql.Connection;
import java.sql.SQLException;

interface BatchOperation {
    void execute(Connection connection) throws SQLException;
}

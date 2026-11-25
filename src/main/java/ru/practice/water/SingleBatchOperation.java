package ru.practice.water;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

abstract class SingleBatchOperation implements BatchOperation {
    protected final long batchId;
    private final String sql;
    private final String conflictMessage;

    protected SingleBatchOperation(long batchId, String sql, String conflictMessage) {
        this.batchId = batchId;
        this.sql = sql;
        this.conflictMessage = conflictMessage;
    }

    @Override
    public final void execute(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement);
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException(conflictMessage);
            }
        }
    }

    protected abstract void bindParameters(PreparedStatement statement) throws SQLException;
}

package ru.practice.water;

import java.sql.PreparedStatement;
import java.sql.SQLException;

final class WarehouseReceipt extends SingleBatchOperation {
    private static final String INSERT = """
            INSERT INTO receipt(batch_id,quantity)
            SELECT b.id,b.filled-b.rejected FROM batch b JOIN quality_check q ON q.batch_id=b.id
            WHERE b.id=? AND q.approved AND b.filled>b.rejected ON CONFLICT (batch_id) DO NOTHING
            """;

    WarehouseReceipt(long batchId) {
        super(batchId, INSERT, "Приёмка недоступна. Проверьте статус и количество годных бутылок.");
    }

    @Override
    protected void bindParameters(PreparedStatement statement) throws SQLException {
        statement.setLong(1, batchId);
    }
}

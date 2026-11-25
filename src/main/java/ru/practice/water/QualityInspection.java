package ru.practice.water;

import java.sql.PreparedStatement;
import java.sql.SQLException;

final class QualityInspection extends SingleBatchOperation {
    private static final String INSERT = """
            INSERT INTO quality_check(batch_id,approved,comment)
            SELECT id,?,? FROM batch WHERE id=? ON CONFLICT (batch_id) DO NOTHING
            """;
    private final boolean approved;
    private final String comment;

    QualityInspection(long batchId, boolean approved, String comment) {
        super(batchId, INSERT, "Партия отсутствует или уже прошла контроль.");
        if (comment.length() > 300 || (!approved && comment.isBlank())) {
            throw new IllegalArgumentException("Для отклонения укажите причину. Длина комментария — до 300 символов.");
        }
        this.approved = approved;
        this.comment = comment.strip();
    }

    @Override
    protected void bindParameters(PreparedStatement statement) throws SQLException {
        statement.setBoolean(1, approved);
        statement.setString(2, comment);
        statement.setLong(3, batchId);
    }
}

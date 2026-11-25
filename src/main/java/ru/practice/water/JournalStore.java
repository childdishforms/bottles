package ru.practice.water;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Repository;

@Repository
public class JournalStore {
    public record Product(long id, String name, int volume) {}
    public record Batch(long id, String number, LocalDate day, String product, int volume, int filled,
                        int rejected, Boolean approved, String comment, LocalDate checkedOn, LocalDate receivedOn) {
        public int good() { return BatchRules.good(filled, rejected); }
        public int available() { return BatchRules.available(filled, rejected, approved, receivedOn != null); }
        public String status() {
            if (receivedOn != null) return "На складе";
            if (approved == null) return "Ожидает контроля";
            return approved ? "Допущена" : "Отклонена";
        }
    }

    private Connection connect() throws SQLException {
        String url = System.getenv("WATER_DB_URL");
        String login = System.getenv("WATER_DB_USER");
        if (url == null || url.isBlank() || login == null || login.isBlank()) {
            throw new SQLException("Database is not configured", "08001");
        }
        var props = new Properties();
        props.setProperty("user", login);
        props.setProperty("password", System.getenv().getOrDefault("WATER_DB_PASSWORD", ""));
        props.setProperty("connectTimeout", "4");
        props.setProperty("socketTimeout", "10");
        return DriverManager.getConnection(url, props);
    }

    public void addUser(String login, char[] password) throws SQLException {
        if (login.isBlank() || login.length() > 80 || password.length == 0 || password.length > 256) {
            throw new IllegalArgumentException();
        }
        try (var db = connect(); var s = db.prepareStatement("INSERT INTO app_user(login,password_hash) VALUES (?,?)")) {
            s.setString(1, login);
            s.setString(2, Passwords.encode(password));
            s.executeUpdate();
        }
    }

    public boolean authenticate(String login, char[] password) throws SQLException {
        try (var db = connect(); var s = db.prepareStatement("SELECT password_hash FROM app_user WHERE login=?")) {
            s.setString(1, login);
            try (var rs = s.executeQuery()) {
                return rs.next() && Passwords.matches(password, rs.getString(1));
            }
        }
    }

    public List<Product> products() throws SQLException {
        try (var db = connect(); var s = db.createStatement(); var rs = s.executeQuery("SELECT id,name,volume_ml FROM product ORDER BY name,volume_ml")) {
            var products = new ArrayList<Product>();
            while (rs.next()) products.add(new Product(rs.getLong(1), rs.getString(2), rs.getInt(3)));
            return products;
        }
    }

    public List<Batch> batches() throws SQLException {
        String sql = """
                SELECT b.id,b.number,b.bottled_on,p.name,p.volume_ml,b.filled,b.rejected,
                       q.approved,q.comment,q.checked_at::date,r.received_at::date
                FROM batch b JOIN product p ON p.id=b.product_id
                LEFT JOIN quality_check q ON q.batch_id=b.id LEFT JOIN receipt r ON r.batch_id=b.id
                ORDER BY b.bottled_on DESC,b.id DESC
                """;
        try (var db = connect(); var s = db.createStatement(); var rs = s.executeQuery(sql)) {
            var batches = new ArrayList<Batch>();
            while (rs.next()) {
                batches.add(new Batch(rs.getLong(1), rs.getString(2), rs.getObject(3, LocalDate.class),
                        rs.getString(4), rs.getInt(5), rs.getInt(6), rs.getInt(7), rs.getObject(8, Boolean.class),
                        rs.getString(9), rs.getObject(10, LocalDate.class), rs.getObject(11, LocalDate.class)));
            }
            return batches;
        }
    }

    public void create(String number, LocalDate day, long product, int filled, int rejected) throws SQLException {
        BatchRules.good(filled, rejected);
        if (number.isBlank() || number.length() > 40) throw new IllegalArgumentException("Укажите номер партии длиной до 40 символов.");
        try (var db = connect(); var s = db.prepareStatement("""
                INSERT INTO batch(number,bottled_on,product_id,filled,rejected) VALUES (?,?,?,?,?)
                """)) {
            s.setString(1, number.strip());
            s.setObject(2, day);
            s.setLong(3, product);
            s.setInt(4, filled);
            s.setInt(5, rejected);
            s.executeUpdate();
        } catch (SQLException ex) {
            if ("23505".equals(ex.getSQLState())) throw new IllegalArgumentException("Партия с таким номером уже существует.");
            if ("23503".equals(ex.getSQLState())) throw new IllegalArgumentException("Выберите продукцию из справочника.");
            throw ex;
        }
    }

    public void inspect(long id, boolean approved, String comment) throws SQLException {
        execute(new QualityInspection(id, approved, comment));
    }

    public void receive(long id) throws SQLException {
        execute(new WarehouseReceipt(id));
    }

    private void execute(BatchOperation operation) throws SQLException {
        try (Connection connection = connect()) {
            operation.execute(connection);
        }
    }
}

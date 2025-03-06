package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private String username;
    private String password;
    private String role;

    private String currentDatabase = null;
    private static final String ADMIN_POSTGRES_URL = "jdbc:postgresql://localhost:5432/postgres";

    public Database(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    private boolean databaseExists(String dbName) throws SQLException {
        String checkDbQuery = "SELECT 1 FROM pg_database WHERE datname = ?";

        try (Connection conn = DriverManager.getConnection(ADMIN_POSTGRES_URL, username, password);
             PreparedStatement stmt = conn.prepareStatement(checkDbQuery)) {

            stmt.setString(1, dbName);

            ResultSet rs = stmt.executeQuery();
            boolean exists = rs.next();
            return exists;
        }
    }

    private Connection getConnection(boolean usePostgres) throws SQLException {
        if (role.equals("admin")) {
            if (usePostgres) {
                return DriverManager.getConnection(ADMIN_POSTGRES_URL, username, password);
            } else if (currentDatabase != null) {
                String dbUrl = "jdbc:postgresql://localhost:5432/" + currentDatabase;
                return DriverManager.getConnection(dbUrl, username, password);
            } else {
                throw new SQLException("Ошибка: база данных не была создана!");
            }
        }

        if (role.equals("guest")) {
            String guestDbName = "car_rental";
            if (!databaseExists(guestDbName)) {
                throw new SQLException("Ошибка: база данных " + guestDbName + " не найдена!");
            }
            String dbUrl = "jdbc:postgresql://localhost:5432/" + guestDbName;

            return DriverManager.getConnection(dbUrl, username, password);
        }

        throw new SQLException("Ошибка: неизвестная роль!");
    }



    public void initializeDatabase() throws SQLException, IOException {
        InputStream in = getClass().getClassLoader().getResourceAsStream("stored_procedures.sql");
        if (in == null) {
            throw new IOException("Файл stored_procedures.sql не найден в ресурсах.");
        }
        String script = readFromInputStream(in);

        String systemMarker = "-- BEGIN CAR_RENTAL PROCEDURES";
        int splitIndex = script.indexOf(systemMarker);
        if (splitIndex < 0) {
            throw new IOException("Не найден разделитель для системных и пользовательских процедур.");
        }
        String systemScript = script.substring(0, splitIndex);
        String carRentalScript = script.substring(splitIndex);

        if (role.equals("admin")) {
            try (Connection conn = getConnection(true)) {
                runSqlScript(conn, systemScript);
            }
        }

        try (Connection conn = getConnection(false)) {
            runSqlScript(conn, carRentalScript);
        }
    }

    private String readFromInputStream(InputStream inputStream) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }

    private void runSqlScript(Connection conn, String script) throws SQLException {
        StringBuilder command = new StringBuilder();
        boolean inDollarBlock = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("--")) {
                    continue;
                }
                if (line.contains("$$")) {
                    inDollarBlock = !inDollarBlock;
                }
                command.append(line).append("\n");
                if (!inDollarBlock && line.trim().endsWith(";")) {
                    String sql = command.toString();
                    try (Statement stmt = conn.createStatement()) {
                        stmt.execute(sql);
                    } catch (SQLException e) {
                        System.err.println("Ошибка выполнения команды:\n" + sql);
                        throw e;
                    }
                    command.setLength(0);
                }
            }
            if (command.length() > 0) {
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(command.toString());
                }
            }
        } catch (IOException e) {
            throw new SQLException("Ошибка чтения SQL-скрипта", e);
        }
    }

    public void createDatabase(String dbName) throws SQLException {
        try (Connection conn = getConnection(true);
             Statement stmt = conn.createStatement()) {
            String sql = "CALL public.sp_create_database('" + dbName.replace("'", "''") + "')";
            stmt.execute(sql);
            currentDatabase = dbName;
        }
    }

    public void dropDatabase(String dbName) throws SQLException {
        try (Connection conn = getConnection(true);
             Statement stmt = conn.createStatement()) {
            String sql = "CALL public.sp_drop_database('" + dbName.replace("'", "''") + "')";
            stmt.execute(sql);
        }
    }

    public void createTable() throws SQLException {
        if (currentDatabase == null) {
            throw new SQLException("Ошибка: база данных не была создана!");
        }

        try (Connection conn = getConnection(false);
             Statement stmt = conn.createStatement()) {
            String sql = "CALL public.sp_create_table()";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error while creating table: " + e.getMessage());
        }
    }


    public void clearTable() throws SQLException {
        try (Connection conn = getConnection(false);
             Statement stmt = conn.createStatement()) {
            String sql = "CALL public.sp_clear_table()";
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error while cleaning table: " + e.getMessage());
        }
    }

    public void insertCar(String brand, String model, int year, BigDecimal price) throws SQLException {
        try (Connection conn = getConnection(false);
             Statement stmt = conn.createStatement()) {
            String sql = String.format(
                    "CALL sp_insert_car('%s', '%s', %d, %s)",
                    brand.replace("'", "''"),
                    model.replace("'", "''"),
                    year,
                    price.toPlainString()
            );
            stmt.execute(sql);

        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error while adding car: " + e.getMessage());
        }
    }

    public void updateCar(int id, String brand, String model, int year, BigDecimal price) throws SQLException {
        try (Connection conn = getConnection(false);
             Statement stmt = conn.createStatement()) {
            String sql = String.format(
                    "CALL sp_update_car(%d, '%s', '%s', %d, %s)",
                    id,
                    brand.replace("'", "''"),
                    model.replace("'", "''"),
                    year,
                    price.toPlainString()
            );
            stmt.execute(sql);
        }
    }

    public void deleteCarByModel(String model) throws SQLException {
        try (Connection conn = getConnection(false);
             Statement stmt = conn.createStatement()) {
            String sql = String.format(
                    "CALL sp_delete_car_by_model('%s')",
                    model.replace("'", "''")
            );
            stmt.execute(sql);
        }
    }


    public List<Object[]> searchCar(String model) throws SQLException {
        List<Object[]> results = new ArrayList<>();
        String sql = "SELECT id, brand, model, year, price FROM cars WHERE model = ?";

        try (Connection conn = getConnection(false);
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, model);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("id"),
                        rs.getString("brand"),
                        rs.getString("model"),
                        rs.getInt("year"),
                        rs.getBigDecimal("price")
                };
                results.add(row);
            }
        }
        return results;
    }


    public List<String> viewCars() throws SQLException {
        List<String> results = new ArrayList<>();
        try (Connection conn = getConnection(false);
             CallableStatement stmt = conn.prepareCall("{ call sp_view_cars() }")) {

            boolean hasResults = stmt.execute();

            if (hasResults) {
                try (ResultSet rs = stmt.getResultSet()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String brand = rs.getString("brand");
                        String carModel = rs.getString("model");
                        int year = rs.getInt("year");
                        BigDecimal price = rs.getBigDecimal("price");
                        results.add(id + " | " + brand + " | " + carModel + " | " + year + " | " + price);
                    }
                }
            }
        }
        return results;
    }
}
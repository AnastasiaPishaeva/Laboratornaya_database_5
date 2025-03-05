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

    // URL для подключения к базе данных car_rental и к системной базе postgres (для create/drop базы)
    private static final String ADMIN_CAR_DB_URL = "jdbc:postgresql://localhost:5432/car_rental";
    private static final String ADMIN_POSTGRES_URL = "jdbc:postgresql://localhost:5432/postgres";
    private static final String GUEST_CAR_DB_URL = "jdbc:postgresql://localhost:5432/car_rental";

    public Database(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    private Connection getConnection(boolean usePostgres) throws SQLException {
        if (role.equals("admin")) {
            if (usePostgres) {
                return DriverManager.getConnection(ADMIN_POSTGRES_URL, username, password);
            } else {
                return DriverManager.getConnection(ADMIN_CAR_DB_URL, username, password);
            }
        }
        if (role.equals("guest")){
            return DriverManager.getConnection(GUEST_CAR_DB_URL, username, password);
        }
        return null;
    }

    /**
     * Инициализация базы – выполнение SQL-скрипта из ресурсов.
     * Сценарий разделён на две части:
     * 1. Системные процедуры (исполняются на базе postgres)
     * 2. Процедуры для аренды автомобилей (исполняются на базе car_rental)
     */
    public void initializeDatabase() throws SQLException, IOException {
        // Загружаем скрипт из ресурсов
        InputStream in = getClass().getClassLoader().getResourceAsStream("stored_procedures.sql");
        if (in == null) {
            throw new IOException("Файл stored_procedures.sql не найден в ресурсах.");
        }
        String script = readFromInputStream(in);

        // Разбиваем скрипт на две части по разделителю
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

        // Выполнение скрипта car_rental (общего для всех)
        try (Connection conn = getConnection(false)) {
            runSqlScript(conn, carRentalScript);
        }
    }

    // Вспомогательный метод для чтения InputStream в строку
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


    // Простой парсер SQL-скрипта, учитывающий блоки $$ (без идеальной поддержки всех случаев)
    private void runSqlScript(Connection conn, String script) throws SQLException {
        StringBuilder command = new StringBuilder();
        boolean inDollarBlock = false;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Пропускаем пустые строки и комментарии (начинающиеся с --)
                if (line.trim().isEmpty() || line.trim().startsWith("--")) {
                    continue;
                }
                // Если встречается $$, переключаем флаг блока
                if (line.contains("$$")) {
                    inDollarBlock = !inDollarBlock;
                }
                command.append(line).append("\n");
                // Если не в блоке $$ и строка заканчивается точкой с запятой, выполняем команду
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
            // На случай, если осталась команда без завершающего символа
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
        try (Connection conn = getConnection(false);
             Statement stmt = conn.createStatement()) {
            // Выполняем вызов хранимой процедуры для создания таблицы
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


    public List<String> searchCar(String model) throws SQLException {
        List<String> results = new ArrayList<>();
        try (Connection conn = getConnection(false)) {
            CallableStatement stmt = conn.prepareCall("{ ? = call sp_search_car(?) }");
            stmt.registerOutParameter(1, Types.OTHER);
            stmt.setString(2, model);
            stmt.execute();
            ResultSet rs = (ResultSet) stmt.getObject(1);
            while (rs.next()) {
                int id = rs.getInt("id");
                String brand = rs.getString("brand");
                String carModel = rs.getString("model");
                int year = rs.getInt("year");
                BigDecimal price = rs.getBigDecimal("price");
                results.add(id + " | " + brand + " | " + carModel + " | " + year + " | " + price);
            }
        }
        return results;
    }

    public List<String> viewCars() throws SQLException {
        List<String> results = new ArrayList<>();
        try (Connection conn = getConnection(false);
             CallableStatement stmt = conn.prepareCall("{ call sp_view_cars() }")) {

            boolean hasResults = stmt.execute(); // Выполнить процедуру

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

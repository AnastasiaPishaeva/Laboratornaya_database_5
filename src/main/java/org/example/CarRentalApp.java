package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

public class CarRentalApp extends JFrame {
    private Database db;
    private String role; // "admin" или "guest"

    // Компоненты панели логина
    private JPanel loginPanel;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleComboBox;
    private JButton loginButton;

    // Основная панель
    private JPanel mainPanel;
    private JTextArea outputArea;

    // Панель для операций администратора
    private JPanel adminPanel;
    private JTextField dbNameField;
    private JButton createDbButton;
    private JButton dropDbButton;
    private JButton createTableButton;
    private JButton clearTableButton;
    private JTextField brandField;
    private JTextField modelField;
    private JTextField yearField;
    private JTextField priceField;
    private JButton insertCarButton;
    private JTextField updateIdField;
    private JTextField updateBrandField;
    private JTextField updateModelField;
    private JTextField updateYearField;
    private JTextField updatePriceField;
    private JButton updateCarButton;
    private JTextField deleteModelField;
    private JButton deleteCarButton;
    private JTable carTable;
    private DefaultTableModel tableModel;

    // Панель для общих операций (доступных и гостю)
    private JPanel commonPanel;
    private JTextField searchModelField;
    private JButton searchCarButton;
    private JButton viewCarsButton;

    public CarRentalApp() {
        setTitle("Car Rental App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        initLoginPanel();

        add(loginPanel);
        setVisible(true);
    }

    private void initLoginPanel() {
        loginPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        loginPanel.setBorder(BorderFactory.createTitledBorder("Вход в систему"));

        loginPanel.add(new JLabel("Username:"));
        usernameField = new JTextField();
        loginPanel.add(usernameField);

        loginPanel.add(new JLabel("Password:"));
        passwordField = new JPasswordField();
        loginPanel.add(passwordField);

        loginPanel.add(new JLabel("Роль:"));
        roleComboBox = new JComboBox<>(new String[] { "admin", "guest" });
        loginPanel.add(roleComboBox);

        loginButton = new JButton("Войти");
        loginPanel.add(loginButton);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            role = (String) roleComboBox.getSelectedItem();

            db = new Database(username, password, role);

            try {
                db.initializeDatabase();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка инициализации БД: " + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }

            initMainPanel();
            remove(loginPanel);
            add(mainPanel);
            revalidate();
            repaint();
        });
    }

    private void initMainPanel() {
        mainPanel = new JPanel(new BorderLayout());

        // Таблица для автомобилей
        tableModel = new DefaultTableModel(new String[]{"ID", "Brand", "Model", "Year", "Price"}, 0);
        carTable = new JTable(tableModel);
        JScrollPane tableScrollPane = new JScrollPane(carTable);
        mainPanel.add(tableScrollPane, BorderLayout.CENTER);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        JPanel operationsPanel = new JPanel(new GridLayout(2, 1));

        // Панель администратора
        adminPanel = new JPanel();
        adminPanel.setLayout(new BoxLayout(adminPanel, BoxLayout.Y_AXIS));
        adminPanel.setBorder(BorderFactory.createTitledBorder("Операции администратора"));

        JPanel dbPanel = new JPanel(new FlowLayout());
        dbPanel.add(new JLabel("Имя БД:"));
        dbNameField = new JTextField(10);
        dbPanel.add(dbNameField);
        createDbButton = new JButton("Создать БД");
        dbPanel.add(createDbButton);
        dropDbButton = new JButton("Удалить БД");
        dbPanel.add(dropDbButton);
        adminPanel.add(dbPanel);

        JPanel tablePanel = new JPanel(new FlowLayout());
        createTableButton = new JButton("Создать таблицу");
        tablePanel.add(createTableButton);
        clearTableButton = new JButton("Очистить таблицу");
        tablePanel.add(clearTableButton);
        adminPanel.add(tablePanel);

        // Вставка новой записи
        JPanel insertPanel = new JPanel(new FlowLayout());
        insertPanel.add(new JLabel("Brand:"));
        brandField = new JTextField(8);
        insertPanel.add(brandField);
        insertPanel.add(new JLabel("Model:"));
        modelField = new JTextField(8);
        insertPanel.add(modelField);
        insertPanel.add(new JLabel("Year:"));
        yearField = new JTextField(4);
        insertPanel.add(yearField);
        insertPanel.add(new JLabel("Price:"));
        priceField = new JTextField(6);
        insertPanel.add(priceField);
        insertCarButton = new JButton("Добавить автомобиль");
        insertPanel.add(insertCarButton);
        adminPanel.add(insertPanel);

        // Обновление записи
        JPanel updatePanel = new JPanel(new FlowLayout());
        updatePanel.add(new JLabel("ID:"));
        updateIdField = new JTextField(4);
        updatePanel.add(updateIdField);
        updatePanel.add(new JLabel("Brand:"));
        updateBrandField = new JTextField(8);
        updatePanel.add(updateBrandField);
        updatePanel.add(new JLabel("Model:"));
        updateModelField = new JTextField(8);
        updatePanel.add(updateModelField);
        updatePanel.add(new JLabel("Year:"));
        updateYearField = new JTextField(4);
        updatePanel.add(updateYearField);
        updatePanel.add(new JLabel("Price:"));
        updatePriceField = new JTextField(6);
        updatePanel.add(updatePriceField);
        updateCarButton = new JButton("Обновить автомобиль");
        updatePanel.add(updateCarButton);
        adminPanel.add(updatePanel);

        // Удаление записи по полю model
        JPanel deletePanel = new JPanel(new FlowLayout());
        deletePanel.add(new JLabel("Model:"));
        deleteModelField = new JTextField(8);
        deletePanel.add(deleteModelField);
        deleteCarButton = new JButton("Удалить автомобиль по Model");
        deletePanel.add(deleteCarButton);
        adminPanel.add(deletePanel);

        // Общие операции (доступны и гостю)
        commonPanel = new JPanel(new FlowLayout());
        commonPanel.setBorder(BorderFactory.createTitledBorder("Общие операции"));
        commonPanel.add(new JLabel("Search Model:"));
        searchModelField = new JTextField(8);
        commonPanel.add(searchModelField);
        searchCarButton = new JButton("Найти автомобиль");
        commonPanel.add(searchCarButton);
        viewCarsButton = new JButton("Показать все автомобили");
        commonPanel.add(viewCarsButton);

        operationsPanel.add(adminPanel);
        operationsPanel.add(commonPanel);
        mainPanel.add(operationsPanel, BorderLayout.NORTH);

        if (role.equals("guest")) {
            adminPanel.setEnabled(false);
            for (Component comp : adminPanel.getComponents()) {
                comp.setEnabled(false);
            }
        }

        // Назначение слушателей событий
        createDbButton.addActionListener(e -> {
            String dbName = dbNameField.getText();
            try {
                db.createDatabase(dbName);
                outputArea.append("База данных " + dbName + " успешно создана.\n");
            } catch (Exception ex) {
                outputArea.append("Ошибка при создании БД: " + ex.getMessage() + "\n");
            }
        });

        dropDbButton.addActionListener(e -> {
            String dbName = dbNameField.getText();
            try {
                db.dropDatabase(dbName);
                outputArea.append("База данных " + dbName + " успешно удалена.\n");
            } catch (Exception ex) {
                outputArea.append("Ошибка при удалении БД: " + ex.getMessage() + "\n");
            }
        });

        createTableButton.addActionListener(e -> {
            try {
                db.createTable();
                outputArea.append("Таблица создана успешно.\n");
            } catch (Exception ex) {
                outputArea.append("Ошибка при создании таблицы: " + ex.getMessage() + "\n");
            }
        });

        clearTableButton.addActionListener(e -> {
            try {
                db.clearTable();
                outputArea.append("Таблица очищена успешно.\n");

                loadCarData();
            } catch (Exception ex) {
                outputArea.append("Ошибка при очистке таблицы: " + ex.getMessage() + "\n");
            }
        });

        insertCarButton.addActionListener(e -> {
            String brand = brandField.getText();
            String model = modelField.getText();
            int year = Integer.parseInt(yearField.getText());
            BigDecimal price = new BigDecimal(priceField.getText());
            try {
                db.insertCar(brand, model, year, price);
                outputArea.append("Автомобиль добавлен успешно.\n");

                loadCarData();
            } catch (Exception ex) {
                outputArea.append("Ошибка при добавлении автомобиля: " + ex.getMessage() + "\n");
            }
        });

        updateCarButton.addActionListener(e -> {
            int id = Integer.parseInt(updateIdField.getText());
            String brand = updateBrandField.getText();
            String model = updateModelField.getText();
            int year = Integer.parseInt(updateYearField.getText());
            BigDecimal price = new BigDecimal(updatePriceField.getText());
            try {
                db.updateCar(id, brand, model, year, price);
                outputArea.append("Автомобиль обновлён успешно.\n");

                loadCarData();
            } catch (Exception ex) {
                outputArea.append("Ошибка при обновлении автомобиля: " + ex.getMessage() + "\n");
            }
        });


        deleteCarButton.addActionListener(e -> {
            String model = deleteModelField.getText();
            try {
                db.deleteCarByModel(model);
                outputArea.append("Автомобиль(и) с model = " + model + " удалены успешно.\n");

                loadCarData();
            } catch (Exception ex) {
                outputArea.append("Ошибка при удалении автомобиля: " + ex.getMessage() + "\n");
            }
        });

        searchCarButton.addActionListener(e -> {
            String model = searchModelField.getText();
            try {
                List<String> result = db.searchCar(model);
                outputArea.append("Результаты поиска:\n");
                for (String row : result) {
                    outputArea.append(row + "\n");
                }

            } catch (Exception ex) {
                outputArea.append("Ошибка при поиске: " + ex.getMessage() + "\n");
            }
        });

        viewCarsButton.addActionListener(e -> loadCarData());
    }

    private void loadCarData() {
        try {
            List<String> cars = db.viewCars();
            tableModel.setRowCount(0);

            for (String car : cars) {
                String[] data = car.split(" \\| ");
                tableModel.addRow(data);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при загрузке данных: " + e.getMessage());
        }
    }
}

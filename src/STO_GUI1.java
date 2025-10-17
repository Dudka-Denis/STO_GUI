import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class STO_GUI1 extends JFrame {
    private Connection conn;
    private JScrollPane tableScrollPane;

    public STO_GUI1() {
        setTitle("СТО - Запити до бази даних");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        tableScrollPane = new JScrollPane();
        add(tableScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 1));
        addButton(buttonPanel, "1. Клієнти з обраною маркою авто", this::query1);
        addButton(buttonPanel, "2. Працівники, ім'я яких починається на...", this::query2);
        addButton(buttonPanel, "3. Замовлення у заданому діапазоні дат", this::query3);
        addButton(buttonPanel, "4. Кількість замовлень за останні 7 днів", this::query4);
        addButton(buttonPanel, "5. Кількість замовлень на працівника", this::query5);
        addButton(buttonPanel, "6. Працівник з найбільшою кількістю замовлень", this::query6);
        addButton(buttonPanel, "7. Найпродуктивніші працівники по посадах", this::query7);
        addButton(buttonPanel, "8. Працівники без замовлень цього тижня", this::query8);
        addButton(buttonPanel, "9. Статус завершення замовлень авто", this::query9);

        add(buttonPanel, BorderLayout.WEST);
        connectToDatabase();
    }

    private void addButton(JPanel panel, String text, ActionListener action) {
        JButton button = new JButton(text);
        button.addActionListener(action);
        panel.add(button);
    }

    private void connectToDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/sto", "root", "1231");
        } catch (SQLException e) {
            showError("Помилка з'єднання з БД: " + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Помилка", JOptionPane.ERROR_MESSAGE);
    }

    private void executeAndShow(String sql) {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            List<String[]> rows = new ArrayList<>();

            while (rs.next()) {
                String[] row = new String[cols];
                for (int i = 0; i < cols; i++) row[i] = rs.getString(i + 1);
                rows.add(row);
            }
            if (rows.isEmpty()) {
                showError("Немає результатів для запиту.");
                return;
            }
            String[][] data = rows.toArray(new String[0][]);
            String[] columnNames = new String[cols];
            for (int i = 0; i < cols; i++) columnNames[i] = meta.getColumnName(i + 1);

            getContentPane().remove(tableScrollPane);
            tableScrollPane = new JScrollPane(new JTable(data, columnNames));
            add(tableScrollPane, BorderLayout.CENTER);
            revalidate();
            repaint();
        } catch (SQLException e) {
            showError("Помилка виконання запиту: " + e.getMessage());
        }
    }

    private List<String> getBrandsFromDatabase() {
        List<String> brands = new ArrayList<>();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT DISTINCT brand FROM Car ORDER BY brand")) {
            while (rs.next()) {
                brands.add(rs.getString("brand"));
            }
        } catch (SQLException e) {
            showError("Помилка завантаження марок авто: " + e.getMessage());
        }
        return brands;
    }

    // --- Скорочений query1 ---
    private void query1(ActionEvent e) {
        List<String> brands = getBrandsFromDatabase();
        if (brands.isEmpty()) {
            showError("Марки авто відсутні у базі даних.");
            return;
        }
        JComboBox<String> brandBox = new JComboBox<>(brands.toArray(new String[0]));
        if (JOptionPane.showConfirmDialog(this, brandBox, "Оберіть марку авто", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            executeAndShow("SELECT DISTINCT c.name, c.phone FROM Client c " +
                    "JOIN Car ca ON c.id = ca.clientId " +
                    "WHERE ca.brand = '" + brandBox.getSelectedItem() + "' ORDER BY c.name;");
        }
    }

    // --- Скорочений query2 ---
    private void query2(ActionEvent e) {
        String letter = JOptionPane.showInputDialog(this, "Ім'я починається на:");
        if (letter == null || letter.trim().isEmpty()) return;

        String sql = "SELECT name, phone FROM Employee WHERE name LIKE '" + letter + "%'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (!rs.isBeforeFirst()) {
                showError("Немає працівників, ім'я яких починається на '" + letter + "'");
                return;
            }
            executeAndShow(sql);
        } catch (SQLException ex) {
            showError("Помилка запиту: " + ex.getMessage());
        }
    }

    private void query3(ActionEvent e) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);

        JFormattedTextField dateField1 = new JFormattedTextField(dateFormat);
        dateField1.setValue(new java.util.Date());
        JFormattedTextField dateField2 = new JFormattedTextField(dateFormat);
        dateField2.setValue(new java.util.Date());

        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Початкова дата (yyyy-MM-dd):"));
        panel.add(dateField1);
        panel.add(new JLabel("Кінцева дата (yyyy-MM-dd):"));
        panel.add(dateField2);

        int result = JOptionPane.showConfirmDialog(this, panel, "Виберіть діапазон дат", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String date1Str = dateField1.getText().trim();
            String date2Str = dateField2.getText().trim();

            java.util.Date date1, date2;
            try {
                date1 = dateFormat.parse(date1Str);
                date2 = dateFormat.parse(date2Str);
            } catch (Exception ex) {
                showError("Неправильний формат дати. Будь ласка, введіть дати у форматі yyyy-MM-dd.");
                return;
            }

            if (date1.after(date2)) {
                showError("Початкова дата не може бути пізнішою за кінцеву дату.");
                return;
            }

            executeAndShow("SELECT * FROM `Order` WHERE dateCreated BETWEEN '" + date1Str + "' AND '" + date2Str + "';");
        }
    }

    private void query4(ActionEvent e) {
        executeAndShow("SELECT COUNT(*) AS recent_orders FROM `Order` WHERE dateCreated >= CURRENT_DATE - INTERVAL 7 DAY;");
    }

    private void query5(ActionEvent e) {
        executeAndShow("SELECT e.id, e.name, COUNT(o.id) AS orders_count FROM Employee e " +
                "LEFT JOIN `Order` o ON e.id = o.employeeId GROUP BY e.id, e.name;");
    }

    private void query6(ActionEvent e) {
        executeAndShow("SELECT e.id, e.name FROM Employee e JOIN `Order` o ON e.id = o.employeeId " +
                "GROUP BY e.id, e.name HAVING COUNT(o.id) >= ALL (SELECT COUNT(*) FROM `Order` GROUP BY employeeId);");
    }

    private void query7(ActionEvent e) {
        executeAndShow("SELECT e1.id, e1.name, p.name_position AS position FROM Employee e1 " +
                "JOIN Position p ON e1.positionId = p.id " +
                "WHERE NOT EXISTS (SELECT 1 FROM Employee e2 " +
                "JOIN `Order` o2 ON e2.id = o2.employeeId " +
                "WHERE e2.positionId = e1.positionId GROUP BY e2.id " +
                "HAVING COUNT(o2.id) > (SELECT COUNT(*) FROM `Order` o1 WHERE o1.employeeId = e1.id));");
    }

    private void query8(ActionEvent e) {
        executeAndShow("SELECT e.id, e.name FROM Employee e LEFT JOIN `Order` o ON e.id = o.employeeId " +
                "AND WEEK(o.dateCreated) = WEEK(CURRENT_DATE) WHERE o.id IS NULL;");
    }

    private void query9(ActionEvent e) {
        executeAndShow("SELECT ca.id, ca.plate, ca.brand, 'Має завершене замовлення' AS status FROM Car ca " +
                "WHERE EXISTS (SELECT 1 FROM `Order` o WHERE o.carId = ca.id AND o.dateCompleted IS NOT NULL) " +
                "UNION SELECT ca.id, ca.plate, ca.brand, 'Немає завершених замовлень' AS status FROM Car ca " +
                "WHERE NOT EXISTS (SELECT 1 FROM `Order` o WHERE o.carId = ca.id AND o.dateCompleted IS NOT NULL);");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new STO_GUI().setVisible(true));
    }
}

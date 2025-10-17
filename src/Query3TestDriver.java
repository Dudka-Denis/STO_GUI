import java.sql.*;

public class Query3TestDriver {
    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();
        char testLetter = '1';
        dbManager.query3(testLetter);
    }
}

class DatabaseManager {
    public void query3(char letter) {
        if (!Character.isLetter(letter)) {
            System.out.println("Помилка: введено не літеру, а символ '" + letter + "'");
            return;
        }

        String url = "jdbc:mysql://localhost:3306/sto";
        String user = "root";
        String password = "1231";

        String sql = "SELECT * FROM employee WHERE name LIKE ?";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, letter + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Немає співробітників із ім'ям на літеру " + letter);
                } else {
                    System.out.println("Список співробітників:");

                    do {
                        String Name = rs.getString("name");
                        String phone = rs.getString("phone");
                        System.out.printf(" - %s, номер телефону: %s%n", Name, phone);
                    } while (rs.next());
                }
            }

        } catch (SQLException e) {
            System.err.println("Помилка при виконанні SQL-запиту: " + e.getMessage());
        }
    }
}
import db.DBHelper;

import java.sql.Connection;

public class DBTest {
    public static void main(String[] args) {
        try {
            Connection connection = DBHelper.getConnection();
            System.out.println("Connected to MySQL successfully");
            connection.close();
        } catch (Exception e) {
            System.out.println("Connection Failed: " + e.getMessage());
        }
    }
}

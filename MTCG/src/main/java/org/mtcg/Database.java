package org.mtcg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;


//TEMPLATE CLASS FOR DATABASE
public class Database {
        public static void main(String[] args) {
            // Verbindungsdetails
            String url = "jdbc:postgresql://localhost:5432/meine_datenbank";
            String user = "postgres";
            String password = "tomi2002";

            // SQL-Befehl zum Einfügen von Daten
            String sql = "INSERT INTO benutzer (name, email) VALUES (?, ?)";

            try (Connection conn = DriverManager.getConnection(url, user, password);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // Daten einfügen
                pstmt.setString(1, "Anna Schmidt");
                pstmt.setString(2, "anna.schmidt@example.com");
                pstmt.executeUpdate();

                System.out.println("Daten erfolgreich eingefügt!");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

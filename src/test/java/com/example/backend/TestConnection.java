package com.example.backend;

import java.sql.*;

public class TestConnection {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://db.hptbymbybvqceyglkvtx.supabase.co:5432/postgres";
        String user = "postgres";
        String password = "Palagiri677245";
        
        System.out.println("Testing Supabase connection...");
        System.out.println("URL: " + url);
        
        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            System.out.println("✅ CONNECTION SUCCESSFUL!");
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT version();");
            if (rs.next()) {
                System.out.println("PostgreSQL Version: " + rs.getString(1));
            }
            
            conn.close();
        } catch (SQLException e) {
            System.out.println("❌ Connection failed: " + e.getMessage());
        }
    }
}
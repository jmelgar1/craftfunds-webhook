package com.example.webhook.repository;

import com.example.webhook.config.DatabaseConfig;
import com.example.webhook.models.PaymentRecord;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PaymentRepository {
    private static final Gson gson = new Gson();
    
    
    public void savePayment(PaymentRecord payment) throws SQLException {
        String insertSQL = """
            INSERT INTO donations (timestamp, event_type, amount, currency, webhook_data)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, payment.timestamp);
            pstmt.setString(2, payment.eventType);
            
            if (payment.amount != null) {
                pstmt.setBigDecimal(3, new java.math.BigDecimal(payment.amount));
            } else {
                pstmt.setNull(3, Types.DECIMAL);
            }
            
            pstmt.setString(4, payment.currency);
            pstmt.setString(5, gson.toJson(payment.webhookData));
            
            pstmt.executeUpdate();
        }
    }
    
    public List<PaymentRecord> getAllPayments() throws SQLException {
        String selectSQL = "SELECT * FROM donations ORDER BY created_at DESC";
        List<PaymentRecord> payments = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                PaymentRecord payment = new PaymentRecord();
                payment.timestamp = rs.getString("timestamp");
                payment.eventType = rs.getString("event_type");
                payment.amount = rs.getBigDecimal("amount") != null ? 
                    rs.getBigDecimal("amount").toString() : null;
                payment.currency = rs.getString("currency");
                payment.webhookData = gson.fromJson(rs.getString("webhook_data"), JsonObject.class);
                
                payments.add(payment);
            }
        }
        
        return payments;
    }
    
    public int getPaymentCount() throws SQLException {
        String countSQL = "SELECT COUNT(*) FROM donations";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSQL)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    public String getLastPaymentTimestamp() throws SQLException {
        String lastPaymentSQL = "SELECT timestamp FROM donations ORDER BY created_at DESC LIMIT 1";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(lastPaymentSQL)) {
            
            if (rs.next()) {
                return rs.getString("timestamp");
            }
            return null;
        }
    }
}
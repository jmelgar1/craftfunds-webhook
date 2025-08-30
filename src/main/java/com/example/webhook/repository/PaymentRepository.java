package com.example.webhook.repository;

import com.example.webhook.config.DatabaseConfig;
import com.example.webhook.models.PaymentRecord;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PaymentRepository {
    private static final Gson gson = new Gson();
    
    
    public void savePayment(PaymentRecord payment) throws SQLException {
        String insertSQL = """
            INSERT INTO donations (name, amount, date, currency)
            VALUES (?, ?, ?, ?)
        """;
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
            pstmt.setString(1, payment.eventType); // Using eventType as name
            
            if (payment.amount != null) {
                pstmt.setBigDecimal(2, new java.math.BigDecimal(payment.amount));
            } else {
                pstmt.setNull(2, Types.DECIMAL);
            }
            
            pstmt.setDate(3, Date.valueOf(payment.timestamp.substring(0, 10))); // Convert timestamp to date
            pstmt.setString(4, payment.currency);
            
            pstmt.executeUpdate();
        }
    }
    
    public List<PaymentRecord> getAllPayments() throws SQLException {
        String selectSQL = "SELECT * FROM donations ORDER BY date DESC";
        List<PaymentRecord> payments = new ArrayList<>();
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                PaymentRecord payment = new PaymentRecord();
                payment.timestamp = rs.getDate("date").toString();
                payment.eventType = rs.getString("name");
                payment.amount = rs.getBigDecimal("amount") != null ? 
                    rs.getBigDecimal("amount").toString() : null;
                payment.currency = rs.getString("currency");
                payment.webhookData = new JsonObject(); // Empty since no webhook_data column
                
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
        String lastPaymentSQL = "SELECT date FROM donations ORDER BY date DESC LIMIT 1";
        
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(lastPaymentSQL)) {
            
            if (rs.next()) {
                return rs.getDate("date").toString();
            }
            return null;
        }
    }
    
    public BigDecimal getDonationTotalForPeriod(LocalDate startDate, LocalDate endDate) throws SQLException {
        String totalSQL = "SELECT COALESCE(SUM(amount), 0) FROM donations WHERE date >= ? AND date <= ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(totalSQL)) {
            
            pstmt.setDate(1, Date.valueOf(startDate));
            pstmt.setDate(2, Date.valueOf(endDate));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    BigDecimal result = rs.getBigDecimal(1);
                    return result != null ? result : BigDecimal.ZERO;
                }
                return BigDecimal.ZERO;
            }
        }
    }
    
    public void updateTotalSpending(BigDecimal payoutAmount) throws SQLException {
        String updateSQL = "UPDATE total_spending SET total_spent = total_spent + ?";
        
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSQL)) {
            
            pstmt.setBigDecimal(1, payoutAmount);
            pstmt.executeUpdate();
        }
    }
}
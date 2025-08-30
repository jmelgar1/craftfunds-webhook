package com.example.webhook.services;

import com.example.webhook.models.PaymentRecord;
import com.example.webhook.models.PaymentSummary;
import com.example.webhook.repository.PaymentRepository;
import com.google.gson.JsonObject;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentService {
    private final PaymentRepository paymentRepository;
    
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
    public void savePayment(JsonObject webhook) throws SQLException {
        PaymentRecord payment = new PaymentRecord();
        payment.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        payment.eventType = webhook.get("event_type").getAsString();
        payment.webhookData = webhook;
        
        extractPaymentAmount(payment, webhook);
        
        paymentRepository.savePayment(payment);
    }
    
    public PaymentSummary calculatePaymentSummary() throws SQLException {
        List<PaymentRecord> payments = paymentRepository.getAllPayments();
        int paymentCount = paymentRepository.getPaymentCount();
        String lastPaymentDate = paymentRepository.getLastPaymentTimestamp();
        
        if (payments.isEmpty()) {
            return new PaymentSummary(false, "No payment data found", null, 0, null);
        }
        
        Map<String, Double> balances = new HashMap<>();
        
        for (PaymentRecord payment : payments) {
            try {
                if (payment.amount != null && payment.currency != null) {
                    double amount = Double.parseDouble(payment.amount);
                    balances.merge(payment.currency, amount, Double::sum);
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid payment amount: " + payment.amount);
            }
        }
        
        return new PaymentSummary(true, "Success", balances, paymentCount, lastPaymentDate);
    }
    
    public List<PaymentRecord> getAllDonations() throws SQLException {
        return paymentRepository.getAllPayments();
    }
    
    public double getCurrentMonthDonationTotal() throws SQLException {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        
        return paymentRepository.getDonationTotalForPeriod(startDate, endDate);
    }
    
    private void extractPaymentAmount(PaymentRecord payment, JsonObject webhook) {
        try {
            if (webhook.has("resource")) {
                JsonObject resource = webhook.getAsJsonObject("resource");
                if (resource.has("amount")) {
                    JsonObject amount = resource.getAsJsonObject("amount");
                    payment.amount = amount.get("value").getAsString();
                    payment.currency = amount.get("currency_code").getAsString();
                }
            }
        } catch (Exception e) {
            System.err.println("Could not extract payment amount: " + e.getMessage());
        }
    }
}
package com.example.webhook.services;

import com.example.webhook.models.PaymentRecord;
import com.example.webhook.models.PaymentSummary;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PaymentService {
    private final FileService fileService;
    
    public PaymentService(FileService fileService) {
        this.fileService = fileService;
    }
    
    public void savePayment(JsonObject webhook) throws IOException {
        PaymentRecord payment = new PaymentRecord();
        payment.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        payment.eventType = webhook.get("event_type").getAsString();
        payment.webhookData = webhook;
        
        extractPaymentAmount(payment, webhook);
        
        List<PaymentRecord> payments = fileService.loadPayments();
        payments.add(payment);
        
        fileService.savePayments(payments);
    }
    
    public PaymentSummary calculatePaymentSummary() throws IOException {
        List<PaymentRecord> payments = fileService.loadPayments();
        
        if (payments.isEmpty()) {
            return new PaymentSummary(false, "No payment data found", null, 0, null);
        }
        
        Map<String, Double> balances = new HashMap<>();
        String lastPaymentDate = null;
        
        for (PaymentRecord payment : payments) {
            try {
                if (payment.amount != null && payment.currency != null) {
                    double amount = Double.parseDouble(payment.amount);
                    balances.merge(payment.currency, amount, Double::sum);
                    
                    if (lastPaymentDate == null || payment.timestamp.compareTo(lastPaymentDate) > 0) {
                        lastPaymentDate = payment.timestamp;
                    }
                }
            } catch (NumberFormatException e) {
                System.err.println("Invalid payment amount: " + payment.amount);
            }
        }
        
        return new PaymentSummary(true, "Success", balances, payments.size(), lastPaymentDate);
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
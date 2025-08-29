package com.example.webhook.models;

import java.util.Map;

public class PaymentSummary {
    public boolean success;
    public String message;
    public Map<String, Double> balances;
    public int transactionCount;
    public String lastPaymentDate;
    
    public PaymentSummary(boolean success, String message, Map<String, Double> balances, 
                        int transactionCount, String lastPaymentDate) {
        this.success = success;
        this.message = message;
        this.balances = balances;
        this.transactionCount = transactionCount;
        this.lastPaymentDate = lastPaymentDate;
    }
}
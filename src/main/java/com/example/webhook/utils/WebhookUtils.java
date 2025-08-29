package com.example.webhook.utils;

public class WebhookUtils {
    
    public static boolean isPaymentEvent(String eventType) {
        return eventType.contains("PAYMENT") || 
               eventType.contains("CHECKOUT") ||
               eventType.contains("SALE") ||
               eventType.equals("PAYMENTS.PAYMENT.CREATED");
    }
}
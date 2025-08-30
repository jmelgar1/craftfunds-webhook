package com.example.webhook.utils;

public class WebhookUtils {
    
    public static boolean isPaymentEvent(String eventType) {
        return eventType.equals("PAYMENT.CAPTURE.COMPLETED");
    }
    
    public static boolean isPayoutEvent(String eventType) {
        return eventType.equals("PAYMENT.PAYOUTS-ITEM.SUCCEEDED");
    }
}
package com.example.webhook.models;

import com.google.gson.JsonObject;

public class PaymentRecord {
    public String timestamp;
    public String eventType;
    public String amount;
    public String currency;
    public JsonObject webhookData;
}
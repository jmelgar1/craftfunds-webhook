package com.example.webhook.handlers;

import com.example.webhook.services.PaymentService;
import com.example.webhook.utils.WebhookUtils;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class WebhookHandler implements HttpHandler {
    private final PaymentService paymentService;
    private final Gson gson;
    
    public WebhookHandler(PaymentService paymentService, Gson gson) {
        this.paymentService = paymentService;
        this.gson = gson;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("POST".equals(exchange.getRequestMethod())) {
            try {
                String body = new String(exchange.getRequestBody().readAllBytes());
                System.out.println("Received webhook: " + body);
                
                JsonObject webhook = gson.fromJson(body, JsonObject.class);
                
                String eventType = webhook.get("event_type").getAsString();
                System.out.println("Processing webhook event: " + eventType);
                
                if (WebhookUtils.isPaymentEvent(eventType)) {
                    logPaymentDetails(webhook, eventType);
                    paymentService.savePayment(webhook);
                    System.out.println("Payment saved successfully: " + eventType);
                } else {
                    System.out.println("Ignoring non-payment event: " + eventType);
                }
                
                String response = "OK";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
                
            } catch (Exception e) {
                System.err.println("Error processing webhook: " + e.getMessage());
                e.printStackTrace();
                
                String response = "Error";
                exchange.sendResponseHeaders(500, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        } else {
            exchange.sendResponseHeaders(405, 0);
        }
    }
    
    private void logPaymentDetails(JsonObject webhook, String eventType) {
        try {
            System.out.println("=== PAYMENT DETAILS ===");
            System.out.println("Event Type: " + eventType);
            
            if (webhook.has("resource")) {
                JsonObject resource = webhook.getAsJsonObject("resource");
                
                if (resource.has("amount")) {
                    JsonObject amount = resource.getAsJsonObject("amount");
                    String value = amount.get("value").getAsString();
                    String currency = amount.get("currency_code").getAsString();
                    System.out.println("Amount: " + value + " " + currency);
                }
                
                if (resource.has("id")) {
                    System.out.println("Payment ID: " + resource.get("id").getAsString());
                }
                
                if (resource.has("state")) {
                    System.out.println("Payment State: " + resource.get("state").getAsString());
                }
                
                if (resource.has("create_time")) {
                    System.out.println("Created: " + resource.get("create_time").getAsString());
                }
                
                if (resource.has("payer") && resource.getAsJsonObject("payer").has("payer_info")) {
                    JsonObject payerInfo = resource.getAsJsonObject("payer").getAsJsonObject("payer_info");
                    if (payerInfo.has("email")) {
                        System.out.println("Payer Email: " + payerInfo.get("email").getAsString());
                    }
                    if (payerInfo.has("first_name") && payerInfo.has("last_name")) {
                        String firstName = payerInfo.get("first_name").getAsString();
                        String lastName = payerInfo.get("last_name").getAsString();
                        System.out.println("Payer Name: " + firstName + " " + lastName);
                    }
                }
            }
            
            if (webhook.has("id")) {
                System.out.println("Webhook ID: " + webhook.get("id").getAsString());
            }
            
            if (webhook.has("create_time")) {
                System.out.println("Webhook Created: " + webhook.get("create_time").getAsString());
            }
            
            System.out.println("========================");
            
        } catch (Exception e) {
            System.err.println("Error logging payment details: " + e.getMessage());
        }
    }
}
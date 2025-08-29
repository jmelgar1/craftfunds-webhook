package com.example.webhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PayPalWebhookServer {
    private static final int PORT = 8080;
    private static final String PAYMENTS_FILE = "payments.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/paypal/webhook", new WebhookHandler());
        server.createContext("/api/payments", new PaymentsApiHandler());
        server.createContext("/status", new StatusHandler());
        server.setExecutor(null);
        
        System.out.println("PayPal Webhook Server started on port " + PORT);
        System.out.println("Webhook endpoint: http://localhost:" + PORT + "/paypal/webhook");
        System.out.println("Payments API: http://localhost:" + PORT + "/api/payments");
        System.out.println("Status endpoint: http://localhost:" + PORT + "/status");
        
        server.start();
    }
    
    static class WebhookHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equals(exchange.getRequestMethod())) {
                try {
                    // Read the webhook payload
                    String body = new String(exchange.getRequestBody().readAllBytes());
                    System.out.println("Received webhook: " + body);
                    
                    // Parse the PayPal webhook
                    JsonObject webhook = gson.fromJson(body, JsonObject.class);
                    
                    // Check if it's a payment event
                    String eventType = webhook.get("event_type").getAsString();
                    if (isPaymentEvent(eventType)) {
                        savePayment(webhook);
                        System.out.println("Payment saved: " + eventType);
                    }
                    
                    // Respond with 200 OK (required by PayPal)
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
                exchange.sendResponseHeaders(405, 0); // Method not allowed
            }
        }
        
        private boolean isPaymentEvent(String eventType) {
            return eventType.contains("PAYMENT") || 
                   eventType.contains("CHECKOUT") ||
                   eventType.contains("SALE") ||
                   eventType.equals("PAYMENTS.PAYMENT.CREATED");
        }
        
        private void savePayment(JsonObject webhook) throws IOException {
            PaymentRecord payment = new PaymentRecord();
            payment.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            payment.eventType = webhook.get("event_type").getAsString();
            payment.webhookData = webhook;
            
            // Try to extract payment amount
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
            
            // Read existing payments
            List<PaymentRecord> payments = loadPayments();
            payments.add(payment);
            
            // Save updated payments
            try (FileWriter writer = new FileWriter(PAYMENTS_FILE)) {
                gson.toJson(payments, writer);
            }
        }
        
        private List<PaymentRecord> loadPayments() {
            Path paymentsPath = Paths.get(PAYMENTS_FILE);
            if (!Files.exists(paymentsPath)) {
                return new ArrayList<>();
            }
            
            try (FileReader reader = new FileReader(PAYMENTS_FILE)) {
                PaymentRecord[] paymentsArray = gson.fromJson(reader, PaymentRecord[].class);
                return paymentsArray != null ? List.of(paymentsArray) : new ArrayList<>();
            } catch (Exception e) {
                System.err.println("Error loading payments: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }
    
    static class PaymentsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Enable CORS for cross-origin requests
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
            
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, 0);
                return;
            }
            
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    PaymentSummary summary = calculatePaymentSummary();
                    String jsonResponse = gson.toJson(summary);
                    
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(jsonResponse.getBytes());
                    }
                    
                    System.out.println("API request served: " + summary.transactionCount + " transactions");
                    
                } catch (Exception e) {
                    System.err.println("Error processing API request: " + e.getMessage());
                    e.printStackTrace();
                    
                    String errorResponse = "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}";
                    exchange.sendResponseHeaders(500, errorResponse.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(errorResponse.getBytes());
                    }
                }
            } else {
                exchange.sendResponseHeaders(405, 0); // Method not allowed
            }
        }
        
        private PaymentSummary calculatePaymentSummary() throws IOException {
            List<PaymentRecord> payments = loadPayments();
            
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
        
        private List<PaymentRecord> loadPayments() {
            Path paymentsPath = Paths.get(PAYMENTS_FILE);
            if (!Files.exists(paymentsPath)) {
                return new ArrayList<>();
            }
            
            try (FileReader reader = new FileReader(PAYMENTS_FILE)) {
                PaymentRecord[] paymentsArray = gson.fromJson(reader, PaymentRecord[].class);
                return paymentsArray != null ? List.of(paymentsArray) : new ArrayList<>();
            } catch (Exception e) {
                System.err.println("Error loading payments: " + e.getMessage());
                return new ArrayList<>();
            }
        }
    }
    
    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "PayPal Webhook Server is running";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    static class PaymentSummary {
        boolean success;
        String message;
        Map<String, Double> balances;
        int transactionCount;
        String lastPaymentDate;
        
        public PaymentSummary(boolean success, String message, Map<String, Double> balances, 
                            int transactionCount, String lastPaymentDate) {
            this.success = success;
            this.message = message;
            this.balances = balances;
            this.transactionCount = transactionCount;
            this.lastPaymentDate = lastPaymentDate;
        }
    }
    
    static class PaymentRecord {
        String timestamp;
        String eventType;
        String amount;
        String currency;
        JsonObject webhookData;
    }
}
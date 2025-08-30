package com.example.webhook.handlers;

import com.example.webhook.services.PaymentService;
import com.example.webhook.models.PaymentRecord;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DonationsApiHandler implements HttpHandler {
    private final PaymentService paymentService;
    private final Gson gson;
    
    public DonationsApiHandler(PaymentService paymentService, Gson gson) {
        this.paymentService = paymentService;
        this.gson = gson;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, 0);
            return;
        }
        
        if ("GET".equals(exchange.getRequestMethod())) {
            String path = exchange.getRequestURI().getPath();
            
            try {
                if (path.equals("/api/donations")) {
                    handleDonationsList(exchange);
                } else if (path.equals("/api/donations/fund")) {
                    handleCurrentMonthFund(exchange);
                } else {
                    exchange.sendResponseHeaders(404, 0);
                }
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
            exchange.sendResponseHeaders(405, 0);
        }
    }
    
    private void handleDonationsList(HttpExchange exchange) throws IOException, SQLException {
        List<PaymentRecord> donations = paymentService.getAllDonations();
        String jsonResponse = gson.toJson(donations);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes());
        }
        
        System.out.println("Donations list served: " + donations.size() + " donations");
    }
    
    private void handleCurrentMonthFund(HttpExchange exchange) throws IOException, SQLException {
        BigDecimal totalAmount = paymentService.getCurrentMonthDonationTotal();
        
        Map<String, Object> response = new HashMap<>();
        response.put("amount", totalAmount);
        
        String jsonResponse = gson.toJson(response);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, jsonResponse.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(jsonResponse.getBytes());
        }
        
        System.out.println("Current month fund total served: " + totalAmount);
    }
}
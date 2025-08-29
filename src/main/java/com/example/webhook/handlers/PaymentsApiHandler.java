package com.example.webhook.handlers;

import com.example.webhook.models.PaymentSummary;
import com.example.webhook.services.PaymentService;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class PaymentsApiHandler implements HttpHandler {
    private final PaymentService paymentService;
    private final Gson gson;
    
    public PaymentsApiHandler(PaymentService paymentService, Gson gson) {
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
            try {
                PaymentSummary summary = paymentService.calculatePaymentSummary();
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
            exchange.sendResponseHeaders(405, 0);
        }
    }
}
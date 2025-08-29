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
                if (WebhookUtils.isPaymentEvent(eventType)) {
                    paymentService.savePayment(webhook);
                    System.out.println("Payment saved: " + eventType);
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
}
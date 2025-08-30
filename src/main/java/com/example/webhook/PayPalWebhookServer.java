package com.example.webhook;

import com.example.webhook.handlers.*;
import com.example.webhook.config.DatabaseConfig;
import com.example.webhook.repository.PaymentRepository;
import com.example.webhook.services.PaymentService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PayPalWebhookServer {
    private static final int PORT = 8080;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public static void main(String[] args) throws IOException {
        DatabaseConfig.testConnection();
        
        PaymentRepository paymentRepository = new PaymentRepository();
        PaymentService paymentService = new PaymentService(paymentRepository);
        
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/paypal/webhook", new WebhookHandler(paymentService, gson));
        server.createContext("/api/payments", new PaymentsApiHandler(paymentService, gson));
        server.createContext("/status", new StatusHandler());
        server.setExecutor(null);
    
        System.out.println("PayPal Webhook Server started on port " + PORT);
        System.out.println("Webhook endpoint: http://localhost:" + PORT + "/paypal/webhook");
        System.out.println("Payments API: http://localhost:" + PORT + "/api/payments");
        System.out.println("Status endpoint: http://localhost:" + PORT + "/status");
        
        server.start();
    }
}
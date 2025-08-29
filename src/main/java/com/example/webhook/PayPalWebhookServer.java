package com.example.webhook;

import com.example.webhook.handlers.*;
import com.example.webhook.services.FileService;
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
        FileService fileService = new FileService();
        PaymentService paymentService = new PaymentService(fileService);
        
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/paypal/webhook", new WebhookHandler(paymentService, gson));
        server.createContext("/api/payments", new PaymentsApiHandler(paymentService, gson));
        server.createContext("/status", new StatusHandler());
        server.createContext("/debug/payments-file", new FileViewHandler(fileService));
        server.setExecutor(null);
        
        System.out.println("PayPal Webhook Server started on port " + PORT);
        System.out.println("Webhook endpoint: http://localhost:" + PORT + "/paypal/webhook");
        System.out.println("Payments API: http://localhost:" + PORT + "/api/payments");
        System.out.println("Status endpoint: http://localhost:" + PORT + "/status");
        System.out.println("Debug payments file: http://localhost:" + PORT + "/debug/payments-file");
        
        server.start();
    }
}
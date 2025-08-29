package com.example.webhook.handlers;

import com.example.webhook.services.FileService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class FileViewHandler implements HttpHandler {
    private final FileService fileService;
    
    public FileViewHandler(FileService fileService) {
        this.fileService = fileService;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            try {
                if (!fileService.paymentsFileExists()) {
                    String response = "payments.json file not found";
                    exchange.sendResponseHeaders(404, response.length());
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    return;
                }
                
                String fileContent = fileService.readPaymentsFile();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, fileContent.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(fileContent.getBytes());
                }
                
            } catch (Exception e) {
                String errorResponse = "Error reading payments file: " + e.getMessage();
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
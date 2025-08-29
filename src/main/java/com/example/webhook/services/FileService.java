package com.example.webhook.services;

import com.example.webhook.models.PaymentRecord;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FileService {
    private static final String PAYMENTS_FILE = "payments.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    public List<PaymentRecord> loadPayments() {
        Path paymentsPath = Paths.get(PAYMENTS_FILE);
        if (!Files.exists(paymentsPath)) {
            return new ArrayList<>();
        }
        
        try (FileReader reader = new FileReader(PAYMENTS_FILE)) {
            PaymentRecord[] paymentsArray = gson.fromJson(reader, PaymentRecord[].class);
            return paymentsArray != null ? new ArrayList<>(List.of(paymentsArray)) : new ArrayList<>();
        } catch (Exception e) {
            System.err.println("Error loading payments: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public void savePayments(List<PaymentRecord> payments) throws IOException {
        try (FileWriter writer = new FileWriter(PAYMENTS_FILE)) {
            gson.toJson(payments, writer);
        }
    }
    
    public String readPaymentsFile() throws IOException {
        Path paymentsPath = Paths.get(PAYMENTS_FILE);
        if (!Files.exists(paymentsPath)) {
            return null;
        }
        return Files.readString(paymentsPath);
    }
    
    public boolean paymentsFileExists() {
        return Files.exists(Paths.get(PAYMENTS_FILE));
    }
}
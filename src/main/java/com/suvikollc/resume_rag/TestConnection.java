// src/main/java/com/suvikollc/resume_rag/TestConnection.java
package com.suvikollc.resume_rag;

import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.ShareServiceClientBuilder;

public class TestConnection {

    public static void main(String[] args) {
        // --- PASTE YOUR FULL AND LATEST CONNECTION STRING HERE ---
        String connectionString = "<your_connection_string>";

        System.out.println("Attempting to connect to Azure File Storage...");
        System.out.println("Using connection string with AccountName: " + getAccountName(connectionString));

        try {
            // Build the client directly, just like the Spring starter does
            ShareServiceClient client = new ShareServiceClientBuilder()
                .connectionString(connectionString)
                .buildClient();

            // Perform one simple, authenticated read operation
            client.getProperties();

            System.out.println("\nSUCCESS: Authentication successful! The connection string is valid.");

        } catch (Exception e) {
            System.err.println("\nERROR: Authentication failed! The connection string is invalid or there is a network issue.");
            System.err.println("Received exception: " + e.getMessage());
        }
    }

    // Helper method to parse account name without printing the secret key
    private static String getAccountName(String connStr) {
        for (String part : connStr.split(";")) {
            if (part.toLowerCase().startsWith("accountname=")) {
                return part.substring("accountname=".length());
            }
        }
        return "Not found";
    }
}
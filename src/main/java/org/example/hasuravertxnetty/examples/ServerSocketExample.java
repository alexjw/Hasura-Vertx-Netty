package org.example.hasuravertxnetty.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketExample {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8089, 100); // Bind to port 8080
        System.out.println("Server listening on port 8080...");
        while (true) {
            Socket clientSocket = serverSocket.accept(); // Accept client connection
            new Thread(() -> {
                try {
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    out.println("Hello, client!");
                    System.out.println("Received: " + in.readLine());
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}

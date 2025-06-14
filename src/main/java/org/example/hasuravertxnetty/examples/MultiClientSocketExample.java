package org.example.hasuravertxnetty.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

public class MultiClientSocketExample {
    private static final String HOST = "localhost";
    private static final int PORT = 8089;
    private static final int NUM_CLIENTS = 32;

    public static void main(String[] args) throws InterruptedException {
        var threads = new ArrayList<Thread>();

        // Create 32 client sockets and threads
        for (int i = 0; i < NUM_CLIENTS; i++) {
            Socket socket = new Socket();
            int clientId = i;
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(Math.round(Math.random() * 10000));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                handleClient(socket, clientId);
            });
            threads.add(thread);
            thread.start();
            try {
                socket.connect(new InetSocketAddress(HOST, PORT));
                System.out.println("Client " + clientId + " connected");
            } catch (IOException e) {
                System.err.println("Client " + clientId + " connection failed: " + e.getMessage());
                thread.interrupt();
            }
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }
        System.out.println("All clients closed");
    }

    private static void handleClient(Socket socket, int clientId) {
        try {
            if (socket.isConnected()) {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                // Send message
                out.println("Hello from client " + clientId);
                // Receive response
                String response = in.readLine();
                System.out.println("Client " + clientId + " received: " + response);

                // Close socket after receiving message
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Client " + clientId + " error: " + e.getMessage());
        }
    }
}

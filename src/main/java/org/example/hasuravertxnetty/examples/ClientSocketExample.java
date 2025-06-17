package org.example.hasuravertxnetty.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientSocketExample {
    public static void main(String[] args) throws IOException, InterruptedException {
        Socket socket = new Socket("localhost", 8089); // Connect to server
        Thread.sleep(1000);
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("Received: " + in.readLine());
        out.println("Hello, server!");
        socket.close();
    }
}

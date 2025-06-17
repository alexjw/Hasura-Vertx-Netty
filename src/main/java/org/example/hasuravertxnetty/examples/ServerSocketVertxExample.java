package org.example.hasuravertxnetty.examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;

public class ServerSocketVertxExample extends AbstractVerticle {

    @Override
    public void start() {
        // Create an HTTP server
        HttpServer httpServer = vertx.createHttpServer();

        // Handle requests
        httpServer.requestHandler(request -> {
            // Send a response
            String path = request.path(); // Get the requested path

            String name = request.getParam("name");
            String message = "Hello, ";

            // Customize the response based on the parameter
            if (name != null) {
                message += name + "!";
            } else {
                message += "anonymous user!";
            }

            if ("/".equals(path)) {
                request.response()
                        .setStatusCode(200) // Redundant
                        .putHeader("content-type", "text/plain")
                        .end(message);
            }
            else if ("/hello".equals(path)) {
                request.response()
                        .putHeader("content-type", "text/plain")
                        .end(message + " from /hello!");
            }
            else if ("/goodbye".equals(path)) {
                request.response()
                        .putHeader("content-type", "text/plain")
                        .end(message + " from /goodbye!");
            } else {
                request.response()
                        .setStatusCode(404)
                        .putHeader("content-type", "text/plain")
                        .end("Page not found");
            }
        });

        // Add a TCP server for raw socket connections
        NetServerOptions options = new NetServerOptions().setPort(8089);
        NetServer tcpServer = vertx.createNetServer(options);
        tcpServer.connectHandler(socket -> {
            System.out.println("A raw socket client connected!");
            // Send a welcome message immediately
            socket.write(Buffer.buffer("Welcome to the TCP server!\n"));
            socket.handler(buffer -> {
                String message = buffer.toString();
                System.out.println("Received from client: " + message);
                socket.write(Buffer.buffer("Hello from server: " + message));
            });
            socket.closeHandler(v -> {
                System.out.println("A raw socket client disconnected!");
            });
        });

        tcpServer.listen(res -> {
            if (res.succeeded()) {
                System.out.println("TCP server is listening on port 8087");
            } else {
                System.out.println("Failed to start TCP server: " + res.cause());
            }
        });

        /*httpServer.webSocketHandler(webSocket -> {
            // This runs when a client connects
            System.out.println("A client connected!");
            webSocket.writeTextMessage("Welcome to the Vert.x WebSocket server!");

            // Close the connection when the client disconnects
            webSocket.closeHandler(v -> {
                System.out.println("A client disconnected!");
            });
        });

        // Start the server on port 8088
        httpServer.listen(8088, res -> {
            if (res.succeeded()) {
                System.out.println("Server is listening on port 8088");
            } else {
                System.out.println("Failed to start server: " + res.cause());
            }
        });*/
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();          // Create a Vert.x instance
        vertx.deployVerticle(new ServerSocketVertxExample()); // Start the verticle
    }
}


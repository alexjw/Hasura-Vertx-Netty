package org.example.hasuravertxnetty.examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;

public class HelloVerticle extends AbstractVerticle {

    @Override
    public void start() {
        // Create an HTTP server
        HttpServer server = vertx.createHttpServer();

        // Handle requests
        server.requestHandler(request -> {
            // Send a response
            request.response()
                    .putHeader("content-type", "text/plain")
                    .end("Hello, this is a Vert.x server!");
        });

        // Start the server on port 8088
        server.listen(8088, res -> {
            if (res.succeeded()) {
                System.out.println("Server is listening on port 8080");
            } else {
                System.out.println("Failed to start server: " + res.cause());
            }
        });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();          // Create a Vert.x instance
        vertx.deployVerticle(new HelloVerticle()); // Start the verticle
    }
}


package org.example.hasuravertxnetty.examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class HelloVerticle extends AbstractVerticle {

    @Override
    public void start() {
        // This runs when the verticle starts
        vertx.setPeriodic(1000, id -> {
            System.out.println("Hello from Vert.x!");
        });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();          // Create a Vert.x instance
        vertx.deployVerticle(new HelloVerticle()); // Start the verticle
    }
}


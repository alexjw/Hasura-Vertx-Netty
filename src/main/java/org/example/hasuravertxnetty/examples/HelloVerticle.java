package org.example.hasuravertxnetty.examples;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;

public class HelloVerticle extends AbstractVerticle {

    @Override
    public void start() {
        System.out.println("Started on thread: " + Thread.currentThread().getName());
        vertx.setPeriodic(1000, id -> {
            System.out.println("Task on thread: " + Thread.currentThread().getName());
        });
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();          // Create a Vert.x instance
        vertx.deployVerticle(new HelloVerticle()); // Start the verticle
    }
}


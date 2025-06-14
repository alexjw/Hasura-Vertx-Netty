package org.example.hasuravertxnetty.examples;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class MultiClientSocketNettyExample {
    private static final String HOST = "localhost";
    private static final int PORT = 8089;
    private static final int NUM_CLIENTS = 5;

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        List<ChannelFuture> futures = new ArrayList<>();

        // Generate 32 unique numbers (1-50) and add to queue, concurrentLikedQueue is thread-safe
        ConcurrentLinkedQueue<Integer> numberQueue = new ConcurrentLinkedQueue<>();
        Set<Integer> numbers = new HashSet<>();
        while (numbers.size() < NUM_CLIENTS) {
            numbers.add(ThreadLocalRandom.current().nextInt(1, 51));
        }
        numberQueue.addAll(numbers);

        try {
            Bootstrap bootstrap = new Bootstrap()
                    .group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder());
                            pipeline.addLast(new StringEncoder());
                            pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                    System.out.println("Client received: " + msg);
                                    // Close only on specific message
                                    if ("Battle ended".equalsIgnoreCase(msg.trim())) {
                                        ctx.close();
                                    }
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    String clientId = ctx.channel().id().asShortText();
                                    Integer number = numberQueue.poll();

                                    ctx.writeAndFlush("Hello from client " + clientId + ": --" + number + "--\n");
                                    System.out.println("Client " + clientId + " sent: " + number);

                                }
                            });
                        }
                    });

            // Create 32 client connections
            for (int i = 0; i < NUM_CLIENTS; i++) {
                Thread.sleep(Math.round(Math.random() * 100));
                ChannelFuture future = bootstrap.connect(HOST, PORT);
                futures.add(future);
                int finalI = i;
                future.addListener((ChannelFutureListener) f -> {
                    if (f.isSuccess()) {
                        System.out.println("Client " + finalI + " connected");
                    } else {
                        System.err.println("Client " + finalI + " connection failed: " + f.cause().getMessage());
                    }
                });
            }

            // Wait for all channels to close
            for (ChannelFuture future : futures) {
                future.channel().closeFuture().sync();
            }
            System.out.println("All clients closed");
        } finally {
            group.shutdownGracefully();
        }
    }
}

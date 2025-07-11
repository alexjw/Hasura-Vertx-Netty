package org.example.hasuravertxnetty.examples;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.example.hasuravertxnetty.controller.BattleController;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

public class MultiClientSocketNettyExample {
    private static final String NETTY = "Netty";
    private static final String VERTX = "Vert.x";
    private static final String SERVER = VERTX;
    private static final String HOST = "localhost";
    private static final int PORT = VERTX.equals(SERVER) ? 8088 : 8089;   // 8089 for Netty, 8088 for Vertx
    private static final int NUM_CLIENTS = BattleController.MATCH_SIZE;

    public static void main(String[] args) throws InterruptedException {
        EventLoopGroup group = new NioEventLoopGroup();
        List<ChannelFuture> futures = new ArrayList<>();
        System.out.println(args[0]);
        String server = args[0];
        int port = Integer.parseInt(args[1]);
        System.out.println(port);
        System.out.println("Starting " + server + " server on " + HOST + ":" + port);

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
                                }

                                @Override
                                public void channelActive(ChannelHandlerContext ctx) {
                                    String clientId = ctx.channel().id().asShortText();
                                    Integer number = numberQueue.poll();

                                    ctx.writeAndFlush("Hello from client " + clientId + ": --" + number + "--\n");
                                    //System.out.println("Client " + clientId + " sent: " + number);


                                }
                            });
                        }
                    });

            // Create 32 client connections
            for (int i = 0; i < NUM_CLIENTS; i++) {
                Thread.sleep(Math.round(Math.random() * 100));
                ChannelFuture future = bootstrap.connect(HOST, port);
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

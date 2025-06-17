package org.example.hasuravertxnetty.examples;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.IOException;

public class ServerSocketNettyExample {
    public static void main(String[] args) throws IOException, InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // Like ServerSocket
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new StringDecoder()); // Decode incoming strings
                            pipeline.addLast(new StringEncoder()); // Encode outgoing strings
                            pipeline.addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                    // Send welcome message when client connects
                                    ctx.writeAndFlush("Welcome to the Netty server!\n");
                                    System.out.println("Sent welcome message: Welcome to the Netty server!");
                                }
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String message) throws Exception {
                                    // Handle incoming message from client
                                    System.out.println("Received from client: " + message);
                                    ctx.writeAndFlush("Echo from server: " + message + "\n");
                                    System.out.println("Sent response: Echo from server: " + message);
                                }
                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    System.err.println("Error: " + cause.getMessage());
                                    ctx.close();
                                }
                            });
                        }
                    });
            bootstrap.bind(8089).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

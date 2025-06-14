package org.example.hasuravertxnetty.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.example.hasuravertxnetty.models.Battle;
import org.example.hasuravertxnetty.models.BattleParticipant;
import org.example.hasuravertxnetty.models.Player;
import org.example.hasuravertxnetty.services.BattleService;
import org.example.hasuravertxnetty.services.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@RestController
@RequestMapping("/battle")
public class BattleController {
    private static final Logger logger = LoggerFactory.getLogger(BattleController.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String hasuraEndpoint;
    private final String hasuraAdminSecret;

    private final PlayerService playerService;
    private final BattleService battleService;


    public BattleController(RestClient.Builder restClientBuilder, ObjectMapper objectMapper,
                            @Value("${hasura.endpoint}") String hasuraEndpoint,
                            @Value("${hasura.admin-secret}") String hasuraAdminSecret, PlayerService playerService, BattleService battleService) {
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
        this.hasuraEndpoint = hasuraEndpoint;
        this.hasuraAdminSecret = hasuraAdminSecret;
        this.playerService = playerService;
        this.battleService = battleService;
    }



    @PostMapping("/start")
    public String startBattle(@RequestBody Map<String, Object> input) throws InterruptedException {
        int matchSize = 32;
        logger.info("Starting battle simulation with {} players", matchSize);
        Integer id = (Integer) ((Map<String, Object>)((Map<String, Object>)((Map<String, Object>) input.get("event")).get("data")).get("new")).get("id");

        List<Player> players = new ArrayList<>();
        Set<Channel> clientChannels = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(matchSize);

        // Start Netty server
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ChannelFuture serverFuture = null;

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // Like ServerSocket
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new StringDecoder());
                            ch.pipeline().addLast(new StringEncoder());

                            ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                public void channelActive(ChannelHandlerContext context) {
                                    // Assign random player
                                    synchronized (players) {
                                        Player player = playerService.findRandomPlayerByIdRange(1, 50);
                                        if (player != null && !players.contains(player)) {
                                            player.setLookingForBattle(false);
                                            playerService.save(player);
                                            players.add(player);
                                            clientChannels.add(context.channel());
                                            //context.writeAndFlush("Hello\n"); // Match ServerSocket protocol
                                            logger.info("Player {} connected", player.getUsername());
                                        } else {
                                            logger.warn("No unique player found, closing connection");
                                            context.close();
                                            latch.countDown();
                                        }
                                    }
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                    logger.info("Received: {}", msg);
                                    latch.countDown(); // Signal player connection
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                    logger.error("Server error: {}", cause.getMessage());
                                    ctx.close();
                                    latch.countDown();
                                }
                            });
                            //ch.writeAndFlush("Hello"); // Non-blocking
                        }
                    });

            // Reference for later operations
            serverFuture = bootstrap.bind(8089).sync();

            // Wait for 32 players to connect
            latch.await();

            // Start battle
            Battle theBattle = battleService.findBattleById(id);
            theBattle.setStartTime(LocalDateTime.now());
            battleService.save(theBattle);

            for (int i = 0; i < players.size(); i++) {
                BattleParticipant battleParticipant = new BattleParticipant();
                battleParticipant.setBattle(theBattle);
                battleParticipant.setPlayer(players.get(i));
                battleParticipant.setTeam(i % 2 == 0 ? "allies" : "axis");
                battleParticipant.setScore((int) Math.round(Math.random() * 1000));
                battleService.saveBattleParticipant(battleParticipant);
            }


            logger.info("Battle simulation started");
            long duration = Math.round(Math.random() * 10000);
            Thread.sleep(duration);

            // Broadcast battle end to all clients
            for (Channel channel : clientChannels) {
                channel.writeAndFlush("Battle ended\n");
                channel.close();
            }

            players.forEach(player -> player.setLookingForBattle(true));
            playerService.saveAll(players);


            logger.info("Battle simulation completed in {} ms", duration);
            return "Battle simulation completed";

        } finally {
            if (serverFuture != null) {
                serverFuture.channel().close().sync();
            }
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }


    }
}

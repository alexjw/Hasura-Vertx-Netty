package org.example.hasuravertxnetty.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
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
    public String startBattle2(@RequestBody Map<String, Object> input) throws InterruptedException {
        int matchSize = 32;
        logger.info("Starting battle simulation with {} players", matchSize);
        Integer id = (Integer) ((Map<String, Object>)((Map<String, Object>)((Map<String, Object>) input.get("event")).get("data")).get("new")).get("id");
        List<Player> players = new ArrayList<>();

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // Like ServerSocket
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new StringEncoder());
                            System.out.println("Received: " + ch.toString());
                            ch.writeAndFlush("Hello"); // Non-blocking
                        }
                    });
            bootstrap.bind(8089).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }


        Battle theBattle = battleService.findBattleById(id);
        theBattle.setStartTime(LocalDateTime.now());

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
        players.forEach(player -> player.setLookingForBattle(true));
        playerService.saveAll(players);
        logger.info("Battle simulation completed in {} ms", duration);
        return "Battle simulation completed";
    }
    public String startBattle(@RequestBody Map<String, Object> input) throws InterruptedException {
        int matchSize = 32;
        logger.info("Starting battle simulation with {} players", matchSize);
        Integer id = (Integer) ((Map<String, Object>)((Map<String, Object>)((Map<String, Object>) input.get("event")).get("data")).get("new")).get("id");
        List<Player> players;

        synchronized(this) {
            players = playerService.fetchPlayersForBattle();

            if (players.size() < matchSize) {
                logger.warn("Not enough players: found {}", players.size());
                return "Insufficient players";
            }

            players.forEach(player -> player.setLookingForBattle(false));
            playerService.saveAll(players);
        }

        CountDownLatch latch = new CountDownLatch(players.size());



        for (Player player : players) {
            new Thread(() -> {
                EventLoopGroup group = new NioEventLoopGroup();
                try {
                    //Thread.sleep(Math.round(Math.random() * 10000));
                    Bootstrap client = new Bootstrap()
                            .group(group)
                            .channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<Channel>() {
                                @Override
                                protected void initChannel(Channel ch) {
                                    ch.pipeline().addLast(new StringEncoder(), new StringDecoder());
                                }
                            });
                    ChannelFuture cf = client.connect("localhost", 8080).sync();
                    cf.channel().writeAndFlush("Player" + Thread.currentThread().getId());
                    cf.channel().closeFuture().addListener(future -> latch.countDown());
                    logger.info("Player {} connected", player.getUsername());
                } catch (Exception e) {
                    logger.error("Client connection failed", e);
                } finally {
                    group.shutdownGracefully();
                }
            }).start();
        }

        latch.await();
        Battle theBattle = battleService.findBattleById(id);
        theBattle.setStartTime(LocalDateTime.now());

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
        players.forEach(player -> player.setLookingForBattle(true));
        playerService.saveAll(players);
        logger.info("Battle simulation completed in {} ms", duration);
        return "Battle simulation completed";
    }
}

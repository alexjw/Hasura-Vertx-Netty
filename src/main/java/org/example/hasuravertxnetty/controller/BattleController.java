package org.example.hasuravertxnetty.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.example.hasuravertxnetty.models.Player;
import org.example.hasuravertxnetty.repositories.PlayerRepository;
import org.example.hasuravertxnetty.services.BattleService;
import org.example.hasuravertxnetty.services.HasuraService;
import org.example.hasuravertxnetty.services.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

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
    public String startBattle(@RequestBody Map<String, Object> input) throws InterruptedException {
        logger.info("Starting 16v16 battle simulation");
        Integer id = (Integer) ((Map<String, Object>)((Map<String, Object>)((Map<String, Object>) input.get("event")).get("data")).get("new")).get("id");
        List<Player> players;

        synchronized(this) {
            players = playerService.fetchPlayersForBattle();

            if (players.size() < 32) {
                logger.warn("Not enough players: found {}", players.size());
                return "Insufficient players";
            }

            players.forEach(player -> player.setLookingForBattle(false));
            playerService.saveAll(players);
        }

        CountDownLatch latch = new CountDownLatch(32);



        for (int i = 1; i <= 32; i++) {
            new Thread(() -> {
                EventLoopGroup group = new NioEventLoopGroup();
                try {
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
                } catch (Exception e) {
                    logger.error("Client connection failed", e);
                } finally {
                    group.shutdownGracefully();
                }
            }).start();
        }

        latch.await();
        players.forEach(player -> player.setLookingForBattle(true));
        playerService.saveAll(players);
        logger.info("Battle simulation completed");
        return "Battle simulation completed";
    }
}

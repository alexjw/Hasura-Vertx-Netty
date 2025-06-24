package org.example.hasuravertxnetty.controller;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/battle")
public class BattleController {
    private static final Logger logger = LoggerFactory.getLogger(BattleController.class);

    private final PlayerService playerService;
    private final BattleService battleService;
    private static final Pattern PLAYER_ID_PATTERN = Pattern.compile("--(\\d+)--");
    public static final int MATCH_SIZE = 32;


    public BattleController(PlayerService playerService, BattleService battleService) {
        this.playerService = playerService;
        this.battleService = battleService;
    }



    @PostMapping("/start")
    @RateLimiter(name = "startBattleRateLimiter", fallbackMethod = "fallbackStartBattle")   // Prevents too many requests
    public synchronized String startBattle(@RequestBody Map<String, Object> input) throws InterruptedException {
        logger.info("Starting battle simulation with {} players", MATCH_SIZE);
        Integer id = (Integer) ((Map<String, Object>)((Map<String, Object>)((Map<String, Object>) input.get("event")).get("data")).get("new")).get("id");

        List<Player> players = new ArrayList<>();
        Set<Channel> clientChannels = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(MATCH_SIZE);

        // Start Netty server
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        ChannelFuture serverFuture = null;
        ConcurrentHashMap<Player, Channel> playerChannelMap = new ConcurrentHashMap<>();

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
                                    clientChannels.add(context.channel());
                                }

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, String msg) {
                                    Matcher matcher = PLAYER_ID_PATTERN.matcher(msg);
                                    logger.info("Received: {}", msg);
                                    if (matcher.find()) {
                                        // Assign random player
                                        synchronized (players) {
                                            int id = Integer.parseInt(matcher.group(1));
                                            Player player = playerService.findById(id);
                                            if (player != null && !players.contains(player)) {
                                                players.add(player);
                                                playerChannelMap.put(player, ctx.channel());
                                                logger.info("Player {} with id {} connected", player.getUsername(), player.getId());
                                                ctx.writeAndFlush("Hello" + player.getUsername() + ", you're connected, waiting for players\n"); // Match ServerSocket protocol
                                            } else {
                                                logger.warn("No unique player found, closing connection");
                                            }
                                        }
                                    }
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

            // Broadcast battle start to all clients
            for (Channel channel : clientChannels) {
                channel.writeAndFlush("Battle started\n");
                //channel.close();
            }

            logger.info("Battle simulation started");
            long duration = Math.round(Math.random() * 10000) + 5000;

            // Event Loop telling players that they are still in battle
            EventLoop eventLoop = workerGroup.next(); // Get an event loop from workerGroup
            ScheduledFuture<?> battleUpdateTask = eventLoop.scheduleAtFixedRate(() -> {
                for (Channel channel : clientChannels) {
                    channel.writeAndFlush("You're in Battle!\n");
                }
                logger.info("Sent 'You're in Battle!' to all players");
            }, 0, 1000, TimeUnit.MILLISECONDS);


            Thread.sleep(duration); // Simulate battle
            logger.info("Battle simulation completed in {} ms", duration);
            theBattle.setDuration(duration);
            battleService.save(theBattle);

            // Battle ended
            battleUpdateTask.cancel(true);

            for (int i = 0; i < players.size(); i++) {
                BattleParticipant battleParticipant = new BattleParticipant();
                battleParticipant.setBattle(theBattle);
                battleParticipant.setPlayer(players.get(i));
                battleParticipant.setTeam(i % 2 == 0 ? "allies" : "axis");
                battleParticipant.setScore((int) Math.round(Math.random() * 1000));
                battleService.saveBattleParticipant(battleParticipant);

                playerChannelMap.get(players.get(i)).writeAndFlush("Your score (" + players.get(i).getUsername() + ") is " + battleParticipant.getScore() + "\n");
            }

            logger.info("Scores sent to players");

            // Broadcast battle end to all clients
            for (Channel channel : clientChannels) {
                channel.writeAndFlush("Battle ended\n");
                channel.close();
            }
            logger.info("End of Battle sent to players");

            return "Battle simulation completed";

        } finally {
            if (serverFuture != null) {
                serverFuture.channel().close().sync();
            }
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }


    }

    // Fallback method
    private String fallbackStartBattle(Map<String, Object> input, Throwable t) {
        logger.error("Failed to start battle: {}", t.getMessage());
        return "Battle start failed due to too many requests. Please try again later.";
    }
}

package org.example.hasuravertxnetty.controller;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/battle-vertx")
public class BattleControllerVertx {

    private static final Logger logger = LoggerFactory.getLogger(BattleControllerVertx.class);
    private final PlayerService playerService;
    private final BattleService battleService;
    private final Vertx vertx;
    private static final int MATCH_SIZE = BattleController.MATCH_SIZE;
    private static final Pattern PLAYER_ID_PATTERN = Pattern.compile("--(\\d+)--");

    public BattleControllerVertx(PlayerService playerService, BattleService battleService) {
        this.playerService = playerService;
        this.battleService = battleService;
        this.vertx = Vertx.vertx(); // Initialize Vert.x
    }

    private void startVertxServer() {

    }

    @PostMapping("/start")
    public String startVertxBattle(@RequestBody Map<String, Object> input) throws InterruptedException {
        logger.info("Starting Vert.x battle simulation with {} players", MATCH_SIZE);
        Integer id = (Integer) ((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) input.get("event")).get("data")).get("new")).get("id");

        AtomicInteger connectedPlayers = new AtomicInteger(0);
        List<Player> players = new ArrayList<>();
        Set<NetSocket> clientChannels = ConcurrentHashMap.newKeySet();// Initialize CountDownLatch for 32 players
        CountDownLatch latch = new CountDownLatch(MATCH_SIZE);

        ConcurrentHashMap<Player, NetSocket> playerChannelMap = new ConcurrentHashMap<>();

        // Start the TCP server
        NetServerOptions options = new NetServerOptions().setPort(8088);
        NetServer tcpServer = vertx.createNetServer(options);
        tcpServer.connectHandler(socket -> {
            logger.info("A raw socket client connected!");
            clientChannels.add(socket);
            connectedPlayers.incrementAndGet();

            // Main Handler
            socket.handler(buffer -> {
                String message = buffer.toString();
                logger.info("Received from client: " + message);

                Matcher matcher = PLAYER_ID_PATTERN.matcher(message);
                if (matcher.find()) {
                    int playerId = Integer.parseInt(matcher.group(1));
                    Player player = playerService.findById(playerId);
                    players.add(player);
                    if (player != null && !playerChannelMap.containsKey(player)) {
                        playerChannelMap.put(player, socket);
                        logger.info("Player " + player.getUsername() + " with ID " + playerId + " connected");
                        socket.write(Buffer.buffer("Hello " + player.getUsername() + ", you're connected, waiting for players\n"));
                        logger.info("Sent welcome: Hello " + player.getUsername() + ", you're connected, waiting for players");
                    } else {
                        logger.warn("No unique player found for ID " + playerId + ", closing connection");
                        socket.close();
                    }
                }
                latch.countDown();
            });



            socket.closeHandler(v -> {
                logger.info("A raw socket client disconnected!");
                clientChannels.remove(socket);
                connectedPlayers.decrementAndGet();
                playerChannelMap.entrySet().removeIf(entry -> entry.getValue() == socket);
            });
        });

        tcpServer.listen(res -> {
            if (res.succeeded()) {
                logger.info("TCP server is listening on port 8088");
            } else {
                logger.error("Failed to start TCP server: " + res.cause());
            }
        });

        // Wait for 32 players to connect
        latch.await();

        // Start battle
        Battle theBattle = battleService.findBattleById(id);
        theBattle.setStartTime(LocalDateTime.now());

        // Broadcast battle start (initial step)
        for (NetSocket clientChannel : clientChannels) {
            clientChannel.write(Buffer.buffer("Battle started!\n"));
        }

        logger.info("Battle simulation started");
        long duration = Math.round(Math.random() * 10000) + 5000;

        long battleUpdateTaskId = vertx.setPeriodic(1000, id1 -> {
            for (NetSocket clientChannel : clientChannels) {
                clientChannel.write(Buffer.buffer("You're in Battle!\n"));
            }
            logger.info("Sent 'You're in Battle!' to all players");
        });

        Thread.sleep(duration);    // Simulate battle duration
        logger.info("Battle simulation completed in {} ms", duration);

        vertx.cancelTimer(battleUpdateTaskId);
        theBattle.setDuration(duration);
        battleService.save(theBattle);

        for (int i = 0; i < players.size(); i++) {
            BattleParticipant battleParticipant = new BattleParticipant();
            battleParticipant.setBattle(theBattle);
            battleParticipant.setPlayer(players.get(i));
            battleParticipant.setTeam(i % 2 == 0 ? "allies" : "axis");
            battleParticipant.setScore((int) Math.round(Math.random() * 1000));
            battleService.saveBattleParticipant(battleParticipant);

            playerChannelMap.get(players.get(i)).write("Your score (" + players.get(i).getUsername() + ") is " + battleParticipant.getScore() + "\n");
        }

        logger.info("Scores sent to players");

        // Broadcast battle end to all clients
        for (NetSocket clientChannel : clientChannels) {
            clientChannel.write(Buffer.buffer("Battle ended\n"));
            clientChannel.close();
        }
        logger.info("End of Battle sent to players");

        // Shut down the TCP server
        tcpServer.close(ar -> {
            if (ar.succeeded()) {
                logger.info("TCP server shut down successfully");
            } else {
                logger.error("Failed to shut down TCP server: " + ar.cause());
            }
        });

        return "Battle simulation completed";
    }
}

package org.example.hasuravertxnetty.controller;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.NetSocket;
import org.example.hasuravertxnetty.models.Battle;
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
            System.out.println("A raw socket client connected!");
            clientChannels.add(socket);
            connectedPlayers.incrementAndGet();

            // Main Handler
            socket.handler(buffer -> {
                String message = buffer.toString();
                System.out.println("Received from client: " + message);

                Matcher matcher = PLAYER_ID_PATTERN.matcher(message);
                if (matcher.find()) {
                    int playerId = Integer.parseInt(matcher.group(1));
                    Player player = playerService.findById(playerId);
                    if (player != null && !playerChannelMap.containsKey(player)) {
                        playerChannelMap.put(player, socket);
                        System.out.println("Player " + player.getUsername() + " with ID " + playerId + " connected");
                        socket.write(Buffer.buffer("Hello " + player.getUsername() + ", you're connected, waiting for players\n"));
                        System.out.println("Sent welcome: Hello " + player.getUsername() + ", you're connected, waiting for players");
                    } else {
                        System.out.println("No unique player found for ID " + playerId + ", closing connection");
                        socket.close();
                    }
                } else {
                    System.out.println("Invalid player ID format, closing connection");
                    socket.close();
                }
                latch.countDown();
            });



            socket.closeHandler(v -> {
                System.out.println("A raw socket client disconnected!");
                clientChannels.remove(socket);
                connectedPlayers.decrementAndGet();
                playerChannelMap.entrySet().removeIf(entry -> entry.getValue() == socket);
            });
        });

        tcpServer.listen(res -> {
            if (res.succeeded()) {
                System.out.println("TCP server is listening on port 8087");
            } else {
                System.out.println("Failed to start TCP server: " + res.cause());
            }
        });

        // Wait for 32 players to connect
        latch.await();

        // Start battle
        Battle theBattle = battleService.findBattleById(id);
        theBattle.setStartTime(LocalDateTime.now());
        battleService.save(theBattle);

        // Broadcast battle start (initial step)
        for (NetSocket clientChannel : clientChannels) {
            clientChannel.write(Buffer.buffer("Battle started!\n"));
        }

        logger.info("Battle simulation started");
        long duration = Math.round(Math.random() * 10000) + 5000;

        return "";
    }
}

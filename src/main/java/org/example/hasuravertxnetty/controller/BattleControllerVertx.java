package org.example.hasuravertxnetty.controller;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import jakarta.annotation.PreDestroy;
import org.example.hasuravertxnetty.models.Player;
import org.example.hasuravertxnetty.services.BattleService;
import org.example.hasuravertxnetty.services.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/battle-vertx")
public class BattleControllerVertx {

    private static final Logger logger = LoggerFactory.getLogger(BattleControllerVertx.class);
    private final PlayerService playerService;
    private final BattleService battleService;
    private final Vertx vertx;
    private HttpServer server;
    private final Set<ServerWebSocket> clientConnections = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Player, ServerWebSocket> playerToWebSocket = new ConcurrentHashMap<>();
    private final AtomicLong battleDuration = new AtomicLong(0);
    private static final int MATCH_SIZE = 32;
    private AtomicInteger connectedPlayers = new AtomicInteger(0);
    private Promise<Void> connectionPromise;

    public BattleControllerVertx(PlayerService playerService, BattleService battleService) {
        this.playerService = playerService;
        this.battleService = battleService;
        this.vertx = Vertx.vertx(); // Initialize Vert.x
        startVertxServer();
    }

    private void startVertxServer() {
        NetServerOptions options = new NetServerOptions().setPort(8088);
        NetServer tcpServer = vertx.createNetServer(options);
        tcpServer.connectHandler(socket -> {
            System.out.println("A raw socket client connected!");
            // Send a welcome message immediately
            socket.write(Buffer.buffer("Welcome to the TCP server!\n"));
            socket.handler(buffer -> {
                String message = buffer.toString();
                System.out.println("Received from client: " + message);
                socket.write(Buffer.buffer("Hello from server: " + message));
            });
            socket.closeHandler(v -> {
                System.out.println("A raw socket client disconnected!");
            });
        });

        tcpServer.listen(res -> {
            if (res.succeeded()) {
                System.out.println("TCP server is listening on port 8087");
            } else {
                System.out.println("Failed to start TCP server: " + res.cause());
            }
        });
    }

    @PostMapping("/start")
    public String startVertxBattle(@RequestBody Map<String, Object> input) {
        logger.info("Starting Vert.x battle simulation with {} players", MATCH_SIZE);
        Integer battleId = (Integer) ((Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) input.get("event")).get("data")).get("new")).get("id");

        return "";
    }

    @PreDestroy
    public void shutdown() {
        if (server != null) {
            server.close();
        }
        vertx.close();
        logger.info("Vert.x server and context shut down");
    }
}

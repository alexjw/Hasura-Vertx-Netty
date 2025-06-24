package org.example.hasuravertxnetty.services;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.example.hasuravertxnetty.models.Player;
import org.example.hasuravertxnetty.repositories.PlayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    private final PlayerRepository playerRepository;

    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    public List<Player> fetchPlayersForBattle() {
        return playerRepository.findTop32ByLookingForBattleTrue();
    }

    public void saveAll(List<Player> players) {
        playerRepository.saveAll(players);
    }

    public void save(Player player) {
        playerRepository.save(player);
    }

    @Retry(name = "databaseRetry")
    @CircuitBreaker(name = "databaseCircuitBreaker", fallbackMethod = "fallbackFindById")
    @Bulkhead(name = "databaseBulkhead")
    public Player findById(int id) {
        return playerRepository.findById(id).orElse(null);
    }

    private Player fallbackFindById(int id, Throwable t) {
        logger.error("Failed to find player {}: {}", id, t.getMessage());
        return new Player(); // Or a default Player object
    }
}

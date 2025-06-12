package org.example.hasuravertxnetty.services;

import org.example.hasuravertxnetty.models.Player;
import org.example.hasuravertxnetty.repositories.PlayerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlayerService {
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
}

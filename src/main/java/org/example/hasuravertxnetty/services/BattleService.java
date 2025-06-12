package org.example.hasuravertxnetty.services;

import org.example.hasuravertxnetty.models.Battle;
import org.example.hasuravertxnetty.models.BattleParticipant;
import org.example.hasuravertxnetty.models.Player;
import org.example.hasuravertxnetty.repositories.BattleParticipantRepository;
import org.example.hasuravertxnetty.repositories.BattleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BattleService {
    private static final Logger logger = LoggerFactory.getLogger(BattleService.class);
    private final BattleRepository battleRepository;
    private final PlayerService playerService;
    private final BattleParticipantRepository battleParticipantRepository;

    public BattleService(BattleRepository battleRepository, PlayerService playerService, BattleParticipantRepository battleParticipantRepository) {
        this.battleRepository = battleRepository;
        this.playerService = playerService;
        this.battleParticipantRepository = battleParticipantRepository;
    }

    @Transactional
    public Battle createBattle(String mode) throws Exception {
        List<Player> players = playerService.fetchPlayersForBattle();
        if (players.size() < 32) {
            logger.warn("Not enough players for battle: found {}", players.size());
            throw new IllegalStateException("Insufficient players: need 32, got " + players.size());
        }

        Battle battle = new Battle();
        battle.setMode(mode);
        battle.setStartTime(LocalDateTime.now());
        //battle.setDuration(Duration.ofMinutes(15));

        return battleRepository.save(battle);
    }

    public Battle findBattleById(Integer id) {
        return battleRepository.findById(id).orElse(null);
    }

    public void saveBattleParticipant(BattleParticipant battleParticipant) {
        battleParticipantRepository.save(battleParticipant);
    }
}

package org.example.hasuravertxnetty.repositories;

import org.example.hasuravertxnetty.models.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Integer> {
    List<Player> findTop32ByLookingForBattleTrue();
}

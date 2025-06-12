package org.example.hasuravertxnetty.repositories;

import org.example.hasuravertxnetty.models.BattleParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleParticipantRepository  extends JpaRepository<BattleParticipant, Integer> {
}

package org.example.hasuravertxnetty.repositories;

import org.example.hasuravertxnetty.models.Battle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BattleRepository extends JpaRepository<Battle, Integer> {

}

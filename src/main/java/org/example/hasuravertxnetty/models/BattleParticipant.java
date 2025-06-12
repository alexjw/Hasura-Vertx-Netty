package org.example.hasuravertxnetty.models;

import jakarta.persistence.*;

@Entity
@Table(name = "battle_participants")
public class BattleParticipant {
    public Integer getId() {
        return id;
    }

    public Battle getBattle() {
        return battle;
    }

    public Player getPlayer() {
        return player;
    }

    public String getTeam() {
        return team;
    }

    public Integer getScore() {
        return score;
    }

    public void setBattle(Battle battle) {
        this.battle = battle;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "battle_id", nullable = false)
    private Battle battle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(nullable = false)
    private String team;

    @Column(nullable = false)
    private Integer score;
}

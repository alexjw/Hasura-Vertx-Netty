package org.example.hasuravertxnetty.models;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "players")
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private Integer level;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public void setLookingForBattle(Boolean lookingForBattle) {
        this.lookingForBattle = lookingForBattle;
    }

    @Column(name = "looking_for_battle", nullable = false)
    private Boolean lookingForBattle;
}

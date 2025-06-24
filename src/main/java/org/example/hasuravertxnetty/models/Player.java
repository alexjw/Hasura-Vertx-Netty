package org.example.hasuravertxnetty.models;


import jakarta.persistence.*;

@Entity
@Table(name = "players")
public class Player {
    public Integer getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id = 0;

    @Column(nullable = false, unique = true)
    private String username = "";

    @Column(nullable = false)
    private Integer level = 0;

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

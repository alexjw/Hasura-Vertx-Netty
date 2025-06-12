package org.example.hasuravertxnetty.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "battles")
public class Battle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String mode;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    private Long duration;

    @OneToMany(mappedBy = "battle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BattleParticipant> participants = new ArrayList<>();
}

package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameFlowCount {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private int gameFlowCount;

    @Column(nullable = false)
    private int round;

    @Column(nullable = false)
    private Long roomId;

    public GameFlowCount update(int gameFlowListSize) {
        this.gameFlowCount = gameFlowListSize;
        return this;
    }
}

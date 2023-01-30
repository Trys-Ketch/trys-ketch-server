package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserPlayTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private LocalDateTime playStartTime;

    @Column
    private LocalDateTime playEndTime;

    @Column
    private Long gameRoomId;

    @OneToOne
    private GameRoomUser gameRoomUser;

    public void updateUserPlayTime(LocalDateTime playEndTime) {
        this.playEndTime = playEndTime;
    }
}

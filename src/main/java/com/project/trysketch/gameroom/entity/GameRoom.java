package com.project.trysketch.gameroom.entity;

import com.project.trysketch.global.entity.Timestamped;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

// 1. 기능   : 게임 방 엔티티
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
@Entity
@AllArgsConstructor
@Builder
public class GameRoom extends Timestamped {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private String status;
}

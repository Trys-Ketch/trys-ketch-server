package com.project.trysketch.entity;

import com.project.trysketch.global.entity.Timestamped;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

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

    @Column
    private Long hostId;

    @Column
    private String hostNick;

    @Column(nullable = false)
    private String status;

    @OneToMany(mappedBy = "gameRoom")
    @Builder.Default
    private List<GameRoomUser> gameRoomUserList = new ArrayList<>();

    public void GameRoomStatusUpdate(String status) {
        this.status = status;
    }
}

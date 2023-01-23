package com.project.trysketch.entity;

import com.project.trysketch.global.utill.Timestamped;
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

    @Column
    private String randomCode;

    @Column(nullable = false)
    @Builder.Default
    private boolean isPlaying = false;

    @OneToMany(mappedBy = "gameRoom")
    @Builder.Default
    private List<GameRoomUser> gameRoomUserList = new ArrayList<>();

    public void GameRoomStatusUpdate(boolean isPlaying) {
        this.isPlaying = isPlaying;
    }

    public void GameRoomUpdate(Long hostId, String hostNick, boolean isPlaying) {
        this.hostId = hostId;
        this.hostNick = hostNick;
        this.isPlaying = isPlaying;
    }
}

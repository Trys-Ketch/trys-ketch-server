package com.project.trysketch.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import javax.persistence.*;

// 1. 기능   : 방에 들어가있는 유저 엔티티
// 2. 작성자 : 김재영, 서혁수
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class GameRoomUser {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @JoinColumn(name = "gameroom_id")
    @ManyToOne
    private GameRoom gameRoom;      // 게임방

    @Column(nullable = false)
    private Long userId;            // 유저 ID

    @Column(nullable = false)
    private String nickname;        // 유저 닉네임

    @Column
    private String webSessionId;    // webSessionId

    @Column(nullable = false)
    private boolean readyStatus;    // 레디상태

    @Column(nullable = false)
    private String imgUrl;          // 유저 프로필사진 URL

    @OneToOne(mappedBy = "gameRoomUser", cascade = CascadeType.REMOVE)
    private UserPlayTime userPlayTime;      // 해당 유저의 플레이타임

    public void update(boolean readyStatus) {
        this.readyStatus = readyStatus;
    }

    public void update(boolean readyStatus, String webSessionId) {
        this.readyStatus = readyStatus;
        this.webSessionId = webSessionId;
    }

}

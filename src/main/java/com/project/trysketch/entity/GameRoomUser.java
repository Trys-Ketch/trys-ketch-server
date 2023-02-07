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
    private GameRoom gameRoom;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String nickname;

    @Column
    private String webSessionId;

    @Column(nullable = false)
    private boolean readyStatus;

    @Column(nullable = false)
    private String imgUrl;

    @OneToOne(mappedBy = "gameRoomUser", cascade = CascadeType.REMOVE) // 1.31
    private UserPlayTime userPlayTime;
    
    public void update(boolean readyStatus) {
        this.readyStatus = readyStatus;
    }

    public void update(boolean readyStatus, String userUUID) {
        this.readyStatus = readyStatus;
        this.webSessionId = userUUID;
    }

}

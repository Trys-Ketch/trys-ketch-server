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

//    @ManyToOne
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "nickname")
    private String nickname;

    @Column(name = "web_session_id")
    private String webSessionId;

    @Column(name = "ready_status")
    private boolean readyStatus;

    @Column
    private String imgUrl;

    public void update(boolean readyStatus) {
        this.readyStatus = readyStatus;
    }

    public void update(boolean readyStatus, String userUUID) {
        this.readyStatus = readyStatus;
        this.webSessionId = userUUID;
    }

    @Override
    public String toString() {
        return "GameRoomUser{" +
                "id=" + id +
                ", gameRoom=" + gameRoom +
                ", userId=" + userId +
                ", nickname='" + nickname + '\'' +
                ", webSessionId='" + webSessionId + '\'' +
                ", readyStatus=" + readyStatus +
                ", imgUrl='" + imgUrl + '\'' +
                '}';
    }
}

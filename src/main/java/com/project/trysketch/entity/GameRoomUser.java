package com.project.trysketch.entity;

import com.project.trysketch.entity.GameRoom;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Optional;

// 1. 기능   : 방에 들어가있는 유저 엔티티
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
public class GameRoomUser {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @JoinColumn(name = "gameroomid")
    @ManyToOne
    private GameRoom gameRoom;

//    @ManyToOne
    @Column(name = "userid")
    private Long user;

//    public GameRoomUser(GameRoom gameRoom, User user) {
//        this.gameRoom = gameRoom;
//        this.user = user;
//    }

    public GameRoomUser(GameRoom gameRoom, Long userId) {
        this.gameRoom = gameRoom;
        this.user = userId;
    }

    public GameRoomUser(Optional<GameRoom> gameRoom, Long user){
        this.gameRoom = gameRoom.get();
        this.user = user;
    }
}

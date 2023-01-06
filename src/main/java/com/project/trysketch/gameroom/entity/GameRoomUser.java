package com.project.trysketch.gameroom.entity;

import com.project.trysketch.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Optional;

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

    @JoinColumn(name = "userid")
    @ManyToOne
    private User user;

    public GameRoomUser(GameRoom gameRoom, User user) {
        this.gameRoom = gameRoom;
        this.user = user;
    }

    public GameRoomUser(Optional<GameRoom> gameRoom, User user){
        this.gameRoom = gameRoom.get();
        this.user = user;
    }
}

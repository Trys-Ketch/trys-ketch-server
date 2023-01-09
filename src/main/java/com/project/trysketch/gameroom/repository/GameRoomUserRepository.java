package com.project.trysketch.gameroom.repository;

import com.project.trysketch.gameroom.entity.GameRoom;
import com.project.trysketch.gameroom.entity.GameRoomUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 1. 기능   : 게임 방에 입장한 유저 repository
// 2. 작성자 : 김재영
public interface GameRoomUserRepository extends JpaRepository<GameRoomUser, Long> {
    List<GameRoomUser> findByGameRoom(GameRoom gameRoom);
    List<GameRoomUser> findByGameRoom(Optional<GameRoom> gameRoom);
//    GameRoomUser findByUser(User user);

    boolean existsByGameRoom_IdAndUser(Long gameRoom, Long userId);

    Long countByGameRoom_IdOrderByUser(Long gameRoom);
}
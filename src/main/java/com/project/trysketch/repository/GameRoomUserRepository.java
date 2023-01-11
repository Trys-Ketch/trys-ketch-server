package com.project.trysketch.repository;

import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 1. 기능   : 게임 방에 입장한 유저 repository
// 2. 작성자 : 김재영
public interface GameRoomUserRepository extends JpaRepository<GameRoomUser, Long> {
    List<GameRoomUser> findByGameRoom(GameRoom gameRoom);

    GameRoom findByGameRoomId(Long gameRoomId);

    GameRoomUser findByUserId(Long userId);

    Long countByGameRoomIdOrderByUserId(Long gameRoomId);

    boolean existsByUserId(Long userId);

    GameRoomUser findByUserIdAndGameRoomId(Long id, Long gameRoomId);

    List<GameRoomUser> findAllByGameRoomId(Long roomId);

    GameRoomUser findByWebsessionId(String userUUID);

}
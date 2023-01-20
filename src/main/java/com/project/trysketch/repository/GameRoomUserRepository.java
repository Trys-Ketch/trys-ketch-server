package com.project.trysketch.repository;

import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

// 1. 기능   : 게임 방에 입장한 유저 repository
// 2. 작성자 : 김재영
public interface GameRoomUserRepository extends JpaRepository<GameRoomUser, Long> {
    List<GameRoomUser> findByGameRoom(GameRoom gameRoom);

    List<GameRoomUser> findAllByGameRoom(GameRoom gameRoom);

    GameRoomUser findByUserId(Long userId);

    Long countByGameRoomIdOrderByUserId(Long gameRoomId);

    boolean existsByUserId(Long userId);

    GameRoomUser findByUserIdAndGameRoomId(Long id, Long gameRoomId);

    List<GameRoomUser> findAllByGameRoomId(Long roomId);

    Optional<GameRoomUser> findByWebSessionId(String userUUID);

    boolean existsByGameRoomIdAndUserId(Long roomId, Long userId);

    GameRoomUser findByGameRoomIdAndWebSessionId(Long gameRoomId, String userUUID);

    void deleteByWebSessionId(String userUUID);

}
package com.project.trysketch.gameroom.repository;

import com.project.trysketch.gameroom.entity.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;

// 1. 기능   : 게임 방 repository
// 2. 작성자 : 김재영
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
}

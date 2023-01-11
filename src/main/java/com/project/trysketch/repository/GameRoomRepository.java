package com.project.trysketch.repository;

import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 1. 기능   : 게임 방 repository
// 2. 작성자 : 김재영
public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
}

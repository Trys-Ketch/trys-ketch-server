package com.project.trysketch.gameroom.repository;

import com.project.trysketch.gameroom.entity.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {
}

package com.project.trysketch.repository;

import com.project.trysketch.entity.GameFlowCount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameFlowCountRespository extends JpaRepository<GameFlowCount, Long> {
    GameFlowCount findByRoomIdAndRound(Long roomId, int round);

    void deleteAllByRoomId(Long id);
}

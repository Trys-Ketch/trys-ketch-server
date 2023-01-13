package com.project.trysketch.repository;

import com.project.trysketch.entity.GameFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GameFlowRepository extends JpaRepository<GameFlow, Long> {
    Optional<GameFlow> findByRoomIdAndRoundAndKeywordIndex(Long roomId, int round, int keywordIndex);

    void deleteAllByRoomId(Long roomId);
}

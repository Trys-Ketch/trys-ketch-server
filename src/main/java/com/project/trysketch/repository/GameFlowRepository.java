package com.project.trysketch.repository;

import com.project.trysketch.entity.GameFlow;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// 1. 기능   : 게임 진행 repository
// 2. 작성자  : 김재영, 서혁수, 안은솔, 황미경
public interface GameFlowRepository extends JpaRepository<GameFlow, Long> {
    Optional<GameFlow> findByRoomIdAndRoundAndKeywordIndex(Long roomId, int round, int keywordIndex);

    void deleteAllByRoomId(Long roomId);

    boolean existsByRoomIdAndRoundAndWebSessionId(Long roomId, int round, String webSessionId);

    GameFlow findByRoomIdAndRoundAndWebSessionId(Long roomId, int round, String webSessionId);

    List<GameFlow> findAllByWebSessionIdAndRoomId(String webSessionId, Long roomId);
}

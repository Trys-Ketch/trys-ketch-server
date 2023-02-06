package com.project.trysketch.repository;

import com.project.trysketch.entity.GameFlowCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;

public interface GameFlowCountRepository extends JpaRepository<GameFlowCount, Long> {

    // 비관적 잠금(Pessimistic Lock)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GameFlowCount g where g.roomId = :roomId and g.round = :round")
    GameFlowCount findByRoomIdAndRoundForUpdate(@Param("roomId") Long roomId, @Param("round") int round);

    void deleteAllByRoomId(Long id);
}

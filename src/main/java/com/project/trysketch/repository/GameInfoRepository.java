package com.project.trysketch.repository;

import com.project.trysketch.entity.GameInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameInfoRepository extends JpaRepository<GameInfo, Long> {
}

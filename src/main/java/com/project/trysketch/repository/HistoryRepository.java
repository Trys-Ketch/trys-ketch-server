package com.project.trysketch.repository;

import com.project.trysketch.entity.History;
import com.project.trysketch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// 1. 기능   : 게임 달성업적 Repository
// 2. 작성자  : 김재영
public interface HistoryRepository extends JpaRepository<History, Integer> {
    Optional<History> findByUser(User user);
}

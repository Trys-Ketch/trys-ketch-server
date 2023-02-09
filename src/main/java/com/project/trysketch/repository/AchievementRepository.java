package com.project.trysketch.repository;

import com.project.trysketch.entity.Achievement;
import com.project.trysketch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// 1. 기능   : 유저 업적 repository
// 2. 작성자  : 김재영
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findAllByUser(User user);
}

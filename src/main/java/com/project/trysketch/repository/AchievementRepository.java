package com.project.trysketch.repository;

import com.project.trysketch.entity.Achievement;
import com.project.trysketch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    List<Achievement> findAllByUser(User user);
}

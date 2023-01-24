package com.project.trysketch.repository;

import com.project.trysketch.entity.Achievment;
import com.project.trysketch.entity.History;
import com.project.trysketch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AchievmentRepository extends JpaRepository<Achievment, Long> {

    List<Achievment> findAllByUser(User user);
}

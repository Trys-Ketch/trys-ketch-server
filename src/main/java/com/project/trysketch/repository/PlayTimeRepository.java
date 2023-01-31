package com.project.trysketch.repository;

import com.project.trysketch.entity.UserPlayTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlayTimeRepository extends JpaRepository<UserPlayTime, Integer> {

    List<UserPlayTime> findAllByGameRoomId(Long gameRoomId);
}

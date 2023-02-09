package com.project.trysketch.repository;

import com.project.trysketch.entity.UserPlayTime;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// 1. 기능   : playtime Repository
// 2. 작성자  : 김재영
public interface PlayTimeRepository extends JpaRepository<UserPlayTime, Integer> {
    List<UserPlayTime> findAllByGameRoomId(Long gameRoomId);
}

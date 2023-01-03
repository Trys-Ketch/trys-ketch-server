package com.project.trysketch.user.repository;

import com.project.trysketch.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// 1. 기능    : 유저 Repository
// 2. 작성자  : 서혁수, 황미경
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
    Optional<User> findByKakaoId(Long kakaoId);
}

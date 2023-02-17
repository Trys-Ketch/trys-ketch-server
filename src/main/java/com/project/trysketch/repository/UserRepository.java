package com.project.trysketch.repository;

import com.project.trysketch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// 1. 기능    : 유저 Repository
// 2. 작성자  : 서혁수, 황미경, 김재영
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByKakaoId(Long kakaoId);

    Optional<User> findByNaverId(String naverId);

    Optional<User> findByGoogleId(Long googleId);
}

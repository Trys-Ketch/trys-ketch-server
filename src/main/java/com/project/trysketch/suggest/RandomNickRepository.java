package com.project.trysketch.suggest;

import com.project.trysketch.suggest.RandomNick;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// 1. 기능   : 랜덤 닉네임 Repository
// 2. 작성자 : 서혁수
public interface RandomNickRepository extends JpaRepository<RandomNick, Integer> {
    Optional<RandomNick> findByNum(int num);
}

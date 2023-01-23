package com.project.trysketch.repository;

import com.project.trysketch.entity.Adjective;
import org.springframework.data.jpa.repository.JpaRepository;

// 1. 기능   : 형용사 Repository
// 2. 작성자  : 서혁수
public interface AdjectiveRepository extends JpaRepository<Adjective, Integer> {
}

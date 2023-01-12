package com.project.trysketch.suggest;

import org.springframework.data.jpa.repository.JpaRepository;

// 1. 기능   : 형용사 Repository
// 2. 작성자  : 서혁수
public interface AdjectiveRepository extends JpaRepository<AdjectiveEntity, Integer> {
}

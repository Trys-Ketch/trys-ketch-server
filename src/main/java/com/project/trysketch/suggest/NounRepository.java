package com.project.trysketch.suggest;

import org.springframework.data.jpa.repository.JpaRepository;

// 1. 기능   : 명사 Repository
// 2. 작성자 : 서혁수
public interface NounRepository extends JpaRepository<NounEntity, Integer> {
}

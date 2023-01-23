package com.project.trysketch.repository;

import com.project.trysketch.entity.Noun;
import org.springframework.data.jpa.repository.JpaRepository;

// 1. 기능   : 명사 Repository
// 2. 작성자 : 서혁수
public interface NounRepository extends JpaRepository<Noun, Integer> {
}

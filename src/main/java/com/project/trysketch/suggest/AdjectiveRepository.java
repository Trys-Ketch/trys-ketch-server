package com.project.trysketch.suggest;

import org.springframework.data.jpa.repository.JpaRepository;

// 1. 기능   : 형용사 Repository
public interface AdjectiveRepository extends JpaRepository<AdjectiveEntity, Long> {
}

package com.project.trysketch.repository;

import com.project.trysketch.entity.ThumbImg;
import org.springframework.data.jpa.repository.JpaRepository;

// 1. 기능   : 프로필 이미지 repository
// 2. 작성자  : 서혁수
public interface ThumbImgRepository extends JpaRepository<ThumbImg, Integer> {
}

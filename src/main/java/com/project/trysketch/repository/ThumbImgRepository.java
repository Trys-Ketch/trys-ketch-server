package com.project.trysketch.repository;

import com.project.trysketch.entity.ThumbImg;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ThumbImgRepository extends JpaRepository<ThumbImg, Integer> {
}

package com.project.trysketch.repository;

import com.project.trysketch.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

// 1. 기능   : ImageFile Repository
// 2. 작성자 : 황미경
public interface ImageRepository extends JpaRepository<Image, Long> {

}

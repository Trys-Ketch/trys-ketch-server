package com.project.trysketch.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageFileRepository extends JpaRepository<ImageFile, Long> {
    List<ImageFile> findByUserId(Long userId);

}

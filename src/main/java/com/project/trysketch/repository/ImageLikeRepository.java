package com.project.trysketch.repository;

import com.project.trysketch.entity.ImageLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// 1. 기능   : ImageRepository
// 2. 작성자 : 황미경, 김재영
public interface ImageLikeRepository extends JpaRepository<ImageLike, Long> {
    Optional<ImageLike> findByImageIdAndUserId(Long imageId, Long userId);

    Page<ImageLike> findAllByUserId(Long userId, Pageable pageable);
}

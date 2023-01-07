package com.project.trysketch.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageLikeRepository extends JpaRepository<ImageLike, Long> {

    Optional<ImageLike> findByImageIdAndUserId(Long imageId, Long userId);

    List<ImageLike> findAllByUserId(Long userId);
}

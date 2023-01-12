package com.project.trysketch.image;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// 1. 기능   : ImageRepository
// 2. 작성자 : 황미경
public interface ImageLikeRepository extends JpaRepository<ImageLike, Long> {

    Optional<ImageLike> findByImageIdAndUserId(Long imageId, Long userId);

    List<ImageLike> findAllByUserId(Long userId);

    void deleteByImageIdAndUserId(Long imageId, Long userId);

}

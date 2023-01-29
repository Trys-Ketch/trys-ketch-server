package com.project.trysketch.repository;

import com.project.trysketch.entity.ImageLike;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

// 1. 기능   : ImageRepository
// 2. 작성자 : 황미경
public interface ImageLikeRepository extends JpaRepository<ImageLike, Long> {

    Optional<ImageLike> findByImageIdAndUserId(Long imageId, Long userId);

    List<ImageLike> findAllByUserId(Long userId);

    Page<ImageLike> findAllByUserId(Long userId, Pageable pageable); // 수정 추가 김재영 01.29

    void deleteByImageIdAndUserId(Long imageId, Long userId);

    void deleteByImageLike(ImageLike imageLike);

}

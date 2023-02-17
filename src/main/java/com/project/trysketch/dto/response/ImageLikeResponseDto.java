package com.project.trysketch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

// 1. 기능   : 이미지 좋아요 반환 DTO
// 2. 작성자 : 황미경
@AllArgsConstructor
@Builder
@Getter
public class ImageLikeResponseDto {

    private Long imgId;              // image PK
    private String imgPath;          // image 경로
    private String painter;          // 그린사람
    private LocalDateTime createdAt; // 생성 시간
}

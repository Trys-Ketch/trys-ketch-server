package com.project.trysketch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Getter
public class ImageLikeResponseDto {

    private Long imgId; // image PK
    private String imgPath; // image 경로
    private String painter; // 그린사람
    private LocalDateTime createdAt; // 생성 시간
}

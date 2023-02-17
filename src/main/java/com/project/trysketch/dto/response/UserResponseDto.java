package com.project.trysketch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

// 1. 기능   : 회원의 정보를 반환하는 DTO
// 2. 작성자 : 안은솔
@Getter
@AllArgsConstructor
@Builder
public class UserResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private String imagePath;
}

package com.project.trysketch.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 1. 기능   : 네이버 응답 Dto
// 2. 작성자 : 김재영
@Getter
@AllArgsConstructor
public class NaverRequestDto {

    private String id;
    private String email;
    private String nickname;
}

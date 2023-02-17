package com.project.trysketch.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 1. 기능   : 회원 업적 관련
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class AchievementResponseDto {

    private String achievementName;     // 업적의 이름

}

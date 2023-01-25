package com.project.trysketch.global.utill;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AchievementCode {

    /* 플레이타임 업적 코드 */
    PLAYTIME_TROPHY_BRONZE("플레이타임 브론즈"),
    PLAYTIME_TROPHY_SILVER("플레이타임 실버"),
    PLAYTIME_TROPHY_GOLD("플레이타임 골드"),

    /* 플레이 횟수 업적 코드 */
    TRIAL_TROPHY_BRONZE("게임 플레이 횟수 브론즈"),
    TRIAL_TROPHY_SILVER("게임 플레이 횟수 실버"),
    TRIAL_TROPHY_GOLD("게임 플레이 횟수 골드"),

    /* 사이트 로그인 횟수 업적 코드 */
    VISIT_TROPHY_BRONZE("사이트 방문 횟수 브론즈"),
    VISIT_TROPHY_SILVER("사이트 방문 횟수 실버"),
    VISIT_TROPHY_GOLD("사이트 방문 횟수 골드");


    private final String AchievmentName;
}

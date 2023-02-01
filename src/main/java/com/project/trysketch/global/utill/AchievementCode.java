package com.project.trysketch.global.utill;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AchievementCode {

    /* 플레이타임 업적 코드 */
    PLAYTIME_TROPHY_BRONZE("TSK001"),
    PLAYTIME_TROPHY_SILVER("TSK002"),
    PLAYTIME_TROPHY_GOLD("TSK003"),

    /* 플레이 횟수 업적 코드 */
    TRIAL_TROPHY_BRONZE("TSK021"),
    TRIAL_TROPHY_SILVER("TSK022"),
    TRIAL_TROPHY_GOLD("TSK023"),

    /* 사이트 로그인 횟수 업적 코드 */
    VISIT_TROPHY_BRONZE("TSK041"),
    VISIT_TROPHY_SILVER("TSK042"),
    VISIT_TROPHY_GOLD("TSK043");


    private final String AchievementName;
}

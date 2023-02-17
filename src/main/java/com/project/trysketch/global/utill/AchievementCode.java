package com.project.trysketch.global.utill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AchievementCode {

    /* 플레이타임 업적 코드 */
    PLAYTIME_TROPHY_BRONZE("TSK_P_B"),
    PLAYTIME_TROPHY_SILVER("TSK_P_S"),
    PLAYTIME_TROPHY_GOLD("TSK_P_G"),

    /* 플레이 횟수 업적 코드 */
    TRIAL_TROPHY_BRONZE("TSK_T_B"),
    TRIAL_TROPHY_SILVER("TSK_T_S"),
    TRIAL_TROPHY_GOLD("TSK_T_G"),

    /* 사이트 로그인 횟수 업적 코드 */
    VISIT_TROPHY_BRONZE("TSK_V_B"),
    VISIT_TROPHY_SILVER("TSK_V_S"),
    VISIT_TROPHY_GOLD("TSK_V_G");
    private final String AchievementName;
}

package com.project.trysketch.service;

import com.project.trysketch.entity.Achievment;
import com.project.trysketch.entity.History;
import com.project.trysketch.entity.User;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.utill.AchievmentCode;
import com.project.trysketch.repository.AchievmentRepository;
import com.project.trysketch.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final AchievmentRepository achievmentRepository;

    // 게임 플레이 시간에 따른 업적
    public void getTrophyOfTime(User user) {

        // 해당 유저의 활동이력 검색
        History history = historyRepository.findByUser(user).orElseThrow(
                () -> new CustomException(StatusMsgCode.HISTORY_NOT_FOUND)
        );
        // 해당 유저가 획득한 업적
        List<Achievment> achievmentList = achievmentRepository.findAllByUser(user);

        Map<Integer, Achievment> achievements = new HashMap<>();
        achievements.put(5, new Achievment(AchievmentCode.PLAYTIME_TROPHY_BRONZE, user));
        achievements.put(15, new Achievment(AchievmentCode.PLAYTIME_TROPHY_SILVER, user));
        achievements.put(60, new Achievment(AchievmentCode.PLAYTIME_TROPHY_GOLD, user));

        // 유저로 찾아온 history 의 playtime 을 가져옴
        Long playtime = history.getPlaytime();

        for (Integer baseLine : achievements.keySet()) {

            // 유저가 지금 얻으려는 업적을 가직 있는지 검증하고 없다면 저장
            verifyUserAchievment(achievements, achievmentList, playtime, baseLine);
        }
    }

    // 게임 판수에 따른 업적
    public void getTrophyOfTrial(User user) {

        // 해당 유저의 활동이력 검색
        History history = historyRepository.findByUser(user).orElseThrow(
                () -> new CustomException(StatusMsgCode.HISTORY_NOT_FOUND)
        );
        // 해당 유저가 획득한 업적
        List<Achievment> achievmentList = achievmentRepository.findAllByUser(user);

        Map<Integer, Achievment> achievements = new HashMap<>();
        achievements.put(1, new Achievment(AchievmentCode.TRIAL_TROPHY_BRONZE, user));
        achievements.put(5, new Achievment(AchievmentCode.TRIAL_TROPHY_SILVER, user));
        achievements.put(100, new Achievment(AchievmentCode.TRIAL_TROPHY_GOLD, user));

        // 유저로 찾아온 history 의 playtime 을 가져옴
        Long trials = history.getTrials();

        for (Integer baseLine : achievements.keySet()) {

            // 유저가 지금 얻으려는 업적을 가직 있는지 검증하고 없다면 저장
            verifyUserAchievment(achievements, achievmentList, trials, baseLine);
        }
    }

    // 사이트 로그인 횟수에 따른 업적
    public void getTrophyOfVisit(User user) {

        // 해당 유저의 활동이력 검색
        History history = historyRepository.findByUser(user).orElseThrow(
                () -> new CustomException(StatusMsgCode.HISTORY_NOT_FOUND)
        );
        // 해당 유저가 획득한 업적
        List<Achievment> achievmentList = achievmentRepository.findAllByUser(user);

        Map<Integer, Achievment> achievements = new HashMap<>();
        achievements.put(1, new Achievment(AchievmentCode.VISIT_TROPHY_BRONZE, user));
        achievements.put(5, new Achievment(AchievmentCode.VISIT_TROPHY_SILVER, user));
        achievements.put(100, new Achievment(AchievmentCode.VISIT_TROPHY_GOLD, user));

        // 유저로 찾아온 history 의 playtime 을 가져옴
        Long visits = history.getPlaytime();

        for (Integer baseLine : achievements.keySet()) {

            // 유저가 지금 얻으려는 업적을 가직 있는지 검증하고 없다면 저장
            verifyUserAchievment(achievements, achievmentList, visits, baseLine);
        }
    }

    public void verifyUserAchievment(Map<Integer, Achievment> achievements, List<Achievment> achievmentList, Long count, Integer baseLine){

        // 유저 의 playtime 이 기준선을 넘는다면
        if (count > baseLine) {
            Achievment achievment = achievements.get(baseLine);

            // 해당 유저가 현재 가지고 있었던 업적 가져오기
            for (Achievment currentachievment : achievmentList) {

                // 지금 얻으려는 업적과 같다면
                if (achievment.equals(currentachievment)) {
                    continue;
                }
                achievmentRepository.save(achievment);
            }
        }
    }
}
package com.project.trysketch.service;

import com.project.trysketch.entity.Achievement;
import com.project.trysketch.entity.GameRoomUser;
import com.project.trysketch.entity.History;
import com.project.trysketch.entity.User;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.utill.AchievementCode;
import com.project.trysketch.repository.AchievementRepository;
import com.project.trysketch.repository.GameRoomUserRepository;
import com.project.trysketch.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.*;

// 1. 기능   : 유저 활동 내역
// 2. 작성자 : 김재영
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final AchievementRepository achievementRepository;
    private final SimpMessageSendingOperations sendingOperations;
    private final GameRoomUserRepository gameRoomUserRepository;

    public History createHistory() {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [HistoryService - createHistory] >>>>>>>>>>>>>>>>>>>>>>>>");
        History history = History.builder()
                .playtime(0L)
                .trials(0L)
                .visits(0L)
                .user(null)
                .build();
        return historyRepository.saveAndFlush(history);
    }

    // 게임 플레이 시간에 따른 업적
    public List<String> getTrophyOfTime(User user) {

        // 해당 유저의 활동이력 검색
        History history = historyRepository.findByUser(user).orElseThrow(
                () -> new CustomException(StatusMsgCode.HISTORY_NOT_FOUND)
        );
        // 해당 유저가 획득한 업적
        List<Achievement> achievementList = achievementRepository.findAllByUser(user);

        Map<Integer, Achievement> achievements = new HashMap<>();
        achievements.put(10, new Achievement(AchievementCode.PLAYTIME_TROPHY_BRONZE, user));
        achievements.put(20, new Achievement(AchievementCode.PLAYTIME_TROPHY_SILVER, user));
        achievements.put(30, new Achievement(AchievementCode.PLAYTIME_TROPHY_GOLD, user));

        // 유저로 찾아온 history 의 playtime 을 가져옴
        Long playtime = history.getPlaytime();

        // 반환용 List
        List<String> responseList = new ArrayList<>();

        for (Integer baseLine : achievements.keySet()) {

            // 유저가 지금 얻으려는 업적을 가직 있는지 검증하고 없다면 저장
            String newAchievement = verifyUserAchievement(achievements, achievementList, playtime, baseLine, user.getId());
            if (newAchievement != null) {
                responseList.add(newAchievement);
            }
        }
        return responseList;
    }

    // 게임 판수에 따른 업적
    public List<String> getTrophyOfTrial(User user) {

        // 해당 유저의 활동이력 검색
        History history = historyRepository.findByUser(user).orElseThrow(
                () -> new CustomException(StatusMsgCode.HISTORY_NOT_FOUND)
        );
        // 해당 유저가 획득한 업적
        List<Achievement> achievementList = achievementRepository.findAllByUser(user);

        Map<Integer, Achievement> achievements = new HashMap<>();
        achievements.put(1, new Achievement(AchievementCode.TRIAL_TROPHY_BRONZE, user));
        achievements.put(5, new Achievement(AchievementCode.TRIAL_TROPHY_SILVER, user));
        achievements.put(10, new Achievement(AchievementCode.TRIAL_TROPHY_GOLD, user));

        // 유저로 찾아온 history 의 playtime 을 가져옴
        Long trials = history.getTrials();

        // 반환용 List
        List<String> responseList = new ArrayList<>();

        for (Integer baseLine : achievements.keySet()) {

            // 유저가 지금 얻으려는 업적을 가직 있는지 검증하고 없다면 저장
            String newAchievement = verifyUserAchievement(achievements, achievementList, trials, baseLine, user.getId());
            if (newAchievement != null) {
                responseList.add(newAchievement);
            }
        }
        return responseList;
    }

    // 사이트 로그인 횟수에 따른 업적
    public List<String> getTrophyOfVisit(User user) {
        log.info(">>>>>>>>>>>>>>>>> [HistoryService] - getTrophyOfVisit");
        // 해당 유저의 활동이력 검색
        History history = historyRepository.findByUser(user).orElseThrow(
                () -> new CustomException(StatusMsgCode.HISTORY_NOT_FOUND)
        );
        // 해당 유저가 획득한 업적
        List<Achievement> achievementList = achievementRepository.findAllByUser(user);


        Map<Integer, Achievement> achievements = new HashMap<>();
        achievements.put(1, new Achievement(AchievementCode.VISIT_TROPHY_BRONZE, user));
        achievements.put(5, new Achievement(AchievementCode.VISIT_TROPHY_SILVER, user));
        achievements.put(10, new Achievement(AchievementCode.VISIT_TROPHY_GOLD, user));

        // 유저로 찾아온 history 의 playtime 을 가져옴
        Long visits = history.getVisits();

        // 반환용 리스트
        List<String> responseList = new ArrayList<>();

        for (Integer baseLine : achievements.keySet()) {
            // 유저가 지금 얻으려는 업적을 가직 있는지 검증하고 없다면 저장

            String newAchievement = verifyUserLoginAchievement(achievements, achievementList, visits, baseLine);
            if (newAchievement != null) {
                responseList.add(newAchievement);
            }
        }
        return responseList;
    }

    public String verifyUserAchievement(Map<Integer, Achievement> achievements, List<Achievement> currentAchievementList, Long count, Integer baseLine, Long userId) {
        log.info(">>>>>>>>>>>>>>>>> [HistoryService] - verifyUserAchievement");

        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserId(userId);

        // 유저 의 count 가  baseLine 이 기준선을 넘는다면
        int cnt = 0;
        if (count > baseLine) {
            Achievement achievement = achievements.get(baseLine);



            // 해당 유저가 현재 가지고 있었던 업적 가져오기
            if (currentAchievementList.isEmpty()) {
                achievementRepository.save(achievement);
                log.info(">>>>>>>>>>>>>>>>> achievement 처음 만들었다");
            } else {

                for (Achievement currentAchievement : currentAchievementList) {

                    // 지금 얻으려는 업적과 같다면
                    if (achievement.getName().equals(currentAchievement.getName())) {
                        cnt++;
                    }
                }
                if (cnt == 0) {
                    achievementRepository.save(achievement);

//                    Map<String, String> message = new HashMap<>();
//                    message.put("achievement", achievement.getName());
//                    // 이미지 패스 추가
//                    sendingOperations.convertAndSend("/queue/game/achievement/" + gameRoomUser.getWebSessionId(), message);
                    log.info(">>>>>>>>>>>>>>>>> achievement 또 하나 만들었다");

                    return achievement.getName();
                }
            }
        }
        return null;
    }


    public String verifyUserLoginAchievement(Map<Integer, Achievement> achievements, List<Achievement> currentAchievementList, Long count, Integer baseLine) {
        log.info(">>>>>>>>>>>>>>>>> [HistoryService] - verifyUserAchievement");

        // 유저 의 count 가  baseLine 이 기준선을 넘는다면
        int cnt = 0;
        if (count > baseLine) {
            Achievement achievement = achievements.get(baseLine);

            // 해당 유저가 현재 가지고 있었던 업적 가져오기
            if (currentAchievementList.isEmpty()) {
                achievementRepository.save(achievement);
                log.info(">>>>>>>>>>>>>>>>> achievement 처음 만들었다");

                return achievement.getName();
            } else {

                for (Achievement currentAchievement : currentAchievementList) {
                    // 지금 얻으려는 업적과 같다면
                    if (achievement.getName().equals(currentAchievement.getName())) {
                        log.info(">>>>>>>>>>>>>>>>> 얻을려는 achievement : {}", achievement.getName());
                        log.info(">>>>>>>>>>>>>>>>> 가지고있는 achievement : {}", currentAchievement.getName());
                        cnt++;
                    }
                }
                if (cnt == 0) {
                    achievementRepository.save(achievement);
                    log.info(">>>>>>>>>>>>>>>>> achievement 또 하나 만들었다");

                    return achievement.getName();

                }
            }
        }
        return null;
    }


}
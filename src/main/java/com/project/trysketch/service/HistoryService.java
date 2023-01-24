package com.project.trysketch.service;

import com.project.trysketch.entity.Achievment;
import com.project.trysketch.entity.History;
import com.project.trysketch.entity.User;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.repository.AchievmentRepository;
import com.project.trysketch.repository.HistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final AchievmentRepository achievmentRepository;

    public void getAchievment(User user) {
        History history = historyRepository.findByUser(user).orElseThrow(
                () -> new CustomException(StatusMsgCode.HISTORY_NOT_FOUND)
        );
        Map<Integer, Achievment> achievements = new HashMap<>();
        achievements.put(5, new Achievment("뉴비", user));
        achievements.put(30, new Achievment("중견", user));
        achievements.put(60, new Achievment("고인물", user));

        // 유저로 찾아온 history 의 playtime 을 가져옴
        Long playtime = history.getPlaytime();

        for (Integer baseLine : achievements.keySet()) {

            // User 의 playtime 이 기준선을 넘는다면
            if (playtime > baseLine) {
                Achievment achievment = achievements.get(baseLine);
                achievmentRepository.save(achievment);
            }
        }

    }


}

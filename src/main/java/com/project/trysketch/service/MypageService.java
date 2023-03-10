package com.project.trysketch.service;

import com.project.trysketch.dto.request.UserRequestDto;
import com.project.trysketch.dto.response.UserResponseDto;
import com.project.trysketch.entity.Achievement;
import com.project.trysketch.entity.User;
import com.project.trysketch.dto.response.DataMsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.repository.AchievementRepository;
import com.project.trysketch.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 1. 기능   : 내 정보 페이지 서비스
// 2. 작성자 : 김재영, 황미경
@Service
@RequiredArgsConstructor
@Slf4j
public class MypageService {

    private final UserRepository userRepository;
    private final AchievementRepository achievementRepository;
    private final JwtUtil jwtUtil;

    // 마이페이지 회원조회
    public DataMsgResponseDto getMyPage(HttpServletRequest request) {

        // 유저 정보 가져오기
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByEmail(claims.get("email").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // 유저 정보에서 필요한 정보( id, email, nickname, ImgUrl ) 추출
        UserResponseDto userResponseDto = UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .imagePath(user.getImgUrl())
                .build();

        return new DataMsgResponseDto(StatusMsgCode.OK,userResponseDto);
    }

    // 마이페이지 회원 닉네임, 프로필사진 수정
    @Transactional
    public UserResponseDto patchMyPage(UserRequestDto userRequestDto, HttpServletRequest request) {

        // 유저 정보 가져오기
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByEmail(claims.get("email").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // 유저 프로필, 닉네임 변경
        user.update(userRequestDto.getNickname(), userRequestDto.getImgUrl());

        // 유저 정보에서 필요한 정보( id, email, nickname, ImgUrl ) 추출
        UserResponseDto userResponseDto = UserResponseDto.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .imagePath(user.getImgUrl())
                .build();

        return userResponseDto;
    }

    // 뱃지 조회하기
    public Map<String, List<String>> getBadge(HttpServletRequest request) {

        // 유저 정보 가져오기
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByEmail(claims.get("email").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // 해당 유저의 업적 list 가져오기
        List<Achievement> achievementList = achievementRepository.findAllByUser(user);

        Map<String, List<String>> responseMap = new HashMap<>();

        List<String> playtimeList = new ArrayList<>();
        List<String> trialList = new ArrayList<>();
        List<String> visitList = new ArrayList<>();

        for (Achievement achievement : achievementList){
            switch (achievement.getName().split("_")[1]) {
                case "P" -> playtimeList.add(achievement.getName());     // playtime
                case "T" -> trialList.add(achievement.getName());        // trial
                case "V" -> visitList.add(achievement.getName());        // visit
            }
        }

        responseMap.put("playtime", playtimeList);
        responseMap.put("trial", trialList);
        responseMap.put("visit", visitList);

        return responseMap;
    }
}

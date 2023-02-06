package com.project.trysketch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.trysketch.entity.History;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.dto.request.OAuthRequestDto;
import com.project.trysketch.entity.User;
import com.project.trysketch.repository.HistoryRepository;
import com.project.trysketch.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

// 1. 기능    : OAuth2.0 카카오 비즈니스 로직
// 2. 작성자  : 황미경
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final HistoryService historyService;


    @Value("${kakao.oauth2.client.redirect.uri}")
    String redirect_uri;

    @Value("${kakao.oauth2.client.id}")
    String client_id;

    @Value("${kakao.oauth2.client.provider.user-info-uri}")
    String user_info_uri;

    @Value("${kakao.oauth2.client.provider.token-uri}")
    String token_uri;


    public DataMsgResponseDto kakaoLogin(String code, HttpServletResponse response) throws JsonProcessingException {
        String randomNickname = userService.RandomNick().getMessage();

        // 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getToken(code);                                                   // 포스트맨 확인위해서는 주석처리 필요

        // 2. 토큰으로 카카오 API 호출 : "액세스 토큰"으로 "카카오 사용자 정보" 가져오기
        OAuthRequestDto kakaoUserInfo = getKakaoUserInfo(accessToken, randomNickname);         // 포스트맨 확인위해서는 accessToken에서 code로 바꿔야함

        // 3. 필요시에 회원가입
        User kakaoUser = registerKakaoUserIfNeeded(kakaoUserInfo);

        History history = kakaoUser.getHistory().updateVisits(1L);
        historyRepository.save(history);

        List<String> achievementNameList = historyService.getTrophyOfVisit(kakaoUser);

        // 4. JWT 토큰 반환
        String createToken = jwtUtil.createToken(kakaoUser.getEmail(), kakaoUser.getNickname());
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, createToken);
        if (achievementNameList.size() == 0) {
            return new DataMsgResponseDto(StatusMsgCode.LOG_IN);
        } else {
            return new DataMsgResponseDto(StatusMsgCode.LOG_IN, achievementNameList);
        }

    }

    // 1. "인가 코드"로 "액세스 토큰" 요청
    private String getToken(String code) throws JsonProcessingException {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", client_id);
        body.add("redirect_uri", redirect_uri);     // 포스트맨 실험위해서는 다른 주소 들어가야 함
        body.add("code", code);

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                token_uri,
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    // 2. 토큰으로 카카오 API 호출 : "액세스 토큰"으로 "카카오 사용자 정보" 가져오기
    private OAuthRequestDto getKakaoUserInfo(String accessToken, String randomNickname) throws JsonProcessingException {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                user_info_uri,
                HttpMethod.POST,
                kakaoUserInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long id = jsonNode.get("id").asLong();
        String email = jsonNode.get("kakao_account")
                .get("email").asText();

        return new OAuthRequestDto(id, randomNickname, email);
    }

    // 3. 필요시에 회원가입
    private User registerKakaoUserIfNeeded(OAuthRequestDto kakaoUserInfo) {
        // DB 에 중복된 Kakao Id 가 있는지 확인
        Long kakaoId = kakaoUserInfo.getId();
        log.info(">>>>>>>>>>>>> [kakaoService] - 레지스트 부분 kakaoUser 시작");
        User kakaoUser = userRepository.findByKakaoId(kakaoId).orElse(null);
        log.info(">>>>>>>>>>>>> [kakaoService] - 레지스트 부분 kakaoUser 완료");
        if (kakaoUser == null) {
            // 카카오 사용자 email 동일한 email 가진 회원이 있는지 확인
            String kakaoEmail = kakaoUserInfo.getEmail();
            User sameEmailUser = userRepository.findByEmail(kakaoEmail).orElse(null);

            if (sameEmailUser != null) {
                kakaoUser = sameEmailUser;
                // 기존 회원정보에 카카오 Id 추가
                kakaoUser = kakaoUser.kakaoIdUpdate(kakaoId);

                userRepository.save(kakaoUser);
            } else {
                // history 생성부
                History newHistory = historyService.createHistory();
                // 신규 회원가입
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);

                kakaoUser = User.builder()
                        .password(encodedPassword)
                        .kakaoId(kakaoId)
                        .nickname(kakaoUserInfo.getNickname())
                        .email(kakaoUserInfo.getEmail())
                        .imgUrl(userService.getRandomThumbImg().getMessage())
                        .history(newHistory)
                        .build();
                User newUser = userRepository.save(kakaoUser);
                newHistory.updateUser(newUser);
            }
        }
        return kakaoUser;
    }
}
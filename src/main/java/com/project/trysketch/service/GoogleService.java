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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

// 1. 기능    : OAuth2.0 구글 비즈니스 로직
// 2. 작성자  : 서혁수
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final HistoryService historyService;

    @Value("${google.oauth2.client.id}")
    private String clientId;

    @Value("${google.oauth2.secret}")
    private String clientSecret;

    @Value("${google.oauth2.client.request.uri}")
    private String requestUri;

    @Value("${google.oauth2.client.grant.type}")
    private String grantType;

    @Value("${google.oauth2.client.redirect.uri}")
    private String redirectUri;

    @Value("${google.oauth2.client.token.uri}")
    private String tokenUri;

    public DataMsgResponseDto googleLogin(String code, HttpServletResponse response) throws JsonProcessingException {
        String randomNickname = userService.RandomNick().getMessage();

        // 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getToken(code);

        // 2. 토큰으로 구글 API 호출 : "액세스 토큰"으로 "구글 사용자 정보" 가져오기
        OAuthRequestDto googleUserInfo = getGoogleUserInfo(accessToken, randomNickname);

        // 3. 필요시에 회원가입
        User googleUser = registerGoogleUserIfNeeded(googleUserInfo);

        History history = googleUser.getHistory().updateVisits(1L);
        historyRepository.save(history);

        List<String> achievementNameList = historyService.getTrophyOfVisit(googleUser);

        // 4. JWT 토큰 반환
        String createToken =  jwtUtil.createToken(googleUser.getEmail(), googleUser.getNickname());
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, createToken);

        return new DataMsgResponseDto(StatusMsgCode.LOG_IN,achievementNameList);
    }

    // "인가 코드" 로 "액세스 토큰" 요청
    private String getToken(String code) throws JsonProcessingException {
        // 1. HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // 2. HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", grantType);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        // 3. HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> googleTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                tokenUri,
                HttpMethod.POST,
                googleTokenRequest,
                String.class
        );

        // 4. HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    // 토큰으로 구글 API 호출 : "액세스 토큰"으로 "구글 사용자 정보" 가져오기
    private OAuthRequestDto getGoogleUserInfo(String accessToken, String randomNickname) throws JsonProcessingException {
        // 1. HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();

        // 2. HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        headers.add("Authorization", "Bearer " + accessToken);

        // 3. HTTP 요청 보내기
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                requestUri,
                HttpMethod.GET,
                request,
                String.class
        );

        // 4. HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        // 5. 필요한 값만 추출해서 리턴
        Long id = jsonNode.get("exp").asLong();
        String email = jsonNode.get("email").asText();
        return new OAuthRequestDto(id, randomNickname, email);
    }

    // 필요시에 회원가입
    @Transactional
    public User registerGoogleUserIfNeeded(OAuthRequestDto googleUserInfo) {
        // 1. DB 에 중복된 Google Id 가 있는지 확인
        Long googleId = googleUserInfo.getId();
        User googleUser = userRepository.findByGoogleId(googleId).orElse(null);

        if (googleUser == null) {
            // 2. 구글 사용자 email 동일한 email 가진 회원이 있는지 확인
            String googleEmail = googleUserInfo.getEmail();
            // 3. 유저정보에 동일한 이메일을 소유한 유저가 있는지 확인
            User emailCheck = userRepository.findByEmail(googleEmail).orElse(null);

            // 4. null 이 아닌 즉, 유저가 존재할 경우 시작
            if (emailCheck != null) {
                // 5. 기존의 유저 정보를 재활용(새롭게 받아온 유저에 기존 유저 정보를 덮어 씌운다)
                googleUser = emailCheck;
                // 6. 새롭게 받아온 ID 를 기존 계정의 ID 로 변경(기존 유저에서 ID 값만 변경)
                googleUser = googleUser.googleIdUpdate(googleId);

                googleUser = userRepository.save(googleUser);
            } else {
                // history 생성부
                History newHistory = historyService.createHistory();
                // 7. 신규 회원가입
                String password = UUID.randomUUID().toString();             // 난수 비밀번호 생성
                String encodedPassword = passwordEncoder.encode(password);  // 비밀번호 디코딩

                // 8. 새로운 계정 생성
                googleUser = User.builder()
                        .password(encodedPassword)
                        .googleId(googleId)
                        .nickname(googleUserInfo.getNickname())
                        .email(googleUserInfo.getEmail())
                        .imgUrl(userService.getRandomThumbImg().getMessage())
                        .history(newHistory)
                        .build();
                User newUser = userRepository.save(googleUser);
                newHistory.updateUser(newUser);
            }
        }
        return googleUser;
    }
}
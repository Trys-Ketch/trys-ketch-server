package com.project.trysketch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.dto.request.OAuthRequestDto;
import com.project.trysketch.entity.User;
import com.project.trysketch.repository.UserRepository;
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
import java.util.UUID;

// 1. 기능    : OAuth2.0 카카오 비즈니스 로직
// 2. 작성자  : 황미경
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    public MsgResponseDto googleLogin(String code, HttpServletResponse response) throws JsonProcessingException {
        String randomNickname = userService.RandomNick().getMessage();

        // 1. "인가 코드"로 "액세스 토큰" 요청
//        String accessToken = getToken(code);                                                        // 포스트맨 확인위해 주석처리 필요

        // 2. 토큰으로 카카오 API 호출 : "액세스 토큰"으로 "카카오 사용자 정보" 가져오기
        OAuthRequestDto googleUserInfo = getGoogleUserInfo(code, randomNickname);           // 포스트맨 확인위해 accessToken에서 code로 바꿔야함

        // 3. 필요시에 회원가입
        User googleUser = registerGoogleUserIfNeeded(googleUserInfo);

        // 4. JWT 토큰 반환
        String createToken =  jwtUtil.createToken(googleUser.getEmail(), randomNickname);
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, createToken);

        return new MsgResponseDto(StatusMsgCode.LOG_IN);
    }

    // 1. "인가 코드"로 "액세스 토큰" 요청
    private String getToken(String code) throws JsonProcessingException {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", "367608785758-u1itnhg7fcsiabma713cap42j2dm4h2q.apps.googleusercontent.com");
        body.add("client_secret", "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");
        body.add("redirect_uri", "http://localhost:8080/login/oauth2/code/google");  // 포스트맨 실험

//        body.add("redirect_uri", "https://trys-ketch.com/login");                    // 프론트의 주소
        body.add("code", code);

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> googleTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                googleTokenRequest,
                String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    // 2. 토큰으로 카카오 API 호출 : "액세스 토큰"으로 "카카오 사용자 정보" 가져오기
    private OAuthRequestDto getGoogleUserInfo(String accessToken, String randomNickname) throws JsonProcessingException {
        String url = String.format("GET https://www.googleapis.com/drive/v2/files?access_token={$}}")
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("access_token", "Bearer " + accessToken);
//        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://www.googleapis.com/drive/v2/files",
                HttpMethod.GET,
                request,
                String.class
        );
        System.out.println("response.getBody() = " + response.getBody());

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long id = jsonNode.get("id").asLong();
        String email = jsonNode.get("google_account")
                .get("email").asText();

        return new OAuthRequestDto(id, randomNickname, email);
    }

    // 3. 필요시에 회원가입
    private User registerGoogleUserIfNeeded(OAuthRequestDto googleUserInfo) {
        // DB 에 중복된 Kakao Id 가 있는지 확인
        Long googleId = googleUserInfo.getId();
        User googleUser = userRepository.findByGoogleId(googleId)
                .orElse(null);
        if (googleUser == null) {
            // 카카오 사용자 email 동일한 email 가진 회원이 있는지 확인
            String googleEmail = googleUserInfo.getEmail();

            User sameEmailUser = userRepository.findByEmail(googleEmail).orElse(null);

            if (sameEmailUser != null) {
//                googleUser = sameEmailUser;
                // 기존 회원정보에 카카오 Id 추가
                googleUser = googleUser.googleIdUpdate(googleId);
            } else {
                // 신규 회원가입
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);
                String email = googleUserInfo.getEmail();

                googleUser = User.builder()
                        .password(encodedPassword)
                        .googleId(googleId)
                        .nickname(googleUserInfo.getNickname())
                        .email(email)
                        .imgUrl(userService.getRandomThumbImg().getMessage())
                        .build();
            }
            userRepository.save(googleUser);
        }
        return googleUser;
    }
}
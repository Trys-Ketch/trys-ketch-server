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
import java.util.UUID;

// 1. 기능    : OAuth2.0 구글 비즈니스 로직
// 2. 작성자  : 황미경
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Value("${google.oauth2.client.id}")
    private String clientId;

    @Value("${google.oauth2.secret}")
    private String clientSecret;

    @Value("${google.oauth2.scope}")
    private String scope;

    @Value("${google.oauth2.client.grant.type}")
    private String grantType;

    @Value("${google.oauth2.client.redirect.uri}")
    private String redirectUri;

    @Value("${google.oauth2.client.token.uri}")
    private String tokenUri;

    public MsgResponseDto googleLogin(String code, HttpServletResponse response) throws JsonProcessingException {
        log.info(">>>>>>>>>>>>>>>>>> GoogleService 의 googleLogin 메서드 시작 <<<<<<<<<<<<<<<<<<");
        String randomNickname = userService.RandomNick().getMessage();      // 랜덤 닉네임 받아오기

        // 1. "인가 코드"로 "액세스 토큰" 요청
        log.info(">>>>>> GoogleService 의 googleLogin : 메서드 / 받아온 code 값 : {}", code);
        String accessToken = getToken(code);

        log.info(">>>>>> GoogleService 의 googleLogin : 메서드 / 받아온 accessToken 값 : {}", accessToken);

        // 2. 토큰으로 구글 API 호출 : "액세스 토큰"으로 "구글 사용자 정보" 가져오기
        OAuthRequestDto googleUserInfo = getGoogleUserInfo(accessToken, randomNickname);

        // 3. 필요시에 회원가입
        User googleUser = registerGoogleUserIfNeeded(googleUserInfo);

        // 4. JWT 토큰 반환
        String createToken =  jwtUtil.createToken(googleUser.getEmail(), googleUser.getNickname());
        log.info(">>>>>> GoogleService 의 googleLogin : 메서드 / 구글 회원가입 최종 이메일 : {}", googleUser.getEmail());
        log.info(">>>>>> GoogleService 의 googleLogin : 메서드 / 구글 회원가입 최종 닉네임 : {}", randomNickname);
        log.info(">>>>>> GoogleService 의 googleLogin : 메서드 / 받아온 createToken 값 : {}", accessToken);

        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, createToken);

        return new MsgResponseDto(StatusMsgCode.LOG_IN);
    }

    // 1. "인가 코드"로 "액세스 토큰" 요청
    private String getToken(String code) throws JsonProcessingException {
        log.info(">>>>>>>>>>>>>>>>>> GoogleService 의 getToken 메서드 시작 <<<<<<<<<<<<<<<<<<");
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        log.info(">>>>>> GoogleService 의 getToken : 메서드 / [ HTTP Header ] 생성 완료");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", grantType);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        log.info(">>>>>> GoogleService 의 getToken : 메서드 / [ HTTP Body ] 생성 완료");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> googleTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                tokenUri,
                HttpMethod.POST,
                googleTokenRequest,
                String.class
        );

        log.info(">>>>>> GoogleService 의 getToken : 메서드 / [ HTTP 요청 ] 성공 !!!");

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        log.info(">>>>>> GoogleService 의 getToken : 메서드 / 받아온 responseBody 값 : {}", responseBody);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    // 2. 토큰으로 구글 API 호출 : "액세스 토큰"으로 "구글 사용자 정보" 가져오기
    private OAuthRequestDto getGoogleUserInfo(String accessToken, String randomNickname) throws JsonProcessingException {
        log.info(">>>>>>>>>>>>>>>>>> GoogleService 의 getGoogleUserInfo 메서드 시작 <<<<<<<<<<<<<<<<<<");
        String GOOGLE_USERINFO_REQUEST_URL="https://oauth2.googleapis.com/tokeninfo";
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(headers);
        headers.add("Authorization", "Bearer " + accessToken);
        log.info(">>>>>> GoogleService 의 getGoogleUserInfo 메서드 / 토큰 값 : Bearer {}", accessToken);

        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                GOOGLE_USERINFO_REQUEST_URL,
                HttpMethod.GET,
                request,
                String.class
        );
        log.info(">>>>>> GoogleService 의 getGoogleUserInfo 메서드 / [ HTTP 요청 ] 성공 !!!");

        String responseBody = response.getBody();
        log.info(">>>>>> GoogleService 의 getGoogleUserInfo 메서드 / responseBody 값 : {}", responseBody);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        Long id = jsonNode.get("exp").asLong();
        String email = jsonNode.get("email").asText();
        log.info(">>>>>> GoogleService 의 getGoogleUserInfo 메서드 / email 값 : {}", email);

        return new OAuthRequestDto(id, randomNickname, email);
    }

    // 3. 필요시에 회원가입
    private User registerGoogleUserIfNeeded(OAuthRequestDto googleUserInfo) {
        log.info(">>>>>>>>>>>>>>>>>> GoogleService 의 registerGoogleUserIfNeeded 메서드 시작 <<<<<<<<<<<<<<<<<<");
        // DB 에 중복된 Google Id 가 있는지 확인
        Long googleId = googleUserInfo.getId();
        User googleUser = userRepository.findByGoogleId(googleId).orElse(null);

        log.info(">>>>>> GoogleService 의 registerGoogleUserIfNeeded 메서드 / googleId 값 : {}", googleId);
        log.info(">>>>>> GoogleService 의 registerGoogleUserIfNeeded 메서드 / googleUser 값 : {}", googleUser);

        if (googleUser == null) {
            // 구글 사용자 email 동일한 email 가진 회원이 있는지 확인
            String googleEmail = googleUserInfo.getEmail();
            // 유저정보에 동일한 이메일을 소유한 유저가 있는지 확인
            User emailCheck = userRepository.findByEmail(googleEmail).orElse(null);
            log.info(">>>>>> GoogleService 의 registerGoogleUserIfNeeded 메서드 / 동일 유저 확인 / emailCheck : {}", emailCheck);

            // null 이 아닌 즉, 유저가 존재할 경우 시작
            if (emailCheck != null) {
                log.info(">>>>>> GoogleService 의 registerGoogleUserIfNeeded 메서드 / 기존 유저가 있네???");

                // 기존의 유저 정보를 재활용(새롭게 받아온 유저에 기존 유저 정보를 덮어 씌운다)
                googleUser = emailCheck;
                // 새롭게 받아온 ID 를 기존 계정의 ID 로 변경(기존 유저에서 ID 값만 변경)
                googleUser = googleUser.googleIdUpdate(googleId);

                log.info(">>>>>> GoogleService 의 registerGoogleUserIfNeeded 메서드 / 기존 ID : {} -> 변경된 ID : {}", emailCheck.getGoogleId(), googleId);
            } else {
                log.info(">>>>>> GoogleService 의 registerGoogleUserIfNeeded 메서드 / 구글 뉴비 입장 !!!");
                // 신규 회원가입
                String password = UUID.randomUUID().toString();             // 난수 비밀번호 생성
                String encodedPassword = passwordEncoder.encode(password);  // 비밀번호 디코딩

                // 새로운 계정 생성
                googleUser = User.builder()
                        .password(encodedPassword)
                        .googleId(googleId)
                        .nickname(googleUserInfo.getNickname())
                        .email(googleUserInfo.getEmail())
                        .imgUrl(userService.getRandomThumbImg().getMessage())
                        .build();

                log.info(">>>>>> GoogleService 의 registerGoogleUserIfNeeded 메서드 / 구글 뉴비 Email : {}", googleUserInfo.getEmail());
            }
            userRepository.save(googleUser);
        }
        return googleUser;
    }
}
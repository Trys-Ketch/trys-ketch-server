package com.project.trysketch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.trysketch.dto.response.NaverResponseDto;
import com.project.trysketch.entity.User;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

// 1. 기능    : OAuth2.0 네이버 비즈니스 로직
// 2. 작성자  : 김재영
@Service
@RequiredArgsConstructor
@PropertySource("classpath:application-oauth.properties")
public class NaverService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    String client_id;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    String client_secret;
    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    String redirect_uri;
    @Value("${spring.security.oauth2.client.provider.naver.user-info-uri}")
    String user_info_uri;



    public MsgResponseDto naverLogin(String code, HttpServletResponse response) throws JsonProcessingException {
        String randomNickname = userService.RandomNick().getMessage();

        // 1. "인가 코드"로 "액세스 토큰" 요청
//        String accessToken = getToken(code);

        // 2. 토큰으로 Naver API 호출 : "액세스 토큰"으로 "Naver 사용자 정보" 가져오기
        NaverResponseDto naverUserInfo = getNaverUserInfo(code, randomNickname);

        User naverUser = registerNaverUserIfNeeded(naverUserInfo);

        String createToken = jwtUtil.createToken(naverUser.getEmail(), randomNickname);
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, createToken);

        return new MsgResponseDto(StatusMsgCode.LOG_IN);
    }


    public String getToken(String code, String state) {

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", client_id);
        body.add("client_secret", client_secret);
        body.add("redirect_uri", redirect_uri);
        body.add("code" , code);	// 응답으로 받은 코드
        body.add("state", state);

        HttpEntity<MultiValueMap<String, String>> naverTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();

        ResponseEntity<String> accessTokenResponse = rt.exchange(
                "https://nid.naver.com/oauth2.0/token",
                HttpMethod.POST,
                naverTokenRequest,
                String.class
        );

        return "access_token: " + accessTokenResponse.getBody();
    }

    private NaverResponseDto getNaverUserInfo(String accessToken, String randomNickname) throws JsonProcessingException {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> naverUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                user_info_uri,
                HttpMethod.POST,
                naverUserInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String id = String.valueOf(jsonNode.get("response").get("id"));
        String email = jsonNode.get("response").get("email").asText();

        return new NaverResponseDto(id, email, randomNickname);
    }

    private User registerNaverUserIfNeeded(NaverResponseDto naverUserInfo) {
        // DB 에 중복된 Naver Id 가 있는지 확인
        String naverId = naverUserInfo.getId();
        User naverUser = userRepository.findByNaverId(naverId)
                .orElse(null);
        if (naverUser == null) {
            // Naver 사용자 email 동일한 email 가진 회원이 있는지 확인
            String naverEmail = naverUserInfo.getEmail();

            User sameEmailUser = userRepository.findByEmail(naverEmail).orElse(null);

            if (sameEmailUser != null) {
                naverUser = sameEmailUser;
                // 기존 회원정보에 NaverId 추가
                naverUser = naverUser.naverIdUpdate(naverId);
            } else {
                // 신규 회원가입
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);
                String email = naverUserInfo.getEmail();

                naverUser = User.builder()
                        .password(encodedPassword)
                        .naverId(naverId)
                        .nickname(naverUserInfo.getNickname())
                        .email(email)
                        .imgUrl(userService.getRandomThumbImg().getMessage())
                        .build();
            }
            userRepository.save(naverUser);
        }
        return naverUser;
    }


}


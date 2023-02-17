package com.project.trysketch.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.trysketch.dto.SocialLoginVendor;
import com.project.trysketch.dto.request.OAuthRequestDto;
import com.project.trysketch.dto.response.DataMsgResponseDto;
import com.project.trysketch.entity.History;
import com.project.trysketch.global.jwt.RefreshToken;
import com.project.trysketch.entity.User;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.repository.HistoryRepository;
import com.project.trysketch.repository.RefreshTokenRepository;
import com.project.trysketch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// 1. 기능   : 소셜로그인 서비스
// 2. 작성자 : 황미경, 김재영, 서혁수 , 안은솔
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialLoginService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;
    private final UserService userService;
    private final HistoryService historyService;
    private final JwtUtil jwtUtil;
    private final SocialLoginVendor socialLoginVendor;
    private final RefreshTokenRepository refreshTokenRepository;

    public DataMsgResponseDto socialLogin(String vendor, String code, String state, HttpServletResponse response, HttpServletRequest request) throws JsonProcessingException {

        String randomNickname = userService.getRandomNick();

        // "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getToken(vendor, code, state);

        // 토큰으로 소셜 로그인 API 호출 : "액세스 토큰"으로 "소셜 로그인 사용자 정보" 가져오기
        OAuthRequestDto socialUserInfo = getSocialUserInfo(vendor, accessToken, randomNickname);

        // 필요시에 회원가입
        User socialUser = registerSocialUserIfNeeded(vendor, socialUserInfo);

        History history = socialUser.getHistory().updateVisits(1L);
        historyRepository.save(history);

        List<String> achievementNameList = historyService.getTrophyOfVisit(socialUser);

        // AccessToken 토큰 반환 및 헤더에 추가
        String createToken = jwtUtil.createAcToken(socialUser.getEmail(), socialUser.getNickname());
        response.addHeader(JwtUtil.ACCESS_TOKEN_HEADER, createToken);

        Cookie[] cookies = request.getCookies();
        boolean checkToken = false;

        // 쿠키 중 RefreshToken 가져오기 존재하면 삭제
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                String value = cookie.getValue();
                if (name.equals("RefreshToken")) {
                    log.info(">>>>> SocialLoginService 의 socialLogin 메서드 / 기존의 RefreshToken 발견 DB 삭제 진행");
                    checkToken = true;
                    refreshTokenRepository.deleteById(value);
                    break;
                }
            }
        }

        // Email 암호화 및 RefreshToken 생성
        String encodedEmail = passwordEncoder.encode(socialUser.getEmail());
        String rfToken = jwtUtil.createRfToken(socialUser.getEmail(), socialUser.getNickname());

        // Redis 에 key 로 암호화된 이메일, value 로 토큰을 저장
        RefreshToken newRefreshToken = new RefreshToken(encodedEmail, rfToken);
        refreshTokenRepository.save(newRefreshToken);

        log.info(">>>>> SocialLoginService 의 socialLogin 메서드 / 만들어진 RefreshToken : {}", rfToken);

        // 기존에 사용하던 set-cookie 에서는 sameSite 를 지원하지 않는다. 따라서 ResponseCookie 를 사용
        ResponseCookie rfCookie = ResponseCookie.from(JwtUtil.REFRESH_TOKEN_HEADER, encodedEmail)
                .path("/")
                .domain("trys-ketch.com")
                .httpOnly(true)
                .secure(true)
                .maxAge(7 * 24 * 60 * 60)       // 7일
                .build();
        if (checkToken) {
            log.info(">>>>> SocialLoginService 의 socialLogin 메서드 / setHeader 진행");
            response.setHeader("Set-Cookie", rfCookie.toString());
        } else {
            log.info(">>>>> SocialLoginService 의 socialLogin 메서드 / addHeader 진행");
            response.addHeader("Set-Cookie", rfCookie.toString());
        }

        log.info(">>>>> SocialLoginService 의 socialLogin 메서드 / 만들어진 AccessToken : {}", createToken);

        if (achievementNameList.size() == 0) {
            return new DataMsgResponseDto(StatusMsgCode.LOG_IN);
        } else {
            return new DataMsgResponseDto(StatusMsgCode.LOG_IN, achievementNameList);
        }
    }

    // "인가 코드"로 "액세스 토큰" 요청
    private String getToken(String vendor, String code, String state) throws JsonProcessingException {

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        Map<String,String> switchVendor = switchVendor(vendor);

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", switchVendor.get("clientId"));
        body.add("redirect_uri", switchVendor.get("redirectUri"));
        body.add("client_secret", switchVendor.get("clientSecret"));
        body.add("code", code);
        body.add("state", state);

        String tokenUri = switchVendor.get("tokenUri");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> socialTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                tokenUri,
                HttpMethod.POST,
                socialTokenRequest,
                String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    // 토큰으로 소셜 로그인 API 호출 : "액세스 토큰"으로 "소셜 로그인 사용자 정보" 가져오기
    private OAuthRequestDto getSocialUserInfo(String vendor, String accessToken, String randomNickname) throws JsonProcessingException {

        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> SocialUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                switchVendor(vendor).get("userInfoUri"),
                HttpMethod.POST,
                SocialUserInfoRequest,
                String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);

        String id = "";
        String email = "";
        switch (vendor) {
            case "kakao" -> {
                id = jsonNode.get("id").asText();
                email = jsonNode.get("kakao_account").get("email").asText();
            }
            case "google" -> {
                id = jsonNode.get("exp").asText();
                email = jsonNode.get("email").asText();
            }
            case "naver" -> {
                id = String.valueOf(jsonNode.get("response").get("id"));
                email = jsonNode.get("response").get("email").asText();
            }
        }
        return new OAuthRequestDto(id, randomNickname, email);
    }

    // 필요시에 회원가입
    private User registerSocialUserIfNeeded(String vendor, OAuthRequestDto socialUserInfo) {

        // DB 에 중복된 소셜 로그인 id 가 있는지 확인
        String socialId = socialUserInfo.getId();

        User socialUser = null;
        switch (vendor) {
            case "kakao" -> socialUser = userRepository.findByKakaoId(Long.valueOf(socialId)).orElse(null);
            case "google" -> socialUser = userRepository.findByGoogleId(Long.valueOf(socialId)).orElse(null);
            case "naver" -> socialUser = userRepository.findByNaverId(socialId).orElse(null);
        }

        if (socialUser == null) {
            // 각 소셜 로그인 사용자 email 동일한 email 가진 회원이 있는지 확인
            String socialEmail = socialUserInfo.getEmail();
            User sameEmailUser = userRepository.findByEmail(socialEmail).orElse(null);

            if (sameEmailUser != null) {
                // 기존의 유저 정보를 재활용(새롭게 받아온 유저에 기존 유저 정보를 덮어 씌운다)
                socialUser = sameEmailUser;

                // 새롭게 받아온 ID 를 기존 계정의 ID 로 변경(기존 유저에서 ID 값만 변경)
                switch (vendor) {
                    case "kakao" -> socialUser = socialUser.kakaoIdUpdate(Long.valueOf(socialId));
                    case "google" -> socialUser = socialUser.googleIdUpdate(Long.valueOf(socialId));
                    case "naver" -> socialUser = socialUser.naverIdUpdate(socialId);
                }
                socialUser = userRepository.save(socialUser);
            } else {
                // history 생성부
                History newHistory = historyService.createHistory();
                // 신규 회원가입
                String password = UUID.randomUUID().toString();
                String encodedPassword = passwordEncoder.encode(password);
                String imgUrl = userService.getRandomThumbImg();

                User.UserBuilder userBuilder = User.builder()
                        .password(encodedPassword)
                        .nickname(socialUserInfo.getNickname())
                        .email(socialUserInfo.getEmail())
                        .imgUrl(imgUrl)
                        .history(newHistory);

                socialUser = switch (vendor) {
                    case "kakao" -> userBuilder.kakaoId(Long.valueOf(socialId)).build();
                    case "google" -> userBuilder.googleId(Long.valueOf(socialId)).build();
                    case "naver" -> userBuilder.naverId(socialId).build();
                    default -> userBuilder.build();
                };

                User newUser = userRepository.save(socialUser);
                newHistory.updateUser(newUser);
            }
        }
        return socialUser;
    }

    public Map<String,String> switchVendor(String vendor) {

        Map<String,String> socialUserMap = new HashMap<>();
        String clientId = "";
        String redirectUri = "";
        String clientSecret = "";
        String userInfoUri = "";
        String tokenUri = "";
        switch (vendor) {
            case "kakao" -> {
                clientId = socialLoginVendor.getKakaoClientId();
                redirectUri = socialLoginVendor.getKakaoRedirectUri();
                userInfoUri = socialLoginVendor.getKakaoUserInfoUri();
                tokenUri = socialLoginVendor.getKakaoTokenUri();
            }
            case "google" -> {
                clientId = socialLoginVendor.getGoogleClientId();
                redirectUri = socialLoginVendor.getGoogleRedirectUri();
                userInfoUri = socialLoginVendor.getGoogleUserInfoUri();
                tokenUri = socialLoginVendor.getGoogleTokenUri();
                clientSecret = socialLoginVendor.getGoogleClientSecret();
            }
            case "naver" -> {
                clientId = socialLoginVendor.getNaverClientId();
                redirectUri = socialLoginVendor.getNaverRedirectUri();
                userInfoUri = socialLoginVendor.getNaverUserInfoUri();
                tokenUri = socialLoginVendor.getNaverTokenUri();
                clientSecret = socialLoginVendor.getNaverClientSecret();
            }
        }
        socialUserMap.put("clientId", clientId);
        socialUserMap.put("redirectUri", redirectUri);
        socialUserMap.put("userInfoUri", userInfoUri);
        socialUserMap.put("tokenUri", tokenUri);
        socialUserMap.put("clientSecret", clientSecret);
        return socialUserMap;
    }
}
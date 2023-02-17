package com.project.trysketch.service;

import com.project.trysketch.dto.GamerEnum;
import com.project.trysketch.entity.Guest;
import com.project.trysketch.entity.RandomNick;
import com.project.trysketch.global.jwt.RefreshToken;
import com.project.trysketch.entity.ThumbImg;
import com.project.trysketch.entity.User;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.repository.GuestRepository;
import com.project.trysketch.repository.RandomNickRepository;
import com.project.trysketch.repository.RefreshTokenRepository;
import com.project.trysketch.repository.ThumbImgRepository;
import com.project.trysketch.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

// 1. 기능   : 유저 비즈니스 로직
// 2. 작성자 : 서혁수, 황미경, 김재영
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final RandomNickRepository randomNickRepository;
    private final GuestRepository guestRepository;
    private final ThumbImgRepository thumbImgRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;
    private static int IMG_MAXMUM = 3;
    private static int IMG_NUM = 0;

    // 헤더값 추출 및 회원 검증
    public String validHeader(HttpServletRequest request) {

        String userToken = request.getHeader("Authorization");                 // 유저 헤더값 추출
        String guestInfo = request.getHeader("guest");                         // 게스트 헤더값 추출

        log.info(">>>>>> UserService 의 validHeader 메서드 / userToken : " + userToken);
        log.info(">>>>>> UserService 의 validHeader 메서드 / guestInfo : " + guestInfo);
        if (userToken != null) {
            log.info(">>>>>> UserService 의 validHeader 메서드 / userToken != null 회원 분기 시작");
            Claims claims = jwtUtil.authorizeToken(request);                        // 유저 검증
            return claims.get("email").toString();                                  // 이메일 값만을 반환
        } else {
            log.info(">>>>>> UserService 의 validHeader 메서드 / guestInfo != null 비회원 분기 시작");
            return guestInfo;
        }
    }

    // 회원 Id, Nickname 추출
    public HashMap<String, String> getGamerInfo(String token) {

        HashMap<String, String> result = new HashMap<>();                           // 결과물을 담기위한 HashMap

        // 회원, 비회원 분기처리 시작
        if (token.contains("@")) {
            log.info(">>>>>> UserService 의 gamerInfo 메서드 / 회원 분기 시작");
            log.info(">>>>>> UserService 의 gamerInfo 메서드 / 회원 Token : {}", token);

            // 문자열 안에 @ 가 있으면 request 로 받아온다. 유저가 사용한다고 판단하고 시작
            // request 를 통해 받아온 값은 email 이기 때문에 @ 을 포함하고 있다.
            // 그래서 validHeader 를 통한 결과값이 회원 이메일을 받아온 것이므로 회원 기준으로 시작
            User user = userRepository.findByEmail(token).orElseThrow(
                    () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
            );
            result.put(GamerEnum.ID.key(), user.getId().toString());                // 회원 Id 를 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.NICK.key(), user.getNickname());                   // 회원 닉네임을 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.IMG.key(), user.getImgUrl());                      // 회원 img url 을 key 값으로 value 추출 해서 result 에 주입
        } else if (token.startsWith("Bearer ")) {
            log.info(">>>>>> UserService 의 gamerInfo 메서드 / 세션 분기 시작");
            log.info(">>>>>> UserService 의 gamerInfo 메서드 / 세션 Token : {}", token);

            // 3. 문자열의 시작이 Bearer 이면 문자열 형태로 받아오는 webSession 에서 사용된다고 판단하고 시작
            Claims claims = jwtUtil.authorizeSocketToken(token);                    // 검증 및 정보 가져오기
            String email = (String) claims.get("email");                            // 토큰에서 이메일 값만을 추출

            User user = userRepository.findByEmail(email).orElseThrow(
                    () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
            );
            result.put(GamerEnum.ID.key(), user.getId().toString());                // 회원 id 를 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.NICK.key(), user.getNickname());                   // 회원 닉네임을 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.IMG.key(), user.getImgUrl());                      // 회원 img url 을 key 값으로 value 추출 해서 result 에 주입
        } else {
            log.info(">>>>>> UserService 의 gamerInfo 메서드 / 비회원 세션 분기 시작");
            
            // 4. 위의 분기에 해당하지 않을 경우에는 guest 라고 판단하고 시작
            token = URLDecoder.decode(token, StandardCharsets.UTF_8);               // 비회원의 토큰 정보를 얻기 위해서 디코딩
            log.info(">>>>>>> 디코딩 된 token {}", token);

            // 게스트의 원하는 정보를 뽑아서 사용하기 위해서 배열에다가 하나씩 넣어준다.
            // guestInfo 예시 형태 : "10001,유저닉네임,이미지 URL"
            String[] guestInfo = token.split(",");

            log.info(">>>>>> UserService 의 gamerInfo 메서드 / 비회원 Token : {}", Arrays.toString(guestInfo));

            // 게스트 유정 Redis DB 에 존재하는지 확인(검증)
            Guest guest = guestRepository.findByGuestId(guestInfo[0]).orElseThrow(
                    () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
            );
            log.info(">>>>>> UserService 의 비회원 인증 결과 / guestId : " + guest.getId());
            log.info(">>>>>> UserService 의 비회원 인증 결과 / guestNick : " + guest.getNickname());
            log.info(">>>>>> UserService 의 비회원 인증 결과 / guestImg : " + guest.getImgUrl());
            result.put(GamerEnum.ID.key(), guest.getGuestId());               // guest Id 를 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.NICK.key(), guest.getNickname());            // guest 닉네임을 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.IMG.key(), guest.getImgUrl());               // guest img url 을 key 값으로 value 추출 해서 result 에 주입
        }
        return result;
    }

    // 랜덤 닉네임 가져오기
    public String getRandomNick() {

        int num = (int) (Math.random() * 1000 + 1);
        RandomNick randomNick = randomNickRepository.findByNum(num).orElseThrow(
                () -> new CustomException(StatusMsgCode.NOT_FOUND_NICK));
        return randomNick.getNickname();
    }

    // 랜덤 프로필 이미지 가져오기
    public String getRandomThumbImg() {

        IMG_NUM = (IMG_NUM == IMG_MAXMUM) ? 1 : IMG_NUM + 1;
        ThumbImg thumbImg = thumbImgRepository.findById(IMG_NUM).orElseThrow(
                () -> new CustomException(StatusMsgCode.IMAGE_NOT_FOUND));
        return thumbImg.getImgUrl();
    }

    // 회원 정보 조회
    public HashMap<String, String> getUserInfo(HttpServletRequest request) {

        HashMap<String, String> extInfo;
        try {
            String header = validHeader(request);
            extInfo = getGamerInfo(header);
        } catch(Exception e) {
            throw new CustomException(StatusMsgCode.NECESSARY_LOG_IN);
        }
        return extInfo;
    }

    // 토큰 재발급 메서드
    public void issuedToken(HttpServletRequest request, HttpServletResponse response) {
        log.info(">>>>>> UserService 의 issuedToken 시작");
        Cookie[] cookies = request.getCookies();        // 클라이언트가 주는 모든 쿠키 가져오기
        String tokenIndex = "";

        // 쿠키 리스트 null 즉, 가져온 쿠키가 없으면 에러 발생
        if (cookies == null) {
            throw new CustomException(StatusMsgCode.EXPIRED_REFRESH_TOKEN);
        }

        // 쿠키 중 RefreshToken 가져오기
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            String value = cookie.getValue();
            if (name.equals("RefreshToken")) {
                tokenIndex = value;
                break;
            }
        }

        // access token 발급
        if (tokenIndex != null) {
            RefreshToken refreshToken = refreshTokenRepository.findById(tokenIndex).orElseThrow(
                    () -> new CustomException(StatusMsgCode.EXPIRED_REFRESH_TOKEN)
            );

            log.info(">>>>>> UserService 의 issuedToken / RefreshToken 찾음");
            log.info(">>>>>> UserService 의 issuedToken / RefreshToken : {}", refreshToken.getToken());

            Claims claims = jwtUtil.authorizeSocketToken(refreshToken.getToken());
            String token = jwtUtil.createAcToken(claims.get("email").toString(), claims.get("nickname").toString());
            response.addHeader(JwtUtil.ACCESS_TOKEN_HEADER, token);
            log.info(">>>>>> UserService 의 issuedToken / 새로 발급된 AccessToken : {}", token);
        } else {
            throw new CustomException(StatusMsgCode.EXPIRED_REFRESH_TOKEN);
        }
    }

    // 로그아웃
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();

        // 쿠키 중 RefreshToken 있으면 가져와서 삭제
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                String name = cookie.getName();
                String value = cookie.getValue();
                if (name.equals("RefreshToken")) {
                    // RefreshToken DB 에서 삭제 및 쿠키는 maxAge(파기시간) 를 0으로 줘서 쿠키 삭제
                    log.info(">>>>> SocialLoginService 의 UserService 메서드 / 로그아웃 요청으로 인해서 삭제 진행");
                    refreshTokenRepository.deleteById(value);
                    ResponseCookie rfCookie = ResponseCookie.from(JwtUtil.REFRESH_TOKEN_HEADER, value)
                            .path("/")
                            .domain("trys-ketch.com")
                            .httpOnly(true)
                            .secure(true)
                            .maxAge(0)
                            .build();
                    response.setHeader("Set-Cookie", rfCookie.toString());
                    break;
                }
            }
        }
    }
}
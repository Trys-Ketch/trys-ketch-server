package com.project.trysketch.service;

import com.project.trysketch.entity.ThumbImg;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.redis.dto.GamerEnum;
import com.project.trysketch.redis.entity.Guest;
import com.project.trysketch.redis.repositorty.GuestRepository;
import com.project.trysketch.repository.ThumbImgRepository;
import com.project.trysketch.suggest.RandomNick;
import com.project.trysketch.suggest.RandomNickRepository;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.dto.request.SignUpRequestDto;
import com.project.trysketch.dto.request.SignInRequestDto;
import com.project.trysketch.entity.User;
import com.project.trysketch.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Optional;

// 1. 기능   : 유저 비즈니스 로직
// 2. 작성자 : 서혁수
@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RandomNickRepository randomNickRepository;
    private final GuestRepository guestRepository;
    private final ThumbImgRepository thumbImgRepository;
    private static int IMG_MAXIMUM = 3;
    private static int IMG_NUM = 0;


    // 회원가입
    public void signUp(SignUpRequestDto requestDto) {
        // 1. 중복 여부 검사
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new CustomException(StatusMsgCode.EXIST_USER);
        }
        if (userRepository.existsByNickname(requestDto.getNickname())) {
            throw new CustomException(StatusMsgCode.EXIST_NICK);
        }

        // 2. 클라이언트로부터 받아온 비밀번호를 인코딩 해서 가져오기
        String encodePassword = passwordEncoder.encode(requestDto.getPassword());

        // 3. 새롭게 만들 빈 User 객체 생성
        User user = User.builder()
                .email(requestDto.getEmail())
                .nickname(requestDto.getNickname())
                .password(encodePassword)
                .imgUrl(null)
                .build();

        // 4. DB 에 새로운 유저정보 넣어주기
        userRepository.save(user);
    }

    // 폼 로그인
    public void login(SignInRequestDto requestDto, HttpServletResponse response) {
        // 1. 유저 이메일 기준으로 유저 정보 찾아와서
        User user = userRepository.findByEmail(requestDto.getEmail()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );
        // 2. 비밀번호가 일치하는지 검증한다.
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new CustomException(StatusMsgCode.INVALID_PASSWORD);
        }

        // 3. 로그인 성공 및 토큰을 발급받아서 가져온다.
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, jwtUtil.createToken(user.getEmail(), user.getNickname()));
    }

    // 헤더값 추출 및 회원 검증
    public String validHeader(HttpServletRequest request) {
        String userToken = request.getHeader("Authorization");                 // 유저 헤더값 추출
        String guestInfo = request.getHeader("guest");                         // 게스트 헤더값 추출

        if (userToken != null) {
            Claims claims = jwtUtil.authorizeToken(request);                        // 유저 검증
            return claims.get("email").toString();                                  // 이메일 값만을 반환
        } else {
            return guestInfo;
        }
    }

    // 회원 Id, Nickname 추출
    public HashMap<String, String> gamerInfo(String token) {
        HashMap<String, String> result = new HashMap<>();                           // 결과물을 담기위한 HashMap

        // 1. 회원, 비회원 분기처리 시작
        if (token.contains("@")) {
            // 2. 문자열 안에 @ 가 있으면 request 로 받아온다. 유저가 사용한다고 판단하고 시작
            // request 를 통해 받아온 값은 email 이기 때문에 @ 을 포함하고 있다.
            // 그래서 validHeader 를 통한 결과값이 회원 이메일을 받아온 것이므로 회원 기준으로 시작
            User user = userRepository.findByEmail(token).orElseThrow(
                    () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
            );
            result.put(GamerEnum.ID.key(), user.getId().toString());                // 회원 Id 를 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.NICK.key(), user.getNickname());                   // 회원 닉네임을 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.IMG.key(), user.getImgUrl());                      // 회원 img url 을 key 값으로 value 추출 해서 result 에 주입
        } else if (token.startsWith("Bearer ")) {
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
            // 4. 위의 분기에 해당하지 않을 경우에는 guest 라고 판단하고 시작
            token = URLDecoder.decode(token, StandardCharsets.UTF_8);               // 비회원의 토큰 정보를 얻기 위해서 디코딩

            // 게스트의 원하는 정보를 뽑아서 사용하기 위해서 배열에다가 하나씩 넣어준다.
            // guestInfo 예시 형태 : "10001,유저닉네임"
            String[] guestInfo = token.split(",");

            // 게스트 유정 Redis DB 에 존재하는지 확인(검증)
            Optional<Guest> guest = guestRepository.findByGuestId(guestInfo[0]);
            log.info(">>>>>> UserService 의 비회원 인증 결과 / guestId : " + guest.get().getId());
            log.info(">>>>>> UserService 의 비회원 인증 결과 / guestNick : " + guest.get().getNickname());
            log.info(">>>>>> UserService 의 비회원 인증 결과 / guestImg : " + guest.get().getImgUrl());
            result.put(GamerEnum.ID.key(), guestInfo[0]);                           // guest Id 를 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.NICK.key(), guestInfo[1]);                         // guest 닉네임을 key 값으로 value 추출 해서 result 에 주입
            result.put(GamerEnum.IMG.key(), guestInfo[2]);                          // guest img url 을 key 값으로 value 추출 해서 result 에 주입
        }
        return result;
    }


    // 회원탈퇴
    public void deleteUser(User user) {

    }

    // 랜덤 닉네임 발급
    public MsgResponseDto RandomNick() {
        int num = (int) (Math.random() * 1000 +1);
        RandomNick randomNick = randomNickRepository.findByNum(num).orElseThrow(
                () -> new CustomException(StatusMsgCode.NOT_FOUND_NICK));
        String nickname = randomNick.getNickname();

        return new MsgResponseDto(HttpStatus.OK.value(), nickname);
    }

    // 랜덤 이미지 불러오기
    public MsgResponseDto getRandomThumbImg() {
        IMG_NUM = (IMG_NUM == IMG_MAXIMUM) ? 1 : IMG_NUM + 1;
        ThumbImg thumbImg = thumbImgRepository.findById(IMG_NUM).orElseThrow(
                () -> new CustomException(StatusMsgCode.IMAGE_NOT_FOUND));
        String thumbImgResult = thumbImg.getImgUrl();

        return new MsgResponseDto(HttpStatus.OK.value(), thumbImgResult);
    }

    // 회원 정보 조회
    public DataMsgResponseDto getGamerInfo(HttpServletRequest request) {
        HashMap<String, String> extInfo;
        try{
            String header = validHeader(request);
            extInfo = gamerInfo(header);
        } catch(Exception e){
            throw new CustomException(StatusMsgCode.NECESSARY_LOG_IN);
        }
        return new DataMsgResponseDto(StatusMsgCode.OK, extInfo);
    }
}
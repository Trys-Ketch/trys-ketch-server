package com.project.trysketch.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.redis.dto.NonMemberNickRequestDto;
import com.project.trysketch.redis.service.RedisService;
import com.project.trysketch.user.dto.SignInRequestDto;
import com.project.trysketch.user.dto.SignUpRequestDto;
import com.project.trysketch.user.service.KakaoService;
import com.project.trysketch.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

// 1. 기능    : 유저 컨트롤러
// 2. 작성자  : 서혁수, 황미경 (OAuth2.0 카카오톡 로그인 부분)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final KakaoService kakaoService;
    private final RedisService redisService;

    // 회원가입
    @PostMapping("/sign-up")
    public ResponseEntity<MsgResponseDto> signup(@RequestBody SignUpRequestDto requestDto) {
        userService.signUp(requestDto);
        return ResponseEntity.ok(new MsgResponseDto(HttpStatus.OK.value(), "회원가입 성공!"));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<MsgResponseDto> login(@RequestBody SignInRequestDto dto, HttpServletResponse response) {
        userService.login(dto, response);
        return ResponseEntity.ok(new MsgResponseDto(HttpStatus.OK.value(), "로그인 성공!"));
    }

    // 이메일 중복 확인
    @PostMapping("/email-check")
    public ResponseEntity<MsgResponseDto> emailCheck(@RequestBody @Valid SignUpRequestDto requestDto) {
        return userService.dupCheckEmail(requestDto);
    }

    // 닉네임 중복 확인
    @PostMapping("/nick-check")
    public ResponseEntity<MsgResponseDto> nickCheck(@RequestBody SignUpRequestDto requestDto) {
        return userService.dupCheckNick(requestDto);
    }

    // OAuth2.0 카카오톡 로그인
    @GetMapping("/kakao/callback")
    public ResponseEntity<MsgResponseDto> kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {

/*        // code: 카카오 서버로부터 받은 인가 코드
        String createToken = kakaoService.kakaoLogin(code, response);
        // Cookie 생성 및 직접 브라우저에 Set
        Cookie cookie = new Cookie(JwtUtil.AUTHORIZATION_HEADER, createToken.substring(7));
        cookie.setPath("/");
        response.addCookie(cookie);*/


        // code: 카카오 서버로부터 받은 인가 코드
        kakaoService.kakaoLogin(code, response);
        return ResponseEntity.ok(new MsgResponseDto(StatusMsgCode.LOG_IN));
    }

    // ======================== 여기서 부터는 비회원 관련입니다. ========================
    // 비회원 로그인
    @PostMapping("/guest")
    public ResponseEntity<MsgResponseDto> guestLogin(HttpServletRequest request, HttpServletResponse response, @RequestBody NonMemberNickRequestDto requestDto) throws IOException, ParseException {
        redisService.guestLogin(request, response, requestDto);
        return ResponseEntity.ok(new MsgResponseDto(HttpStatus.OK.value(), "비회원 로그인 성공!"));
    }

    // 랜덤 닉네임 받아오는 부분
    @GetMapping("/random-nick")
    public ResponseEntity<MsgResponseDto> guestNick() {
        String nickname = userService.RandomNick();
        return ResponseEntity.ok(new MsgResponseDto(HttpStatus.OK.value(), nickname));
    }

    @GetMapping("/cookie-get")
    public ResponseEntity<MsgResponseDto> getCookie(HttpServletRequest request, HttpServletResponse response) {
        userService.getCookie(request, response);
        return null;
    }

}


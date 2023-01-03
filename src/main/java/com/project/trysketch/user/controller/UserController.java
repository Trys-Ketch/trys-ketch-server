package com.project.trysketch.user.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.user.dto.SignInRequestDto;
import com.project.trysketch.user.dto.SignUpRequestDto;
import com.project.trysketch.user.service.KakaoService;
import com.project.trysketch.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

// 1. 기능    : 유저 컨트롤러
// 2. 작성자  : 서혁수, 황미경 (OAuth2.0 카카오톡 로그인 부분)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final KakaoService kakaoService;

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
        // code: 카카오 서버로부터 받은 인가 코드
        String createToken = kakaoService.kakaoLogin(code, response);

        // Cookie 생성 및 직접 브라우저에 Set
        Cookie cookie = new Cookie(JwtUtil.AUTHORIZATION_HEADER, createToken.substring(7));
        cookie.setPath("/");
        response.addCookie(cookie);

        return ResponseEntity.ok(new MsgResponseDto(StatusMsgCode.LOG_IN));
    }

    // 토큰 재발행
/*    @GetMapping("/issue/token")
    public ResponseEntity<?> issuedToken(@AuthenticationPrincipal UserDetailsImpl userDetails, HttpServletResponse response) {
        return userService.issuedToken(userDetails.getUser().getEmail(), userDetails.getUser().getNickname(), response);
    }*/

    // 로그아웃
/*    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return userService.signOut(userDetails.getUser().getNickname());
    }*/

}


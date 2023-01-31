package com.project.trysketch.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.trysketch.entity.User;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.dto.request.GuestNickRequestDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.service.*;
import com.project.trysketch.dto.request.SignInRequestDto;
import com.project.trysketch.dto.request.SignUpRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

// 1. 기능    : 유저 컨트롤러
// 2. 작성자  : 서혁수, 황미경 (OAuth2.0 카카오톡 로그인 부분)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class UserController {

    private final UserService userService;
    private final KakaoService kakaoService;
    private final NaverService naverService;
    private final GuestService guestService;
    private final GoogleService googleService;

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

    // OAuth2.0 카카오톡 로그인
    @GetMapping("/kakao/callback")
    public ResponseEntity<MsgResponseDto> kakaoLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        return ResponseEntity.ok(kakaoService.kakaoLogin(code, response));
    }

    // OAuth2.0 네이버 로그인
    @GetMapping("/naver/callback")
    public ResponseEntity<MsgResponseDto> naverLogin(@RequestParam String code, @RequestParam String state, HttpServletResponse response) throws JsonProcessingException {
        return ResponseEntity.ok(naverService.naverLogin(code, state, response));
    }

    // OAuth2.0 구글 로그인
    @GetMapping("/google/callback")
    public ResponseEntity<MsgResponseDto> googleLogin(@RequestParam String code, HttpServletResponse response) throws JsonProcessingException {
        return ResponseEntity.ok(googleService.googleLogin(code, response));
    }
    // ======================== 여기서 부터는 비회원 관련입니다. ========================
    // 비회원 로그인
    @PostMapping("/guest")
    public ResponseEntity<MsgResponseDto> guestLogin(HttpServletResponse response, @RequestBody @Valid GuestNickRequestDto requestDto) {
        return ResponseEntity.ok(guestService.guestLogin(response, requestDto));
    }

    // 랜덤 닉네임 받아오는 부분
    @GetMapping("/random-nick")
    public ResponseEntity<MsgResponseDto> guestNick() {
        return ResponseEntity.ok(userService.RandomNick());
    }

    // 랜덤 이미지 받아오는 부분
    @GetMapping("/random-img")
    public ResponseEntity<MsgResponseDto> randomImg() {
        return ResponseEntity.ok(userService.getRandomThumbImg());
    }

    // ======================== 회원 & 비회원 정보 조회 ========================
    // 회원 & 비회원 정보 조회
    @GetMapping("/user-info")
    public ResponseEntity<DataMsgResponseDto> userInfo(HttpServletRequest request) {
        return ResponseEntity.ok(userService.getGamerInfo(request));
    }
}


package com.project.trysketch.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.project.trysketch.dto.request.UserRequestDto;
import com.project.trysketch.dto.response.DataMsgResponseDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final GuestService guestService;
    private final SocialLoginService socialLoginService;
    private final String naver = "naver";
    private final String google = "google";
    private final String kakao = "kakao";

    // OAuth2.0 카카오톡 로그인
    @GetMapping("/kakao/callback")
    public ResponseEntity<DataMsgResponseDto> kakaoLogin(@RequestParam String code, HttpServletResponse response, HttpServletRequest request) throws JsonProcessingException {
        return ResponseEntity.ok(socialLoginService.socialLogin(kakao, code, null, response, request));
    }

    // OAuth2.0 네이버 로그인
    @GetMapping("/naver/callback")
    public ResponseEntity<DataMsgResponseDto> naverLogin(@RequestParam String code, @RequestParam String state, HttpServletResponse response, HttpServletRequest request) throws JsonProcessingException {
        return ResponseEntity.ok(socialLoginService.socialLogin(naver, code, state, response, request));
    }

    // OAuth2.0 구글 로그인
    @GetMapping("/google/callback")
    public ResponseEntity<DataMsgResponseDto> googleLogin(@RequestParam String code, HttpServletResponse response, HttpServletRequest request) throws JsonProcessingException {
        return ResponseEntity.ok(socialLoginService.socialLogin(google, code, null, response, request));
    }

    @GetMapping("issue/token")
    public void issueToken (HttpServletRequest request, HttpServletResponse response) {
        userService.issuedToken(request, response);
    }

    // 비회원 로그인
    @PostMapping("/guest")
    public ResponseEntity<DataMsgResponseDto> guestLogin(HttpServletResponse response, @RequestBody @Valid UserRequestDto requestDto) {
        guestService.guestLogin(response, requestDto);
        return ResponseEntity.ok(new DataMsgResponseDto(StatusMsgCode.LOG_IN));
    }

    // 랜덤 닉네임 받아오는 부분
    @GetMapping("/random-nick")
    public ResponseEntity<DataMsgResponseDto> guestNick() {
        return ResponseEntity.ok(new DataMsgResponseDto(StatusMsgCode.OK, userService.getRandomNick()));
    }

    // 랜덤 이미지 받아오는 부분
    @GetMapping("/random-img")
    public ResponseEntity<DataMsgResponseDto> randomImg() {
        return ResponseEntity.ok(new DataMsgResponseDto(StatusMsgCode.OK, userService.getRandomThumbImg()));
    }

    // 회원 & 비회원 정보 조회
    @GetMapping("/user-info")
    public ResponseEntity<DataMsgResponseDto> userInfo(HttpServletRequest request) {
        return ResponseEntity.ok(new DataMsgResponseDto(StatusMsgCode.OK, userService.getUserInfo(request)));
    }

    // 로그아웃
    @GetMapping("/logout")
    public ResponseEntity<DataMsgResponseDto> logout(HttpServletRequest request, HttpServletResponse response) {
        userService.logout(request, response);
        return ResponseEntity.ok(new DataMsgResponseDto(StatusMsgCode.OK));
    }
}


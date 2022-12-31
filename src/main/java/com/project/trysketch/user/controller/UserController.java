package com.project.trysketch.user.controller;

import com.project.trysketch.global.dto.ResponseMsgDto;
import com.project.trysketch.global.security.UserDetailsImpl;
import com.project.trysketch.user.dto.SigninRequestDto;
import com.project.trysketch.user.dto.SignUpRequestDto;
import com.project.trysketch.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // 회원가입
    @PostMapping("/sign-up")
    public ResponseEntity<ResponseMsgDto> signup(@RequestBody SignUpRequestDto requestDto) {
        userService.signUp(requestDto);
        return ResponseEntity.ok(new ResponseMsgDto(HttpStatus.OK.value(), "회원가입 성공!"));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<ResponseMsgDto> login(@RequestBody SigninRequestDto dto, HttpServletResponse response) {
        userService.login(dto, response);
        return ResponseEntity.ok(new ResponseMsgDto(HttpStatus.OK.value(), "로그인 성공!"));
    }

    // 이메일 중복 확인
    @PostMapping("/email-check")
    public ResponseEntity<ResponseMsgDto> emailCheck(@RequestBody @Valid SignUpRequestDto requestDto) {
        return userService.dupCheckEmail(requestDto);
    }

    // 닉네임 중복 확인
    @PostMapping("/nick-check")
    public ResponseEntity<ResponseMsgDto> nickCheck(@RequestBody SignUpRequestDto requestDto) {
        return userService.dupCheckNick(requestDto);
    }

    // 토큰 재발행
    @GetMapping("/issue/token")
    public ResponseEntity<?> issuedToken(@AuthenticationPrincipal UserDetailsImpl userDetails, HttpServletResponse response) {
        return userService.issuedToken(userDetails.getUser().getEmail(), userDetails.getUser().getNickname(), response);
    }

    // 로그아웃
    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return userService.signOut(userDetails.getUser().getNickname());
    }

}


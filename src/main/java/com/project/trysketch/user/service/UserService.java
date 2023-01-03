package com.project.trysketch.user.service;

import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.user.dto.SignUpRequestDto;
import com.project.trysketch.user.dto.SignInRequestDto;
import com.project.trysketch.user.entity.User;
import com.project.trysketch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import javax.servlet.http.HttpServletResponse;

// 1. 기능   : 유저 비즈니스 로직
// 2. 작성자 : 서혁수
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

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
        User user = new User(requestDto.getEmail(), requestDto.getNickname(), encodePassword);

        // 4. DB 에 새로운 유저정보 넣어주기
        userRepository.save(user);
    }

    // 폼 로그인
    public void login(SignInRequestDto requestDto, HttpServletResponse response) {
        // 1. 유저 이메일 기준으로 유저 정보 찾아와서
        User user = userRepository.findByEmail(requestDto.getEmail()).orElseThrow(
                () -> new CustomException(StatusMsgCode.EXIST_USER)
        );
        // 2. 비밀번호호가 일치하는지 검증한다.
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new CustomException(StatusMsgCode.INVALID_PASSWORD);
        }

        // 3. 로그인 성공 및 토큰을 발급받아서 가져온다.
        response.addHeader(JwtUtil.AUTHORIZATION_HEADER, jwtUtil.createToken(user.getEmail(), user.getNickname()));
    }

    // 중복 이메일 체크
    public ResponseEntity<MsgResponseDto> dupCheckEmail(SignUpRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            return ResponseEntity.badRequest().body(new MsgResponseDto(HttpStatus.BAD_REQUEST.value(), "중복 이메일 존재"));
        } else {
            return ResponseEntity.ok(new MsgResponseDto(HttpStatus.OK.value(), "사용 가능한 이메일입니다."));
        }
    }

    // 중복 닉네임 체크
    public ResponseEntity<MsgResponseDto> dupCheckNick(SignUpRequestDto dto) {
        if (userRepository.existsByNickname(dto.getNickname())) {
            return ResponseEntity.badRequest().body(new MsgResponseDto(HttpStatus.BAD_REQUEST.value(), "중복 닉네임 존재"));
        } else {
            return ResponseEntity.ok(new MsgResponseDto(HttpStatus.OK.value(), "사용 가능한 닉네임입니다."));
        }
    }

    // 회원탈퇴
    public void deleteUser(User user) {

    }
}

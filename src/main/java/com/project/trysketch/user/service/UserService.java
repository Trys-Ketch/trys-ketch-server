package com.project.trysketch.user.service;

import com.project.trysketch.global.dto.ResponseMsgDto;
import com.project.trysketch.global.dto.TokenDto;
import com.project.trysketch.global.entity.RefreshToken;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.global.repository.RefreshTokenRepository;
import com.project.trysketch.user.dto.SignUpRequestDto;
import com.project.trysketch.user.dto.SigninRequestDto;
import com.project.trysketch.user.dto.TestDto;
import com.project.trysketch.user.entity.User;
import com.project.trysketch.user.entity.UserRoleEnum;
import com.project.trysketch.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.servlet.http.HttpServletResponse;
import java.util.Optional;

// 1. 기능   : 유저 서비스
// 2. 작성자 : 서혁수
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    // 회원가입
    public void signUp(SignUpRequestDto requestDto) {
        // 1. 중복 여부 검사
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new IllegalArgumentException("중복 이메일 존재");
        }
        if (userRepository.existsByNickname(requestDto.getNickname())) {
            throw new IllegalArgumentException("중복 닉네임 존재");
        }

        // 2. password 값 인코딩 후 Dto 에 재주입 후 Dto 를 Entity 로 변환
        String encodePassword = passwordEncoder.encode(requestDto.getPassword());

        // 3. 새로운 객체 생성 및 db 에 저장
        User user = new User(requestDto.getEmail(), requestDto.getNickname(), encodePassword, UserRoleEnum.USER);
        userRepository.save(user);
    }

    // 폼 로그인
    @Transactional
    public void login(SigninRequestDto requestDto, HttpServletResponse response) {
        // 1. userId 로 user 정보 호출
        User user = userRepository.findByEmail(requestDto.getEmail()).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 이메일 입니다.")
        );

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        // 2. email 과 nickname 값을 포함한 토큰 생성 후 tokenDto 에 저장
        TokenDto tokenDto = jwtUtil.createAllToken(requestDto.getEmail(), user.getNickname());

        // 3. nickname 값에 해당하는 refreshToken 을 DB 에서 가져옴
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserNickname(user.getNickname());

        // 4. isPresent() 메소드는 Optional 객체의 값이 null 인지 여부
        if (refreshToken.isPresent()) {
            // 5. rsToken 이 null 즉, 존재하지 않으면 새롭게 발급해서 db에 저장
            refreshTokenRepository.save(refreshToken.get().updateToken(tokenDto.getRefreshToken()));
        } else {
            // 6. rsToken 이 존재할 경우 새롭게 발급한 rsToken 으로 다시 db 에 저장
            RefreshToken newToken = new RefreshToken(tokenDto.getRefreshToken(), user.getNickname());
            refreshTokenRepository.save(newToken);
        }

        // 7. 현재 헤더에 새롭게 지정해준다.
        setHeader(response, tokenDto);

    }

    // 토큰 재발행 (전역으로 사용)
    public ResponseEntity<TestDto> issuedToken(String email, String nickname, HttpServletResponse response) {
        response.addHeader(JwtUtil.AC_TOKEN, jwtUtil.createAcToken(email, nickname));
        return ResponseEntity.ok().body(new TestDto(200, "토근 재발행 완료"));
    }

    // 중복 이메일 체크
    public ResponseEntity<ResponseMsgDto> dupCheckEmail(SignUpRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            return ResponseEntity.badRequest().body(new ResponseMsgDto(HttpStatus.BAD_REQUEST.value(), "중복 이메일 존재"));
        } else {
            return ResponseEntity.ok(new ResponseMsgDto(HttpStatus.OK.value(), "사용 가능한 이메일입니다."));
        }
    }

    // 중복 닉네임 체크
    public ResponseEntity<ResponseMsgDto> dupCheckNick(SignUpRequestDto dto) {
        if (userRepository.existsByNickname(dto.getNickname())) {
            return ResponseEntity.badRequest().body(new ResponseMsgDto(HttpStatus.BAD_REQUEST.value(), "중복 닉네임 존재"));
        } else {
            return ResponseEntity.ok(new ResponseMsgDto(HttpStatus.OK.value(), "사용 가능한 닉네임입니다."));
        }
    }

    // Http 헤더에 토큰값 지정
    public void setHeader(HttpServletResponse response, TokenDto tokenDto) {
        response.addHeader(JwtUtil.AC_TOKEN, tokenDto.getAccessToken());
        response.addHeader(JwtUtil.RS_TOKEN, tokenDto.getRefreshToken());
    }

    // 로그아웃
    @Transactional
    public ResponseEntity<TestDto> signOut(String nickname) {
        // 1. 해당 유저의 refreshToken 이 없을 경우
        if (refreshTokenRepository.findByUserNickname(nickname).isEmpty()) {
            return ResponseEntity.badRequest().body(new TestDto(400, "로그인을 해주세요."));
        }
        // 자신의 refreshToken 만 삭제 가능
        // 2. 해당 유저의 토큰으로 부터 닉네임을 가져오는 코드
        String findNick = refreshTokenRepository.findByUserNickname(nickname).get().getUserNickname();
        if (nickname.equals(findNick)) {
            refreshTokenRepository.deleteByUserNickname(nickname);
            return ResponseEntity.ok().body(new TestDto(200, "로그아웃 완료"));
        } else {
            return ResponseEntity.accepted().body(new TestDto(401, "삭제 권한이 없습니다."));
        }
    }

    // 회원탈퇴
    public void deleteUser(User user) {

    }
}

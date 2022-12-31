package com.project.trysketch.global.jwt;

import com.project.trysketch.global.dto.TokenDto;
import com.project.trysketch.global.entity.RefreshToken;
import com.project.trysketch.global.repository.RefreshTokenRepository;
import com.project.trysketch.global.security.UserDetailsServiceImpl;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

// 1. 기능   : JWT 로직
// 2. 작성자 : 서혁수
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {
    // 헤더에 설정 사항
    public static final String AC_TOKEN = "acToken";
    public static final String RS_TOKEN = "rsToken";
    private static final String BEARER_PREFIX = "Bearer ";

    // 만료시간
    private static final long AC_TOKEN_TIME = 60 * 60 * 1000L;          // 1시간
    private static final long RS_TOKEN_TIME = 60 * 60 * 24 * 1000L;     // 24시간

    // 시크릿 키
    @Value("${jwt.secret.key}")
    private String secretKey;
    private Key key;
    // 암호화 방식
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final RefreshTokenRepository refreshTokenRepository;

    // 초기화
    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // accessToken 토큰 생성
    public String createAcToken(String userEmail, String nickname) {
        Date now = new Date();

        return BEARER_PREFIX + Jwts.builder()
                .claim("userEmail", userEmail)
                .claim("nickname", nickname)
                .setExpiration(new Date(now.getTime() + AC_TOKEN_TIME))
                .setIssuedAt(now)
                .signWith(key, signatureAlgorithm)
                .compact();
    }

    // refreshToken 토큰 생성
    public String createRsToken(String userEmail, String nickname) {
        Date now = new Date();
        return BEARER_PREFIX + Jwts.builder()
                .claim("userEmail", userEmail)
                .claim("nickname", nickname)
                .setExpiration(new Date(now.getTime() + RS_TOKEN_TIME))
                .setIssuedAt(now)
                .signWith(key, signatureAlgorithm)
                .compact();
    }

    // accessToken 유효 토큰부분 자르기
    public String resolveAcToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AC_TOKEN);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // refreshToken 유효 토큰부분 자르기
    public String resolveRsToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(RS_TOKEN);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    // 토큰 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.info("Invalid JWT signature, 유효하지 않는 JWT 서명 입니다.");
        } catch (ExpiredJwtException e) {
//            return false;
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }
        return false;
    }

    // 토큰 생성
    public TokenDto createAllToken(String email, String nickname) {
        return new TokenDto(createAcToken(email, nickname), createRsToken(email, nickname));
    }

    // refreshToken 검증
    public boolean rsTokenValidation(String token) {
        // 1차 토큰 검증
        if(!validateToken(token))
            return false;

        // DB 에 저장한 토큰 비교
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByUserNickname(getNickInfoFromToken(token));

        return refreshToken.isPresent() && token.equals(refreshToken.get().getRefreshToken());
    }

    // 인증 객체를 실제로 만드는 부분
    //  - UsernamePasswordAuthenticationToken : 인증용 객체 생성(사용자 인증)
    public Authentication createAuthentication(String nickname) {
        if (refreshTokenRepository.findByUserNickname(nickname).isEmpty()) {
            throw new IllegalArgumentException("Token Error");
        }
        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(nickname);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    // 토큰에서 사용자 닉네임 가져오기
    public String getNickInfoFromToken(String token) {
        return (String) Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody().get("nickname");
    }

    // 토큰에서 사용자 정보 가져오기
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }

    // RefreshToken 존재 유무 확인
    public boolean existRsToken(String rsToken) {
        return refreshTokenRepository.existsByRefreshToken(rsToken);
    }

//    public void setHeaderAcToken(HttpServletResponse response, String acToken) {
//        response.setHeader(AC_TOKEN, acToken);
//    }
}

package com.project.trysketch.global.jwt;

import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
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

// 1. 기능   : JWT 로직
// 2. 작성자 : 서혁수
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    // 헤더에 설정 사항
    public static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // 만료시간
    private static final long TOKEN_TIME = 60 * 60 * 11 * 1000L;        // 11시간

    // 시크릿 키
    @Value("${jwt.secret.key}")
    private String secretKey;

    private final UserDetailsServiceImpl userDetailsServiceImpl;

    // 암호화 방식
    private final SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

    private Key key;

    // 초기화
    @PostConstruct
    public void init() {
        byte[] bytes = Base64.getDecoder().decode(secretKey);
        key = Keys.hmacShaKeyFor(bytes);
    }

    // 토큰 생성
    public String createToken(String userEmail, String nickname) {
        Date date = new Date();
        return BEARER_PREFIX +
                Jwts.builder()
                        .claim("email", userEmail)
                        .claim("nickname", nickname)
                        .setExpiration(new Date(date.getTime() + TOKEN_TIME))
                        .setIssuedAt(date)
                        .signWith(key, signatureAlgorithm)
                        .compact();
    }

    public String createRsToken(String rt) {
        return BEARER_PREFIX +
                Jwts.builder()
                        .claim("nickname", "test")
                        .signWith(key, signatureAlgorithm)
                        .compact();
    }

    // 유효 토큰부분 자르기
    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
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
            log.info("Expired JWT token, 만료된 JWT token 입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token, 지원되지 않는 JWT 토큰 입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT claims is empty, 잘못된 JWT 토큰 입니다.");
        }
        return false;
    }

    // 토큰에서 사용자 정보 가져오기
    public Claims getUserInfoFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }


    // 인증 객체를 실제로 만드는 부분
    public Authentication createAuthentication(String email) {
        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }


    // request 에서 유저 정보 가져오기
    public Claims authorizeToken(HttpServletRequest request) {

        String token = resolveToken(request);
        Claims claims;

        if (token != null) {
            if (validateToken(token)) {
                claims = getUserInfoFromToken(token);
                return claims;
            } else
                throw new CustomException(StatusMsgCode.INVALID_AUTH_TOKEN);
        }
        return null;
    }
}

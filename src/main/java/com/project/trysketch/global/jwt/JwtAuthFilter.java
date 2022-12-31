package com.project.trysketch.global.jwt;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


// 1. 기능   : JWT 인증 필터 / 토큰 유효성 검사
// 2. 작성자 : 서혁수
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 1. 요청에서 받아온 토큰의 문자열 받기
        //  - accessToken, refreshToken 식별자 제거해서 가져오기
        String acToken = jwtUtil.resolveAcToken(request);
        String rsToken = jwtUtil.resolveRsToken(request);

        // 2. 토큰 유효 판별
        // accessToken 이 존재하면 시작
        if (acToken != null) {
            // 3. accessToken 검증에 실패하고, refreshToken 이 존재할 시
            if (!jwtUtil.validateToken(acToken) && rsToken != null) {
                // 4. refreshToken 토큰 검증 및 존재여부 확인
                boolean validRsToken = jwtUtil.validateToken(rsToken);
                boolean isRsToken = jwtUtil.existRsToken("Bearer " + rsToken);

                // 5. 위 두가지 검증과정을 모두 만족시 로그아웃 없이 토큰 재발급을 진행
                if (validRsToken && isRsToken) {
                    // 6. JWT 를 이용해 전송되는 암호화된 정보인 claims 에 rsToken 정보를 담는다.
                    //  - 이 rsToken 의 정보를 담은 claims 를 이용해서 새로운 acToken 발급
                    Claims claims = jwtUtil.getUserInfoFromToken(rsToken);
                    String newToken = jwtUtil.createAcToken((String) claims.get("userEmail"), (String) claims.get("nickname"));

                    // 7. 새로이 발급 받은 토큰을 이용해서 새롭게 권한 설정을 진행
                    String cut = newToken.substring(7);
                    setAuthentication(jwtUtil.getNickInfoFromToken(cut));

                    // 8. 다음 필터로 넘어간다.
                    filterChain.doFilter(request, response);
                    return;
                }
            } else if (!jwtUtil.validateToken(acToken)) {
                // 9. 위의 상황이 아닌 경우 예외처리
                throw new IllegalArgumentException("Access Token Error");
            }
            // 10. 토큰이 유효하다면 토큰에서 정보를 가져와 acToken 에 셋팅한다.
            setAuthentication(jwtUtil.getNickInfoFromToken(acToken));
        } else if (rsToken != null) {
            if (!jwtUtil.rsTokenValidation(rsToken)) {
                throw new IllegalArgumentException("Refresh Token Error");
            }
            setAuthentication(jwtUtil.getNickInfoFromToken(rsToken));
        }
        // 11. 다음 필터로 넘어간다
        filterChain.doFilter(request, response);
    }

    // https://www.inflearn.com/questions/295550/securitycontextholder-getcontext-setauthentication-authentication
    // 권한 설정(등록)
    //  - 로그인 생성 후에 토큰을 스레드내 인증정보 저장소 역할을 하는 SecurityContextHolder 에 저장하고 사용
    //  - 요청이 인증되면, 그 인증은 보통 사용 중인 인증 매커니즘에 의해 SecurityContextHolder 에 저장
    //  - Authentication 이 가지고 있는 authenticated 속성이 true 가 아니면 발견될 때 마다 계속 인증한다.
    public void setAuthentication(String nickname) {
        Authentication authentication = jwtUtil.createAuthentication(nickname);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

//    public void jwtExceptionHandler(HttpServletResponse response, String msg, HttpStatus status) {
//        response.setStatus(status.value());
//        response.setContentType("application/json");
//        try {
//            String json = new ObjectMapper().writeValueAsString(new TestDto(status.value(), msg));
//            response.getWriter().write(json);
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
//    }
}

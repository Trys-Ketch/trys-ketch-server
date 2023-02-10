package com.project.trysketch.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Component
public class SocialLoginVendor {

    // kakao
    @Value("${kakao.oauth2.client.id}")
    private String kakaoClientId;
    @Value("${kakao.oauth2.client.redirect.uri}")
    private String kakaoRedirectUri;
    @Value("${kakao.oauth2.client.provider.user-info-uri}")
    private String kakaoUserInfoUri;
    @Value("${kakao.oauth2.client.provider.token-uri}")
    private String kakaoTokenUri;

    // naver
    @Value("${spring.security.oauth2.client.registration.naver.client-id}")
    private String naverClientId;
    @Value("${spring.security.oauth2.client.registration.naver.client-secret}")
    private String naverClientSecret;
    @Value("${spring.security.oauth2.client.registration.naver.redirect-uri}")
    private String naverRedirectUri;
    @Value("${spring.security.oauth2.client.provider.naver.user-info-uri}")
    private String naverUserInfoUri;
    @Value("${spring.security.oauth2.client.provider.naver.token-uri}")
    private String naverTokenUri;

    // google
    @Value("${google.oauth2.client.id}")
    private String googleClientId;
    @Value("${google.oauth2.client.secret}")
    private String googleClientSecret;
    @Value("${google.oauth2.client.provider.user-info-uri}")
    private String googleUserInfoUri;
    @Value("${google.oauth2.client.redirect.uri}")
    private String googleRedirectUri;
    @Value("${google.oauth2.client.provider.token.uri}")
    private String googleTokenUri;

}

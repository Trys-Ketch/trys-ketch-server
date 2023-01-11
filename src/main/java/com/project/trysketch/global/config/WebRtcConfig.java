package com.project.trysketch.global.config;

import com.project.trysketch.global.rtc.SignalingHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// 1. 기능   : WebRtc 설정
// 2. 작성자 : 안은솔

@Configuration
@EnableWebSocket // 웹 소켓에 대해 자동 설정
public class WebRtcConfig implements WebSocketConfigurer {

    /* TODO WebRTC 관련 */

    // signal 로 요청이 왔을 때 아래의 signalingSocketHandler 가 동작하도록 registry 에 설정
    // 요청은 클라이언트 접속, close, 메시지 발송 등에 대해 특정 메서드를 호출한다
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signalingSocketHandler(), "/signal")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Bean
    public org.springframework.web.socket.WebSocketHandler signalingSocketHandler() {
        return new SignalingHandler();
    }
}
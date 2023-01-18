package com.project.trysketch.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// 1. 기능   : WebSocket 설정
// 2. 작성자 : 안은솔
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableWebSocketMessageBroker // 문자 채팅용
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    // 웹 소켓 연결을 위한 엔드포인트 설정 및 stomp sub/pub 엔드포인트 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // stomp 접속 주소 url => /ws/chat
        registry.addEndpoint("/ws")      // 연결될 Endpoint
                .setAllowedOriginPatterns("*")  // CORS 설정
                .withSockJS()                   // SockJS 설정
                .setHeartbeatTime(1000);        // 연결상태 확인 주기
        log.info(">>>>>>>[ws] 웹소켓 연결 : {}", registry);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지를 구독하는 요청 url => 즉 메시지 받을 때
        registry.enableSimpleBroker("/queue", "/topic");

        // 메시지를 발행하는 요청 url => 즉 메시지 보낼 때
        registry.setApplicationDestinationPrefixes("/app");
        log.info(">>>>>>>[ws] 메시지 브로커 : {}", registry);
    }

}

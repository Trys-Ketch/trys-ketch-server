package com.project.trysketch.chatting;

import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompHandler implements ChannelInterceptor {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    // ChannelInterceptor 인터페이스를 구축해서 인증과정을 거침
    // Websocket sub-protocol 인증을 지원하는데 메세지 안의 유저 헤더를 이용
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        // 메세지의 페이로드 및 헤더에서 인스턴스 생성
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        log.info(">>>>>>> 위치 : StompHandler 의 preSend 메서드 / accessor : {}", accessor);

        // 연결 시도할 때
        if (StompCommand.CONNECT == accessor.getCommand()) {
            String token = null;

            // getFirstNativeHeader 를 통해서 웹소켓 요청시 헤더값의 토큰을 가져올 수 있음
            if (Objects.requireNonNull(accessor.getFirstNativeHeader("Authorization")).equals("Authorization")) {
                token = accessor.getFirstNativeHeader("Authorization");

                String check = Objects.requireNonNull(token).substring(7);
                jwtUtil.validateToken(check);
            } else {
                token = Objects.requireNonNull(accessor.getFirstNativeHeader("guest"));
            }

            log.info(">>>>>>> 위치 : StompHandler 의 preSend 메서드 첫번째 if 문 / token : {}", token);

            HashMap<String, String> realToken = userService.gamerInfo(token);

            log.info(">>>>>>> 위치 : StompHandler 의 preSend 메서드 첫번째 if 문 / ID : {}, NICKNAME : {}", realToken.get("id"), realToken.get("nickname"));
        }

        return message;
    }
}
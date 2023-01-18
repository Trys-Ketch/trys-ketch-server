package com.project.trysketch.chatting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final RedisPublisher RedisPublisher;

    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        RedisPublisher.message(message);

        log.info(">>>>>>> 위치 : ChatController 의 message API / 메시지 타입 : {}", message.getType());
        log.info(">>>>>>> 위치 : ChatController 의 message API / 방 번호 : {}", message.getRoomId());
        log.info(">>>>>>> 위치 : ChatController 의 message API / GAMER ID : {}", message.getGamerId());
        log.info(">>>>>>> 위치 : ChatController 의 message API / GAMER NICK : {}", message.getGamerNick());
        log.info(">>>>>>> 위치 : ChatController 의 message API / 메시지 내용 : {}", message.getMessage());
    }
}
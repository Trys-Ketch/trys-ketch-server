package com.project.trysketch.chatting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ChatService {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;

    public void message(ChatMessage message) {
        log.info(">>>>>>> 위치 : ChatService 의 message 메서드 / message : {}", message);

        // Websocket 에 발행된 메시지를 redis 로 발행(publish)
        redisTemplate.convertAndSend(channelTopic.getTopic(), message);

    }
}


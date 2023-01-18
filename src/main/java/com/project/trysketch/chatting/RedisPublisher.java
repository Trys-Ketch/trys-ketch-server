package com.project.trysketch.chatting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Service;

// 1. 기능    : 채팅 Publisher
// 2. 작성자  : 황미경, 서혁수, 안은솔
@Slf4j
@RequiredArgsConstructor
@Service
public class RedisPublisher {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic channelTopic;

    public void message(ChatMessage message) {
        // Websocket 에 발행된 메시지를 redis 로 발행(publish)
        redisTemplate.convertAndSend(channelTopic.getTopic(), message);

        log.info(">>>>>>> 위치 : RedisPublisher 의 channelTopic 메서드 / getTopic() : {}", channelTopic.getTopic());
        log.info(">>>>>>> 위치 : RedisPublisher 의 message 메서드 / 메시지 타입 : {}", message.getType());
        log.info(">>>>>>> 위치 : RedisPublisher 의 message 메서드 / 방 번호 : {}", message.getRoomId());
        log.info(">>>>>>> 위치 : RedisPublisher 의 message 메서드 / GAMER ID : {}", message.getGamerId());
        log.info(">>>>>>> 위치 : RedisPublisher 의 message 메서드 / GAMER NICK : {}", message.getGamerNick());
        log.info(">>>>>>> 위치 : RedisPublisher 의 message 메서드 / 메시지 내용 : {}", message.getMessage());
    }
}


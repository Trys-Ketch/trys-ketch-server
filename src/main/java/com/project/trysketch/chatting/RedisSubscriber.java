package com.project.trysketch.chatting;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

// redis에 메세지 발행이 될 떄까지 대기하다가 메세지가 발행되면 해당 메세지를 읽어 처리하는 리스너
@Slf4j
@RequiredArgsConstructor
@Service
public class RedisSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;


    // Redis 에서 메시지가 발행(publish)되면 대기하고 있던 Redis Subscriber 가 해당 메시지를 받아 처리.
    // 리스너가 대기하고 있다가 메세지 오면 RedisSubscriber.sendMessage 가 수행됨

    public void sendMessage(String publishMessage) {
        try {
            log.info(">>>>>>> 위치 : RedisSubscriber 의 sendMessage 메서드 / publishMessage : {}", publishMessage);

            // JSON 파일을 객체로 deserialization 하기 위해 ObjectMapper 의 readValue() 메서드 사용
            ChatMessage chatMessage = objectMapper.readValue(publishMessage, ChatMessage.class);
            log.info(">>>>>>> 위치 : RedisSubscriber 의 sendMessage 메서드 / chatMessage : {}", chatMessage);

            // 채팅방을 구독한 클라이언트에게 메시지 발송
            messagingTemplate.convertAndSend("/sub/chat/room/" + chatMessage.getRoomId(), chatMessage);
            log.info(">>>>>>> 위치 : RedisSubscriber 의 sendMessage 메서드 / 주소 : /sub/chat/room/{}", chatMessage.getRoomId());

        } catch (Exception e) {
            log.error("Exception {}", e);
        }
    }
}
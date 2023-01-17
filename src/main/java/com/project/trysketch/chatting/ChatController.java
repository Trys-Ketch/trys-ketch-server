package com.project.trysketch.chatting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {
    private final ChatService ChatService;


    @MessageMapping("/chat/message")
    public void message(ChatMessage message) {
        log.info(">>>>>>> 위치 : ChatController 의 message 메서드 / message : {}", message);
        ChatService.message(message);
    }

}
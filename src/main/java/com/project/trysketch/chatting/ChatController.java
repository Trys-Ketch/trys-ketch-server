//package com.project.trysketch.chatting;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.stereotype.Controller;
//
//// 1. 기능    : 채팅 컨트롤러
//// 2. 작성자  : 황미경, 서혁수, 안은솔
//@Slf4j
//@Controller
//@RequiredArgsConstructor
//public class ChatController {
//    private final ChatPublisher ChatPublisher;
//
//    @MessageMapping("/chat/content")
//    public void message(ChatMessage message) {
//        ChatPublisher.message(message);
//
//        log.info(">>>>>>> 위치 : ChatController 의 content API / 메시지 타입 : {}", message.getType());
//        log.info(">>>>>>> 위치 : ChatController 의 content API / 방 번호 : {}", message.getRoomId());
//        log.info(">>>>>>> 위치 : ChatController 의 content API / GAMER ID : {}", message.getUserId());
//        log.info(">>>>>>> 위치 : ChatController 의 content API / GAMER NICK : {}", message.getNickname());
//        log.info(">>>>>>> 위치 : ChatController 의 content API / 메시지 내용 : {}", message.getContent());
//    }
//}
package com.project.trysketch.chatting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import javax.annotation.PostConstruct;
import java.util.List;

// 1. 기능    : 채팅 Room Repository
// 2. 작성자  : 황미경, 서혁수, 안은솔
@Slf4j
@Repository
@RequiredArgsConstructor
public class ChatRoomRepository {
    private static final String CHAT_ROOMS = "CHAT_ROOM";
    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, ChatRoom> opsHashChatRoom;

    // 종속성 주입이 완료된 후 실행되어야 하는 메서드에 사용. 다른 리소스에서 호출하지 않아도 수행됨
    // 생성자가 호출 되었을 때 빈은 아직 초기화 되지 않은 상태이다.
    // 하지만, PostConstruct 를 사용하면 빈이 초기화 됨과 동시에 의존성을 확인할 수 있다.
    // bean lifecycle 에서 오직 한 번만 수행된다는 것을 보장할 수 있다.(말 그대로 Repository 처럼 사용하는 것이 목적)
    @PostConstruct
    private void init() {
        opsHashChatRoom = redisTemplate.opsForHash();
    }

    // 모든 채팅방 조회
    public List<ChatRoom> findAllRoom() {
        return opsHashChatRoom.values(CHAT_ROOMS);
    }

    // 특정 채팅방 조회
    public ChatRoom findRoomById(String chatRoomId) {
        return opsHashChatRoom.get(CHAT_ROOMS, chatRoomId);
    }

    // 채팅룸 생성
    public ChatRoom saveRoom(ChatRoom chatRoom){
        log.info(">>>>>>> 위치 : ChatRoomRepository 의 saveRoom 메서드 / 만들어진 채팅방 ID : {}", chatRoom.getRoomId());
        log.info(">>>>>>> 위치 : ChatRoomRepository 의 saveRoom 메서드 / 만들어진 채팅방 NAME : {}", chatRoom.getName());

        opsHashChatRoom.put(CHAT_ROOMS, chatRoom.getRoomId(), chatRoom);
        return chatRoom;
    }

    // 채팅룸 삭제
    public void deleteRoom(String chatRoomId){
        opsHashChatRoom.delete(CHAT_ROOMS, chatRoomId);
    }
}

package com.project.trysketch.chatting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Repository
public class ChatRoomRepository {

    private static final String CHAT_ROOMS = "CHAT_ROOM";
    private final RedisTemplate<String, Object> redisTemplate;
    private HashOperations<String, String, ChatRoom> opsHashChatRoom;

    @PostConstruct
    private void init() {
        opsHashChatRoom = redisTemplate.opsForHash();
    }

    // 모든 채팅방 조회
    public List<ChatRoom> findAllRoom() {
        return opsHashChatRoom.values(CHAT_ROOMS);
    }

    // 특정 채팅방 조회
    public ChatRoom findRoomById(String id) {
        return opsHashChatRoom.get(CHAT_ROOMS, id);
    }

    // 채팅룸 생성
    public ChatRoom saveRoom(ChatRoom chatRoom){
        log.info(">>>>>>> 위치 : ChatRoomRepository 의 saveRoom 메서드 / chatRoom.getRoomId : {}", chatRoom.getRoomId());
        log.info(">>>>>>> 위치 : ChatRoomRepository 의 saveRoom 메서드 / chatRoom.getName : {}", chatRoom.getName());
        opsHashChatRoom.put(CHAT_ROOMS, chatRoom.getRoomId(), chatRoom);
        return chatRoom;
    }
    // 채팅룸 삭제
    public void deleteRoom(String chatRoomId){
        opsHashChatRoom.delete(CHAT_ROOMS, chatRoomId);
    }
}

package com.project.trysketch.chatting;

import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomService {
    private final ChatRoomRepository chatRoomRepository;

    public MsgResponseDto createChatRoom(String roomId, String name) {
        log.info(">>>>>>> 위치 : ChatRoomService 의 createChatRoom 메서드 / roomId : {}", roomId);
        log.info(">>>>>>> 위치 : ChatRoomService 의 createChatRoom 메서드 / name : {}", name);

        ChatRoom chatRoom = ChatRoom.create(name, roomId);
        log.info(">>>>>>> 위치 : ChatRoomService 의 createChatRoom 메서드 / chatRoom : {}", chatRoom);

        chatRoomRepository.saveRoom(chatRoom);

        return new MsgResponseDto(StatusMsgCode.CREATE_ROOM);

    }

}

package com.project.trysketch.gameroom.service;

import com.project.trysketch.gameroom.dto.request.GameRoomRequestDto;
import com.project.trysketch.gameroom.entity.GameRoom;
import com.project.trysketch.gameroom.entity.GameRoomUser;
import com.project.trysketch.gameroom.repository.GameRoomRepository;
import com.project.trysketch.gameroom.repository.GameRoomUserRepository;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.redis.service.RedisService;
import com.project.trysketch.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.HashMap;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameRoomService {
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomUserRepository gameRoomUserRepository;
    private final RedisService redisService;

    //게임방 생성
    public MsgResponseDto createRoom(GameRoomRequestDto gameRoomRequestDto, User user){

        GameRoom gameRoom = GameRoom.builder()
                .title(gameRoomRequestDto.getTitle())
                .host(user.getNickname())
                .status("false")
                .build();

        //DB에 저장
        gameRoomRepository.save(gameRoom);

        GameRoomUser gameRoomUser = new GameRoomUser(gameRoom,user);
        gameRoomUserRepository.save(gameRoomUser);

//        HashMap<String, String> roomInfo = new HashMap<>();
//
//        roomInfo.put("gameRoomtitle",gameRoom.getTitle());
//        roomInfo.put("roomId", String.valueOf(gameRoom.getId()));
//        roomInfo.put("host",gameRoom.getHost());
//        roomInfo.put("status", gameRoom.getStatus());

        return new MsgResponseDto(200,"방 생성 완료");

    };

    // 테스트중.... by 서혁수
    public void RoomUsersInfo() {
        redisService.getRoomUsers(2L);
    }
}

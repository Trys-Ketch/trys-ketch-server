package com.project.trysketch.service;

import com.project.trysketch.dto.response.GameRoomResponseDto;
import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.repository.GameRoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

// 1. 기능   : 방정보 조회 SSE 서비스
// 2. 작성자 : 황미경
@Slf4j
@RequiredArgsConstructor
@Service
public class SseService {

    private final GameRoomRepository gameRoomRepository;

    // 방 생성 및 퇴장 시 해당 유저 반영 못하므로 +1, -1 할 수 있도록 매개변수로 받음
    public List<GameRoomResponseDto> getRooms(int num){

        // gameRoomResponseDto list 생성
        List<GameRoomResponseDto> gameRoomList = new ArrayList<>();

        // 모든 gameRoom 불러와서 dto에 담기 -> list에 저장
        List<GameRoom> gameRooms = gameRoomRepository.findAll();
        for (GameRoom room : gameRooms){
            GameRoomResponseDto gameRoomResponseDto = GameRoomResponseDto.builder()
                    .id(room.getId())
                    .title(room.getTitle())
                    .hostNick(room.getHostNick())
                    .GameRoomUserCount(room.getGameRoomUserList().size() + num)
                    .isPlaying(room.isPlaying())
                    .createdAt(room.getCreatedAt())
                    .modifiedAt(room.getModifiedAt())
                    .randomCode(room.getRandomCode())
                    .build();
            gameRoomList.add(gameRoomResponseDto);
        }
        return gameRoomList;
    }
}
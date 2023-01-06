package com.project.trysketch.gameroom.controller;

import com.project.trysketch.gameroom.dto.request.GameRoomRequestDto;
import com.project.trysketch.gameroom.dto.response.GameRoomResponseDto;
import com.project.trysketch.gameroom.entity.GameRoom;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.gameroom.service.GameRoomService;
import com.project.trysketch.global.dto.MsgResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    @GetMapping("/rooms")
    public ResponseEntity<List<GameRoomResponseDto>> getAllGameRoom(){
        return ResponseEntity.ok(gameRoomService.getAllGameRoom());
    }

    @PostMapping("/room")
    public ResponseEntity<DataMsgResponseDto> createGameRoom(@RequestBody GameRoomRequestDto gameRoomRequestDto, HttpServletRequest request)
    {
        log.info(">>> 메인페이지 이동 - 방 이름 : {},", gameRoomRequestDto.getTitle());
        return ResponseEntity.ok(gameRoomService.createGameRoom(gameRoomRequestDto, request));
    }

    @PostMapping("/room/{id}")
    public ResponseEntity<MsgResponseDto> enterGameRoom(@PathVariable Long id, HttpServletRequest request){
        log.info(">>> 방 입장 - 방 id : {}, 유저 id : {}", id, request);
        return ResponseEntity.ok(gameRoomService.enterGameRoom(id,request));
    }

}

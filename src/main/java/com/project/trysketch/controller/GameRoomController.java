package com.project.trysketch.controller;

import com.project.trysketch.dto.request.GameRoomRequestDto;
import com.project.trysketch.dto.response.GameRoomResponseDto;

import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.service.GameRoomService;
import com.project.trysketch.global.dto.MsgResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.simple.parser.ParseException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    // 게임 방 전체 조회 페이징 처리
    @GetMapping("/rooms")
    public ResponseEntity<List<GameRoomResponseDto>> getAllGameRoom(@PageableDefault(size = 10
                                                                    ,sort = "createdAt"
                                                                    ,direction = Sort.Direction.DESC) Pageable pageable)
    {
        return ResponseEntity.ok(gameRoomService.getAllGameRoom(pageable));
    }

    // 게임 방 생성
    @PostMapping("/room")
    public ResponseEntity<DataMsgResponseDto> createGameRoom(@RequestBody GameRoomRequestDto gameRoomRequestDto,
                                                             HttpServletRequest request) throws ParseException {
        log.info(">>> 메인페이지 이동 - 방 이름 : {},", gameRoomRequestDto.getTitle());
        return ResponseEntity.ok(gameRoomService.createGameRoom(gameRoomRequestDto, request));
    }

    // 게임 방 입장
    @PostMapping("/room/{id}")
    public ResponseEntity<MsgResponseDto> enterGameRoom(@PathVariable Long id,
                                                        HttpServletRequest request) throws ParseException {
        log.info(">>> 방 입장 - 방 id : {}, 유저 id : {}", id, request);
        return ResponseEntity.ok(gameRoomService.enterGameRoom(id,request));
    }

    // 게임 방 나가기
    @DeleteMapping("/room/{id}/exit")
    public ResponseEntity<MsgResponseDto> exitGameRoom(@PathVariable Long id,
                                                       HttpServletRequest request)
    {
        log.info(">>> 방 퇴장 - 방 id : {}, 유저 id : {}", id, request);
        return ResponseEntity.ok(gameRoomService.exitGameRoom(id,request));
    }

}

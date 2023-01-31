package com.project.trysketch.controller;

import com.project.trysketch.dto.request.GameRoomRequestDto;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.service.GameRoomService;
import com.project.trysketch.global.dto.MsgResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.Map;

// 1. 기능    : 게임 방 컨트롤러
// 2. 작성자  : 김재영, 안은솔
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    // 게임 방 전체 조회 페이징 처리
    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getAllGameRoom(@PageableDefault(size = 5, sort = "createdAt" , direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(gameRoomService.getAllGameRoom(pageable));
    }

    // 게임 방 상세 조회
    @GetMapping("/room/{id}")
    public ResponseEntity<DataMsgResponseDto> getGameRoom(@PathVariable Long id,
                                                          HttpServletRequest request) {
        log.info(">>> 방 상세조회 - 방 id : {}, 유저 id : {}", id, request);
        return ResponseEntity.ok(gameRoomService.getGameRoom(id, request));
    }

    // 게임 방 생성
    @PostMapping("/room")
    public ResponseEntity<DataMsgResponseDto> createGameRoom(@RequestBody @Valid GameRoomRequestDto gameRoomRequestDto,
                                                             HttpServletRequest request) {
        log.info(">>> 메인페이지 이동 - 방 이름 : {},", gameRoomRequestDto.getTitle());
        return ResponseEntity.ok(gameRoomService.createGameRoom(gameRoomRequestDto, request));
    }

    // 게임 방 입장
    @PostMapping("/room/enter/{randomCode}")
    public ResponseEntity<DataMsgResponseDto> enterGameRoom(@PathVariable String randomCode,
                                                        HttpServletRequest request) {
        log.info(">>> 방 입장 - 방 randomCode : {}, 유저 id : {}", randomCode, request);
        return ResponseEntity.ok(gameRoomService.enterGameRoom(randomCode, request));
    }

    // 게임 방 나가기
//    @DeleteMapping("/room/exit/{id}")
//    public ResponseEntity<MsgResponseDto> exitGameRoom(@PathVariable Long id,
//                                                       HttpServletRequest request) {
//        log.info(">>> 방 퇴장 - 방 id : {}, 유저 id : {}", id, request);
////        return ResponseEntity.ok(gameRoomService.exitGameRoom(id, request, null));
//        return ResponseEntity.ok(gameRoomService.exitGameRoom(null));
//    }
}

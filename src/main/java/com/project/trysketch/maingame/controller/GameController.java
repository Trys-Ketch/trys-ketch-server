package com.project.trysketch.maingame.controller;

import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.maingame.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class GameController {

    private final GameService gameService;

    //  게임시작
    @PostMapping("/game/{roomId}/start")
    public ResponseEntity<MsgResponseDto> startGame(@PathVariable Long roomId, HttpServletRequest request){
        log.info(">>> 게임이 시작되었습니다 - 게임 방 번호 : {},",roomId);
        return ResponseEntity.ok(gameService.startGame(roomId,request));
    }
}

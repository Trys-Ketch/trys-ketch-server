package com.project.trysketch.controller;

import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    //
    @PostMapping("/ingame/{keywordnum}/{round}")
    public void keywordStatus(@PathVariable Long keywordnum, @PathVariable Long round, HttpServletRequest request){
//        gameService.

    }


//
//    api(순번, 라운드, {라운드데이터})
//1user : 제시어 api(1,1)
//2user : 제시어 api(2,1)
//3user : 제시어 api(3,1)
//4user : 제시어 api(4,1)
//5user : 제시어 api(5,1)


//                그림 테이블 : (user's nickname), roundId, keyword's Id, gameUd
//                제시어 테이블:                   roundId, keyword's Id, gameid
//
//            [ [제시어, 그림, 제시어(5번유저) ],
//            [제시어, 그림, 제시어(4번유저), 제시어, 그림, 제시어],
//            [제시어, 그림, 제시어(5번유저) ],
//            [제시어, 그림, 제시어(1번유저) ],
//            [제시어, 그림, 제시어(2번유저), 그림, 제시어 ]]



}
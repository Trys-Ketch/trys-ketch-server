package com.project.trysketch.controller;

import com.project.trysketch.dto.response.ImageResponseDto;
import com.project.trysketch.dto.response.KeywordResponseDto;
import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;

// 1. 기능    : 게임 컨트롤러
// 2. 작성자  : 김재영, 황미경
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

    // 게임 종료
    @PostMapping("/game/{roomId}/end")
    public ResponseEntity<MsgResponseDto> endGame(@PathVariable Long roomId){
        log.info(">>> 게임이 정상 종료되었습니다 - 게임 방 번호 : {},",roomId);
        return ResponseEntity.ok(gameService.endGame(roomId));
    }


    // 단어 제출하는 라운드 끝났을 때 for Test!
    @PostMapping("/game/finish/{roomId}/voca")
    public MsgResponseDto submitVocabulary(@RequestParam int round,@RequestParam int keywordIndex, @RequestParam String keyword, @PathVariable Long roomId){
        log.info(">>>>>> {} : 라운드 시작",round);
        log.info(">>>>>> {} : 받을 제시어 순번",keywordIndex);
        log.info(">>>>>> {} : 게임 방 번호",roomId);
        log.info(">>>>>> {} : 받을 제시어",keyword);
        return gameService.submitVocabulary(round,keywordIndex,keyword,roomId);
    }

    // 그림 제출하는 라운드 끝났을 때 for Test!
    @PostMapping("/test/finish/{roomId}/image")
    public MsgResponseDto submitImage(@RequestParam int round,@RequestParam int keywordIndex, @PathVariable Long roomId
            , @RequestParam String imagePath) {
        log.info(">>>>>> {} : 그림 라운드 끝",round);
        log.info(">>>>>> {} : 받을 제시어 순번",keywordIndex);
        log.info(">>>>>> {} : 게임 방 번호",roomId);
        log.info(">>>>>> {} : imagePath", imagePath);
        return gameService.submitImage(round,keywordIndex,imagePath,roomId);
    }


    // 단어적는 라운드 시작  ← 이전 라운드의 그림 response 로 줘야함! (GepMapping)
    @GetMapping("/test/start/{roomId}/before/image")
    public ImageResponseDto startKeywordRound(@RequestParam int round,@RequestParam int keywordIndex, @PathVariable Long roomId) {
        log.info(">>>>>> {} : 라운드 시작",round);
        log.info(">>>>>> {} : 받을 제시어 순번",keywordIndex);
        log.info(">>>>>> {} : 게임 방 번호",roomId);
        return gameService.startKeywordRound(round, keywordIndex, roomId);
    }

    // 그림그리는 라운드 시작  ← 이전 라운드의 단어 response 로 줘야함! (GetMapping)
    @GetMapping("/test/start/{roomId}/before/keyword")
    public KeywordResponseDto startImageRound(@RequestParam int round, @RequestParam int keywordIndex, @PathVariable Long roomId) {
        log.info(">>>>>>> {} : 이번 라운드", round);

//        log.info(">>>>>>> {} : ");
        return gameService.startImageRound(round, keywordIndex, roomId);
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
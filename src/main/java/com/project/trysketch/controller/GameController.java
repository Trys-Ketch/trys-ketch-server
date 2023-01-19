package com.project.trysketch.controller;

import com.project.trysketch.dto.request.GameFlowRequestDto;
import com.project.trysketch.dto.response.ImageResponseDto;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

// 1. 기능    : 게임 컨트롤러
// 2. 작성자  : 김재영, 황미경
@Slf4j
@RequiredArgsConstructor
@RestController
public class GameController {

    private final GameService gameService;

    // MessageMapping 을 통해 webSocket 로 들어오는 메시지를 발신 처리한다.
    // 이때 클라이언트에서는 /app/game/** 로 요청하게 되고 이것을 controller 가 받아서 처리한다.
    // 처리가 완료되면 /topic/game/room/{roomId} 로 메시지가 전송된다.
    // 1.게임시작
    @MessageMapping("/game/start")
    public ResponseEntity<MsgResponseDto> startGame(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - startGame 실행");
        log.info(">>> 게임이 시작되었습니다 - 게임 방 번호 : {},", requestDto.getRoomId());
        return ResponseEntity.ok(gameService.startGame(requestDto));
    }

    // 2.최초 랜덤 제시어 하나 가져오기
    @MessageMapping("/game/random-keyword")
    public void getRandomKeyword(GameFlowRequestDto requestDto){
        log.info(">>>>>>>>>>>> GameController - getRandomKeyword 실행");
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        log.info(">>>>>> {} : 내가 누구냐",requestDto.getToken());
        gameService.getRandomKeyword(requestDto);
    }

    // 3.단어 제출하는 라운드 끝났을 때 for Test!
    @MessageMapping("/game/submit-word")
    public void postVocabulary(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - postVocabulary 실행");
        log.info(">>>>>> {} : 라운드 시작", requestDto.getRound());
        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        log.info(">>>>>> {} : 받을 제시어", requestDto.getKeyword());
        gameService.postVocabulary(requestDto);
    }

    // 4. 그림그리는 라운드 시작  ← 이전 라운드의 단어 response 로 줘야함! (GetMapping)
//    @MessageMapping("/game/before-word")
//    public void getPreviousKeyword(GameFlowRequestDto requestDto) {
//        log.info(">>>>>>>>>>>> GameController - getPreviousKeyword 실행");
//        log.info(">>>>>> {} : 이번 라운드", requestDto.getRound());
//        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
//        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
////        return gameService.getPreviousKeyword(requestDto);
//    } // 사용안함 1.18 리팩토링

    // 5. 그림 제출하는 라운드 끝났을 때 for Test!
//    @PostMapping(value = "/test/finish/{roomId}/image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @MessageMapping("/game/submit-image")
    public void postImage(GameFlowRequestDto requestDto, String image) throws IOException {
        log.info(">>>>>>>>>>>> GameController - postImage 실행");
        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        gameService.postImage(requestDto, image);
    }

    // 6. 단어적는 라운드 시작  ← 이전 라운드의 그림 response 로 줘야함! (GepMapping) → 3번으로 돌아감
//    @MessageMapping("/game/before-image")
//    public ImageResponseDto getPreviousImage(GameFlowRequestDto requestDto) {
//        log.info(">>>>>>>>>>>> GameController - getPreviousImage 실행");
//        log.info(">>>>>> {} : 라운드 시작", requestDto.getRound());
//        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
//        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
//        return gameService.getPreviousImage(requestDto);
//    } // 사용안함 1.18 리팩토링

    // 7. 결과 페이지
    @MessageMapping("/game/result")
    public Object[][] getGameFlow(GameFlowRequestDto requestDto) {
        return gameService.getGameFlow(requestDto);
    }

    // 8. 게임 종료
    @MessageMapping("/game/end")
    public ResponseEntity<MsgResponseDto> endGame(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - endGame 실행");
        log.info(">>> 게임이 정상 종료되었습니다 - 게임 방 번호 : {},", requestDto.getRoomId());
        return ResponseEntity.ok(gameService.endGame(requestDto));
    }
}
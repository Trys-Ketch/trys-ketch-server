package com.project.trysketch.controller;

import com.project.trysketch.dto.request.GameFlowRequestDto;
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
    private final String word = "word";
    private final String image = "image";
    private final String next = "next";
    private final String prev = "prev";

    // MessageMapping 을 통해 webSocket 로 들어오는 메시지를 발신 처리한다.
    // 이때 클라이언트에서는 /app/game/** 로 요청하게 되고 이것을 controller 가 받아서 처리한다.
    // 처리가 완료되면 /topic/game/room/{roomId} 로 메시지가 전송된다.
    // 1. 게임시작
    @MessageMapping("/game/start")
    public ResponseEntity<MsgResponseDto> startGame(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - startGame 실행");
        log.info(">>> 게임이 시작되었습니다 - 게임 방 번호 : {},", requestDto.getRoomId());
        return ResponseEntity.ok(gameService.startGame(requestDto));
    }

    // 2. 최초 랜덤 제시어 하나 가져오기
    @MessageMapping("/game/ingame-data")
    public void getInGameData(GameFlowRequestDto requestDto){
        log.info(">>>>>>>>>>>> GameController - getRandomKeyword 실행");
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        log.info(">>>>>> {} : 내가 누구냐",requestDto.getToken());
        gameService.getInGameData(requestDto);
    }

    // 3. 제출 여부 확인하고 DB 저장
    @MessageMapping("/game/toggle-ready")
    public void getToggleSubmit(GameFlowRequestDto requestDto) throws IOException {
        log.info(">>>>>>>>>>>> GameController - getToggleSubmit 실행");
        log.info(">>>>>> {} : 라운드 시작", requestDto.getRound());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        gameService.getToggleSubmit(requestDto);
    }

    // 4. 단어 제출하는 라운드 끝났을 때
    @MessageMapping("/game/submit-word")
    public void postVocabulary(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - postVocabulary 실행");
        log.info(">>>>>> {} : 라운드 시작", requestDto.getRound());
        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        log.info(">>>>>> {} : 받을 제시어", requestDto.getKeyword());
        gameService.checkSubmit(requestDto, word);
    }

    // 5. 그림 제출하는 라운드 끝났을 때
    @MessageMapping("/game/submit-image")
    public void postImage(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - postImage 실행");
        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        gameService.checkSubmit(requestDto, image);
    }

    // 6. 게임 결과 페이지
    @MessageMapping("/game/result")
    public Object[][] getGameFlow(GameFlowRequestDto requestDto) {
        return gameService.getGameFlow(requestDto);
    }

    // 7. 게임 결과창 - 다음 키워드 가져오기
    @MessageMapping("/game/next-keyword-index")
    public ResponseEntity<MsgResponseDto> nextKeywordIndex(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - nextResultIndex 실행");
        return ResponseEntity.ok(gameService.getKeywordIndex(requestDto, next));
    }

    // 8. 게임 결과창 - 이전 키워드 가져오기
    @MessageMapping("/game/prev-keyword-index")
    public ResponseEntity<MsgResponseDto> prevKeywordIndex(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - prevResultIndex 실행");
        return ResponseEntity.ok(gameService.getKeywordIndex(requestDto, prev));
    }

    // 9. 게임 종료
    @MessageMapping("/game/end")
    public ResponseEntity<MsgResponseDto> endGame(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - endGame 실행");
        log.info(">>> 게임이 정상 종료되었습니다 - 게임 방 번호 : {},", requestDto.getRoomId());
        return ResponseEntity.ok(gameService.endGame(requestDto));
    }

}
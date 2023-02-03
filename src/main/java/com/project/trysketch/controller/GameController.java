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
    private final String increase = "increase-time";
    private final String decrease = "decrease-time";

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

    // 2. 방에 입장시(생성 포함) 타임리미트, 난이도 전달
    @MessageMapping("/game/gameroom-data")
    public void getGameMode(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - getGameMode 실행");
        log.info(">>> 게임에 입장 - 게임 방 번호 : {},", requestDto.getRoomId());
        gameService.getGameMode(requestDto);
    }

    // 3. 최초 랜덤 제시어 하나 가져오기
    @MessageMapping("/game/ingame-data")
    public void getInGameData(GameFlowRequestDto requestDto){
        log.info(">>>>>>>>>>>> GameController - getRandomKeyword 실행");
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        log.info(">>>>>> {} : 내가 누구냐",requestDto.getToken());
        gameService.getInGameData(requestDto);
    }

    // 4. 난이도 조절 버튼
    @MessageMapping("/game/difficulty")
    public void changeDifficulty(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - difficulty 실행");
        gameService.changeDifficulty(requestDto);
    }

    // 5. 시간 조절 버튼 - 30초 증가
    @MessageMapping("/game/increase-time")
    public void increaseRoundTime(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - increase-time 실행");
        gameService.changeTimeLimit(requestDto, increase);
    }

    // 6. 시간 조절 버튼 - 30초 감소
    @MessageMapping("/game/decrease-time")
    public void decreaseRoundTime(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - decrease-time 실행");
        gameService.changeTimeLimit(requestDto, decrease);
    }

    // 7. 제출 여부 확인하고 DB 저장
    @MessageMapping("/game/toggle-ready")
    public synchronized void getToggleSubmit(GameFlowRequestDto requestDto) throws IOException {
        log.info(">>>>>>>>>>>> GameController - getToggleSubmit 실행");
        log.info(">>>>>> {} : 라운드 시작", requestDto.getRound());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        gameService.getToggleSubmit(requestDto);
    }

    // 8. 단어 제출하는 라운드 끝났을 때
    @MessageMapping("/game/submit-word")
    public void postVocabulary(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - postVocabulary 실행");
        log.info(">>>>>> {} : 라운드 시작", requestDto.getRound());
        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        log.info(">>>>>> {} : 받을 제시어", requestDto.getKeyword());
        gameService.checkSubmit(requestDto, word);
    }

    // 9. 그림 제출하는 라운드 끝났을 때
    @MessageMapping("/game/submit-image")
    public void postImage(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - postImage 실행");
        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        gameService.checkSubmit(requestDto, image);
    }

    // 10. 게임 결과 페이지
    @MessageMapping("/game/result")
    public Object[][] getGameFlow(GameFlowRequestDto requestDto) {
        return gameService.getGameFlow(requestDto);
    }

    // 11. 게임 결과창 - 다음 키워드 가져오기
    @MessageMapping("/game/next-keyword-index")
    public ResponseEntity<MsgResponseDto> nextKeywordIndex(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - nextResultIndex 실행");
        return ResponseEntity.ok(gameService.getKeywordIndex(requestDto, next));
    }

    // 12. 게임 결과창 - 이전 키워드 가져오기
    @MessageMapping("/game/prev-keyword-index")
    public ResponseEntity<MsgResponseDto> prevKeywordIndex(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - prevResultIndex 실행");
        return ResponseEntity.ok(gameService.getKeywordIndex(requestDto, prev));
    }

    // 13. 게임 종료
    @MessageMapping("/game/end")
    public ResponseEntity<MsgResponseDto> endGame(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>> GameController - endGame 실행");
        log.info(">>> 게임이 정상 종료되었습니다 - 게임 방 번호 : {},", requestDto.getRoomId());
        return ResponseEntity.ok(gameService.endGame(requestDto));
    }

}
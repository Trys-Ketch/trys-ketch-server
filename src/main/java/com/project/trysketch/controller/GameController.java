package com.project.trysketch.controller;

import com.project.trysketch.dto.request.GameFlowRequestDto;
import com.project.trysketch.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public void startGame(GameFlowRequestDto requestDto) {
        gameService.startGame(requestDto);
    }

    // 2. 방에 입장시(생성 포함) 타임리미트, 난이도 전달
    @MessageMapping("/game/gameroom-data")
    public void getGameMode(GameFlowRequestDto requestDto) {
        gameService.getGameMode(requestDto);
    }

    // 3. 최초 랜덤 제시어 하나 가져오기
    @MessageMapping("/game/ingame-data")
    public void getInGameData(GameFlowRequestDto requestDto){
        gameService.getInGameData(requestDto);
    }

    // 4. 난이도 조절 버튼
    @MessageMapping("/game/difficulty")
    public void changeDifficulty(GameFlowRequestDto requestDto) {
        gameService.changeDifficulty(requestDto);
    }

    // 5. 시간 조절 버튼 - 30초 증가
    @MessageMapping("/game/increase-time")
    public void increaseRoundTime(GameFlowRequestDto requestDto) {
        gameService.changeTimeLimit(requestDto, increase);
    }

    // 6. 시간 조절 버튼 - 30초 감소
    @MessageMapping("/game/decrease-time")
    public void decreaseRoundTime(GameFlowRequestDto requestDto) {
        gameService.changeTimeLimit(requestDto, decrease);
    }

    // 7. 제출 여부 확인하고 DB 저장
    @MessageMapping("/game/toggle-ready")
    public void getToggleSubmit(GameFlowRequestDto requestDto) throws IOException {
        gameService.getToggleSubmit(requestDto);
    }

    // 8. 단어 제출하는 라운드 끝났을 때
    @MessageMapping("/game/submit-word")
    public void postVocabulary(GameFlowRequestDto requestDto) {
        gameService.checkLastRound(requestDto, word);
    }

    // 9. 그림 제출하는 라운드 끝났을 때
    @MessageMapping("/game/submit-image")
    public void postImage(GameFlowRequestDto requestDto) {
        gameService.checkLastRound(requestDto, image);
    }

    // 10. 게임 결과 페이지
    @MessageMapping("/game/result")
    public void getGameFlow(GameFlowRequestDto requestDto) {
        gameService.getGameFlow(requestDto);
    }

    // 11. 게임 결과창 - 다음 키워드 가져오기
    @MessageMapping("/game/next-keyword-index")
    public void nextKeywordIndex(GameFlowRequestDto requestDto) {
        gameService.getKeywordIndex(requestDto, next);
    }

    // 12. 게임 결과창 - 이전 키워드 가져오기
    @MessageMapping("/game/prev-keyword-index")
    public void prevKeywordIndex(GameFlowRequestDto requestDto) {
        gameService.getKeywordIndex(requestDto, prev);
    }

    // 13. 게임 종료
    @MessageMapping("/game/end")
    public void endGame(GameFlowRequestDto requestDto) {
        gameService.endGame(requestDto);
    }
}
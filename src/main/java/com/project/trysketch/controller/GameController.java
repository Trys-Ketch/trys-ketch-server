package com.project.trysketch.controller;

import com.project.trysketch.dto.request.GameFlowRequestDto;
import com.project.trysketch.dto.response.ImageResponseDto;
import com.project.trysketch.dto.response.KeywordResponseDto;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

// 1. 기능    : 게임 컨트롤러
// 2. 작성자  : 김재영, 황미경
@Slf4j
@RequiredArgsConstructor
@RestController
//@RequestMapping("/api")
public class GameController {

    private final GameService gameService;

    // MessageMapping 을 통해 webSocket 로 들어오는 메시지를 발신 처리한다.
    // 이때 클라이언트에서는 /app/game/** 로 요청하게 되고 이것을 controller 가 받아서 처리한다.
    // 처리가 완료되면 /topic/game/room/{roomId} 로 메시지가 전송된다.
    // 게임시작
    @MessageMapping("/game/start")
    public ResponseEntity<MsgResponseDto> startGame(GameFlowRequestDto requestDto) {
        log.info(">>> 게임이 시작되었습니다 - 게임 방 번호 : {},", requestDto.getRoomId());
        return ResponseEntity.ok(gameService.startGame(requestDto));
    }

    // 게임 종료
    @MessageMapping("/game/end")
    public ResponseEntity<MsgResponseDto> endGame(GameFlowRequestDto requestDto) {
        log.info(">>> 게임이 정상 종료되었습니다 - 게임 방 번호 : {},", requestDto.getRoomId());
        return ResponseEntity.ok(gameService.endGame(requestDto));
    }

    // 단어 제출하는 라운드 끝났을 때 for Test!
    @MessageMapping("/game/finish/voca")
    public MsgResponseDto postVocabulary(GameFlowRequestDto requestDto) {
        log.info(">>>>>> {} : 라운드 시작", requestDto.getRound());
        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        log.info(">>>>>> {} : 받을 제시어", requestDto.getKeyword());
        return gameService.postVocabulary(requestDto);
    }

    // 그림 제출하는 라운드 끝났을 때 for Test!
    @PostMapping(value = "/test/finish/{roomId}/image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public MsgResponseDto postImage(@RequestParam int round, @RequestParam int keywordIndex,
                                    @PathVariable Long roomId, HttpServletRequest request,
                                    @RequestPart(value = "file") MultipartFile multipartFile) throws IOException {
        log.info(">>>>>> {} : 그림 라운드 끝", round);
        log.info(">>>>>> {} : 받을 제시어 순번", keywordIndex);
        log.info(">>>>>> {} : 게임 방 번호", roomId);
        return gameService.postImage(round, keywordIndex, request, multipartFile, roomId);
    }


    // 단어적는 라운드 시작  ← 이전 라운드의 그림 response 로 줘야함! (GepMapping)
    @MessageMapping("/test/start/before/image")
//    @SendToUser
    public ImageResponseDto getPreviousImage(GameFlowRequestDto requestDto) {
        log.info(">>>>>> {} : 라운드 시작", requestDto.getRound());
        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        return gameService.getPreviousImage(requestDto);
    }

    // 그림그리는 라운드 시작  ← 이전 라운드의 단어 response 로 줘야함! (GetMapping)
    @MessageMapping("/test/start/before/keyword")
    public KeywordResponseDto getPreviousKeyword(GameFlowRequestDto requestDto) {
        log.info(">>>>>>> {} : 이번 라운드", requestDto.getRound());
        log.info(">>>>>> {} : 받을 제시어 순번", requestDto.getKeywordIndex());
        log.info(">>>>>> {} : 게임 방 번호", requestDto.getRoomId());
        return gameService.getPreviousKeyword(requestDto);
    }


    // 결과 페이지
    @MessageMapping("/test/result/{roomId}")
    public String[][] getGameFlow(GameFlowRequestDto requestDto) {
        return gameService.getGameFlow(requestDto.getRoomId());
    }
}
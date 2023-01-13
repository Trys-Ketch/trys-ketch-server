package com.project.trysketch.controller;

import com.project.trysketch.dto.response.ImageResponseDto;
import com.project.trysketch.dto.response.KeywordResponseDto;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

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
    public ResponseEntity<MsgResponseDto> startGame(@PathVariable Long roomId, HttpServletRequest request) {
        log.info(">>> 게임이 시작되었습니다 - 게임 방 번호 : {},", roomId);
        return ResponseEntity.ok(gameService.startGame(roomId, request));
    }

    // 게임 종료
    @PostMapping("/game/{roomId}/end")
    public ResponseEntity<MsgResponseDto> endGame(@PathVariable Long roomId) {
        log.info(">>> 게임이 정상 종료되었습니다 - 게임 방 번호 : {},", roomId);
        return ResponseEntity.ok(gameService.endGame(roomId));
    }


    // 단어 제출하는 라운드 끝났을 때 for Test!
    @PostMapping("/game/finish/{roomId}/voca")
    public MsgResponseDto postVocabulary(@RequestParam int round, @RequestParam int keywordIndex, @RequestParam String keyword, @PathVariable Long roomId
            , HttpServletRequest request) {
        log.info(">>>>>> {} : 라운드 시작", round);
        log.info(">>>>>> {} : 받을 제시어 순번", keywordIndex);
        log.info(">>>>>> {} : 게임 방 번호", roomId);
        log.info(">>>>>> {} : 받을 제시어", keyword);
        return gameService.postVocabulary(round, keywordIndex, keyword, roomId, request);
    }

    // 그림 제출하는 라운드 끝났을 때 for Test!
    @PostMapping(value = "/test/finish/{roomId}/image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public MsgResponseDto postImage(@RequestParam int round, @RequestParam int keywordIndex, @PathVariable Long roomId,
                                    HttpServletRequest request,
                                    @RequestPart(value = "file") MultipartFile multipartFile) throws IOException {
        log.info(">>>>>> {} : 그림 라운드 끝", round);
        log.info(">>>>>> {} : 받을 제시어 순번", keywordIndex);
        log.info(">>>>>> {} : 게임 방 번호", roomId);
        return gameService.postImage(round, keywordIndex, request, multipartFile, roomId);
    }


    // 단어적는 라운드 시작  ← 이전 라운드의 그림 response 로 줘야함! (GepMapping)
    @GetMapping("/test/start/{roomId}/before/image")
    public ImageResponseDto getPreviousImage(@RequestParam int round, @RequestParam int keywordIndex, @PathVariable Long roomId) {
        log.info(">>>>>> {} : 라운드 시작", round);
        log.info(">>>>>> {} : 받을 제시어 순번", keywordIndex);
        log.info(">>>>>> {} : 게임 방 번호", roomId);
        return gameService.getPreviousImage(round, keywordIndex, roomId);
    }

    // 그림그리는 라운드 시작  ← 이전 라운드의 단어 response 로 줘야함! (GetMapping)
    @GetMapping("/test/start/{roomId}/before/keyword")
    public KeywordResponseDto getPreviousKeyword(@RequestParam int round, @RequestParam int keywordIndex, @PathVariable Long roomId) {
        log.info(">>>>>>> {} : 이번 라운드", round);
        log.info(">>>>>> {} : 받을 제시어 순번", keywordIndex);
        log.info(">>>>>> {} : 게임 방 번호", roomId);
        return gameService.getPreviousKeyword(round, keywordIndex, roomId);
    }


    // 결과 페이지
    @GetMapping("/test/result/{roomId}")
    public String[][] getGameFlow(@PathVariable Long roomId) {
        return gameService.getGameFlow(roomId);
    }
}
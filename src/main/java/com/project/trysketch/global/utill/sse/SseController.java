package com.project.trysketch.global.utill.sse;

import com.project.trysketch.service.GameService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

// 1. 기능   : SSE 연결을 위한 controller
// 2. 작성자 : 황미경

@Slf4j
@RestController
public class SseController {
    private final SseEmitters sseEmitters;

    @Autowired
    private GameService gameService;

    public SseController(SseEmitters sseEmitters) {
        this.sseEmitters = sseEmitters;
    }


    // Emitter 생성 및 SSE 최초 연결
    @CrossOrigin
    @GetMapping(value = "/api/sse/rooms", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect() {
        log.info("[SSE] - Controller 시작 / connect() 메서드 시작");

        // Emitter 객체 생성. 5분으로 설정
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        sseEmitters.add(emitter);

        // SSE 연결 및 데이터 전송
        try {
            log.info("[SSE] - Controller 의 connect() 메서드 / try 문 안의 emitter 객체 생성");
            emitter.send(SseEmitter.event()
                    .name("connect")                 // event 의 이름
                    .data(gameService.getRooms(0)));      // event 에 담을 data
            log.info("[SSE] - Controller 의 connect() 메서드 / try 문 안의 / 생성된 emitter : {}", emitter);
        } catch (IOException e) {
            log.info("[SSE] - Controller 의 connect() 메서드 / try 문 안의 예외 처리 터짐 / remove 실행");
            log.info("", e);
            sseEmitters.remove(emitter);
        }

        // 타임아웃 발생시 콜백 등록
        emitter.onTimeout(() -> {
            log.info("[SSE] - ★★★★★★★★SseController 파일 connect() 메서드 / [ onTimeout ] -> 객체 삭제하고싶음");
//            sseEmitters.remove(emitter);
            emitter.complete();

        });

        // 타임아웃 발생시 브라우저에 재요청 연결 보내는데, 이때 새로운 객체 다시 생성하므로 기존의 Emitter 객체 리스트에서 삭제
        emitter.onCompletion(() -> {
            log.info("[SSE] - ★★★★★★★★SseController 파일 connect() 메서드 / [ onCompletion ] -> 객체 삭제하고싶음");
            sseEmitters.remove(emitter);
        });

        emitter.onError(throwable -> {
            log.error("[SSE] - ★★★★★★★★SseController 파일 connect() 메서드 / [ onError ] -> 객체 삭제하고싶음");
//            sseEmitters.remove(emitter);
            emitter.complete();
        });

        return ResponseEntity.ok(emitter);
    }
}
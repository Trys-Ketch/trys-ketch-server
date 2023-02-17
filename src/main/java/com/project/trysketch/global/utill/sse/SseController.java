package com.project.trysketch.global.utill.sse;

import com.project.trysketch.service.SseService;
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
    private SseService sseService;

    public SseController(SseEmitters sseEmitters) {
        this.sseEmitters = sseEmitters;
    }

    // Emitter 생성 및 SSE 최초 연결
    @CrossOrigin
    @GetMapping(value = "/api/sse/rooms", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect() {

        // Emitter 객체 생성. 5분으로 설정
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L);
        sseEmitters.add(emitter);

        // SSE 연결 및 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")                 // event 의 이름
                    .data(sseService.getRooms(0)));       // event 에 담을 data
        } catch (IOException e) {
            sseEmitters.remove(emitter);
        }

        // 타임아웃 발생시 콜백 등록
        emitter.onTimeout(() -> {
            emitter.complete();
        });

        // 타임아웃 발생시 브라우저에 재요청 연결 보내는데, 이때 새로운 객체 다시 생성하므로 기존의 Emitter 객체 리스트에서 삭제
        emitter.onCompletion(() -> sseEmitters.remove(emitter));

        // 에러 발생시 처리
        emitter.onError(throwable -> emitter.complete());

        return ResponseEntity.ok(emitter);
    }
}
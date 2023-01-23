package com.project.trysketch.global.utill.sse;

import com.project.trysketch.service.GameRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;

// 1. 기능   : SSE 연결을 위한 controller
// 2. 작성자 : 황미경

@RestController
public class SseController {
    private final SseEmitters sseEmitters;

    @Autowired
    private GameRoomService gameRoomService;

    public SseController(SseEmitters sseEmitters) {
        this.sseEmitters = sseEmitters;
    }


    // Emitter 생성 및 SSE 최초 연결
    @GetMapping(value = "/api/sse/rooms", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect() {

        // Emitter 객체 생성. 1분으로 설정
        SseEmitter emitter = new SseEmitter(60 * 1000L);
        sseEmitters.add(emitter);

        // SSE 연결 및 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")              // event 의 이름
                    .data(gameRoomService.getrooms()));      // event 에 담을 data
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(emitter);
    }
}
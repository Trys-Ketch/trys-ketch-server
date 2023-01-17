package com.project.trysketch.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// 1. 기능   : SSE 연결시 발생시킬 event 생성로직
// 2. 작성자 : 황미경

@Component
@Slf4j
public class SseEmitters {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();


    // SSE emitter 등록 메서드
    SseEmitter add(SseEmitter emitter) {
        this.emitters.add(emitter);
        log.info("new emitter added: {}", emitter);
        log.info("emitter list size: {}", emitters.size());
        log.info("emitter list: {}", emitters);

        // 비동기요청 완료시 콜백 등록
        emitter.onCompletion(() -> {
            log.info("onCompletion callback");

            // 타임아웃 발생시 브라우저에 재요청 연결 보내는데, 이때 새로운 객체 다시 생성하므로 기존의 Emitter객체 리스트에서 삭제
            this.emitters.remove(emitter);
        });

        // 타임아웃 발생시 콜백 등록
        emitter.onTimeout(() -> {
            log.info("onTimeout callback");
            emitter.complete();
        });

        return emitter;
    }


    // 게임 방 생성, 소멸시 SSE 커넥션 연결된 모든 클라이언트에 방리스트 전달
    public void changeRoom(Object roomInfo) {
        emitters.forEach(emitter -> {

            try {
                emitter.send(SseEmitter.event()
                        .name("changeRoom")     // event의 이름 지정
                        .data(roomInfo));                  // event에 담을 data
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
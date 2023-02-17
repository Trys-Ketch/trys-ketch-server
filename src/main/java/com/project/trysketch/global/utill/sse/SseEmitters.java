package com.project.trysketch.global.utill.sse;

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

    // thread-safe한 자료구조 사용 필요. (콜백이 SseEmitter를 관리하는 다른 스레드에서 실행되기 때문)
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    // SSE emitter 등록 메서드
    void add(SseEmitter emitter) {
        this.emitters.add(emitter);

        // Broken Pipe 발생시
        emitter.onError(throwable -> {
            emitter.complete();
        });

        // 타임아웃 발생시 콜백 등록
        emitter.onTimeout(() -> {
            emitter.complete();       // complete()이 실행되면 SSE연결 disconnect해주며 onCompletion() 이 호출시킴
        });

        // 비동기요청 완료시 emitter 객체 삭제
        emitter.onCompletion(() -> {
            this.emitters.remove(emitter);
        });
    }

    void remove(SseEmitter emitter) {
        this.emitters.remove(emitter);
    }

    // 게임 방 생성, 입장, 퇴장시 SSE 커넥션 연결된 모든 클라이언트에 방리스트 전달
    public void changeRoom(Object roomInfo) {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("changeRoom")      // event의 이름 지정
                        .data(roomInfo));                  // event에 담을 data
            } catch (IOException e) {
                this.emitters.remove(emitter);
            }
        });
    }
}
package com.project.trysketch.sse;

import com.project.trysketch.dto.request.GameRoomRequestDto;
import com.project.trysketch.service.GameRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SseEmitters {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @Autowired
    private GameRoomService gameRoomService;


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


    // 게임 방 생성 (생성한 게임방에 대해서만 SSE 커넥션이 열려있는 모든 클라이언트에게 전달)
    public void createRoom(GameRoomRequestDto gameRoomRequestDto, HttpServletRequest request) {
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("createRoom")
                        .data(gameRoomService.createGameRoom(gameRoomRequestDto, request)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


//    // 게임 방 생성 (생성된 게임방 포함 모든 게임방 정보 SSE 커넥션이 열려있는 모든 클라이언트에게 전달)
//    public void createRoom(GameRoomRequestDto gameRoomRequestDto, HttpServletRequest request, Pageable pageable) {
//        emitters.forEach(emitter -> {
//            gameRoomService.createGameRoom(gameRoomRequestDto, request);
//            try {
//                emitter.send(SseEmitter.event()
//                        .name("createRoom")
//                        .data(gameRoomService.getAllGameRoom(pageable)));
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        });
//    }

}
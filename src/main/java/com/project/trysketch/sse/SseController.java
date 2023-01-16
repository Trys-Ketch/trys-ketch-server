package com.project.trysketch.sse;

import java.io.IOException;
import com.project.trysketch.dto.request.GameRoomRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import javax.servlet.http.HttpServletRequest;

@RestController
@Slf4j
public class SseController {
    private final SseEmitters sseEmitters;

    public SseController(SseEmitters sseEmitters) {
        this.sseEmitters = sseEmitters;
    }


    // Emitter 생성 및 SSE 연결
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> connect() {

        // Emitter 객체 생성. 1분으로 설정
        SseEmitter emitter = new SseEmitter(60 * 1000L);
        sseEmitters.add(emitter);

        // SSE 연결 및 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")               // 이벤트의 이름
                    .data("connected!"));              // 만료시간 전 데이터 받을수 있도록 SSE 연결시 데이터 전달
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return ResponseEntity.ok(emitter);
    }



    // 게임 방 생성 (생성한 게임방에 대해서만 SSE 커넥션이 열려있는 모든 클라이언트에게 전달)
    @PostMapping("/create/room")
    public ResponseEntity<Void> createGameRoom(@RequestBody GameRoomRequestDto gameRoomRequestDto,
                                                             HttpServletRequest request) {
        log.info(">>> 메인페이지 이동 - 방 이름 : {},", gameRoomRequestDto.getTitle());
        sseEmitters.createRoom(gameRoomRequestDto, request);
        return ResponseEntity.ok().build();
    }


//    // 게임 방 생성 (생성된 게임방 포함 모든 게임방 정보 SSE 커넥션이 열려있는 모든 클라이언트에게 전달)
//    @PostMapping("/create/room")
//    public ResponseEntity<Void> createGameRoom(@RequestBody GameRoomRequestDto gameRoomRequestDto,
//                                               HttpServletRequest request,@PageableDefault(size = 10, sort = "createdAt" , direction = Sort.Direction.DESC) Pageable pageable) {
//        log.info(">>> 메인페이지 이동 - 방 이름 : {},", gameRoomRequestDto.getTitle());
//        sseEmitters.createRoom(gameRoomRequestDto, request, pageable);
//        return ResponseEntity.ok().build();
//    }

}
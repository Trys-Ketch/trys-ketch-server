package com.project.trysketch.gameroom.controller;

import com.project.trysketch.gameroom.dto.request.GameRoomRequestDto;
import com.project.trysketch.gameroom.service.GameRoomService;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class GameRoomController {

    private final GameRoomService gameRoomService;

    @PostMapping("/room")
    public ResponseEntity<MsgResponseDto> createRoom(@RequestBody GameRoomRequestDto gameRoomRequestDto,@AuthenticationPrincipal UserDetailsImpl userDetail)
    {
        log.info(">>> 메인페이지 이동 - 방 이름 : {},", gameRoomRequestDto.getTitle());
        return ResponseEntity.ok(gameRoomService.createRoom(gameRoomRequestDto, userDetail.getUser()));

    }
}

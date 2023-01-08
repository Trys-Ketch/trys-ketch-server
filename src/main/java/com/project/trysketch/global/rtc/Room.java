package com.project.trysketch.global.rtc;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

// 1. 기능   : WebRTC관련 Room domain
// 2. 작성자 : 안은솔

@Getter
@AllArgsConstructor
public class Room {
    @NotNull
    private final Long id;
    // sockets by user names
    private final Map<String, WebSocketSession> clients = new HashMap<>();
}

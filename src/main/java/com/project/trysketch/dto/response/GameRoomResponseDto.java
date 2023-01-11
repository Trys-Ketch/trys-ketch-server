package com.project.trysketch.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.awt.print.Pageable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// 1. 기능   : 게임 방 response용 dto
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class GameRoomResponseDto {
    private Long id;
    private String title;
    private int GameRoomUserCount;
    private String hostNick;
    private String status;
    private HashMap<String, Integer> pageInfo;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime modifiedAt;


}

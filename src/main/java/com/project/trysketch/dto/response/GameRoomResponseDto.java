package com.project.trysketch.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

// 1. 기능   : 게임 방 response 용 DTO
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
    private Boolean isPlaying;
    private String randomCode;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private LocalDateTime modifiedAt;


}

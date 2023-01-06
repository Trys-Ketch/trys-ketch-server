package com.project.trysketch.gameroom.dto.response;

import com.project.trysketch.user.dto.UserResponseDto;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 1. 기능   : 게임 방 response용 dto
// 2. 작성자 : 김재영
@Getter
@NoArgsConstructor
public class GameRoomResponseDto {
    private Long id;
    private String title;
    private List<UserResponseDto> User;
    private String host;
    private String status;
}

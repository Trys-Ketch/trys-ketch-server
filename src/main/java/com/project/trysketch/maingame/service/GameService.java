package com.project.trysketch.maingame.service;

import com.project.trysketch.gameroom.entity.GameRoom;
import com.project.trysketch.gameroom.entity.GameRoomUser;
import com.project.trysketch.gameroom.repository.GameRoomRepository;
import com.project.trysketch.gameroom.repository.GameRoomUserRepository;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.suggest.AdjectiveEntity;
import com.project.trysketch.suggest.AdjectiveRepository;
import com.project.trysketch.suggest.NounEntity;
import com.project.trysketch.suggest.NounRepository;
import com.project.trysketch.user.entity.User;
import com.project.trysketch.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class GameService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomUserRepository gameRoomUserRepository;
    private final AdjectiveRepository adjectiveRepository;
    private final NounRepository nounRepository;
    private final int adSize = 117;
    private final int nounSize = 1335;

    public MsgResponseDto startGame(Long roomId, HttpServletRequest request) {
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
               () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 방에 참여한 유저정보 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findByGameRoom(gameRoom);

        // 방장이 아닐경우
        if (!gameRoom.getHost().equals(user.getNickname())){
            throw new CustomException(StatusMsgCode.YOUR_NOT_HOST);
        }

        // 게임이 시작되었습니다
        gameRoom.GameRoomStatusUpdate("true");

        // 형용사 리스트 가져오기
        List<AdjectiveEntity> adjectiveEntityList = adjectiveRepository.findAll();

        // 제시어 조합부
        for (GameRoomUser gameRoomUser : gameRoomUserList){

            // 형용사 리스트중 1개
            int adId = (int) (Math.random() * adSize +1);
            AdjectiveEntity adjectiveEntity = adjectiveRepository.findById(adId).orElse(null);

            // 동사 리스트중 1개
            int nuId = (int) (Math.random() * nounSize +1);
            NounEntity nounEntity = nounRepository.findById(nuId).orElse(null);

            // 형용사 + 동사
            String Keyword = adjectiveEntity.getAdjective() + nounEntity.getNoun();


        }
    }
}

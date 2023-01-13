package com.project.trysketch.service;

import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.repository.GameInfoRepository;
import com.project.trysketch.repository.GameRoomRepository;
import com.project.trysketch.repository.GameRoomUserRepository;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.suggest.AdjectiveRepository;
import com.project.trysketch.suggest.NounRepository;
import com.project.trysketch.entity.User;
import com.project.trysketch.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

// 1. 기능   : 프로젝트 메인 로직
// 2. 작성자 : 김재영, 황미경
@Slf4j
@RequiredArgsConstructor
@Service
public class GameService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final GameRoomRepository gameRoomRepository;
    private final GameRoomUserRepository gameRoomUserRepository;
    private final GameInfoRepository gameInfoRepository;
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
        if (!gameRoom.getHostNick().equals(user.getNickname())) {
            throw new CustomException(StatusMsgCode.YOUR_NOT_HOST);
        }

        // 게임이 시작되었습니다
        gameRoom.GameRoomStatusUpdate("true");

        return new MsgResponseDto(StatusMsgCode.START_GAME);
        }
    }

//    public String gameStarter(    )
    
//
//
//
//        // 게임을 시작하면 기본으로 제공되는 제시어
//        HashMap<String, String> keywordMap = new HashMap<>();
//
//        // 최초 제시어를 조합해서 GameRoomUser 와 매칭
//        for (GameRoomUser gameRoomUser : gameRoomUserList) {
//            // 형용사 리스트중 1개
//            int adId = (int) (Math.random() * adSize +1);
//            AdjectiveEntity adjectiveEntity = adjectiveRepository.findById(adId).orElse(null);
//
//            // 명사 리스트중 1개
//            int nuId = (int) (Math.random() * nounSize +1);
//            NounEntity nounEntity = nounRepository.findById(nuId).orElse(null);
//
//            // 형용사 + 명사
//            String keyword = adjectiveEntity.getAdjective() + nounEntity.getNoun();
//
//            keywordMap.put(gameRoomUser.getNickname(), keyword);
//        }
//
//        // 새로운 게임 생성
//        GameInfo gameInfo = GameInfo.builder()
//                .gameRoomId(gameRoom.getId())
//                .roundTimeout(60)
//                .build();
//
//        GameStartResponseDto gameStartResponseDto = GameStartResponseDto.builder()
//                .gameRoomId(gameInfo.getGameRoomId())
//                .roundTimeout(gameInfo.getRoundTimeout())
//                .build();
//
//        // 새로운 게임 저장
//        gameInfoRepository.save(gameInfo);
//
//        // 시작된 게임정보 반환
//        return new DataMsgResponseDto(StatusMsgCode.START_GAME,gameStartResponseDto);
//    }

// websocket/keyword
//    api(순번<- 게임룸 유저 테이블의 PK값, 라운드 0, {라운드데이터})
//1user : 제시어 api(1,1)
//2user : 제시어 api(2,1)
//3user : 제시어 api(3,1)
//4user : 제시어 api(4,1)
//5user : 제시어 api(5,1)

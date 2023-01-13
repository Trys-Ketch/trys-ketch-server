package com.project.trysketch.service;

import com.project.trysketch.dto.response.ImageResponseDto;
import com.project.trysketch.dto.response.KeywordResponseDto;
import com.project.trysketch.entity.GameFlow;
import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.repository.GameFlowRepository;
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
import org.springframework.transaction.annotation.Transactional;
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
    private final AdjectiveRepository adjectiveRepository;
    private final NounRepository nounRepository;
    private final GameFlowRepository gameFlowRepository;
    private final int adSize = 117;
    private final int nounSize = 1335;


    // 게임 시작
    @Transactional
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

        // GameRoom 의 상태를 true 로 변경
        gameRoom.GameRoomStatusUpdate("true");
        
        // 게임 시작
        return new MsgResponseDto(StatusMsgCode.START_GAME);
    }

    // 게임 종료
    @Transactional
    public MsgResponseDto endGame(Long roomId) {

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(roomId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 현재 GameRoom 이 시작되지 않았다면
        if (gameRoom.getStatus().equals("false")){
            throw new CustomException(StatusMsgCode.NOT_STARTED_YET);
        }

        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate("false");

        // 게임 종료
        return new MsgResponseDto(StatusMsgCode.END_GAME);
    }

    // 강제 종료( 비정상적인 종료 )
    @Transactional
    public MsgResponseDto shutDownGame(Long roomId) {

        return new MsgResponseDto(StatusMsgCode.SHUTDOWN_GAME);
    }



    // 단어 적는 라운드 끝났을 때 GameFlow 에 결과 저장
    @Transactional
    public MsgResponseDto submitVocabulary(int round, int keywordIndex, String keyword, Long roomId) {
        GameFlow gameFlow = GameFlow.builder()
                .roomId(roomId)
                .round(round)
                .keywordIndex(keywordIndex)
                .keyword(keyword)
                .build();
        gameFlowRepository.save(gameFlow);
        return new MsgResponseDto(StatusMsgCode.SUBMIT_KEYWORD_DONE);
    }


    // 그림그리는 라운드 시작  ← 이전 라운드의 단어 response 로 줘야함! (GetMapping)
    public KeywordResponseDto startImageRound(int round, int keywordIndex, Long roomId) {

        // 라운드가 0인지 아닌지 검증
        if (round <= 0){
            throw new CustomException(StatusMsgCode.GAME_NOT_ONLINE);
        }
        //  이전 라운드의 단어 어떤 것이었는 지 알려줘야 함
        int previousRound = round - 1;
        GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(roomId, previousRound, keywordIndex).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_NOT_ONLINE)
        );

        return new KeywordResponseDto(gameFlow); // 수정
    }

    // 그림그리는 라운드 끝났을 때 GameFlow 에 결과 저장 (PostMapping)
    @Transactional
    public MsgResponseDto submitImage(int round, int keywordIndex, String imagePath, Long roomId) {
        GameFlow gameFlow = GameFlow.builder()
                .roomId(roomId)
                .round(round)
                .keywordIndex(keywordIndex)
                .imagePath(imagePath)
                .build();


        gameFlowRepository.save(gameFlow);
        return new MsgResponseDto(StatusMsgCode.SUBMIT_IMAGE_DONE);
    }


    // 단어적는 라운드 시작  ← 이전 라운드의 그림 response 로 줘야함! (GepMapping)
    public ImageResponseDto startKeywordRound(int round, int keywordIndex, Long roomId) {

        // 라운드가 0인지 아닌지 검증
        if (round <= 0){
            throw new CustomException(StatusMsgCode.GAME_NOT_ONLINE);
        }

        //  이전 라운드에 어떤 그림 이었는 지 알려줘야 함
        int previousRound = round - 1;
        GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(roomId, previousRound, keywordIndex).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_NOT_ONLINE)
        );

        return new ImageResponseDto(gameFlow);
    }








    
}



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

package com.project.trysketch.service;

import com.project.trysketch.dto.response.ImageResponseDto;
import com.project.trysketch.dto.response.KeywordResponseDto;
import com.project.trysketch.entity.GameFlow;
import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.image.AmazonS3Service;
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
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
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
    private final AmazonS3Service s3Service;
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
        gameRoom.GameRoomStatusUpdate(true);

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
        if (!gameRoom.isStatus()) {
            throw new CustomException(StatusMsgCode.NOT_STARTED_YET);
        }

        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate(false);

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
    public MsgResponseDto postVocabulary(int round, int keywordIndex, String keyword, Long roomId, HttpServletRequest request) {
        // 혁수님께서 가공하신 유저인증 메서드는 일단 패스 !
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        GameFlow gameFlow = GameFlow.builder()
                .roomId(roomId)
                .round(round)
                .keywordIndex(keywordIndex)
                .keyword(keyword)
                .nickname(user.getNickname())
                .build();
        gameFlowRepository.save(gameFlow);
        return new MsgResponseDto(StatusMsgCode.SUBMIT_KEYWORD_DONE);
    }


    // 그림그리는 라운드 시작  ← 이전 라운드의 단어 response 로 줘야함! (GetMapping)
    public KeywordResponseDto getPreviousKeyword(int round, int keywordIndex, Long roomId) {

        // 라운드가 0인지 아닌지 검증
        if (round <= 0) {
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
    public MsgResponseDto postImage(int round, int keywordIndex, HttpServletRequest request, MultipartFile multipartFile, Long roomId) throws IOException {
        // 혁수님께서 가공하신 유저인증 메서드는 일단 패스 !
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // Image 엔티티 painter( 그린사람 nickname )+ imagePath 저장
        // image 엔티티 imagePath → s3 저장
        if (multipartFile != null) {
            s3Service.upload(multipartFile, "static", user, round, keywordIndex, roomId);
        }
//        GameFlow gameFlow = GameFlow.builder()
//                .roomId(roomId)
//                .round(round)
//                .keywordIndex(keywordIndex)
//                .imagePath(imagePath)
//                .build();

        return new MsgResponseDto(StatusMsgCode.SUBMIT_IMAGE_DONE);
    }


    // 단어적는 라운드 시작  ← 이전 라운드의 그림 response 로 줘야함! (GepMapping)
    public ImageResponseDto getPreviousImage(int round, int keywordIndex, Long roomId) {

        // 라운드가 0인지 아닌지 검증
        if (round <= 0) {
            throw new CustomException(StatusMsgCode.GAME_NOT_ONLINE);
        }

        //  이전 라운드에 어떤 그림 이었는 지 알려줘야 함
        int previousRound = round - 1;
        GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(roomId, previousRound, keywordIndex).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_NOT_ONLINE)
        );

        return new ImageResponseDto(gameFlow);
    }

    // 결과 보여주기
    public String[][] getGameFlow(Long roomId) {

        // 현재 방의 정보로 GameFlow 정보 List 형태로 가져오기
        List<GameFlow> gameFlowList = gameFlowRepository.findAllByRoomId(roomId);

        if (gameFlowList.isEmpty()) {
            throw new CustomException(StatusMsgCode.GAMEFLOW_NOT_FOUND);
        }

        // 총 라운드 수
        int roundTotalNum = 0;
        for (GameFlow gameFlow : gameFlowList) {
            roundTotalNum = Math.max(gameFlow.getRound(), roundTotalNum);
        }

        String[][] listlist = new String[roundTotalNum][roundTotalNum];
        for (int i = 1; i <= roundTotalNum; i++) {
            for (int j = 1; j <= roundTotalNum; j++) {
                if (j % 2 == 0) {
                    // 짝수 round 일 때 -> 이미지 불러와야 함
                    GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(roomId, j, i).orElseThrow(
                            () -> new CustomException(StatusMsgCode.GAMEFLOW_NOT_FOUND)
                    );
                    listlist[i - 1][j - 1] = gameFlow.getImagePath();

                } else {
                    // 홀수 round 일 때 -> 제시어 가져오기!
                    GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(roomId, j, i).orElseThrow(
                            () -> new CustomException(StatusMsgCode.GAMEFLOW_NOT_FOUND)
                    );
                    listlist[i - 1][j - 1] = gameFlow.getKeyword();
                }
            }
        }

        return listlist;
    }

}


//        (키워드인덱스, round)
//            1,1   1,2    1,3   1,4   1,5
//         [제시어, 그림, 제시어, 그림, 제시어],
//            2,1   2,2    2,3   2,4   2,5
//         [제시어, 그림, 제시어, 그림, 제시어],
//            3,1   3,2   3,3    3,4   3,5
//         [제시어, 그림, 제시어, 그림, 제시어],
//            4,1   4,2    4,3   4,4   4,5
//         [제시어, 그림, 제시어, 그림, 제시어],
//           5,1    5,2   5,3    5,4   5,5
//         [제시어, 그림, 제시어, 그림, 제시어]
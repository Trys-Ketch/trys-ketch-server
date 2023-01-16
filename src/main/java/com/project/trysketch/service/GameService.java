package com.project.trysketch.service;

import com.project.trysketch.dto.request.GameFlowRequestDto;
import com.project.trysketch.dto.response.ImageResponseDto;
import com.project.trysketch.dto.response.KeywordResponseDto;
import com.project.trysketch.entity.GameFlow;
import com.project.trysketch.entity.GameRoom;
import com.project.trysketch.entity.GameRoomUser;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.image.AmazonS3Service;
import com.project.trysketch.redis.dto.GamerEnum;
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
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final UserService userService;
    private final SimpMessageSendingOperations sendingOperations;
    private final int adSize = 117;
    private final int nounSize = 1335;
    private final String directoryName = "static";


    // 아래에서 사용되는 convertAndSend 를 사용하기 위해서 선언
    // convertAndSend 는 객체를 인자로 넘겨주면 자동으로 Message 객체로 변환 후 도착지로 전송한다.

    // log template
    // log.info(">>>>>>> [GameService] 내용 이것저것 : {}", 들어갈객체);
    // 유저가 있으면 다찍기
    // requestDto 상태변경시 비포 에프터 찍기
    // roomID 찍기

    // 게임 시작
    // requestDto 필요한 정보
    // token, roomId
    @Transactional
    public MsgResponseDto startGame(GameFlowRequestDto requestDto) {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - startGame >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService] RoomId : {}", requestDto.getRoomId());
        log.info(">>>>>>> [GameService] gamerInfo : {}", gamerInfo.toString());

//        gamerInfo.get(GamerKey.NICK.key()); // 검증을 통과한 User 의 닉네임

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        log.info(">>>>>>> [GameService] gameRoom 변경 전 : {}", gameRoom.toString());

        // 방장이 아닐경우
        if (!gameRoom.getHostNick().equals(gamerInfo.get(GamerEnum.NICK.key()))) {
            throw new CustomException(StatusMsgCode.YOUR_NOT_HOST);
        }

        // GameRoom 의 상태를 true 로 변경
        gameRoom.GameRoomStatusUpdate(true);
        log.info(">>>>>>> [GameService] gameRoom 변경 후 : {}", gameRoom);

        // 구독하고 있는 User 에게 start 메세지 전송
        sendingOperations.convertAndSend("/topic/game/room/" + requestDto.getRoomId(),"start");

        log.info(">>>>>>> [GameService] requestDto : {}", requestDto);


        // 게임 시작
        return new MsgResponseDto(StatusMsgCode.START_GAME);
    }

    // 게임 종료
    // requestDto 필요한 정보
    // roomId
    @Transactional
    public MsgResponseDto endGame(GameFlowRequestDto requestDto) {

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        log.info(">>>>>>> [GameService]-endgame roomId {}", requestDto.getRoomId());

        // 현재 GameRoom 이 시작되지 않았다면
        if (!gameRoom.isPlaying()) {
            throw new CustomException(StatusMsgCode.NOT_STARTED_YET);
        }

        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());
        log.info(">>>>>>> [GameService]-endgame gameRoom 의 id : {}", gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate(false);

        // 구독하고 있는 User 에게 start 메세지 전송
        sendingOperations.convertAndSend("/topic/game/room/" + requestDto.getRoomId(),"end");
        log.info(">>>>>>> [GameService]-endgame 의 roomId : {}", requestDto.getRoomId());

        // 게임 종료
        return new MsgResponseDto(StatusMsgCode.END_GAME);
    }

    // 강제 종료( 비정상적인 종료 )
    @Transactional
    public MsgResponseDto shutDownGame(GameFlowRequestDto requestDto) {

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        log.info(">>>>>>> [GameService]-shutDownGame roomId {}", requestDto.getRoomId());

        // 현재 GameRoom 이 시작되지 않았다면
        if (!gameRoom.isPlaying()) {
            throw new CustomException(StatusMsgCode.NOT_STARTED_YET);
        }

        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());
        log.info(">>>>>>> [GameService]-shutDownGame gameRoom 의 id : {}", gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate(false);

        // 구독하고 있는 User 에게 start 메세지 전송
        sendingOperations.convertAndSend("/topic/game/room/" + requestDto.getRoomId(),"shutDown");
        log.info(">>>>>>> [GameService]-shutDownGame 의 roomId : {}", requestDto.getRoomId());

        // 게임 강제 종료
        return new MsgResponseDto(StatusMsgCode.SHUTDOWN_GAME);
    }


    // 단어 적는 라운드 끝났을 때 GameFlow 에 결과 저장
    // requestDto 필요한 정보
    // token, roomId, round,keywordIndex, round, keyword
    @Transactional
    public MsgResponseDto postVocabulary(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - postVocabulary >>>>>>>>>>>>>>>>>>>>>>>>");
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>> [GameService] - postVocabulary 의 유저 해쉬맵 : {}", gamerInfo);

        GameFlow gameFlow = GameFlow.builder()
                .roomId(requestDto.getRoomId())
                .round(requestDto.getRound())
                .keywordIndex(requestDto.getKeywordIndex())
                .keyword(requestDto.getKeyword())
                .nickname(gamerInfo.get(GamerEnum.NICK.key()))
                .build();
        gameFlowRepository.save(gameFlow);
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 방 번호 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 라운드 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 키워드 번호 : {}", gameFlow.getKeywordIndex());
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 키워드 : {}", gameFlow.getKeyword());
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 닉네임 : {}", gameFlow.getNickname());

        return new MsgResponseDto(StatusMsgCode.SUBMIT_KEYWORD_DONE);
    }


    // 그림그리는 라운드 시작  ← 이전 라운드의 단어 response 로 줘야함! (GetMapping)
    // requestDto 필요한 정보
    // token, round, roomId, keywordIndex
    public KeywordResponseDto getPreviousKeyword(GameFlowRequestDto requestDto) {

        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - getPreviousKeyword >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService] - getPreviousKeyword 라운드 : {}", requestDto.getRound());
        // 라운드가 0인지 아닌지 검증
        if (requestDto.getRound() <= 0) {
            throw new CustomException(StatusMsgCode.GAME_NOT_ONLINE);
        }

        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>> [GameService] - getPreviousKeyword 접속한 유저 정보 : {}", gamerInfo);

        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserIdAndGameRoomId(
                Long.valueOf(gamerInfo.get(GamerEnum.ID.key())), requestDto.getRoomId());

        log.info(">>>>>>> [GameService] - getPreviousKeyword 이전 라운드 단어 : {}", gamerInfo);
        // 이전 라운드의 단어 어떤 것이었는 지 알려줘야 함
        int previousRound = requestDto.getRound() - 1;
        GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(
                        requestDto.getRoomId(),
                        previousRound,
                        requestDto.getKeywordIndex()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_NOT_ONLINE)
        );
        log.info(">>>>>>> [GameService] - getPreviousKeyword 의 GameFlow -> 방 번호 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService] - getPreviousKeyword 의 GameFlow -> 라운드 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService] - getPreviousKeyword 의 GameFlow -> 키워드 번호 : {}", gameFlow.getKeywordIndex());
        log.info(">>>>>>> [GameService] - getPreviousKeyword 의 GameFlow -> 키워드 : {}", gameFlow.getKeyword());
        log.info(">>>>>>> [GameService] - getPreviousKeyword 의 GameFlow -> 닉네임 : {}", gameFlow.getNickname());

        log.info(">>>>>>> [GameService] - getPreviousKeyword 접속한 유저의 WebSessionId : {}", gameRoomUser.getWebSessionId());
        sendingOperations.convertAndSendToUser(gameRoomUser.getWebSessionId(), "/queue/game", new KeywordResponseDto(gameFlow));
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
            s3Service.upload(multipartFile, directoryName, user, round, keywordIndex, roomId);
        }

        return new MsgResponseDto(StatusMsgCode.SUBMIT_IMAGE_DONE);
    }


    // 단어적는 라운드 시작  ← 이전 라운드의 그림 response 로 줘야함! (GepMapping)
    // requestDto 필요한 정보
    // round,  roomId, , keywordIndex token,
    public ImageResponseDto getPreviousImage(GameFlowRequestDto requestDto) {

        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - getPreviousImage >>>>>>>>>>>>>>>>>>>>>>>>");
        if (requestDto.getRound() <= 0) {
            throw new CustomException(StatusMsgCode.GAME_NOT_ONLINE);
        }

        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>> [GameService] - getPreviousImage 의 유저 해쉬맵 : {}", gamerInfo);

        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserIdAndGameRoomId(
                Long.valueOf(gamerInfo.get(GamerEnum.ID.key())), requestDto.getRoomId());
        log.info(">>>>>>> [GameService] - getPreviousImage 의 GameRoomUser -> 방 번호 : {}", gameRoomUser.getGameRoom());
        log.info(">>>>>>> [GameService] - getPreviousImage 의 GameRoomUser -> 플레이 상태 : {}", gameRoomUser.isReadyStatus());

        //  이전 라운드에 어떤 그림 이었는 지 알려줘야 함
        int previousRound = requestDto.getRound() - 1;
        GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(requestDto.getRoomId(), previousRound, requestDto.getKeywordIndex()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_NOT_ONLINE)
        );
        log.info(">>>>>>> [GameService] - getPreviousImage 의 previousRound -> 이전 라운드 : {}", previousRound);

        sendingOperations.convertAndSendToUser(gameRoomUser.getWebSessionId(), "/queue/game", new ImageResponseDto(gameFlow));
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

        // 반환될 2차원 배열 선언
        String[][] listlist = new String[roundTotalNum][roundTotalNum];

        for (int i = 1; i <= roundTotalNum; i++) {
            for (int j = 1; j <= roundTotalNum; j++) {

                List<String> resultList = new ArrayList<>();

                if (j % 2 == 0) {
                    // 짝수 round 일 때 -> 이미지 불러와야 함
                    GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(roomId, j, i).orElseThrow(
                            () -> new CustomException(StatusMsgCode.GAMEFLOW_NOT_FOUND)
                    );

                    resultList.add(gameFlow.getNickname());
                    resultList.add(gameFlow.getImagePath());

                    listlist[i - 1][j - 1] = resultList.toString();

                } else {
                    // 홀수 round 일 때 -> 제시어 가져오기!
                    GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(roomId, j, i).orElseThrow(
                            () -> new CustomException(StatusMsgCode.GAMEFLOW_NOT_FOUND)
                    );

                    resultList.add(gameFlow.getNickname());
                    resultList.add(gameFlow.getKeyword());

                    listlist[i - 1][j - 1] = resultList.toString();
                }
            }
        }

        return listlist;
    }

}

//                  // 써야 할 기능
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
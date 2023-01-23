package com.project.trysketch.service;

import com.project.trysketch.dto.request.GameFlowRequestDto;
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
import com.project.trysketch.suggest.AdjectiveEntity;
import com.project.trysketch.suggest.AdjectiveRepository;
import com.project.trysketch.suggest.NounEntity;
import com.project.trysketch.suggest.NounRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;



// 1. 기능   : 프로젝트 메인 로직
// 2. 작성자 : 김재영, 황미경, 안은솔
@Slf4j
@RequiredArgsConstructor
@Service
public class GameService {
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


    // convertAndSend 는 객체를 인자로 넘겨주면 자동으로 Message 객체로 변환 후 도착지로 전송한다.

    // 게임 시작
    // requestDto 필요한 정보
    // token, roomId
    @Transactional
    public MsgResponseDto startGame(GameFlowRequestDto requestDto) {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - startGame] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - startGame] RoomId : {}", requestDto.getRoomId());
        log.info(">>>>>>> [GameService - startGame] gamerInfo id : {}", gamerInfo.get(GamerEnum.ID.key()));
        log.info(">>>>>>> [GameService - startGame] gamerInfo nickname : {}", gamerInfo.get(GamerEnum.NICK.key()));

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 방장이 아닐경우
        if (!gameRoom.getHostNick().equals(gamerInfo.get(GamerEnum.NICK.key()))) {
            throw new CustomException(StatusMsgCode.YOUR_NOT_HOST);
        }

        // GameRoom 의 상태를 true 로 변경
        gameRoom.GameRoomStatusUpdate(true);

        // isIngaeme 으로 구독하고 있는 User 에게 start 메세지 전송
        Map<String, Boolean> message = new HashMap<>();
        message.put("isIngame", true);
        sendingOperations.convertAndSend("/topic/game/start/" + requestDto.getRoomId(), message);

        // 게임 시작
        return new MsgResponseDto(StatusMsgCode.START_GAME);
    }

    // 게임 종료
    // requestDto 필요한 정보
    // token, roomId
    @Transactional
    public MsgResponseDto endGame(GameFlowRequestDto requestDto) {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - endGame] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - endGame] RoomId : {}", requestDto.getRoomId());
        log.info(">>>>>>> [GameService - endGame] gamerInfo id : {}", gamerInfo.get(GamerEnum.ID.key()));
        log.info(">>>>>>> [GameService - endGame] gamerInfo nickname : {}", gamerInfo.get(GamerEnum.NICK.key()));

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 방장 정보 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserIdAndGameRoomId(
                Long.valueOf(gamerInfo.get(GamerEnum.ID.key())),
                requestDto.getRoomId());

        // 방장의 webSessionId 가져오기
        String hostWebSessionId = "";
        log.info(">>>>>>> [GameService - endGame] gameRoom.getHostId() {}", gameRoom.getHostId());
        log.info(">>>>>>> [GameService - endGame] gameRoomUser.getUserId() {}", gameRoomUser.getUserId());
        if (gameRoom.getHostId().equals(gameRoomUser.getUserId())) {
            hostWebSessionId = gameRoomUser.getWebSessionId();
        }else {
            throw new CustomException(StatusMsgCode.YOUR_NOT_HOST);
        }
        log.info(">>>>>>> [GameService - endGame] hostWebSessionId {}", hostWebSessionId);

        // 현재 GameRoom 이 시작되지 않았다면
        if (!gameRoom.isPlaying()) {
            throw new CustomException(StatusMsgCode.NOT_STARTED_YET);
        }

        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate(false);

        // end 로 구독하고 있는 User 에게 end 메세지 전송
        Map<String, Boolean> message = new HashMap<>();
        message.put("end", true);
        sendingOperations.convertAndSend("/topic/game/end/" + requestDto.getRoomId(), message);

        // 게임 종료
        return new MsgResponseDto(StatusMsgCode.END_GAME);
    }

    // 강제 종료( 비정상적인 종료 )
    @Transactional
    public MsgResponseDto shutDownGame(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - shutDownGame] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - shutDownGame] RoomId : {}", requestDto.getRoomId());

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 현재 GameRoom 이 시작되지 않았다면
        if (!gameRoom.isPlaying()) {
            throw new CustomException(StatusMsgCode.NOT_STARTED_YET);
        }

        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate(false);

        // 구독하고 있는 User 에게 start 메세지 전송
        sendingOperations.convertAndSend("/topic/game/shutDown/" + requestDto.getRoomId(),"shutDown");

        // 게임 강제 종료
        return new MsgResponseDto(StatusMsgCode.SHUTDOWN_GAME);
    }

    // 최초 디폴트 제시어 던져주기
    // requestDto 필요한 정보
    // token, roomId
    @Transactional
    public void getRandomKeyword(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getRandomKeyword] >>>>>>>>>>>>>>>>>>>>>>>>");

        // 해당 gameRoom 의 전체 유저 리스트 조회
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

        // GameRoomUser 돌면서 키워드 전송
        for (int i = 0; i < gameRoomUserList.size(); i++) {

            String webSessionId = gameRoomUserList.get(i).getWebSessionId();

            // 형용사 리스트중 1개
            int adId = (int) (Math.random() * adSize + 1);
            AdjectiveEntity adjectiveEntity = adjectiveRepository.findById(adId).orElse(null);

            // 명사 리스트중 1개
            int nuId = (int) (Math.random() * nounSize + 1);
            NounEntity nounEntity = nounRepository.findById(nuId).orElse(null);

            Map<String, Object> message = new HashMap<>();

            message.put("keyword", adjectiveEntity.getAdjective() + nounEntity.getNoun());
            message.put("keywordIndex", i + 1);

            sendingOperations.convertAndSend("/queue/game/keyword/" + webSessionId, message);
        }
    }

    // 라운드 끝났을 때, 기존 데이터에 따른 제출 여부 확인과 GameFlow DB 저장
    // requestDto 필요한 정보
    // token, roomId, round, keyword, keywordIndex, image, webSessionId, isSubmitted
    @Transactional
    public void getToggleSubmit(GameFlowRequestDto requestDto) throws IOException {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getToggleSubmit] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - getToggleSubmit] gamerInfo id : {}", gamerInfo.get(GamerEnum.ID.key()));
        log.info(">>>>>>> [GameService - getToggleSubmit] gamerInfo nickname : {}", gamerInfo.get(GamerEnum.NICK.key()));
        log.info(">>>>>>> [GameService - getToggleSubmit] 제출이면 false, 아니면 true : {}", requestDto.isSubmitted());

        // 1. 받아온 유저의 게임 정보가 gameFlow 에 있다면 -> 기존에서 변경
        GameFlow gameFlow;
        if (gameFlowRepository.existsByRoomIdAndRoundAndWebSessionId(
                requestDto.getRoomId(),
                requestDto.getRound(),
                requestDto.getWebSessionId()
        )) {

            // roomId, round, webSessionId 로 조회
            gameFlow = gameFlowRepository.findByRoomIdAndRoundAndWebSessionId(
                    requestDto.getRoomId(),
                    requestDto.getRound(),
                    requestDto.getWebSessionId()
            );

            // 1-1. isSubmitted 의 상태가 true 라면 (취소) -> 제출 상태만 update
            if (gameFlow.isSubmitted()) {
                gameFlow.update(!requestDto.isSubmitted());
                log.info(">>>>>>> [GameService - getToggleSubmit] {} 에서 {}로 상태 변경", requestDto.isSubmitted(), !requestDto.isSubmitted());
            }

            // 1-2. isSubmitted 의 상태가 false 라면 (제출) -> 관련 내용 update
            else {

                // 1-2-1. image 가 없다면 -> keyword
                if (requestDto.getImage() == null || requestDto.getImage().length() == 0) {
                    gameFlow.update(!requestDto.isSubmitted(),
                            requestDto.getKeyword());
                    log.info(">>>>>>> [GameService - getToggleSubmit] 변경된 키워드 저장 : {}", requestDto.getKeyword());
                }

                // 1-2-2. image 가 있다면 -> image
                else {
                    gameFlow.update(saveImage(requestDto),
                            !requestDto.isSubmitted());
                    log.info(">>>>>>> [GameService - getToggleSubmit] 변경된 이미지 저장 : {}", requestDto.getImage());
                }
            }
        }

        // 2. 받아온 유저의 정보가 gameFlow 에 없다면 -> 새롭게 생성
        else {

            // 2-1. image 가 없다면 -> keyword 로 저장
            if (requestDto.getImage() == null || requestDto.getImage().length() == 0) {
                gameFlow = GameFlow.builder()
                        .roomId(requestDto.getRoomId())
                        .round(requestDto.getRound())
                        .keywordIndex(requestDto.getKeywordIndex())
                        .keyword(requestDto.getKeyword())
                        .nickname(gamerInfo.get(GamerEnum.NICK.key()))
                        .webSessionId(requestDto.getWebSessionId())
                        .isSubmitted(!requestDto.isSubmitted()).build();
                log.info(">>>>>>> [GameService - getToggleSubmit] 처음으로 제출하는 유저의 키워드 : {}", gameFlow.getKeyword());
            }

            // 2-2. image 가 있다면 -> image 로 저장
            else {
                gameFlow = GameFlow.builder()
                        .roomId(requestDto.getRoomId())
                        .round(requestDto.getRound())
                        .keywordIndex(requestDto.getKeywordIndex())
                        .imagePath(saveImage(requestDto))
                        .nickname(gamerInfo.get(GamerEnum.NICK.key()))
                        .webSessionId(requestDto.getWebSessionId())
                        .isSubmitted(!requestDto.isSubmitted()).build();
                log.info(">>>>>>> [GameService - getToggleSubmit] 처음으로 제출하는 유저의 이미지 : {}", gameFlow.getImagePath());
            }
        }

        gameFlowRepository.saveAndFlush(gameFlow);

        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 방 번호 : {}", gameFlow.getRoomId());
        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 라운드 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 닉네임 : {}", gameFlow.getNickname());
        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 보내는 사람 세션Id : {}", gameFlow.getWebSessionId());
        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 제출 여부 : {}", gameFlow.isSubmitted());

        // DB 기준 제출 여부 조회 후 메시지 전송
        sendSubmitMessage(requestDto);
    }

    // 받아온 그림 S3에 저장 후 imagePath 반환
    @Transactional
    public String saveImage(GameFlowRequestDto requestDto) throws IOException {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - saveImage] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - saveImage] 이미지 파일 있니? : {}", !requestDto.getImage().isEmpty());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - saveImage] image.length() : {}", requestDto.getImage().length());

        // data로 들어온  'data:image/png;base64,iVBORw0KGgoAAA..... 문자열 자르기
        String[] strings = requestDto.getImage().split(",");
        String base64Image = strings[1];
        String extension = switch (strings[0]) {
            case "data:image/jpeg;base64" -> "jpeg";
            case "data:image/png;base64" -> "png";
            default -> "jpg";
        };

        // 자른 base64 코드를 byte 배열로 파싱
        byte[] imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(base64Image);
        String path = "/home/ubuntu/projects/image/image." + extension;
        File file = new File(path);

        // 파싱된 byte 배열을 ByteArrayInputStream 클래스로 넣어 읽기
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));

        // 이미지를 해당 포맷으로 path 위치에 저장
        ImageIO.write(img, "png", file);

        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        String nickname = gamerInfo.get(GamerEnum.NICK.key());
        log.info(">>>>>>> [GameService - saveImage] token 으로 부터 나온 nickname : {}", nickname);

        // image entity painter( 그린사람 nickname )
        // s3 저장 → image DB 저장 → imagePath 반환
        return s3Service.upload(file, directoryName, nickname);
    }

    // 전체 유저의 제출 여부와 해당 유저의 제출 여부 조회 후 메시지 전송
    @Transactional
    public void sendSubmitMessage(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - sendSubmitMessage] >>>>>>>>>>>>>>>>>>>>>>>>");
        List<GameFlow> gameFlowList = gameFlowRepository.findAllByRoomIdAndRound(requestDto.getRoomId(), requestDto.getRound());
        List<GameRoomUser> gameRoomUserList  = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

        // 전체 유저의 제출 여부와 해당 유저의 제출 여부 조회
        boolean allFlag = true;
        boolean userFlag = true;
        for (GameFlow gameflow : gameFlowList) {
            if (gameflow.getWebSessionId().equals(requestDto.getWebSessionId())) {
                userFlag = gameflow.isSubmitted();
            }
            if (!gameflow.isSubmitted()) {
                allFlag = false;
            }
        }
        log.info(">>>>>>> [GameService - getToggleSubmit] 전체가 다 제출했습니까?! : {}", allFlag);

        // 본인에게 본인 제출 여부 메시지 전송
        Map<String, Object> submitMessage = new HashMap<>();
        submitMessage.put("isSubmitted", userFlag);
        sendingOperations.convertAndSend("/queue/game/is-submitted/" + requestDto.getWebSessionId(), submitMessage);
        log.info(">>>>>>> [GameService - getToggleSubmit] 제출할 때 마다 개인에게 메시지 전송 성공! : {}", submitMessage);

        // 이미지 포함 여부에 따라 destination 부여
        String destination = requestDto.getImage() == null || requestDto.getImage().length() == 0 ? "word" : "image";

        // 전체 제출 여부 확인
        if (gameFlowList.size() == gameRoomUserList.size() && allFlag) {

            // 전체가 제출 했다면 모두에게 메시지 전송
            Map<String, Object> allSubmitMessage = new HashMap<>();
            allSubmitMessage.put("completeSubmit", true);
            sendingOperations.convertAndSend("/topic/game/submit-" + destination + "/" + requestDto.getRoomId(), allSubmitMessage);
            log.info(">>>>>>> [GameService - isAllSubmit] 전체 제출 후 메시지 전송 성공! : {}", allSubmitMessage);
        }
    }


    // 제출 여부와 라운드를 확인하고, 이전 라운드 제시어 or 그림 제시 메서드를 호출 하거나 결과 메서드 호출
    // requestDto 필요한 정보
    // token, roomId, round, keyword, keywordIndex, webSessionId
    @Transactional
    public void checkSubmit(GameFlowRequestDto requestDto, String destination) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - checkSubmit] >>>>>>>>>>>>>>>>>>>>>>>>");

        // 라운드 별 진행된 게임 리스트와 현재 방의 전체 유저 리스트 조회
        List<GameFlow> gameFlowList = gameFlowRepository.findAllByRoomIdAndRound(requestDto.getRoomId(), requestDto.getRound());
        List<GameRoomUser> gameRoomUserList  = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

        // 전체 제출 여부 확인
        if (gameFlowList.size() == gameRoomUserList.size()) {

            // 라운드를 계속 체크해서 라운드가 인원수와 같다면 [결과페이지 이동]
            if (requestDto.getRound() == gameRoomUserList.size()) {
                log.info(">>>>>>> [GameService - checkSubmit] 마지막 라운드 : {}", requestDto.getRound());
                sendResultMessage(requestDto.getRoomId());
            }

            // 라운드가 인원수와 다르다면 이전 제시어 or 그림 불러오기
            else {
                getPrevious(requestDto, destination);
            }
        }
    }

    // 모두에게 결과 메시지 전송
    @Transactional
    public void sendResultMessage(Long roomId) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - sendResultMessage] >>>>>>>>>>>>>>>>>>>>>>>>");
        Map<String, Object> beforeMessage = new HashMap<>();
        beforeMessage.put("isResult", true);
        sendingOperations.convertAndSend("/topic/game/before-result/" + roomId, beforeMessage);
    }


    // 라운드 시작  ← 이전 라운드의 제시어 or 그림 제시
    // requestDto 필요한 정보
    // token, round, roomId, keywordIndex, keyword
    @Transactional
    public void getPrevious(GameFlowRequestDto requestDto, String destination) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getPrevious] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - getPrevious] 라운드 : {}", requestDto.getRound());
        log.info(">>>>>>> [GameService - getPrevious] 받는사람 키워드 순번 : {}", requestDto.getKeywordIndex());

        // 라운드가 0인지 아닌지 검증
        if (requestDto.getRound() <= 0) {
            throw new CustomException(StatusMsgCode.GAME_NOT_ONLINE);
        }

        // 현재 GameRoom 의 UserList 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

        // 다음순번인 키워드의 index 계산하고 DB 에서 조회
        int nextKeywordIndex = calculateKeywordIndex(requestDto.getKeywordIndex(), gameRoomUserList.size());
        GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(
                requestDto.getRoomId(),
                requestDto.getRound(),
                nextKeywordIndex).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_NOT_ONLINE)
        );
        log.info(">>>>>>> [GameService - getPrevious] GameFlow -> 방 번호 : {}", gameFlow.getRoomId());
        log.info(">>>>>>> [GameService - getPrevious] GameFlow -> 라운드 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService - getPrevious] GameFlow -> 새로운 키워드 번호 : {}", nextKeywordIndex);
        log.info(">>>>>>> [GameService - getPrevious] GameFlow -> 새로운 키워드 : {}", gameFlow.getKeyword());
        log.info(">>>>>>> [GameService - getPrevious] GameFlow -> 닉네임 : {}", gameFlow.getNickname());
        log.info(">>>>>>> [GameService - getPrevious] GameFlow -> WebSessionId : {}", gameFlow.getWebSessionId());

        // 요청한 유저에게 다음 순번의 키워드 index 와 키워드 메시지 전송
        Map<String, Object> message = new HashMap<>();
        message.put("keywordIndex", nextKeywordIndex);

        switch (destination) {
            case "word" -> {
                message.put("keyword", gameFlow.getKeyword());
                log.info(">>>>>>> [GameService - getPreviousKeyword] 메시지 : {}", message);
            }
            case "image" -> {
                message.put("image", gameFlow.getImagePath());
                log.info(">>>>>>> [GameService - getPreviousImage] 메시지 : {}", message);
            }
            default -> {
                log.info(">>>>>>> [GameService - getPreviousImage] destination 잘못된 요청입니다.");
            }
        }
        sendingOperations.convertAndSend("/queue/game/before-" + destination + "/" + requestDto.getWebSessionId(), message);
    }

    // 다음 순번 키워드 index 계산
    @Transactional
    public int calculateKeywordIndex(int nowKeywordIndex, int maxUserSize){
        // 반환될 다음순번인 키워드의 index
        int nextKeywordIndex;
        if (nowKeywordIndex == maxUserSize){
            nextKeywordIndex = nowKeywordIndex % maxUserSize + 1;
        }else {
            nextKeywordIndex = nowKeywordIndex + 1;
        }
        return  nextKeywordIndex;
    }

    // 2차원 배열로 결과 보여주기
    // requestDto 필요한 정보
    // roomId, webSessionId, token
    public Object[][] getGameFlow(GameFlowRequestDto requestDto) {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getGameFlow] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - getGameFlow] gamerInfo id : {}", gamerInfo.get(GamerEnum.ID.key()));
        log.info(">>>>>>> [GameService - getGameFlow] gamerInfo nickname : {}", gamerInfo.get(GamerEnum.NICK.key()));

        // 요청한 유저가 방장인지 아닌지 조회
        Long userId = Long.valueOf(gamerInfo.get(GamerEnum.ID.key()));
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        boolean isHost = false;
        if (gameRoom.getHostId().equals(userId)){
            isHost = true;
        }

        // 현재 방의 정보로 GameFlow 정보 List 형태로 가져오기
        List<GameFlow> gameFlowList = gameFlowRepository.findAllByRoomId(requestDto.getRoomId());

        if (gameFlowList.isEmpty()) {
            throw new CustomException(StatusMsgCode.GAMEFLOW_NOT_FOUND);
        }

        // 총 라운드 수
        int roundTotalNum = 0;
        for (GameFlow gameFlow : gameFlowList) {
            roundTotalNum = Math.max(gameFlow.getRound(), roundTotalNum);
        }

        // 반환될 2차원 배열 선언
        Object[][] listlist = new Object[roundTotalNum][roundTotalNum];

        for (int i = 1; i <= roundTotalNum; i++) {
            for (int j = 1; j <= roundTotalNum; j++) {
                List<String> resultList = new ArrayList<>();
                GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(requestDto.getRoomId(), j, i).orElseThrow(
                        () -> new CustomException(StatusMsgCode.GAMEFLOW_NOT_FOUND)
                );
                resultList.add(gameFlow.getNickname());
                if (j % 2 == 0) {
                    // 짝수 round 일 때 -> 이미지 가져오기
                    resultList.add(gameFlow.getImagePath());
                } else {
                    // 홀수 round 일 때 -> 제시어 가져오기
                    resultList.add(gameFlow.getKeyword());
                }
                listlist[i - 1][j - 1] = resultList;
            }
        }
        log.info(">>>>>>> [GameService - getGameFlow] 대망의 마지막 2차원 배열 : {}", Arrays.deepToString(listlist));

        // 요청한 유저에게 게임 결과와 본인의 방장 여부 메시지 전송
        Map<String, Object> message = new HashMap<>();
        message.put("result", listlist);
        message.put("isHost", isHost);
        sendingOperations.convertAndSend("/queue/game/result/" + requestDto.getWebSessionId(), message);
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

package com.project.trysketch.service;

import com.project.trysketch.dto.request.GameFlowRequestDto;
import com.project.trysketch.entity.*;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.dto.GamerEnum;
import com.project.trysketch.repository.*;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.imageio.ImageIO;
import java.awt.image.*;
import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private final PlayTimeRepository playTimeRepository;
    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;
    private final AmazonS3Service s3Service;
    private final UserService userService;
    private final HistoryService historyService;
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

        // 현재 방의 유저 정보 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoom(gameRoom);

        // 게임 시작시간
        LocalDateTime startTime = LocalDateTime.of(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonth(),
                LocalDateTime.now().getDayOfMonth(),
                LocalDateTime.now().getHour(),
                LocalDateTime.now().getMinute(),
                LocalDateTime.now().getSecond());

        // 게임이 시작된 시간을 유저마다 저장
        for (GameRoomUser gameRoomUser : gameRoomUserList) {
            if (gameRoomUser.getUserId() < 10000){
                UserPlayTime userPlayTime = UserPlayTime.builder()
                        .playStartTime(startTime)
                        .playEndTime(null)
                        .gameRoomUser(gameRoomUser)
                        .gameRoomId(gameRoom.getId())
                        .build();
                playTimeRepository.save(userPlayTime);
            }
        }

        // 방장이 아닐경우
        if (!gameRoom.getHostNick().equals(gamerInfo.get(GamerEnum.NICK.key()))) {
            throw new CustomException(StatusMsgCode.HOST_AUTHORIZATION_NEED);
        }

        // GameRoom 의 상태를 true 로 변경
        gameRoom.GameRoomStatusUpdate(true);

        // GameRoom의 roundMaxNum을 저장
        gameRoom.RoundMaxNumUpdate(gameRoom.getGameRoomUserList().size());
        log.info(">>>>>>>>>>>>>> [GameService - startgame 메서드, gameRoom.getRoundMaxNum()] {}", gameRoom.getRoundMaxNum());

        // isIngame 으로 구독하고 있는 User 에게 start 메세지 전송
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
            throw new CustomException(StatusMsgCode.HOST_AUTHORIZATION_NEED);
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
        gameRoom.update(0);

        // 현재 방의 유저 정보 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoom.getId());

        // 방장 제외한 모든 유저들의 ready 상태를 false 로 변경
        for (GameRoomUser gameRoomUsers : gameRoomUserList) {
            if (!gameRoomUsers.getWebSessionId().equals(hostWebSessionId)) {
                gameRoomUsers.update(false);
                gameRoomUserRepository.save(gameRoomUsers);
            } else {
                gameRoomUsers.update(true);
                gameRoomUserRepository.save(gameRoomUsers);
            }
        }

        if (gameRoomUser.getUserId() < 10000){
            // 게임 종료시간
            LocalDateTime endTime = LocalDateTime.of(
                    LocalDateTime.now().getYear(),
                    LocalDateTime.now().getMonth(),
                    LocalDateTime.now().getDayOfMonth(),
                    LocalDateTime.now().getHour(),
                    LocalDateTime.now().getMinute(),
                    LocalDateTime.now().getSecond());

            // 현재 방의 모든 유저의 playTime 정보 가져오기
            List<UserPlayTime> userPlayTimeList = playTimeRepository.findAllByGameRoomId(gameRoom.getId());

            // 게임이 시작된 시간을 유저마다 저장
            for (GameRoomUser currentGameRoomUser : gameRoomUserList) {
                for (UserPlayTime userPlayTime : userPlayTimeList) {
                    // GameRoomUser 와 현재 for 문의 userPlayTime 의 주인이 같다면
                    if (currentGameRoomUser.equals(userPlayTime.getGameRoomUser())) {

                        // 게임 종료시간 업데이트
                        userPlayTime.updateUserPlayTime(endTime);

                        Long userId = currentGameRoomUser.getUserId();
                        log.info(">>>>>>> [GameService - endGame] userId {}", userId);

                        // 유저 정보 가져오기
                        User currentUser = userRepository.findById(userId).orElseThrow(
                                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
                        );

                        // startGame 과 endGame 의 차이 구하기
                        Duration duration = Duration.between(userPlayTime.getPlayStartTime(), userPlayTime.getPlayEndTime());

                        // 분 단위로 변환
                        Long difference = duration.getSeconds() / 60;
                        log.info(">>>>>>> [GameService - endGame] 실질적인 플레이타임 {}", difference);

                        // 해당 history 에 실질적인 플레이타임 업데이트
//                        History history = currentUser.getHistory().updatePlaytime(difference);
                        historyRepository.save(currentUser.getHistory().updatePlaytime(difference));
                        historyService.getTrophyOfTime(currentUser);

                        playTimeRepository.delete(userPlayTime);

//                        history = currentUser.getHistory().updateTrials(1L);
                        historyRepository.save(currentUser.getHistory().updateTrials(1L));
                        historyService.getTrophyOfTrial(currentUser);
                    }
                }
            }
        }

        // end 로 구독하고 있는 User 에게 end 메세지 전송
        Map<String, Boolean> message = new HashMap<>();
        message.put("end", true);
        sendingOperations.convertAndSend("/topic/game/end/" + requestDto.getRoomId(), message);

        // 게임 종료
        return new MsgResponseDto(StatusMsgCode.END_GAME);
    }

    // 강제 종료( 비정상적인 종료 )
    @Transactional
      public MsgResponseDto shutDownGame(Long gameRoomId) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - shutDownGame] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - shutDownGame] RoomId : {}", gameRoomId);

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        log.info(">>>>>>>>>>>>>>>>>>>>> [GameService - shutDownGame] gameRoom.getId() {}", gameRoom.getId());
        log.info(">>>>>>>>>>>>>>>>>>>>> [GameService - shutDownGame] gameRoom.getTitle() {}", gameRoom.getTitle());
        log.info(">>>>>>>>>>>>>>>>>>>>> [GameService - shutDownGame] gameRoom.isPlaying() {}", gameRoom.isPlaying());


        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate(false);

        // 방장 제외한 모든 유저들의 ready 상태를 false 로 변경
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoom.getId());
        for (GameRoomUser gameRoomUsers : gameRoomUserList) {
            if (!gameRoomUsers.getUserId().equals(gameRoom.getHostId())) {
                gameRoomUsers.update(false);
                log.info(">>>>>>>> [GameService - shutDownGame] 유저 readyStatus 업데이트 : {}", gameRoomUsers.isReadyStatus());
                gameRoomUserRepository.save(gameRoomUsers);
            } else {
                gameRoomUsers.update(true);
                gameRoomUserRepository.save(gameRoomUsers);
                log.info(">>>>>>>> [GameService - shutDownGame] 유저 readyStatus 업데이트 : {}", gameRoomUsers.isReadyStatus());
            }
        }

        // 구독하고 있는 User 에게 shutdown 메세지 전송
        Map<String, Boolean> message = new HashMap<>();
        message.put("shutdown", true);
        sendingOperations.convertAndSend("/topic/game/shutdown/" + gameRoomId, message);

        // 게임 강제 종료
        return new MsgResponseDto(StatusMsgCode.SHUTDOWN_GAME);
    }

    // 최초 디폴트 제시어 던져주기
    // requestDto 필요한 정보
    // token, roomId
    @Transactional
    public void getInGameData(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getRandomKeyword] >>>>>>>>>>>>>>>>>>>>>>>>");

        // 해당 gameRoom 의 전체 유저 리스트 조회
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

        // GameRoomUser 돌면서 키워드 전송
        for (int i = 0; i < gameRoomUserList.size(); i++) {

            String webSessionId = gameRoomUserList.get(i).getWebSessionId();

            // 형용사 리스트중 1개
            int adId = (int) (Math.random() * adSize + 1);
            Adjective adjective = adjectiveRepository.findById(adId).orElse(null);

            // 명사 리스트중 1개
            int nuId = (int) (Math.random() * nounSize + 1);
            Noun noun = nounRepository.findById(nuId).orElse(null);

            Map<String, Object> message = new HashMap<>();
            Map<String, Long> sendCount = new HashMap<>();

            // 제출한 인원수 및 게임중인 인원수 메시지 전송
            Long maxCount = gameRoomUserRepository.countByGameRoomId(requestDto.getRoomId());
            sendCount.put("trueCount", 0L);
            sendCount.put("maxTrueCount", maxCount);
            log.info(">>>>>>> [GameService - getInGameData] #{} 번 방의 현재 인원 : {}", requestDto.getRoomId(), maxCount);

            message.put("keyword", adjective.getAdjective() + " " + noun.getNoun());
            message.put("keywordIndex", i + 1);

            sendingOperations.convertAndSend("/topic/game/true-count/" + requestDto.getRoomId(), sendCount);
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
                    gameFlow.update(saveImage(requestDto).getPath(), // 수정 추가 김재영 01.29
                            !requestDto.isSubmitted());
                    log.info(">>>>>>> [GameService - getToggleSubmit] 변경된 이미지 저장 : {}", requestDto.getImage());
                }
            }
        }
        // 2. 받아온 유저의 정보가 gameFlow 에 없다면 -> 새롭게 생성
        else {
            GameRoomUser gameRoomUser = gameRoomUserRepository.findByWebSessionId(requestDto.getWebSessionId()).orElseThrow(
                    () -> new CustomException(StatusMsgCode.GAME_ROOM_USER_NOT_FOUND)
            );
            // 2-1. image 가 없다면 -> keyword 로 저장
            if (requestDto.getImage() == null || requestDto.getImage().length() == 0) {
                gameFlow = GameFlow.builder()
                        .roomId(requestDto.getRoomId())
                        .round(requestDto.getRound())
                        .keywordIndex(requestDto.getKeywordIndex())
                        .keyword(requestDto.getKeyword())
                        .nickname(gamerInfo.get(GamerEnum.NICK.key()))
                        .webSessionId(requestDto.getWebSessionId())
                        .userImgPath(gameRoomUser.getImgUrl())
                        .isSubmitted(!requestDto.isSubmitted()).build();
                log.info(">>>>>>> [GameService - getToggleSubmit] 처음으로 제출하는 유저의 키워드 : {}", gameFlow.getKeyword());
            }
            // 2-2. image 가 있다면 -> image 로 저장
            else {
                gameFlow = GameFlow.builder()
                        .roomId(requestDto.getRoomId())
                        .round(requestDto.getRound())
                        .keywordIndex(requestDto.getKeywordIndex())
                        .imagePath(saveImage(requestDto).getPath())
                        .imagePk(saveImage(requestDto).getId()) // 수정 추가 김재영 01.29
                        .nickname(gamerInfo.get(GamerEnum.NICK.key()))
                        .webSessionId(requestDto.getWebSessionId())
                        .userImgPath(gameRoomUser.getImgUrl())
                        .isSubmitted(!requestDto.isSubmitted()).build();
                log.info(">>>>>>> [GameService - getToggleSubmit] 처음으로 제출하는 유저의 이미지 : {}", gameFlow.getImagePath());
            }
        }
        gameFlowRepository.saveAndFlush(gameFlow);
        // 제출한 사람 인원수 및 현재 방의 플레이중인 인원수 체크
        Long trueCount = 0L;
        Long nowUserCount = gameRoomUserRepository.countByGameRoomId(gameFlow.getRoomId());
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        List<GameFlow> gameFlowList = gameFlowRepository.findAllByRoomIdAndRound(gameFlow.getRoomId(), gameFlow.getRound());
        for (GameFlow findList : gameFlowList) {
            if (findList.isSubmitted()) {
                trueCount++;
            }
        }
        if (gameRoom.getRoundMaxNum() > nowUserCount) {
            trueCount = trueCount - (gameRoom.getRoundMaxNum() - nowUserCount);
        }
        // 제출한 인원수 및 게임중인 인원수 메시지 전송
        HashMap<String, Long> message = new HashMap<>();
        message.put("trueCount", trueCount);
        message.put("maxTrueCount", nowUserCount);
        sendingOperations.convertAndSend("/topic/game/true-count/" + requestDto.getRoomId(), message);
        log.info(">>>>>>> [GameService - getToggleSubmit] #{} 번 방의 {} 번째 라운드의 현재 trueCount : {}", gameFlow.getRoomId(), gameFlow.getRound(), trueCount);
        log.info(">>>>>>> [GameService - getToggleSubmit] #{} 번 방의 {} 번째 라운드의 현재 maxTrueCount : {}", gameFlow.getRoomId(), gameFlow.getRound(), nowUserCount);
        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 방 번호 : {}", gameFlow.getRoomId());
        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 라운드 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 닉네임 : {}", gameFlow.getNickname());
        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 보내는 사람 세션Id : {}", gameFlow.getWebSessionId());
        log.info(">>>>>>> [GameService - getToggleSubmit] GameFlow -> 제출 여부 : {}", gameFlow.isSubmitted());
        // 동시성 제어를 위해 분리했던 메소드 다시 결합
        // DB 기준 제출 여부 조회 후 메시지 전송
//        sendSubmitMessage(requestDto);
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - sendSubmitMessage] >>>>>>>>>>>>>>>>>>>>>>>>");
//        List<GameFlow> gameFlowList = gameFlowRepository.findAllByRoomIdAndRound(requestDto.getRoomId(), requestDto.getRound());
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());
//        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
//                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
//        );
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
        log.info(">>>>>>> [GameService - sendSubmitMessage] gameFlowList.size() {}", gameFlowList.size());
        log.info(">>>>>>> [GameService - sendSubmitMessage] gameRoomUserList.size() {}", gameRoomUserList.size());
        log.info(">>>>>>> [GameService - sendSubmitMessage] gameRoom.getRoundMaxNum() {}", gameRoom.getRoundMaxNum());
        if (gameFlowList.size() == gameRoom.getRoundMaxNum() && allFlag) {
            // 전체가 제출 했다면 모두에게 메시지 전송
            Map<String, Object> allSubmitMessage = new HashMap<>();
            allSubmitMessage.put("completeSubmit", true);
            sendingOperations.convertAndSend("/topic/game/submit-" + destination + "/" + requestDto.getRoomId(), allSubmitMessage);
            log.info(">>>>>>> [GameService - isAllSubmit] 전체 제출 후 메시지 전송 성공! : {}", allSubmitMessage);
        }
    }

    // 받아온 그림 S3에 저장 후 imagePath 반환
    // 수정 리턴값 변경 String → Image 김재영 01.29
    @Transactional
    public Image saveImage(GameFlowRequestDto requestDto) throws IOException {
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
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

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
        log.info(">>>>>>> [GameService - sendSubmitMessage] gameFlowList.size() {}", gameFlowList.size());
        log.info(">>>>>>> [GameService - sendSubmitMessage] gameRoomUserList.size() {}", gameRoomUserList.size());
        log.info(">>>>>>> [GameService - sendSubmitMessage] gameRoom.getRoundMaxNum() {}", gameRoom.getRoundMaxNum());
        if (gameFlowList.size() == gameRoom.getRoundMaxNum() && allFlag) {
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
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 전체 제출 여부 확인
        if (gameFlowList.size() == gameRoom.getRoundMaxNum()) {
            log.info(">>>>>>> [GameService - checkSubmit메서드] : gameFlowList.size() {}", gameFlowList.size());
            log.info(">>>>>>> [GameService - checkSubmit메서드] : gameRoomUserList.size() {}", gameRoomUserList.size());
            log.info(">>>>>>> [GameService - checkSubmit메서드] : gameRoom.getRoundMaxNum() {}", gameRoom.getRoundMaxNum());

            // 라운드를 계속 체크해서 라운드가 인원수와 같다면 [결과페이지 이동]
            if (requestDto.getRound() == gameRoom.getRoundMaxNum()) {
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

        // gameRoom 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 다음순번인 키워드의 index 계산하고 DB 에서 조회
        int nextKeywordIndex = calculateKeywordIndex(requestDto.getKeywordIndex(), gameRoom.getRoundMaxNum());
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
                // 제출하지 못한 이미지는, 미제출 이미지 보여주기
//                if(gameFlow.getImagePath().equals("미제출")){
//                    UnsubmissionImg unsubmissionImg = unsubmissionImgRepository.findById(1).orElseThrow(
//                            () -> new CustomException(StatusMsgCode.IMAGE_NOT_FOUND));
//                    message.put("image", unsubmissionImg.getUnsubmissionImg());
//                } else{
                    message.put("image", gameFlow.getImagePath());
//                }
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
    public int calculateKeywordIndex(int nowKeywordIndex, int maxUserSize) {
        // 반환될 다음순번인 키워드의 index
        int nextKeywordIndex;
        if (nowKeywordIndex == maxUserSize) {
            nextKeywordIndex = nowKeywordIndex % maxUserSize + 1;
        } else {
            nextKeywordIndex = nowKeywordIndex + 1;
        }
        return nextKeywordIndex;
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
        Object[][] resultList = new Object[roundTotalNum][roundTotalNum];

        // 중첩 for문 돌면서 round, keyword index에 해당하는 데이터 불러오기
        for (int i = 1; i <= roundTotalNum; i++) {
            for (int j = 1; j <= roundTotalNum; j++) {
                log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> i{}", i);
                log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> j {}", j);
                log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> requestDto.getRoomId() {}", requestDto.getRoomId());

                // 2차원 배열의 요소에 해당하는 리스트 생성 (요소에 들어가는 것 : 닉네임, 키워드 or imagePath, 프로필사진)
//                List<String> gameResult = new ArrayList<>();
                Map<String, String> gameResultMap = new HashMap<>(); // 수정 추가 김재영 01.29

                GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(requestDto.getRoomId(), j, i).orElseThrow(
                        () -> new CustomException(StatusMsgCode.GAMEFLOW_NOT_FOUND)
                );
                // 2차원 배열의 요소에 닉네임 저장
//                gameResult.add(gameFlow.getNickname());
                gameResultMap.put("nickname",gameFlow.getNickname()); // 수정 추가 김재영 01.29

                // 2차원 배열의 요소에 키워드 or imagePath저장
                if (j % 2 == 0) {
                    // 짝수 round 일 때 -> 이미지 가져오기
//                    if(gameFlow.getImagePath().equals("미제출")){
                        // 게임중 방 나가서 제출 못했을 경우, 미제출 이미지 보여주기
//                        UnsubmissionImg unsubmissionImg = unsubmissionImgRepository.findById(1).orElseThrow(
//                            () -> new CustomException(StatusMsgCode.IMAGE_NOT_FOUND));
//                        gameResult.add(unsubmissionImg.getUnsubmissionImg());
//                        gameResultMap.put("imgPath", unsubmissionImg.getUnsubmissionImg()); // 수정 추가 김재영 01.29
//                    } else{
//                        gameResult.add(gameFlow.getImagePath());
                        gameResultMap.put("imgPath", gameFlow.getImagePath()); // 수정 추가 김재영 01.29

//                        gameResult.add(gameFlow.getImagePk().toString());
                        gameResultMap.put("imgId", String.valueOf(gameFlow.getImagePk())); // 수정 추가 김재영 01.29
//                    }
                } else {
                    // 홀수 round 일 때 -> 제시어 가져오기
//                    gameResult.add(gameFlow.getKeyword());
                    gameResultMap.put("keyword", gameFlow.getKeyword()); // 수정 추가 김재영 01.29
                }
                // 2차원 배열의 요소에 프로필사진 url 저장
//                gameResult.add(gameFlow.getUserImgPath());
                gameResultMap.put("userImgPath", gameFlow.getUserImgPath()); // 수정 추가 김재영 01.29

                // 닉네임, 키워드 or imagePath, 프로필사진 담긴 리스트를 2차원 배열의 요소로 저장
//                resultList[i - 1][j - 1] = gameResult;
                resultList[i - 1][j - 1] = gameResultMap; // 수정 변경 김재영 01.29
            }
        }
        log.info(">>>>>>> [GameService - getGameFlow] 2차원 배열 : {}", Arrays.deepToString(resultList));

        // 요청한 유저가 방장인지 아닌지 조회
        Long userId = Long.valueOf(gamerInfo.get(GamerEnum.ID.key()));
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        boolean isHost = false;
        if (gameRoom.getHostId().equals(userId)){
            isHost = true;
        }

        // gamerList에 프로필사진, 닉네임, 방장여부 저장하기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoom.getId());
        List<Map<String, Object>> gamerList = new ArrayList<>();
        for(GameRoomUser gameRoomUser : gameRoomUserList) {
            Map<String, Object> gamerMap = new HashMap<>();
            gamerMap.put("imgUrl", gameRoomUser.getImgUrl());
            gamerMap.put("nickname", gameRoomUser.getNickname());
            gamerMap.put("isHost", gameRoom.getHostId().equals(gameRoomUser.getUserId()));
            gamerList.add(gamerMap);
        }

        // 요청한 유저에게 게임 결과, 본인의 방장 여부, 게임 참여자 리스트 메시지 전송
        Map<String, Object> message = new HashMap<>();
        message.put("result", resultList);       // 게임 결과 (2차원 배열)
        message.put("isHost", isHost);           // 방장 유무
        message.put("gamerList", gamerList);     // 게임 참여자 리스트

        sendingOperations.convertAndSend("/queue/game/result/" + requestDto.getWebSessionId(), message);
        return resultList;
    }

    // 게임 중간에 나갈 시 남은 라운드 결과 null로 모두 제출
    public void submitLeftRound(String userUUID) {
        if (userUUID == null) {
            // 강퇴당한 경우 userUUID 가 null 이기 때문에 바로 리턴
            log.info(">>>>>>> 위치 : GameRoomService 의 submitLeftRound 메서드 / 이미 강퇴된 상태면 아무런 로직 실행 없이 바로 반환");
            return;
        }

        // gameRoomUser 정보로부터 isPlaying 확인
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByWebSessionId(userUUID).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_ROOM_USER_NOT_FOUND)
        );
        boolean isPlaying = gameRoomUser.getGameRoom().isPlaying();
        log.info(">>>>>>> [Gameservice - submitLeftRound] isPlaying {}", isPlaying);

        // 게임 중일 경우에만 남은 라운드 null로 제출
        if (!isPlaying) {
            return;
        } else {
            // gameRoomUser로부터 roomId, roundMaxNum 저장
            Long gameRoomId = gameRoomUser.getGameRoom().getId();
            int roundMaxNum = gameRoomUser.getGameRoom().getRoundMaxNum();

            // 해당 방에서 만들어진 유저의 gameFlow에서 가장 마지막 round, keywordIndex 구하기, next keywordIndex 구하기
            List<GameFlow> gameFlows = gameFlowRepository.findAllByWebSessionIdAndRoomId(userUUID, gameRoomId);
            int maxRound = 0;           // 방에서 나간 유저의 마지막 gameFlow의 round 숫자
            int lastKeywordIndex = 0;   // 방에서 나간 유저의 마지막 gameFlow의 keywordIndex
            int currentKeywordIndex;    // 나간시점에서 저장해야할 첫 gameFLow의 keywordIndex
            for (GameFlow gameFlow : gameFlows) {
                maxRound = (maxRound > gameFlow.getRound()) ? maxRound : gameFlow.getRound();
                lastKeywordIndex = (maxRound > gameFlow.getRound()) ? lastKeywordIndex : gameFlow.getKeywordIndex();
            }
//            currentKeywordIndex = lastKeywordIndex + 1;
            currentKeywordIndex = lastKeywordIndex + 1 > roundMaxNum ? (lastKeywordIndex + 1 - roundMaxNum) : lastKeywordIndex + 1;


            // 첫 라운드에서 방 나가서 유저의 gameFlow가 하나도 없는 경우
            int count = 0;
            if (lastKeywordIndex == 0) {
                // 첫 라운드에서 gameRoomUserList의 id 순서대로 keywordIndex 오름차순으로 받으므로 이를 계산
                List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoomId);
                log.info(">>>>>>> [Gameservice - submitLeftRound] gameRoomUserList.size() {}", gameRoomUserList.size());
                for (GameRoomUser roomUser : gameRoomUserList) {
                    log.info(">>>>>>> [Gameservice - submitLeftRound] roomUser.getId() {}", roomUser.getId());

                    count = (gameRoomUser.getId() >= roomUser.getId()) ? count + 1 : count;
                    currentKeywordIndex = count;
                    log.info(">>>>>>> [Gameservice - submitLeftRound] count {}", count);
                }
            }
            log.info(">>>>>>> [Gameservice - submitLeftRound] lastKeywordIndex {}", lastKeywordIndex);
            log.info(">>>>>>> [Gameservice - submitLeftRound] maxRound {}", maxRound);
            log.info(">>>>>>> [Gameservice - submitLeftRound] roundMaxNum {}", roundMaxNum);
            log.info(">>>>>>> [Gameservice - submitLeftRound] currentKeywordIndex {}", currentKeywordIndex);


            // 남은 라운드부터 마지막 라운드까지 null로 저장하기
            for (int i = maxRound + 1, j = currentKeywordIndex; i <= roundMaxNum; i++, j++) {
                j = (j > roundMaxNum) ? j - roundMaxNum : j;

                if (i % 2 == 0) {
                    // round 가 짝수, 즉 그리기 제출 라운드 일 때
                    GameFlow gameFlow = GameFlow.builder()
                            .roomId(gameRoomId)
                            .round(i)
                            .keywordIndex(j)
                            .nickname(gameRoomUser.getNickname())
                            .webSessionId(userUUID)
                            .imagePath("null")
                            .userImgPath(gameRoomUser.getImgUrl())
                            .isSubmitted(true).build();
                    gameFlowRepository.saveAndFlush(gameFlow);
                } else {
                    // round 가 홀수, 즉 키워드 제출 라운드 일 때
                    GameFlow gameFlow = GameFlow.builder()
                            .roomId(gameRoomId)
                            .round(i)
                            .keywordIndex(j)
                            .nickname(gameRoomUser.getNickname())
                            .webSessionId(userUUID)
                            .keyword("null")
                            .userImgPath(gameRoomUser.getImgUrl())
                            .isSubmitted(true).build();
                    gameFlowRepository.saveAndFlush(gameFlow);
                }


                // 다음 라운드로 넘어가는 trigger 역할 하는 sendSubmitMessage 메서드 실행
//                if (j % 2 == 0) {
//                    // round 가 짝수, 즉 그리기 제출 라운드 일 때
//                    GameFlowRequestDto requestDto = GameFlowRequestDto.builder()
//                            .webSessionId(userUUID)
//                            .roomId(gameRoomId)
//                            .round(i)
//                            .image("미제출")
//                            .build();
//                    sendSubmitMessage(requestDto);
//                } else {
//                    // round가 홀수, 즉 키워드 제출 라운드 일 때
//                    GameFlowRequestDto requestDto = GameFlowRequestDto.builder()
//                            .webSessionId(userUUID)
//                            .roomId(gameRoomId)
//                            .round(i)
//                            .build();
//                    sendSubmitMessage(requestDto);
//                }
            }
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

    // 게임 결과창 - 다음 또는 이전 키워드 번호 가져오기
    @Transactional
    public MsgResponseDto getKeywordIndex(GameFlowRequestDto requestDto, String destination) {
        // 1. 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());

        // 2. 요청한 유저가 방장인지 아닌지 조회 아니면 Exception 발생
        Long reqUserId = Long.valueOf(gamerInfo.get(GamerEnum.ID.key()));
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        if (!gameRoom.getHostId().equals(reqUserId)) {
            throw new CustomException(StatusMsgCode.HOST_AUTHORIZATION_NEED);
        }

        Map<String, Integer> message = new HashMap<>();
        int nowResultCount = 0;

        switch (destination) {
            case "next" -> {
                // 3. 요청이 "next" 이면 다음 키워드 번호 가져오기 DB 에 다음 키워드 번호 등록
                nowResultCount = gameRoom.getResultCount() + 1;
                gameRoom.update(nowResultCount);

                // 4. 키워드 번호가 최대 라운드 수 보다 크면 Exception 발생
                if (nowResultCount > gameRoom.getRoundMaxNum()) {
                    throw new CustomException(StatusMsgCode.KEYWORD_INDEX_NOT_FOUND);
                }

                // 5. next-keyword-index 로 구독하고 있는 User 에게 next-keyword-index 메세지 전송
                message.put("keywordIndex", nowResultCount);
                sendingOperations.convertAndSend("/topic/game/next-keyword-index/" + requestDto.getRoomId(), message);
            }
            case "prev" -> {
                // 6. 요청이 "prev" 이면 이전 키워드 번호 가져오기 및 DB 에 이전 키워드 번호 등록
                nowResultCount = gameRoom.getResultCount() - 1;
                gameRoom.update(nowResultCount);

                // 7. 키워드 번호가 최소 라운드 수 보다 적으면 Exception 발생
                if (nowResultCount < 0) {
                    throw new CustomException(StatusMsgCode.KEYWORD_INDEX_NOT_FOUND);
                }

                // 8. prev-keyword-index 로 구독하고 있는 User 에게 prev-keyword-index 메세지 전송
                message.put("keywordIndex", nowResultCount);
                sendingOperations.convertAndSend("/topic/game/prev-keyword-index/" + requestDto.getRoomId(), message);
            }
        }

        return new MsgResponseDto(StatusMsgCode.OK);
    }

}
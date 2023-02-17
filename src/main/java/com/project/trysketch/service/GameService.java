package com.project.trysketch.service;

import com.project.trysketch.dto.request.GameFlowRequestDto;
import com.project.trysketch.entity.*;
import com.project.trysketch.dto.GamerEnum;
import com.project.trysketch.global.utill.sse.SseEmitters;
import com.project.trysketch.repository.*;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final GameFlowCountRepository gameFlowCountRepository;
    private final PlayTimeRepository playTimeRepository;
    private final UserRepository userRepository;
    private final HistoryRepository historyRepository;
    private final AmazonS3Service s3Service;
    private final UserService userService;
    private final HistoryService historyService;
    private final SseService sseService;
    private final SseEmitters sseEmitters;
    private final SimpMessageSendingOperations sendingOperations;
    private final int adSize = 117;
    private final int nounSize = 1335;
    private final String dirName = "static";

    // convertAndSend 는 객체를 인자로 넘겨주면 자동으로 Message 객체로 변환 후 도착지로 전송한다.

    // 게임 시작 -> 방장만 호출 가능
    // requestDto 필요한 정보
    // token, roomId
    @Transactional
    public void startGame(GameFlowRequestDto requestDto) {

        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.getGamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - startGame] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - startGame] RoomId : {}", requestDto.getRoomId());

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 방장이 아닐경우
        if (!gameRoom.getHostId().toString().equals(gamerInfo.get(GamerEnum.ID.key()))) {
            throw new CustomException(StatusMsgCode.HOST_AUTHORIZATION_NEED);
        }

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
            if (gameRoomUser.getUserId() < 10000) {
                UserPlayTime userPlayTime = UserPlayTime.builder()
                        .playStartTime(startTime)
                        .playEndTime(null)
                        .gameRoomUser(gameRoomUser)
                        .gameRoomId(gameRoom.getId())
                        .build();
                playTimeRepository.save(userPlayTime);
            }
        }

        // GameRoom 의 상태를 true(게임 시작)로 변경
        gameRoom.GameRoomStatusUpdate(true);

        // GameRoom 의 roundMaxNum(최대 라운드)을 저장
        gameRoom.RoundMaxNumUpdate(gameRoom.getGameRoomUserList().size());

        // isIngame 으로 구독하고 있는 User 에게 start 메세지 전송
        Map<String, Boolean> message = new HashMap<>();
        message.put("isIngame", true);
        sendingOperations.convertAndSend("/topic/game/start/" + requestDto.getRoomId(), message);

        // SSE event 생성
        sseEmitters.changeRoom(sseService.getRooms(0));
    }

    // 방에 입장시 타임 리미트, 난이도 조절
    // requestDto 필요한 정보
    // token, roomId, webSessionId
    public void getGameMode(GameFlowRequestDto requestDto) {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.getGamerInfo(requestDto.getToken());

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        log.info(">>>>>>> [GameService - getGameMode] #{}번 방 / 요청한 유저의 웹세션 ID : {}", requestDto.getRoomId(), requestDto.getWebSessionId());

        Map<String, Object> message = new HashMap<>();

        // message 에 난이도와 타임리미트 추가
        message.put("difficulty", gameRoom.getDifficulty());
        message.put("timeLimit", gameRoom.getTimeLimit());

        sendingOperations.convertAndSend("/queue/game/gameroom-data/" + requestDto.getWebSessionId(), message);
    }

    // 게임 종료 -> 방장만 호출 가능
    // requestDto 필요한 정보
    // token, roomId
    @Transactional
    public void endGame(GameFlowRequestDto requestDto) {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.getGamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - endGame] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - endGame] RoomId : {}", requestDto.getRoomId());

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 방장이 아닐경우
        if (!gameRoom.getHostId().toString().equals(gamerInfo.get(GamerEnum.ID.key()))) {
            throw new CustomException(StatusMsgCode.HOST_AUTHORIZATION_NEED);
        }

        // 현재 GameRoom 이 시작되지 않았다면
        if (!gameRoom.isPlaying()) {
            throw new CustomException(StatusMsgCode.NOT_STARTED_YET);
        }

        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());

        // GameRoom 에서 진행된 모든 GameFlowCount 삭제
        gameFlowCountRepository.deleteAllByRoomId(gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate(false);
        gameRoom.update(0);

        // 현재 방의 유저 정보 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoom.getId());

        // 현재 방의 모든 유저의 playTime 정보 가져오기
        List<UserPlayTime> userPlayTimeList = playTimeRepository.findAllByGameRoomId(gameRoom.getId());

        // 현재 시간 저장
        LocalDateTime endTime = LocalDateTime.of(
                LocalDateTime.now().getYear(),
                LocalDateTime.now().getMonth(),
                LocalDateTime.now().getDayOfMonth(),
                LocalDateTime.now().getHour(),
                LocalDateTime.now().getMinute(),
                LocalDateTime.now().getSecond());

        // 방장 제외한 모든 유저들의 ready 상태를 false 로 변경
        for (GameRoomUser gameRoomUsers : gameRoomUserList) {
            if (!gameRoomUsers.getUserId().equals(gameRoom.getHostId())) {
                gameRoomUsers.update(false);
                gameRoomUserRepository.save(gameRoomUsers);
            }
            // 방장은 readyStatus 가 false 가 되는 경우가 없음 다시 true 로 만들 필요없음

            if (gameRoomUsers.getUserId() < 10000) {
                for (UserPlayTime userPlayTime : userPlayTimeList) {
                    // GameRoomUser 와 현재 for 문의 userPlayTime 의 주인이 같다면
                    if (gameRoomUsers.equals(userPlayTime.getGameRoomUser())) {

                        // 게임 종료시간 업데이트
                        userPlayTime.updateUserPlayTime(endTime);

                        // 유저 정보 가져오기
                        User currentUser = userRepository.findById(gameRoomUsers.getUserId()).orElseThrow(
                                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
                        );

                        // startGame 과 endGame 의 차이 구하기
                        Duration duration = Duration.between(userPlayTime.getPlayStartTime(), userPlayTime.getPlayEndTime());

                        // 분 단위로 변환
                        Long difference = duration.getSeconds() / 60;

                        // 해당 history 에 실질적인 플레이타임 업데이트
                        historyRepository.save(currentUser.getHistory().updatePlaytime(difference));

                        // 유저가 획득한 playtime 관련 업적 리스트
                        List<String> timeTrophyList = historyService.getTrophyOfTime(currentUser);

                        // 해당 플레이타임 삭제
                        playTimeRepository.delete(userPlayTime);

                        // 해당 history 에 실질적인 판수 업데이트
                        historyRepository.save(currentUser.getHistory().updateTrials(1L));

                        // 유저가 획득한 trial 관련 업적 리스트
                        List<String> trialTrophyList = historyService.getTrophyOfTrial(currentUser);

                        // 플레이타임 list 와 판수 list 를 하나로 만들기
                        List<String> responseList = Stream.concat(timeTrophyList.stream(), trialTrophyList.stream()).collect(Collectors.toList());

                        // 얻는 업적이 있을 경우
                        if (responseList.size() != 0) {
                            Map<String, Object> message = new HashMap<>();
                            message.put("achievement", responseList);
                            sendingOperations.convertAndSend("/queue/game/achievement/" + gameRoomUsers.getWebSessionId(), message);
                        }
                    }
                }
            }
        }

        // end 로 구독하고 있는 User 에게 end 메세지 전송
        Map<String, Boolean> message = new HashMap<>();
        message.put("end", true);
        sendingOperations.convertAndSend("/topic/game/end/" + requestDto.getRoomId(), message);

        // SSE event 생성
        sseEmitters.changeRoom(sseService.getRooms(0));
    }

    // 강제 종료( 비정상적인 종료 )
    @Transactional
    public void shutDownGame(Long gameRoomId) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - shutDownGame] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - shutDownGame] RoomId : {}", gameRoomId);

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(gameRoomId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());

        // GameRoom 에서 진행된 모든 GameFlowCount 삭제
        gameFlowCountRepository.deleteAllByRoomId(gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate(false);

        // 현재 방에 남은 모든 유저 정보 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoom.getId());

        for (GameRoomUser gameRoomUsers : gameRoomUserList) {

            // 방장 제외한 모든 유저들의 ready 상태를 false 로 변경
            if (!gameRoomUsers.getUserId().equals(gameRoom.getHostId())) {
                gameRoomUsers.update(false);
                gameRoomUserRepository.save(gameRoomUsers);
            }
            if (gameRoomUsers.getUserId() < 10000) {
                UserPlayTime userPlayTime = gameRoomUsers.getUserPlayTime();

                // 해당 플레이타임 삭제
                playTimeRepository.delete(userPlayTime);
            }
        }

        // 구독하고 있는 User 에게 shutdown 메세지 전송
        Map<String, Boolean> message = new HashMap<>();
        message.put("shutdown", true);
        sendingOperations.convertAndSend("/topic/game/shutdown/" + gameRoomId, message);
    }

    // 최초 디폴트 제시어 던져주기 -> 방장만 호출 가능
    // requestDto 필요한 정보
    // token, roomId, webSessionId
    @Transactional
    public void getInGameData(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getInGameData] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getInGameData] webSessionId : {}", requestDto.getWebSessionId());

        // 해당 gameRoom 의 전체 유저 리스트 조회
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        Map<String, Object> message = new HashMap<>();
        Map<String, Long> submitCount = new HashMap<>();          // 제출 인원 / 총 인원
        Map<Integer, String> keywordList = new HashMap<>();     // 키워드 리스트

        // 난이도 easy 이냐 hard 이냐에 따라 가져오는 키워드가 다르다
        switch (gameRoom.getDifficulty()) {
            case "easy" -> {
                for (int i = 0; i < gameRoomUserList.size(); i++) {
                    // 명사 리스트중 1개
                    int nounId = (int) (Math.random() * nounSize + 1);
                    Noun noun = nounRepository.findById(nounId).orElse(null);

                    keywordList.put(i, noun.getNoun());
                }
            }
            case "hard" -> {
                for (int i = 0; i < gameRoomUserList.size(); i++) {
                    // 형용사 리스트중 1개
                    int adId = (int) (Math.random() * adSize + 1);
                    Adjective adjective = adjectiveRepository.findById(adId).orElse(null);

                    // 명사 리스트중 1개
                    int nounId = (int) (Math.random() * nounSize + 1);
                    Noun noun = nounRepository.findById(nounId).orElse(null);

                    keywordList.put(i, adjective.getAdjective() + " " + noun.getNoun());
                }
            }
        }

        // GameRoomUser 돌면서 키워드 전송
        for (int i = 0; i < gameRoomUserList.size(); i++) {
            String webSessionId = gameRoomUserList.get(i).getWebSessionId();

            // 제출한 인원수 및 게임중인 인원수 메시지 전송
            message.put("keyword", keywordList.get(i));
            message.put("keywordIndex", i + 1);
            message.put("timeLimit", gameRoom.getTimeLimit());

            sendingOperations.convertAndSend("/queue/game/ingame-data/" + webSessionId, message);
        }

        Long maxCount = gameRoomUserRepository.countByGameRoomId(requestDto.getRoomId());

        submitCount.put("trueCount", 0L);
        submitCount.put("maxTrueCount", maxCount);

        sendingOperations.convertAndSend("/topic/game/true-count/" + requestDto.getRoomId(), submitCount);

        log.info(">>>>>>>> [GameService - getInGameData] #{}번 방 / 카운트 전송 완료",gameRoom.getId());

        // 방장이 첫 라운드 gameFlowCount 미리 생성
        String hostWebSessionId = gameRoomUserRepository.findByUserId(gameRoom.getHostId()).getWebSessionId();
        if (requestDto.getWebSessionId().equals(hostWebSessionId)) {
            log.info(">>>>>>> [GameService - getInGameData] 방장이 gameFlowCount 생성");
            GameFlowCount gameFlowCount = GameFlowCount.builder()
                    .gameFlowCount(0)
                    .roomId(requestDto.getRoomId())
                    .round(1)
                    .build();
            gameFlowCountRepository.save(gameFlowCount);
        }
    }

    // 타임 리미트 변경
    // requestDto 필요한 정보
    // token, roomId
    @Transactional
    public void changeTimeLimit(GameFlowRequestDto requestDto, String changeTime) {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.getGamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - changeTimeLimit] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - changeTimeLimit] RoomId : {}", requestDto.getRoomId());

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 방장이 아닐경우
        if (!gameRoom.getHostId().toString().equals(gamerInfo.get(GamerEnum.ID.key()))) {
            throw new CustomException(StatusMsgCode.HOST_AUTHORIZATION_NEED);
        }

        Map<String, Object> message = new HashMap<>();
        Long currentTimeLimit = gameRoom.getTimeLimit();  // 현재 방의 라운드 타임 가져오기
        Long timeInterval = 30000L;                       // 한번에 30초씩 업/다운(밀리세컨드)


        switch (changeTime) {
            case "increase-time" -> {
                currentTimeLimit = currentTimeLimit + timeInterval; // 30초 증가
                // 요청한 시간이 2분 30초(밀리 세컨드) 이하일 시 변경 가능하다. 즉, "2분 30초 초과" 이면 변경 불가능
                if (currentTimeLimit <= 150000L) {
                    message.put("timeLimit", currentTimeLimit);
                    gameRoom.timeLimitUpdate(currentTimeLimit);     // 변경된 타임리미트로 방 정보 변경
                    sendingOperations.convertAndSend("/topic/game/time-limit/" + requestDto.getRoomId(), message);
                } else {
                    throw new CustomException(StatusMsgCode.MINMAX_ROUND_TIME);
                }
            }
            case "decrease-time" -> {
                currentTimeLimit = currentTimeLimit - timeInterval;  // 30초 감소
                // 요청한 시간이 1분(밀리 세컨드) 이상일 시 변경 가능하다. 즉, "1분 미만" 이면 변경 불가능
                if (currentTimeLimit >= 30000L) {
                    message.put("timeLimit", currentTimeLimit);
                    gameRoom.timeLimitUpdate(currentTimeLimit);      // 변경된 타임리미트로 방 정보 변경
                    sendingOperations.convertAndSend("/topic/game/time-limit/" + requestDto.getRoomId(), message);
                } else {
                    throw new CustomException(StatusMsgCode.MINMAX_ROUND_TIME);
                }
            }
        }
    }

    // 난이도 변경
    // requestDto 필요한 정보
    // token, roomId, difficulty
    @Transactional
    public void changeDifficulty(GameFlowRequestDto requestDto) {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.getGamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - changeDifficulty] >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService - changeDifficulty] RoomId : {}", requestDto.getRoomId());

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        // 방장이 아닐경우
        if (!gameRoom.getHostId().toString().equals(gamerInfo.get(GamerEnum.ID.key()))) {
            throw new CustomException(StatusMsgCode.HOST_AUTHORIZATION_NEED);
        }

        Map<String, Object> message = new HashMap<>();

        // 들어온 요청에 따라 easy 또는 hard 로 변경
        message.put("difficulty", requestDto.getDifficulty());
        gameRoom.difficultyUpdate(requestDto.getDifficulty());      // gameRoom 의 난이도를 easy 로 변경
        sendingOperations.convertAndSend("/topic/game/difficulty/" + requestDto.getRoomId(), message);
    }

    // 제출, 취소에 따라 gameFlow DB 저장 & 제출 여부 전송
    // requestDto 필요한 정보
    // token, roomId, round, keyword, keywordIndex, image, webSessionId, isSubmitted
    @Transactional
    public void getToggleSubmit(GameFlowRequestDto requestDto) throws IOException {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getToggleSubmit] >>>>>>>>>>>>>>>>>>>>>>>>");
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.getGamerInfo(requestDto.getToken());
        log.info(">>>>>>> [GameService - getToggleSubmit] gamerInfo nickname : {}", gamerInfo.get(GamerEnum.NICK.key()));

        // 현재 gameRoomUser 정보 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByWebSessionId(requestDto.getWebSessionId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_ROOM_USER_NOT_FOUND)
        );

        // 접속한 유저의 제출 여부
        GameFlowCount gameFlowCount;
        // 제출 : gameFlow 가 없다면 -> gameFlow 생성
        if (!gameFlowRepository.existsByRoomIdAndRoundAndWebSessionId(
                requestDto.getRoomId(),
                requestDto.getRound(),
                requestDto.getWebSessionId()
        )) {
            // gameFlow 생성
            GameFlow gameFlow = buildGameFlow(requestDto, gamerInfo, gameRoomUser);
            gameFlowRepository.saveAndFlush(gameFlow);
            // gameFlowCount 업데이트
            gameFlowCount = updateGameFlowCount(requestDto, !requestDto.isSubmitted());
        }
        // 취소 : gameFlow 가 있다면 -> gameFlow 삭제
        else {
            // gameFlow 삭제
            GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndWebSessionId(
                    requestDto.getRoomId(),
                    requestDto.getRound(),
                    requestDto.getWebSessionId()
            );
            gameFlowRepository.delete(gameFlow);
            // gameFlowCount 업데이트
            gameFlowCount = updateGameFlowCount(requestDto, !requestDto.isSubmitted());
        }

        // 라운드 제출 인원수 카운트부
        int trueCount = gameFlowCount.getGameFlowCount();                                     // 현재 제출 한 인원
        Long nowUserCount = gameRoomUserRepository.countByGameRoomId(requestDto.getRoomId()); // 해당 방에서 제출할 총 인원

        // 클라이언트로 제출 인원 메시지 전송
        HashMap<String, Object> message = new HashMap<>();
        message.put("trueCount", trueCount);
        message.put("maxTrueCount", nowUserCount);
        sendingOperations.convertAndSend("/topic/game/true-count/" + requestDto.getRoomId(), message);

        // 클라이언트로 접속한 유저의 제출 여부 메시지 전송
        Map<String, Object> submitMessage = new HashMap<>();
        submitMessage.put("isSubmitted", !requestDto.isSubmitted());
        sendingOperations.convertAndSend("/queue/game/is-submitted/" + requestDto.getWebSessionId(), submitMessage);

        // 클라이언트로 해당 방의 전체 유저 제출 여부 메시지 전송
        GameFlowCount getGameFlowCount = gameFlowCountRepository.findByRoomIdAndRoundForUpdate(
                requestDto.getRoomId(),
                requestDto.getRound()
        );
        log.info(">>>>>>> [GameService - getToggleSubmit] getGameFlowCount : {}", gameFlowCount.getGameFlowCount());

        if (getGameFlowCount.getGameFlowCount() == nowUserCount) {
            // 이미지 라운드 인지 키워드 라운드 인지 판별 후 destination 부여
            String destination = requestDto.getImage() == null || requestDto.getImage().length() == 0 ? "word" : "image";
            // 전체가 제출 했다면 모두에게 메시지 전송
            Map<String, Object> allSubmitMessage = new HashMap<>();
            allSubmitMessage.put("completeSubmit", true);
            sendingOperations.convertAndSend("/topic/game/submit-" + destination + "/" + requestDto.getRoomId(), allSubmitMessage);
            log.info(">>>>>>> [GameService - isAllSubmit] 전체 제출 후 메시지 전송 성공! : {}", allSubmitMessage);
        }
    }

    // 전체 제출 인원 업데이트
    @Transactional
    public GameFlowCount updateGameFlowCount(GameFlowRequestDto requestDto, boolean isSubmitted) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - setGameFlowCount] >>>>>>>>>>>>>>>>>>>>>>>>");
        // gameFlowCount 조회
        GameFlowCount gameFlowCount = gameFlowCountRepository.findByRoomIdAndRoundForUpdate(
                requestDto.getRoomId(),
                requestDto.getRound()
        );

        GameFlowCount newGameFlowCount;
        // 제출이면 gameFlowListSize 업데이트 +1
        if (isSubmitted) {
            newGameFlowCount = gameFlowCount.update(1);
        }
        // 취소면 gameFlowListSize 업데이트 -1
        else {
            newGameFlowCount = gameFlowCount.update(-1);
        }
        return gameFlowCountRepository.saveAndFlush(newGameFlowCount);
    }

    // gameFlow 생성 빌더
    @Transactional
    public GameFlow buildGameFlow(GameFlowRequestDto requestDto, Map<String, String> gamerInfo, GameRoomUser gameRoomUser) throws IOException {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - buildGameFlow] >>>>>>>>>>>>>>>>>>>>>>>>");

        // 이미지가 있다면, 이미지 저장
        Image image = null;
        if (requestDto.getImage() != null && requestDto.getImage().length() > 0){
                image = saveImage(requestDto);
        }
        
        return GameFlow.builder()
                .roomId(requestDto.getRoomId())
                .round(requestDto.getRound())
                .keywordIndex(requestDto.getKeywordIndex())
                .keyword(requestDto.getKeyword())
                .imagePath(requestDto.getImage() != null && requestDto.getImage().length() > 0 ? image.getPath() : null)
                .imagePk(requestDto.getImage() != null && requestDto.getImage().length() > 0 ? image.getId() : null)
                .nickname(gamerInfo.get(GamerEnum.NICK.key()))
                .webSessionId(requestDto.getWebSessionId())
                .userImgPath(gameRoomUser.getImgUrl())
                .isSubmitted(!requestDto.isSubmitted())
                .build();
    }

    // 받아온 그림 S3에 저장 후 imagePath 반환
    // requestDto 필요한 정보
    // token, image
    @Transactional
    public Image saveImage(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - saveImage] >>>>>>>>>>>>>>>>>>>>>>>>");
        try {
            HashMap<String, String> gamerInfo = userService.getGamerInfo(requestDto.getToken());
            String nickname = gamerInfo.get(GamerEnum.NICK.key());

            String[] strings = requestDto.getImage().split(",");
            String base64Image = strings[1];
            String extension = switch (strings[0]) {
                case "data:image/jpeg;base64" -> "jpeg";
                case "data:image/png;base64" -> "png";
                default -> "jpg";
            };
            byte[] imageBytes = javax.xml.bind.DatatypeConverter.parseBase64Binary(base64Image);

            File tempFile = File.createTempFile("image", "." + extension);
            try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                outputStream.write(imageBytes);
            }

            return s3Service.upload(tempFile, dirName, nickname);
        } catch (IOException ex) {
            log.error("IOException Error Message : {}",ex.getMessage());
            throw new CustomException(StatusMsgCode.IMAGE_SAVE_FAILED);
        }
    }

    // 마지막 라운드 여부 확인
    // requestDto 필요한 정보
    // token, roomId, round, keyword, keywordIndex, webSessionId
    @Transactional
    public void checkLastRound(GameFlowRequestDto requestDto, String destination) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - checkLastRound] >>>>>>>>>>>>>>>>>>>>>>>>");
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 라운드를 계속 체크해서 라운드가 인원수와 같다면 결과페이지로 이동
        if (requestDto.getRound() == gameRoom.getRoundMaxNum()) {
            log.info(">>>>>>> [GameService - checkLastRound] 마지막 라운드 : {}", requestDto.getRound());
            sendResultMessage(requestDto.getRoomId());
        }
        // 라운드가 인원수와 다르다면 이전 제시어 or 그림 불러오기
        else {
            getPrevious(requestDto, destination);
        }
    }

    // 모두에게 결과 메시지 전송
    @Transactional
    public void sendResultMessage(Long roomId) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - sendResultMessage] >>>>>>>>>>>>>>>>>>>>>>>>");
        Map<String, Object> resultMessage = new HashMap<>();
        resultMessage.put("isResult", true);
        sendingOperations.convertAndSend("/topic/game/before-result/" + roomId, resultMessage);
    }

    // 라운드 시작  ← 이전 라운드의 제시어 or 그림 제시
    // requestDto 필요한 정보
    // token, round, roomId, keywordIndex, keyword
    @Transactional
    public void getPrevious(GameFlowRequestDto requestDto, String destination) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getPrevious] >>>>>>>>>>>>>>>>>>>>>>>>");

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
        // 요청한 유저에게 다음 순번의 키워드 index 와 키워드 메시지 전송
        Map<String, Object> message = new HashMap<>();
        message.put("keywordIndex", nextKeywordIndex);

        switch (destination) {
            case "word" -> message.put("keyword", gameFlow.getKeyword());
            case "image" -> message.put("image", gameFlow.getImagePath());
            default -> log.info(">>>>>>> [GameService - getPreviousImage] destination 잘못된 요청입니다.");
        }
        sendingOperations.convertAndSend("/queue/game/before-" + destination + "/" + requestDto.getWebSessionId(), message);

        // 방장이 다음 라운드 gameFlowCount 미리 생성
        String hostWebSessionId = gameRoomUserRepository.findByUserId(gameRoom.getHostId()).getWebSessionId();
        if (requestDto.getWebSessionId().equals(hostWebSessionId)) {
            GameFlowCount gameFlowCount = GameFlowCount.builder()
                    .gameFlowCount(0)
                    .roomId(requestDto.getRoomId())
                    .round(requestDto.getRound() + 1)
                    .build();
            gameFlowCountRepository.save(gameFlowCount);
        }
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
    @Transactional
    public void getGameFlow(GameFlowRequestDto requestDto) {
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.getGamerInfo(requestDto.getToken());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getGameFlow] >>>>>>>>>>>>>>>>>>>>>>>>");

        // 게임룸 불러와서 거기서 roundmaxNum 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        Integer roundMaxNum = gameRoom.getRoundMaxNum();

        // 반환될 2차원 배열 선언
        Object[][] resultList = new Object[roundMaxNum][roundMaxNum];

        // 중첩 for 문 돌면서 round, keyword index 에 해당하는 데이터 불러오기
        for (int i = 1; i <= roundMaxNum; i++) {
            for (int j = 1; j <= roundMaxNum; j++) {

                // 2차원 배열의 요소에 해당하는 리스트 생성 (요소에 들어가는 것 : 닉네임, 키워드 or imagePath, 프로필사진)
                Map<String, String> gameResultMap = new HashMap<>();

                GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(requestDto.getRoomId(), j, i).orElseThrow(
                        () -> new CustomException(StatusMsgCode.GAMEFLOW_NOT_FOUND)
                );

                // 2차원 배열의 요소에 닉네임 저장
                gameResultMap.put("nickname",gameFlow.getNickname());

                // 2차원 배열의 요소에 키워드 or imagePath 저장
                if (j % 2 == 0) {
                    // 짝수 round 일 때 -> 이미지 가져오기
                    gameResultMap.put("imgPath", gameFlow.getImagePath());
                    gameResultMap.put("imgId", String.valueOf(gameFlow.getImagePk()));
                } else {
                    // 홀수 round 일 때 -> 제시어 가져오기
                    gameResultMap.put("keyword", gameFlow.getKeyword());
                }

                // 2차원 배열의 요소에 프로필사진 url 저장
                gameResultMap.put("userImgPath", gameFlow.getUserImgPath());

                // 닉네임, 키워드 or imagePath, 프로필사진 담긴 리스트를 2차원 배열의 요소로 저장
                resultList[i - 1][j - 1] = gameResultMap;
            }
        }
        // 요청한 유저가 방장인지 아닌지 조회
        Long userId = Long.valueOf(gamerInfo.get(GamerEnum.ID.key()));
        boolean isHost = gameRoom.getHostId().equals(userId);

        // gamerList 에 프로필사진, 닉네임, 방장여부 저장하기
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
    }

    // 게임 중간에 나갈 시 남은 라운드 결과 null 로 모두 제출
    @Transactional
    public void submitLeftRound(String webSessionId) {
        if (webSessionId == null) {
            // 강퇴당한 경우 webSessionId 가 null 이기 때문에 바로 리턴
            return;
        }

        // gameRoomUser 정보로부터 isPlaying 확인
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByWebSessionId(webSessionId).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_ROOM_USER_NOT_FOUND)
        );
        boolean isPlaying = gameRoomUser.getGameRoom().isPlaying();

        // 게임 중일 경우에만 남은 라운드 null 로 제출
        if (isPlaying) {
            // gameRoomUser 로부터 roomId, roundMaxNum 저장
            Long gameRoomId = gameRoomUser.getGameRoom().getId();
            int roundMaxNum = gameRoomUser.getGameRoom().getRoundMaxNum();

            // 해당 방에서 만들어진 유저의 gameFlow 에서 가장 마지막 round, keywordIndex 구하기, next keywordIndex 구하기
            List<GameFlow> gameFlows = gameFlowRepository.findAllByWebSessionIdAndRoomId(webSessionId, gameRoomId);
            int maxRound = 0;           // 방에서 나간 유저의 마지막 gameFlow 의 round 숫자
            int lastKeywordIndex = 0;   // 방에서 나간 유저의 마지막 gameFlow 의 keywordIndex
            int currentKeywordIndex;    // 나간시점에서 저장해야할 첫 gameFLow 의 keywordIndex
            for (GameFlow gameFlow : gameFlows) {
                maxRound = (maxRound > gameFlow.getRound()) ? maxRound : gameFlow.getRound();
                lastKeywordIndex = (maxRound > gameFlow.getRound()) ? lastKeywordIndex : gameFlow.getKeywordIndex();
            }
            currentKeywordIndex = lastKeywordIndex + 1 > roundMaxNum ? (lastKeywordIndex + 1 - roundMaxNum) : lastKeywordIndex + 1;

            List<GameFlowCount> gameFlowCountList = gameFlowCountRepository.findAllByRoomId(gameRoomId);
            int currentRound = gameFlowCountList.size();
            if (maxRound == currentRound) {
                GameFlowCount gameFlowCount = gameFlowCountRepository.findByRoomIdAndRoundForUpdate(
                        gameRoomId,
                        currentRound
                );
                gameFlowCount.update(-1);
            }


            // 첫 라운드에서 방 나가서 유저의 gameFlow 가 하나도 없는 경우
            int count = 0;
            if (lastKeywordIndex == 0) {
                // 첫 라운드에서 gameRoomUserList 의 id 순서대로 keywordIndex 오름차순으로 받으므로 이를 계산
                List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(gameRoomId);
                for (GameRoomUser roomUser : gameRoomUserList) {

                    count = (gameRoomUser.getId() >= roomUser.getId()) ? count + 1 : count;
                    currentKeywordIndex = count;
                }
            }

            // 남은 라운드부터 마지막 라운드까지 null 로 저장하기
            for (int i = maxRound + 1, j = currentKeywordIndex; i <= roundMaxNum; i++, j++) {
                j = (j > roundMaxNum) ? j - roundMaxNum : j;

                GameFlow gameFlow;
                if (i % 2 == 0) {
                    // round 가 짝수, 즉 그리기 제출 라운드 일 때
                    gameFlow = GameFlow.builder()
                            .roomId(gameRoomId)
                            .round(i)
                            .keywordIndex(j)
                            .nickname(gameRoomUser.getNickname())
                            .webSessionId(webSessionId)
                            .imagePath("null")
                            .userImgPath(gameRoomUser.getImgUrl())
                            .isSubmitted(true).build();
                } else {
                    // round 가 홀수, 즉 키워드 제출 라운드 일 때
                    gameFlow = GameFlow.builder()
                            .roomId(gameRoomId)
                            .round(i)
                            .keywordIndex(j)
                            .nickname(gameRoomUser.getNickname())
                            .webSessionId(webSessionId)
                            .keyword("null")
                            .userImgPath(gameRoomUser.getImgUrl())
                            .isSubmitted(true).build();
                }
                gameFlowRepository.saveAndFlush(gameFlow);
            }
        }
    }

    // 게임 결과창 - 다음 또는 이전 키워드 번호 가져오기
    @Transactional
    public void getKeywordIndex(GameFlowRequestDto requestDto, String destination) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService - getKeywordIndex] >>>>>>>>>>>>>>>>>>>>>>>>");
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.getGamerInfo(requestDto.getToken());

        // 요청한 유저가 방장인지 아닌지 조회 아니면 Exception 발생
        Long reqUserId = Long.valueOf(gamerInfo.get(GamerEnum.ID.key()));

        // 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

        // 방장 검증
        if (!gameRoom.getHostId().equals(reqUserId)) {
            throw new CustomException(StatusMsgCode.HOST_AUTHORIZATION_NEED);
        }

        Map<String, Integer> message = new HashMap<>();

        int nowResultCount = destination.equals("next") ? gameRoom.getResultCount() + 1 : gameRoom.getResultCount() - 1;
        gameRoom.update(nowResultCount);

        if (nowResultCount >= gameRoom.getRoundMaxNum() || nowResultCount < 0) {
            throw new CustomException(StatusMsgCode.KEYWORD_INDEX_NOT_FOUND);
        }

        message.put("keywordIndex", nowResultCount);
        sendingOperations.convertAndSend("/topic/game/" + destination + "-keyword-index/" + requestDto.getRoomId(), message);
    }
}
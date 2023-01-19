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

        // isIngaeme 으로 보내주기
        Map<String, Boolean> message = new HashMap<>();
        message.put("isIngame", true);

        // 구독하고 있는 User 에게 start 메세지 전송
        sendingOperations.convertAndSend("/topic/game/start/" + requestDto.getRoomId(), message);

        log.info(">>>>>>> [GameService] requestDto : {}", requestDto);

        // 게임 시작
        return new MsgResponseDto(StatusMsgCode.START_GAME);
    }

    // 게임 종료
    // requestDto 필요한 정보
    // roomId, token
    @Transactional
    public MsgResponseDto endGame(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - endGame >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService] - endGame RoomId : {}", requestDto.getRoomId());

        // 현재 방 정보 가져오기
        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );
        // 토큰에서 유저 정보 가져오기 [id,nickname,imgUrl]
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>> [GameService]-endgame gamerInfo {}", gamerInfo);

        // 방장 정보 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserIdAndGameRoomId(
                Long.valueOf(gamerInfo.get(GamerEnum.ID.key())),
                requestDto.getRoomId());
        log.info(">>>>>>> [GameService]-endgame gamerInfo.get(GamerEnum.ID) {}", gamerInfo.get(GamerEnum.ID.key()));

        // 방장의 webSessionId 가져오기
        String hostWebSessionId = "";
        log.info(">>>>>>> [GameService]-endgame gameRoom.getHostId() {}", gameRoom.getHostId());
        log.info(">>>>>>> [GameService]-endgame gameRoomUser.getUserId() {}", gameRoomUser.getUserId());
        if (gameRoom.getHostId().equals(gameRoomUser.getUserId())) {
            hostWebSessionId = gameRoomUser.getWebSessionId();
        }else {
            throw new CustomException(StatusMsgCode.YOUR_NOT_HOST);
        }
        log.info(">>>>>>> [GameService]-endgame hostWebSessionId {}", hostWebSessionId);

        // 현재 GameRoom 이 시작되지 않았다면
        if (!gameRoom.isPlaying()) {
            throw new CustomException(StatusMsgCode.NOT_STARTED_YET);
        }

        // GameRoom 에서 진행된 모든 GameFlow 삭제
        gameFlowRepository.deleteAllByRoomId(gameRoom.getId());
        log.info(">>>>>>> [GameService]-endgame gameRoom 의 id : {}", gameRoom.getId());

        // GameRoom 의 상태를 false 로 변경
        gameRoom.GameRoomStatusUpdate(false);

        Map<String, Boolean> message = new HashMap<>();
        message.put("end", true);

        // 구독하고 있는 User 에게 start 메세지 전송
        log.info(">>>>>>> [GameService]-endgame 의 content : {}", message);
        sendingOperations.convertAndSend("/topic/game/end/" + requestDto.getRoomId(), message);

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
        sendingOperations.convertAndSend("/topic/game/shutDown/" + requestDto.getRoomId(),"shutDown");
        log.info(">>>>>>> [GameService]-shutDownGame 의 roomId : {}", requestDto.getRoomId());

        // 게임 강제 종료
        return new MsgResponseDto(StatusMsgCode.SHUTDOWN_GAME);
    }


    // 단어 적는 라운드 끝났을 때 GameFlow 에 결과 저장
    // requestDto 필요한 정보
    // token, roomId, round, keyword, keywordIndex, webSessionId
    @Transactional
    public void postVocabulary(GameFlowRequestDto requestDto) {

        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - postVocabulary >>>>>>>>>>>>>>>>>>>>>>>>");
        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>> [GameService] - postVocabulary 의 유저 해쉬맵 : {}", gamerInfo);
        log.info("<<<<<<<<<<<<<<< 리퀘스트디티오 1 save 하기 전 : {}", requestDto.getKeywordIndex());

        GameFlow gameFlow = GameFlow.builder()
                .roomId(requestDto.getRoomId())
                .round(requestDto.getRound())
                .keywordIndex(requestDto.getKeywordIndex())
                .keyword(requestDto.getKeyword())
                .webSessionId(requestDto.getWebSessionId())
                .nickname(gamerInfo.get(GamerEnum.NICK.key()))
                .build();
        gameFlowRepository.saveAndFlush(gameFlow);

        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 방 번호 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 라운드 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 키워드 번호 : {}", gameFlow.getKeywordIndex());
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 키워드 : {}", gameFlow.getKeyword());
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 닉네임 : {}", gameFlow.getNickname());
        log.info(">>>>>>> [GameService] - postVocabulary 의 GameFlow -> 보내는 사람 세션Id : {}", gameFlow.getWebSessionId());

        List<GameFlow> gameFlowList = gameFlowRepository.findAllByRoomIdAndRound(requestDto.getRoomId(), requestDto.getRound());
        List<GameRoomUser> gameRoomUserList  = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

        if (gameFlowList.size() == gameRoomUserList.size()) {
            Map<String, Object> message = new HashMap<>();
            message.put("completeSubmit", true);
            sendingOperations.convertAndSend("/topic/game/submit-word/" + requestDto.getRoomId(), message);

            // 라운드를 계속 체크해서 라운드가 인원수가 크다면 [결과페이지 이동]
            checkRoundSendMsg(requestDto.getRound(), gameRoomUserList.size(), requestDto);
//            if (requestDto.getRound()  == gameRoomUserList.size()){
//                log.info(">>>>>>>> 마지막 라운드 : {}", requestDto.getRound());
//                Map<String, Object> beforeMessage = new HashMap<>();
//                beforeMessage.put("isResult",true);
//                sendingOperations.convertAndSend("/topic/game/before-result/" + requestDto.getRoomId(), beforeMessage);
//            } // 중복 코드 분리 1.18 리팩토링 김재영
            String destination = "word";
            createDtoCallPreviousData(gameFlowList.size(),gameFlowList, destination);
//            for (int i = 0; i < gameFlowList.size(); i++) {
//                GameFlowRequestDto gameFlowRequestDto = GameFlowRequestDto.builder()
//                        .roomId(gameFlowList.get(i).getRoomId())
//                        .round(gameFlowList.get(i).getRound())
//                        .keyword(gameFlowList.get(i).getKeyword())
//                        .keywordIndex(gameFlowList.get(i).getKeywordIndex())
//                        .webSessionId(gameFlowList.get(i).getWebSessionId())
//                        .build();
//
//                log.info(">>>>>>> 여기가 진짜 이전 키워드 : {}", gameFlowRequestDto.getKeyword());
//                log.info(">>>>>>> 여기가 진짜 이전 키워드 번호 : {}", gameFlowRequestDto.getKeywordIndex());
//                log.info(">>>>>>> 제시어 보낸 webSessionId : {}", gameFlowRequestDto.getWebSessionId());
//                getPreviousKeyword(gameFlowRequestDto);
//            } // 중복 코드 분리 1.18 리팩토링 김재영
        }
    }


    // 그림그리는 라운드 시작  ← 이전 라운드의 단어 response 로 줘야함! (GetMapping)
    // requestDto 필요한 정보
    // token, round, roomId, keywordIndex, keyword
    public void getPrevious(GameFlowRequestDto requestDto, String destination) {

        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - getPreviousKeyword >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>> [GameService] - getPreviousKeyword 라운드 : {}", requestDto.getRound());
        log.info(">>>>>>> [GameService] - getPreviousKeyword 받는사람 키워드 순번 : {}", requestDto.getKeywordIndex());

        // 라운드가 0인지 아닌지 검증
        if (requestDto.getRound() <= 0) {
            throw new CustomException(StatusMsgCode.GAME_NOT_ONLINE);
        }

        // 현재 GameRoom 의 UserList 가져오기
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

        // 다음순번인 키워드의 index
        int nextKeywordIndex = calculateKeywordIndex(requestDto.getKeywordIndex(), gameRoomUserList.size());
//        if (requestDto.getKeywordIndex() == gameRoomUserList.size()){
//            log.info("requestDto.getKeywordIndex() : {}",requestDto.getKeywordIndex());
//            log.info("gameRoomUserList.size() : {}",gameRoomUserList.size());
//            nextKeywordIndex = requestDto.getKeywordIndex() % gameRoomUserList.size() + 1;
//            log.info("nextKeywordIndex : {}",nextKeywordIndex);
//        }else {
//            nextKeywordIndex = requestDto.getKeywordIndex() + 1;
//            log.info("else 를 통과한 requestDto.getKeywordIndex() : {}",requestDto.getKeywordIndex());
//            log.info("else 를 통과한 nextKeywordIndex : {}",nextKeywordIndex);
//        } // 중복 코드 분리 1.18 리팩토링 김재영

        GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(
                requestDto.getRoomId(),
                requestDto.getRound(),
                nextKeywordIndex).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAME_NOT_ONLINE)
        );
        log.info(">>>>>>> [GameService] - getPrevious 의 GameFlow -> 방 번호 : {}", gameFlow.getRoomId());
        log.info(">>>>>>> [GameService] - getPrevious 의 GameFlow -> 라운드 : {}", gameFlow.getRound());
        log.info(">>>>>>> [GameService] - getPrevious 의 GameFlow -> 여기가 진짜 새로운 키워드 번호 : {}", nextKeywordIndex);
        log.info(">>>>>>> [GameService] - getPrevious 의 GameFlow -> 여기가 진짜 새로운 키워드 : {}", gameFlow.getKeyword());
        log.info(">>>>>>> [GameService] - getPrevious 의 GameFlow -> 닉네임 : {}", gameFlow.getNickname());

        log.info(">>>>>>> [GameService] - getPrevious 접속한 유저의 WebSessionId : {}", requestDto.getWebSessionId());

        // keyword, keywordIndex, round
        Map<String, Object> message = new HashMap<>();

        // 요청한 유저가 받을 다음 순번의 키워드의 index
        message.put("keywordIndex", nextKeywordIndex);

        switch (destination) {
            case "word" -> {
                message.put("keyword", gameFlow.getKeyword());
                log.info(">>>>>>> [GameService] - getPreviousKeyword 의 메시지 : {}", message);
            }
            case "image" -> {
                message.put("image", gameFlow.getImagePath());
                log.info(">>>>>>> [GameService] - getPreviousImage 의 메시지 : {}", message);
            }
            default -> {
                log.info(">>>>>>> [GameService] - destination 잘못된 요청입니다.");
            }
        }
        sendingOperations.convertAndSend("/queue/game/before-" + destination + "/" + requestDto.getWebSessionId(), message);
//        if (destination.equals("word")){
//            //  이전 라운드의 키워드
//            message.put("keyword", gameFlow.getKeyword());
//            log.info(">>>>>>> [GameService] - getPreviousKeyword 의 메시지 : {}", message);
////            sendingOperations.convertAndSend("/queue/game/before-word/" + requestDto.getWebSessionId(), message);
//        }else if (destination.equals("image")){
//            //  이전 라운드의 이미지
//            message.put("image", gameFlow.getImagePath());
//            log.info(">>>>>>> [GameService] - getPreviousImage 의 메시지 : {}", message);
//        }
//        sendingOperations.convertAndSend("/queue/game/before-" + destination + "/" + requestDto.getWebSessionId(), message);
                        // 중복 코드 변경 1.18 리팩토링
//        return new KeywordResponseDto(gameFlow); // 수정
    }

    // 그림그리는 라운드 끝났을 때 GameFlow 에 결과 저장 (PostMapping)
    // requestDto 필요한 정보
    // token, round, roomId, keywordIndex, webSessionId
    @Transactional
    public void postImage(GameFlowRequestDto requestDto, String image) throws IOException {

        // 해당 roomId, round, 그리고 인원수
//        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());
//
//        gameRoomUserList.clear(); // 필요없음 1.18 리팩토링 김재영

        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - postImage >>>>>>>>>>>>>>>>>>>>>>>>");
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> 이미지 파일 있니? : {}", !image.isEmpty());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
        if (!image.isEmpty()){
            log.info(">>>>>>>>>>>>>>>>>>>>>>>> 이미지 파일: {}", image);
        }
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> requestDto 방번호 : {}", requestDto.getRoomId());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> requestDto 키워드 index : {}", requestDto.getKeywordIndex());
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> requestDto webSessionId : {}", requestDto.getWebSessionId());

        // data로 들어온  'data:image/png;base64,iVBORw0KGgoAAA..... 문자열 자르기
        String[] strings = image.split(",");
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


        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        String nickname = gamerInfo.get(GamerEnum.NICK.key());
        log.info(">>>>>>>>>>>>> token 으로 부터 나온 nickname : {}", nickname);

        // Image 엔티티 painter( 그린사람 nickname )+ imagePath 저장
        // image 엔티티 imagePath → s3 저장

        if (file.isFile()) {
            s3Service.upload(file, directoryName, nickname, requestDto.getRound(), requestDto.getKeywordIndex(), requestDto.getRoomId(), requestDto.getWebSessionId());
        }
        log.info(">>>>>>>>>>>>> request 으로 부터 나온 getRound : {}", requestDto.getRound());
        log.info(">>>>>>>>>>>>> request 으로 부터 나온 getKeywordIndex : {}", requestDto.getKeywordIndex());
        log.info(">>>>>>>>>>>>> request 으로 부터 나온 getRoomId : {}", requestDto.getRoomId());
        log.info(">>>>>>>>>>>>> request 으로 부터 나온 getWebSessionId : {}", requestDto.getWebSessionId());

        List<GameFlow> gameFlowList = gameFlowRepository.findAllByRoomIdAndRound(requestDto.getRoomId(), requestDto.getRound());
        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

        if (gameFlowList.size() == gameRoomUserList.size()) {
            Map<String, Object> message = new HashMap<>();
            message.put("completeSubmit", true);
//            sendingOperations.convertAndSend("/topic/game/submit-word/" + requestDto.getRoomId(), message);
            sendingOperations.convertAndSend("/topic/game/submit-image/" + requestDto.getRoomId(), message);

            // 라운드를 계속 체크해서 라운드가 인원수가 크다면 [결과페이지 이동]
            checkRoundSendMsg(requestDto.getRound(), gameRoomUserList.size(), requestDto);
//            if (requestDto.getRound()  == gameRoomUserList.size()){
//                log.info(">>>>>>>> 마지막 라운드 : {}", requestDto.getRound());
//                Map<String, Object> beforeMessage = new HashMap<>();
//                beforeMessage.put("isResult",true);
//                sendingOperations.convertAndSend("/topic/game/before-result/" + requestDto.getRoomId(), beforeMessage);
//            } // 중복 코드 분리 1.18 리팩토링 김재영


            // 전체 유저에게 보내줌 뭐를? requestDto
            String destination = "image";
            createDtoCallPreviousData(gameFlowList.size(),gameFlowList, destination);
//            for (int i = 0; i < gameFlowList.size(); i++) {
//                GameFlowRequestDto gameFlowRequestDto = GameFlowRequestDto.builder()
//                        .roomId(gameFlowList.get(i).getRoomId())
//                        .round(gameFlowList.get(i).getRound())
//                        .keyword(gameFlowList.get(i).getKeyword())
//                        .keywordIndex(gameFlowList.get(i).getKeywordIndex())
//                        .webSessionId(gameFlowList.get(i).getWebSessionId())
//                        .build();
//
//                log.info(">>>>>>> 여기가 진짜 이전 키워드 : {}", gameFlowRequestDto.getKeyword());
//                log.info(">>>>>>> 여기가 진짜 이전 키워드 번호 : {}", gameFlowRequestDto.getKeywordIndex());
//                log.info(">>>>>>> 제시어 보낸 webSessionId : {}", gameFlowRequestDto.getWebSessionId());
//
//                getPreviousImage(gameFlowRequestDto);
//            } // 중복 코드 분리 1.18 리팩토링 김재영

        }
    }


    // 단어적는 라운드 시작  ← 이전 라운드의 그림 response 로 줘야함! (GepMapping)
    // requestDto 필요한 정보
    // round,  roomId, keywordIndex, token,
//    public ImageResponseDto getPreviousImage(GameFlowRequestDto requestDto) {
//
//        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - getPreviousImage >>>>>>>>>>>>>>>>>>>>>>>>");
//        if (requestDto.getRound() <= 0) {
//            throw new CustomException(StatusMsgCode.GAME_NOT_ONLINE);
//        }
//
//        // 현재 GameRoom 의 UserList 가져오기
//        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());
//
//        // 다음순번인 키워드의 index
//        int nextKeywordIndex = calculateKeywordIndex(requestDto.getKeywordIndex(), gameRoomUserList.size());
//
////        if (requestDto.getKeywordIndex() == gameRoomUserList.size()){
////            nextKeywordIndex = requestDto.getKeywordIndex() % gameRoomUserList.size() + 1;
////        }else {
////            nextKeywordIndex = requestDto.getKeywordIndex() + 1;
////        } // 중복 코드 분리 1.18 리팩토링 김재영
//
//
//        //  이전 라운드에 어떤 그림 이었는 지 알려줘야 함
//        GameFlow gameFlow = gameFlowRepository.findByRoomIdAndRoundAndKeywordIndex(
//                requestDto.getRoomId(),
//                requestDto.getRound(),
//                nextKeywordIndex).orElseThrow(
//                () -> new CustomException(StatusMsgCode.GAME_NOT_ONLINE)
//        );
//        log.info(">>>>>>> [GameService] - getPreviousImage 의 GameFlow 번호 : {}", gameFlow.getId());
//
//        // img, keywordIndex, round
//        Map<String, Object> message = new HashMap<>();
//        //  이전 라운드의 이미지
//        message.put("image", gameFlow.getImagePath());
//        message.put("keywordIndex", nextKeywordIndex);
//        log.info(">>>>>>> [GameService] - getPreviousImage 의 메시지 : {}", message);
//
//        sendingOperations.convertAndSend("/queue/game/before-image/" + requestDto.getWebSessionId(), message);
//        return new ImageResponseDto(gameFlow);
//    } // 사용안함 1.18 리팩토링

    // 결과 보여주기
    // requestDto 필요한 정보
    // roomId, webSessionId, token
    public Object[][] getGameFlow(GameFlowRequestDto requestDto) {
        log.info(">>>>>>>>>>>>>>>>>>>>>>>> [GameService] - getGameFlow >>>>>>>>>>>>>>>>>>>>>>>>");

        boolean isHost = false;
        // 현재 접속한 유저가 방장인지 아닌지
        // 토큰에서 유저 정보 가져오기 [id,nickname,imgUrl]
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>> [GameService]-getGameFlow gamerInfo {}", gamerInfo);
        Long userId = Long.valueOf(gamerInfo.get(GamerEnum.ID.key()));

        GameRoom gameRoom = gameRoomRepository.findById(requestDto.getRoomId()).orElseThrow(
                () -> new CustomException(StatusMsgCode.GAMEROOM_NOT_FOUND)
        );

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
                    // 짝수 round 일 때 -> 이미지 불러와야 함
                    resultList.add(gameFlow.getImagePath());

                } else {
                    // 홀수 round 일 때 -> 제시어 가져오기!
                    resultList.add(gameFlow.getKeyword());

                }
                listlist[i - 1][j - 1] = resultList;
            }
        }
        log.info(">>>>>>>>>>>>> 대망의 마지막 2차원 배열 : {} ", Arrays.deepToString(listlist));

        Map<String, Object> message = new HashMap<>();
        message.put("result", listlist);
        message.put("isHost", isHost);

        sendingOperations.convertAndSend("/queue/game/result/" + requestDto.getWebSessionId(), message);
        return listlist;
    }

    //최초 디폴트 제시어 던져주기
    // requestDto 필요한 정보
    // roomId, ,token
    @Transactional
    public void getRandomKeyword(GameFlowRequestDto requestDto) {

        // 유저 검증부
        HashMap<String, String> gamerInfo = userService.gamerInfo(requestDto.getToken());
        log.info(">>>>>>> [GameService] - getRandomKeyword 의 유저 해쉬맵 : {}", gamerInfo);

        // 찾은 User 로 GameRoomUser 정보 가져오기
        GameRoomUser gameRoomUser = gameRoomUserRepository.findByUserIdAndGameRoomId(
                Long.valueOf(gamerInfo.get(GamerEnum.ID.key())), requestDto.getRoomId());
        log.info(">>>>>>> [GameService] - getRandomKeyword 의 GameRoomUser -> 방 번호 : {}", gameRoomUser.getGameRoom());
        log.info(">>>>>>> [GameService] - getRandomKeyword 의 GameRoomUser -> 플레이 상태 : {}", gameRoomUser.isReadyStatus());

        List<GameRoomUser> gameRoomUserList = gameRoomUserRepository.findAllByGameRoomId(requestDto.getRoomId());

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

        log.info(">>>>>>> [GameService] - getRandomKeyword 의 webSessionId -> : {}", gameRoomUser.getWebSessionId());
    }

    public void createDtoCallPreviousData(int gameFlowListSize, List<GameFlow> gameFlowList, String destination){
        for (int i = 0; i < gameFlowListSize; i++) {
            GameFlowRequestDto gameFlowRequestDto = GameFlowRequestDto.builder()
                    .roomId(gameFlowList.get(i).getRoomId())
                    .round(gameFlowList.get(i).getRound())
                    .keyword(gameFlowList.get(i).getKeyword())
                    .keywordIndex(gameFlowList.get(i).getKeywordIndex())
                    .webSessionId(gameFlowList.get(i).getWebSessionId())
                    .build();

            log.info(">>>>>>> 여기가 진짜 이전 키워드 : {}", gameFlowRequestDto.getKeyword());
            log.info(">>>>>>> 여기가 진짜 이전 키워드 번호 : {}", gameFlowRequestDto.getKeywordIndex());
            log.info(">>>>>>> 제시어 보낸 webSessionId : {}", gameFlowRequestDto.getWebSessionId());

//            if (isKey){
                getPrevious(gameFlowRequestDto, destination);
//            }else {
//                getPreviousImage(gameFlowRequestDto);
//            }
        }
    }

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

    public void checkRoundSendMsg(int nowRound, int maxUserSize, GameFlowRequestDto requestDto){
        if (nowRound == maxUserSize) {
            log.info(">>>>>>>> 마지막 라운드 : {}", requestDto.getRound());
            Map<String, Object> beforeMessage = new HashMap<>();
            beforeMessage.put("isResult", true);
            sendingOperations.convertAndSend("/topic/game/before-result/" + requestDto.getRoomId(), beforeMessage);
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

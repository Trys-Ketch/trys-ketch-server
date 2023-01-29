package com.project.trysketch.service;

import com.project.trysketch.dto.response.ImageLikeResponseDto;
import com.project.trysketch.dto.response.UserResponseDto;
import com.project.trysketch.global.dto.DataMsgResponseDto;
import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.global.jwt.JwtUtil;
import com.project.trysketch.entity.User;
import com.project.trysketch.entity.Image;
import com.project.trysketch.entity.ImageLike;
import com.project.trysketch.repository.ImageLikeRepository;
import com.project.trysketch.repository.ImageRepository;
import com.project.trysketch.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


// 1. 기능   : ImageLike 서비스
// 2. 작성자 : 황미경
@Service
@RequiredArgsConstructor
public class ImageService {

    private final AmazonS3Service s3Service;
    private final ImageRepository imageRepository;
    private final ImageLikeRepository imageLikeRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;


//    // S3에 이미지 저장
//    public MsgResponseDto saveImage(MultipartFile multipartFile, HttpServletRequest request) throws IOException {
//        Claims claims = jwtUtil.authorizeToken(request);
//        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
//                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
//        );
//
//        if (multipartFile != null) {
//            s3Service.upload(multipartFile, "static", user);
//        }
//        return new MsgResponseDto(StatusMsgCode.DONE_DRAWING);
//    }


    // 이미지 좋아요
    public DataMsgResponseDto likeImage(Long imageId, HttpServletRequest request) {
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        Image image = imageRepository.findById(imageId).orElseThrow(
                () -> new CustomException(StatusMsgCode.IMAGE_NOT_FOUND)
        );

        // ImageLike 에 값이 있는지 확인
        Optional<ImageLike> imageLike = imageLikeRepository.findByImageIdAndUserId(imageId, user.getId());

        if (imageLike.isPresent()) { // 좋아요 하지 않았으면 좋아요 추가
            imageLikeRepository.save(new ImageLike(image, user));
            Map<String, Boolean> checkLikeMap = new HashMap<>();
            checkLikeMap.put("isLike",true);
            return new DataMsgResponseDto(StatusMsgCode.LIKE_IMAGE, checkLikeMap);
        }else {  // 이미 좋아요 했다면 좋아요 취소
            imageLikeRepository.deleteByImageIdAndUserId(imageId, user.getId());
            Map<String, Boolean> checkLikeMap = new HashMap<>();
            checkLikeMap.put("isLike",false);
            return new DataMsgResponseDto(StatusMsgCode.CANCEL_LIKE, checkLikeMap);
        }
    }

//    // 좋아요 여부 확인
//    @Transactional(readOnly = true)
//    public boolean checkLike(Long imageId, HttpServletRequest request) {
//        Claims claims = jwtUtil.authorizeToken(request);
//        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
//                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
//        );
//
//        Optional<ImageLike> imageLike = imageLikeRepository.findByImageIdAndUserId(imageId, user.getId());
//        return imageLike.isPresent();
//    }
//    // 좋아요 삭제
//    @Transactional
//    public MsgResponseDto cancelLike(Long imageId, HttpServletRequest request) {
//        Claims claims = jwtUtil.authorizeToken(request);
//        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
//                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
//        );
//
//        imageRepository.findById(imageId).orElseThrow(
//                () -> new CustomException(StatusMsgCode.IMAGE_NOT_FOUND)
//        );
//        if (!checkLike(imageId, request)) {
//            throw new CustomException(StatusMsgCode.ALREADY_CANCEL_LIKE);
//        }
//        imageLikeRepository.deleteByImageIdAndUserId(imageId, user.getId());
//        return new MsgResponseDto(StatusMsgCode.CANCEL_LIKE);
//    }




    // S3에 업로드 된 이미지 조회
    @Transactional(readOnly = true)
    public Page<ImageLike> getImage(HttpServletRequest request, Pageable pageable) { // 수정 pageable 추가 김재영 01.29
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByNickname(claims.get("nickname").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

//        List<ImageLike> imageLikeList = imageLikeRepository.findAllByUserId(user.getId());

        Page<ImageLike> imageLikePage = imageLikeRepository.findAllByUserId(user.getId(), pageable); // 수정 pageable 추가 김재영 01.29

        // ImageLike 을 Dto 형태로 담아줄 List 선언
        List<ImageLikeResponseDto> imageLikeResponseDtoList = new ArrayList<>();

        // GameRoom 의 정보와 LastPage 정보를 담아줄 Map 선언
        Map<String, Object> getAllImageLike = new HashMap<>();

        for (ImageLike imageLike : imageLikePage){

            ImageLikeResponseDto imageLikeResponseDto = ImageLikeResponseDto.builder()
                    .imgId(imageLike.getImage().getId())
                    .imgPath(imageLike.getImage().getPath())
                    .painter(imageLike.getUser().getNickname())
                    .createdAt(imageLike.getImage().getCreatedAt())
                    .build();
            imageLikeResponseDtoList.add(imageLikeResponseDto);
        }
        getAllImageLike.put("image", imageLikeResponseDtoList);
        getAllImageLike.put("lastPage",imageLikePage.getTotalPages());

        return imageLikePage;
//        List<String> imagePathList = new ArrayList<>();
//
//        for (ImageLike imageLike : imageLikeList) {
//            imagePathList.add(imageLike.getImage().getPath());
//        }
//        return imagePathList;
    }








    // 스케줄러 통해서 관리. 좋아요 안 눌린 이미지 삭제
    @Transactional
    public MsgResponseDto deleteImage() {
        List<Image> imageList = imageRepository.findAll();
        for (Image image : imageList) {
            if (image.getImageLikes().size() == 0) {
                imageRepository.delete(image);
                String path = image.getPath();
                String filename = path.substring(62);
                s3Service.delete(filename);
            }
        }
        return new MsgResponseDto(StatusMsgCode.DELETE_IMAGE);
    }

    // 마이페이지 회원조회
    public DataMsgResponseDto getMyPage(HttpServletRequest request) {

        // 유저 정보 가져오기
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByEmail(claims.get("email").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // 유저 정보에서 필요한 정보( id, email, nickname, ImgUrl ) 추출
        UserResponseDto userResponseDto = UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .imagePath(user.getImgUrl())
                .build();

        return new DataMsgResponseDto(StatusMsgCode.OK,userResponseDto);
    }

    // 마이페이지 회원 닉네임 수정
    @Transactional
    public DataMsgResponseDto patchMyPage(String newNickname, HttpServletRequest request) {

        // 유저 정보 가져오기
        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByEmail(claims.get("email").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        // 유저 닉네임 변경
        user.updateNickname(newNickname);

        // 유저 정보에서 필요한 정보( id, email, nickname, ImgUrl ) 추출
        UserResponseDto userResponseDto = UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .imagePath(user.getImgUrl())
                .build();

        return new DataMsgResponseDto(StatusMsgCode.OK,userResponseDto);
    }
}
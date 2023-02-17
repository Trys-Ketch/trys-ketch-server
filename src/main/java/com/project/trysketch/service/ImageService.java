package com.project.trysketch.service;

import com.project.trysketch.dto.response.ImageLikeResponseDto;
import com.project.trysketch.dto.response.DataMsgResponseDto;
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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ImageService {

    private final ImageRepository imageRepository;
    private final ImageLikeRepository imageLikeRepository;
    private final UserRepository userRepository;
    private final AmazonS3Service s3Service;
    private final JwtUtil jwtUtil;

    // 이미지 좋아요
    public DataMsgResponseDto likeImage(Long imageId, HttpServletRequest request) {

        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByEmail(claims.get("email").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        Image image = imageRepository.findById(imageId).orElseThrow(
                () -> new CustomException(StatusMsgCode.IMAGE_NOT_FOUND)
        );

        Map<String, Boolean> checkLikeMap = new HashMap<>();

        // ImageLike 에 값이 있는지 확인
        ImageLike imageLike = imageLikeRepository.findByImageIdAndUserId(imageId, user.getId()).orElse(null);

        if (imageLike == null) {
            // 좋아요 하지 않았으면 좋아요 추가
            imageLikeRepository.save(new ImageLike(image, user));
            checkLikeMap.put("isLike",true);
            return new DataMsgResponseDto(StatusMsgCode.LIKE_IMAGE, checkLikeMap);
        }else {  // 이미 좋아요 했다면 좋아요 취소
            imageLikeRepository.deleteById(imageLike.getId());
            checkLikeMap.put("isLike",false);
            return new DataMsgResponseDto(StatusMsgCode.CANCEL_LIKE, checkLikeMap);
        }
    }

    // S3에 업로드 된 이미지 조회
    @Transactional(readOnly = true)
    public Map<String, Object> getImage(HttpServletRequest request, Pageable pageable) {

        Claims claims = jwtUtil.authorizeToken(request);
        User user = userRepository.findByEmail(claims.get("email").toString()).orElseThrow(
                () -> new CustomException(StatusMsgCode.USER_NOT_FOUND)
        );

        Page<ImageLike> imageLikePage = imageLikeRepository.findAllByUserId(user.getId(), pageable);

        // ImageLike 을 Dto 형태로 담아줄 List 선언
        List<ImageLikeResponseDto> imageLikeResponseDtoList = new ArrayList<>();

        // GameRoom 의 정보와 LastPage 정보를 담아줄 Map 선언
        Map<String, Object> getAllImageLike = new HashMap<>();

        for (ImageLike imageLike : imageLikePage){
            ImageLikeResponseDto imageLikeResponseDto = ImageLikeResponseDto.builder()
                    .imgId(imageLike.getImage().getId())
                    .imgPath(imageLike.getImage().getPath())
                    .painter(imageLike.getImage().getPainter())
                    .createdAt(imageLike.getImage().getCreatedAt())
                    .build();
            imageLikeResponseDtoList.add(imageLikeResponseDto);
        }
        getAllImageLike.put("image", imageLikeResponseDtoList);
        getAllImageLike.put("lastPage",imageLikePage.getTotalPages());

        return getAllImageLike;
    }

    // 스케줄러 통해서 관리. 좋아요 안 눌린 이미지 삭제
    @Transactional
    public void deleteImage() {

        String dirName = "static";
        List<Image> imageList = imageRepository.findAll();
        for (Image image : imageList) {
            if (image.getImageLikes().size() == 0) {
                imageRepository.delete(image);
                String path = image.getPath();
                String filename = path.substring(path.indexOf(dirName));
                s3Service.delete(filename);
            }
        }
    }
}
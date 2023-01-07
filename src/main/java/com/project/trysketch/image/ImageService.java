package com.project.trysketch.image;

import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


// 1. 기능   : ImageLike 서비스
// 2. 작성자 : 황미경

@Service
@RequiredArgsConstructor
public class ImageService {

    private final AmazonS3Service s3Service;
    private final ImageRepository imageRepository;
    private final ImageLikeRepository imageLikeRepository;

    // S3에 이미지 저장
    public MsgResponseDto saveImage(MultipartFile multipartFile, User user) throws IOException {
        if (multipartFile != null) {
            s3Service.upload(multipartFile, "static", user);
        }
        return new MsgResponseDto(StatusMsgCode.DONE_DRAWING);
    }

    // 이미지 좋아요
    public MsgResponseDto likeImage(Long imageId, User user) {
        Image image = imageRepository.findById(imageId).orElseThrow(
                () -> new CustomException(StatusMsgCode.IMAGE_NOT_FOUND)
        );
        if (checkLike(imageId, user)) {
            throw new CustomException(StatusMsgCode.ALREADY_CLICKED_LIKE);
        }
        imageLikeRepository.save(new ImageLike(image, user));
        return new MsgResponseDto(StatusMsgCode.SAVE_IMAGE);
    }


    // S3에 업로드 된 이미지 조회
    @Transactional(readOnly = true)
    public List<String> getImage(User user) {
        List<ImageLike> imageLikeList = imageLikeRepository.findAllByUserId(user.getId());
        List<String> imagePathList = new ArrayList<>();
        for (ImageLike imageLike : imageLikeList) {
            imagePathList.add(imageLike.getImage().getPath());
                    }
        return imagePathList;
    }

    //좋아요 여부 확인
    @Transactional(readOnly = true)
    public boolean checkLike(Long imageId, User user) {
        Optional<ImageLike> imageLike = imageLikeRepository.findByImageIdAndUserId(imageId, user.getId());
        return imageLike.isPresent();
    }
}
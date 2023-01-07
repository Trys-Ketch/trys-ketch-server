package com.project.trysketch.image;

import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


// 1. 기능   : ImageLike 서비스
// 2. 작성자 : 황미경

@Service
@RequiredArgsConstructor
public class ImageLikeService {

    private final AmazonS3Service s3Service;
    private final ImageFileRepository imageFileRepository;

    // S3에 이미지 저장
    public MsgResponseDto saveImage(MultipartFile multipartFile, User user) throws IOException {
        if (multipartFile != null) {
            s3Service.upload(multipartFile, "static", user);
        }
        return new MsgResponseDto(StatusMsgCode.DONE_LIKE);
    }


    // S3에 업로드 된 이미지 조회
    @Transactional(readOnly = true)
    public List<String> getImage(User user) {
        // User가 좋아요 누른 그림 DB로부터 불러오기
        List<String> imagePathList = new ArrayList<>();
        List<ImageFile> imageFileList = imageFileRepository.findByUserId(user.getId());

        for (ImageFile imageFile : imageFileList) {
            imagePathList.add(imageFile.getPath());
        }
        return imagePathList;
    }
}
package com.project.trysketch.image;

import com.project.trysketch.global.dto.MsgResponseDto;
import com.project.trysketch.global.exception.CustomException;
import com.project.trysketch.global.exception.StatusMsgCode;
import com.project.trysketch.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ImageLikeService {

    private final AmazonS3Service s3Service;
    private final ImageFileRepository imageFileRepository;

    // S3에 이미지 저장
    public MsgResponseDto saveImage(ImageLikeRequestDto requestDto, List<MultipartFile> multipartFilelist, User user) throws IOException {
        if (multipartFilelist != null) {
            s3Service.upload(requestDto, multipartFilelist, "static", user);
        }
        return new MsgResponseDto(StatusMsgCode.DONE_LIKE);
    }

    @Transactional(readOnly = true)
    public List<String> getImage(User user) {
        List<String> imagePathList = new ArrayList<>();
        List<ImageFile> imageFileList = imageFileRepository.findByUserId(user.getId());
            for(ImageFile imageFile : imageFileList) {
                imagePathList.add(imageFile.getPath());
            }
        return imagePathList;

    }
}
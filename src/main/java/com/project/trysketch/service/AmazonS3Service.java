package com.project.trysketch.service;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.project.trysketch.entity.Image;
import com.project.trysketch.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.util.UUID;

// 1. 기능   : S3 업로드 로직
// 2. 작성자 : 황미경
@Slf4j
@Component
@RequiredArgsConstructor
@Service
public class AmazonS3Service {

    private final ImageRepository imageRepository;
    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")  // bucket 이름
    public String bucket;

    // 이미지 업로드 (S3, DB)
    @Transactional
    public Image upload(File file, String dirName, String nickname) {
        Image image = null;
        if (file != null) {
            image = new Image(upload(file, dirName), nickname);
            imageRepository.save(image);
        }
        return image;
    }

    // S3로 파일 업로드 (파일이름 지정, 파일 업로드 메서드 호출)
    @Transactional
    public String upload(File uploadFile, String dirName) {
        String fileName = dirName + "/" + UUID.randomUUID();     // S3에 저장될 파일 이름
        String uploadImageUrl = putS3(uploadFile, fileName);     // s3로 업로드
        removeNewFile(uploadFile);                               // 로컬에 저장된 파일 지우기
        return uploadImageUrl;
    }

    // S3로 실제 파일 업로드
    @Transactional
    public String putS3(File uploadFile, String fileName) {
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile).withCannedAcl(CannedAccessControlList.PublicRead));
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }

    // S3 이미지 삭제
    @Transactional
    public void delete(String fileName) {
        DeleteObjectRequest request = new DeleteObjectRequest(bucket, fileName);
        amazonS3Client.deleteObject(request);
    }

    // JVM 에 저장된 temp file 지우기
    @Transactional
    public void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("File delete success");
            return;
        }
        log.info("File delete fail");
    }
}
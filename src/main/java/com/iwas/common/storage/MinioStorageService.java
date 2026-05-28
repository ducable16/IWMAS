package com.iwas.common.storage;

import com.iwas.common.enums.ErrorCode;
import com.iwas.common.exception.AppException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.storage.endpoint}")
    private String endpoint;

    @Value("${app.storage.bucket}")
    private String bucket;

    @Value("${app.storage.presigned-url-expiration-minutes:60}")
    private long presignedUrlExpirationMinutes;

    @PostConstruct
    public void initBucket() {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("[Storage] Bucket '{}' is ready", bucket);
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("[Storage] Created bucket '{}'", bucket);
        } catch (Exception e) {
            log.warn("[Storage] Could not verify bucket '{}': {}", bucket, e.getMessage());
        }
        applyAvatarPublicPolicy();
    }

    private void applyAvatarPublicPolicy() {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/avatars/*"]
                    }
                  ]
                }
                """.formatted(bucket);
        try {
            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                    .bucket(bucket)
                    .policy(policy)
                    .build());
            log.info("[Storage] Public read policy applied to avatars/*");
        } catch (Exception e) {
            log.warn("[Storage] Could not apply avatar public policy: {}", e.getMessage());
        }
    }

    @Override
    public String upload(MultipartFile file, String key) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            return key;
        } catch (Exception e) {
            log.error("[Storage] Upload failed: key={}", key, e);
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }

    @Override
    public String getUrl(String key) {
        if (key.startsWith("avatars/")) {
            String url = endpoint + "/" + bucket + "/" + key;
            log.info("[Storage] Get url '{}'", url);
            return url;
        }
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build())
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            log.warn("[Storage] Delete failed: key={}, reason={}", key, e.getMessage());
        }
    }
}

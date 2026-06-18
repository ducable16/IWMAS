package com.iwas.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

@Configuration
public class StorageConfig {

    /** Bỏ trống => dùng endpoint mặc định của AWS S3. Đặt giá trị => MinIO/R2/B2. */
    @Value("${app.storage.endpoint:}")
    private String endpoint;

    @Value("${app.storage.region:us-east-1}")
    private String region;

    /** Bỏ trống => lấy credentials từ default provider chain (IAM instance role trên EC2, env, profile...). */
    @Value("${app.storage.access-key:}")
    private String accessKey;

    @Value("${app.storage.secret-key:}")
    private String secretKey;

    /** true cho MinIO (path-style); false cho AWS S3 (virtual-hosted style). */
    @Value("${app.storage.path-style-access:false}")
    private boolean pathStyleAccess;

    private AwsCredentialsProvider credentialsProvider() {
        if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }
        // Không có key tĩnh => IAM role / biến môi trường / ~/.aws/credentials
        return DefaultCredentialsProvider.create();
    }

    private S3Configuration serviceConfiguration() {
        return S3Configuration.builder()
                .pathStyleAccessEnabled(pathStyleAccess)
                .build();
    }

    @Bean
    public S3Client s3Client() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(serviceConfiguration());
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(serviceConfiguration());
        if (StringUtils.hasText(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }
}

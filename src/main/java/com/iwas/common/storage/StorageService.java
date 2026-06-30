package com.iwas.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String upload(MultipartFile file, String key);

    String getUrl(String key);

    void delete(String key);

    default String resolveUrl(String keyOrUrl) {
        if (keyOrUrl == null) return null;
//        if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) return keyOrUrl;
        return getUrl(keyOrUrl);
    }
}

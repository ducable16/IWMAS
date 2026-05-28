package com.iwas.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /** Uploads the file to the given key path and returns the key. */
    String upload(MultipartFile file, String key);

    /** Constructs a fresh presigned URL for a stored key. */
    String getUrl(String key);

    void delete(String key);

    /**
     * Resolves a value that may be either a storage key or a legacy stored URL.
     * Returns null for null input. Returns the value as-is if it already looks like a URL
     * (legacy rows that stored presigned URLs — they remain stale until the user re-uploads).
     * Otherwise generates a fresh presigned URL from the key.
     */
    default String resolveUrl(String keyOrUrl) {
        if (keyOrUrl == null) return null;
//        if (keyOrUrl.startsWith("http://") || keyOrUrl.startsWith("https://")) return keyOrUrl;
        return getUrl(keyOrUrl);
    }
}

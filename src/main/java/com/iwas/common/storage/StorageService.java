package com.iwas.common.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    /** Uploads the file to the given key path and returns the key. */
    String upload(MultipartFile file, String key);

    /** Constructs the public URL for a stored key. */
    String getUrl(String key);

    void delete(String key);
}

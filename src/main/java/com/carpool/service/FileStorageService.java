package com.carpool.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storePublicProfile(MultipartFile file);
    String storePrivateKyc(MultipartFile file);
}

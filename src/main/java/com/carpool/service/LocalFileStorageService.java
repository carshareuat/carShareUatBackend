package com.carpool.service;

import com.carpool.config.AppProperties;
import com.carpool.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_MIME = Set.of("image/jpeg", "image/png", "application/pdf");
    private final AppProperties appProperties;

    @Override
    public String storePublicProfile(MultipartFile file) {
        return store(file, appProperties.getFileStorage().getProfilePublicDir());
    }

    @Override
    public String storePrivateKyc(MultipartFile file) {
        return store(file, appProperties.getFileStorage().getKycPrivateDir());
    }

    private String store(MultipartFile file, String dir) {
        if (file == null || file.isEmpty()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "File is required");
        }
        if (!ALLOWED_MIME.contains(file.getContentType())) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY, "SEMANTIC_ERROR", "Unsupported file type");
        }

        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String safeExt = ext == null ? "bin" : ext.replaceAll("[^a-zA-Z0-9]", "");
        String fileName = UUID.randomUUID() + "." + safeExt;

        Path base = Paths.get(appProperties.getFileStorage().getLocalRoot()).toAbsolutePath().normalize();
        Path targetDir = base.resolve(dir).normalize();
        Path target = targetDir.resolve(fileName).normalize();
        if (!target.startsWith(base)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid path");
        }
        try {
            Files.createDirectories(targetDir);
            file.transferTo(target);
            return dir + "/" + fileName;
        } catch (IOException e) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "FILE_STORAGE_ERROR", "Could not store file");
        }
    }
}

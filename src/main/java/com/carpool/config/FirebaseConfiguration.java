package com.carpool.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@Slf4j
public class FirebaseConfiguration {

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    @Bean
    public FirebaseApp firebaseApp() {
        try {
            String configuredJson = resolveServiceAccountJson();
            if (configuredJson == null || configuredJson.isBlank()) {
                log.warn("Firebase service account JSON not configured; FCM will remain disabled until configured");
                return null;
            }

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(configuredJson.getBytes(StandardCharsets.UTF_8))
            );

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp firebaseApp = FirebaseApp.initializeApp(options);
                log.info("Firebase Admin initialized successfully for project: {}", credentials.getQuotaProjectId());
                return firebaseApp;
            }
            return FirebaseApp.getInstance();
        } catch (Exception e) {
            log.error("Firebase initialization failed", e);
            return null;
        }
    }

    private String resolveServiceAccountJson() {
        if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
            return serviceAccountJson;
        }

        String envJson = System.getenv("FIREBASE_SERVICE_ACCOUNT_JSON");
        if (envJson != null && !envJson.isBlank()) {
            return envJson;
        }

        String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
        if (credentialsPath != null && !credentialsPath.isBlank()) {
            try {
                return Files.readString(Path.of(credentialsPath));
            } catch (Exception e) {
                log.warn("Unable to read GOOGLE_APPLICATION_CREDENTIALS at {}", credentialsPath, e);
            }
        }

        Path currentDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path[] candidatePaths = new Path[] {
            currentDir.resolve("firebase-service-account.json"),
            currentDir.resolve("src/main/resources/firebase-service-account.json"),
            currentDir.resolve("src/main/resources/"),
            currentDir.getParent() != null ? currentDir.getParent().resolve("firebase-service-account.json") : null,
            Path.of("firebase-service-account.json")
        };

        for (Path candidate : candidatePaths) {
            if (candidate == null) {
                continue;
            }
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                try {
                    log.info("Loading Firebase credentials from {}", candidate.toAbsolutePath());
                    return Files.readString(candidate);
                } catch (Exception e) {
                    log.warn("Unable to read Firebase credentials file at {}", candidate, e);
                }
            }
        }

        return null;
    }
}

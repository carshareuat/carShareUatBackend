package com.carpool.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private FileStorage fileStorage = new FileStorage();
    private SecurityRate security = new SecurityRate();
    private Subscription subscription = new Subscription();
    private Razorpay razorpay = new Razorpay();
    private Admin admin = new Admin();
    private Vapid vapid = new Vapid();

    @Data
    public static class Jwt {
        private String secret;
        private long accessTokenMinutes;
        private long refreshTokenDays;
    }

    @Data
    public static class Cors {
        private List<String> allowedOrigins;
    }

    @Data
    public static class FileStorage {
        private String localRoot;
        private String kycPrivateDir;
        private String profilePublicDir;
    }

    @Data
    public static class SecurityRate {
        private RateLimit rateLimit = new RateLimit();

        @Data
        public static class RateLimit {
            private int authPerMinute;
            private int uploadPerMinute;
        }
    }

    @Data
    public static class Subscription {
        private String currency;
        private String provider;
    }

    @Data
    public static class Razorpay {
        private String keyId;
        private String keySecret;
        private String webhookSecret;
    }

    @Data
    public static class Admin {
        private String mobile;
        private String password;
    }

    @Data
    public static class Vapid {
        private String publicKey;
        private String privateKey;
        private String subject;
    }
}

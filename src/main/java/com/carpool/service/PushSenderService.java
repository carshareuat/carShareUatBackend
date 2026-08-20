package com.carpool.service;

import com.carpool.config.AppProperties;
import com.carpool.entity.PushSubscription;
import com.carpool.repository.PushSubscriptionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.Security;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PushSenderService {
    private static final Logger log = LoggerFactory.getLogger(PushSenderService.class);
    private final PushSubscriptionRepository repo;
    private final AppProperties appProperties;
    private final ObjectMapper mapper = new ObjectMapper();
    private PushService pushService;

    private synchronized PushService getPushService() {
        if (pushService != null) return pushService;
        try {
            Security.addProvider(new BouncyCastleProvider());
            AppProperties.Vapid vapid = appProperties.getVapid();
            String vapidPublic = vapid == null ? null : vapid.getPublicKey();
            String vapidPrivate = vapid == null ? null : vapid.getPrivateKey();
            String subject = (vapid == null || vapid.getSubject() == null || vapid.getSubject().isBlank())
                ? "mailto:admin@carshare.example" : vapid.getSubject();
            boolean hasVapid = (vapidPublic != null && !vapidPublic.isBlank() && vapidPrivate != null && !vapidPrivate.isBlank());
            log.info("VAPID keys present={}", hasVapid);
            if (!hasVapid) {
                log.warn("VAPID keys not configured (app.vapid.public-key/private-key or VAPID_PUBLIC/VAPID_PRIVATE env vars); web-push will fail");
                return null;
            }
            PushService ps = new PushService();
            ps.setPublicKey(vapidPublic);
            ps.setPrivateKey(vapidPrivate);
            ps.setSubject(subject);
            this.pushService = ps;
            return ps;
        } catch (Exception e) {
            log.error("Failed to initialize PushService", e);
            return null;
        }
    }

    public PushResult sendToUser(java.util.UUID userId, java.util.Map<String, Object> payload) {
        List<PushSubscription> subs = repo.findByUserId(userId);
        return sendToSubscriptions(subs, payload);
    }

    public PushResult sendToAll(java.util.Map<String, Object> payload) {
        List<PushSubscription> subs = repo.findAll();
        log.info("sendToAll: found {} subscriptions", subs == null ? 0 : subs.size());
        return sendToSubscriptions(subs, payload);
    }

    // Admin/debug helper: return a lightweight view of all subscriptions
    public java.util.List<java.util.Map<String, Object>> getAllSubscriptions() {
        return repo.findAll().stream().map(s -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", s.getId());
            m.put("userId", s.getUser() == null ? null : s.getUser().getId());
            m.put("endpoint", s.getEndpoint());
            m.put("p256dh", s.getP256dh());
            m.put("p256dhLength", s.getP256dh() == null ? 0 : s.getP256dh().length());
            m.put("auth", s.getAuth());
            m.put("authLength", s.getAuth() == null ? 0 : s.getAuth().length());
            m.put("createdAt", s.getCreatedAt());
            return m;
        }).toList();
    }

    // Remove subscriptions that are missing endpoint or keys (return count removed)
    public int removeInvalidSubscriptions() {
        List<PushSubscription> subs = repo.findAll();
        int removed = 0;
        for (PushSubscription s : subs) {
            boolean invalid = s.getEndpoint() == null || s.getEndpoint().isBlank() || s.getP256dh() == null || s.getP256dh().isBlank() || s.getAuth() == null || s.getAuth().isBlank();
            if (invalid) {
                try { repo.deleteById(s.getId()); removed++; } catch (Exception e) { log.warn("Failed to delete invalid subscription {}", s.getId(), e); }
            }
        }
        return removed;
    }

    // Normalize stored base64/base64url encodings for p256dh/auth and save if changed
    public int normalizeSubscriptions() {
        List<PushSubscription> subs = repo.findAll();
        int updated = 0;
        for (PushSubscription s : subs) {
            String p = s.getP256dh();
            String a = s.getAuth();
            String np = normalizeBase64Url(p);
            String na = normalizeBase64Url(a);
            if (np != null && !np.equals(p) || na != null && !na.equals(a)) {
                if (np != null) s.setP256dh(np);
                if (na != null) s.setAuth(na);
                try { repo.save(s); updated++; } catch (Exception e) { log.warn("Failed to save normalized subscription {}", s.getId(), e); }
            }
        }
        return updated;
    }

    // Delete by subscription id (admin helper)
    public void deleteSubscriptionById(String id) {
        try { repo.deleteById(java.util.UUID.fromString(id)); } catch (IllegalArgumentException ia) {
            // if id not a UUID, try string delete if repository supports it
            try { repo.deleteById(java.util.UUID.fromString(id)); } catch (Exception e) { throw new RuntimeException("Invalid id: " + id); }
        }
    }

    private String normalizeBase64Url(String v) {
        if (v == null) return null;
        v = v.trim();
        if (v.isEmpty()) return v;
        // If it already looks like base64url (contains - or _), strip padding and return
        if (v.contains("-") || v.contains("_")) {
            return v.replace("=", "");
        }
        // Convert standard base64 to base64url: replace +/ -> -_ and strip '=' padding
        String converted = v.replace('+', '-').replace('/', '_');
        converted = converted.replaceAll("=+$", "");
        return converted;
    }

    private PushResult sendToSubscriptions(List<PushSubscription> subs, java.util.Map<String, Object> payload) {
        PushResult result = new PushResult();
        result.totalSubscriptions = subs == null ? 0 : subs.size();
        PushService ps = getPushService();
        if (ps == null) {
            log.warn("Skipping push send: PushService unavailable (VAPID not configured)");
            result.errors.add("VAPID keys not configured on server (app.vapid.public-key/private-key)");
            return result;
        }
        for (PushSubscription s : subs) {
            try {
                log.debug("Attempting push to subscription: {} endpoint={}", s.getId(), s.getEndpoint());
                byte[] body = mapper.writeValueAsBytes(payload);
                Notification notification = new Notification(s.getEndpoint(), s.getP256dh(), s.getAuth(), body);
                HttpResponse response = ps.send(notification);
                int status = response.getStatusLine().getStatusCode();
                if (status == 404 || status == 410) {
                    // Subscription is gone/expired on the push service; remove it so we stop retrying.
                    log.info("Removing expired subscription {} (status={})", s.getId(), status);
                    repo.deleteById(s.getId());
                    result.expiredRemoved++;
                } else if (status >= 300) {
                    log.warn("Push to subscription {} returned status={}", s.getId(), status);
                    result.failed++;
                    result.errors.add("subscription " + s.getId() + ": HTTP " + status);
                } else {
                    log.debug("Push sent to subscription {} (status={})", s.getId(), status);
                    result.sent++;
                }
            } catch (Exception e) {
                String msg = e.getMessage() == null ? e.toString() : e.getMessage();
                log.warn("Failed to send push to subscription {} -> {}", s.getId(), msg);
                log.debug("Push send exception for subscription {}", s.getId(), e);
                // If the stored public key is malformed (common: incorrect base64 vs base64url or truncated)
                // remove the subscription so we stop retrying; caller can re-subscribe from client.
                if (msg != null && msg.contains("Incorrect length for uncompressed encoding")) {
                    try {
                        log.info("Deleting subscription {} because of malformed p256dh", s.getId());
                        repo.deleteById(s.getId());
                        result.expiredRemoved++;
                        result.errors.add("subscription " + s.getId() + ": " + msg + " (deleted)");
                        continue;
                    } catch (Exception ex) {
                        log.warn("Failed to delete malformed subscription {}", s.getId(), ex);
                    }
                }
                result.failed++;
                result.errors.add("subscription " + s.getId() + ": " + msg);
            }
        }
        return result;
    }

    public static class PushResult {
        public int totalSubscriptions;
        public int sent;
        public int failed;
        public int expiredRemoved;
        public java.util.List<String> errors = new java.util.ArrayList<>();
    }
}

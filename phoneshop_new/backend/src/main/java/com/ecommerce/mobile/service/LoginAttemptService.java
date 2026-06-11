package com.ecommerce.mobile.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bảo vệ brute force: khóa IP sau MAX_ATTEMPT lần đăng nhập sai trong LOCK_DURATION_MINUTES phút.
 * Dùng in-memory ConcurrentHashMap — đủ cho demo, không cần DB.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPT = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private record AttemptInfo(int count, LocalDateTime firstAttemptAt, LocalDateTime lockedAt) {}

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    /** Gọi khi đăng nhập thất bại. */
    public void loginFailed(String key) {
        AttemptInfo info = attempts.get(key);
        LocalDateTime now = LocalDateTime.now();

        if (info == null) {
            attempts.put(key, new AttemptInfo(1, now, null));
        } else {
            // Reset nếu lần thử đầu tiên đã quá LOCK_DURATION_MINUTES
            if (info.firstAttemptAt() != null &&
                    info.firstAttemptAt().plusMinutes(LOCK_DURATION_MINUTES).isBefore(now)) {
                attempts.put(key, new AttemptInfo(1, now, null));
            } else {
                int newCount = info.count() + 1;
                LocalDateTime lockedAt = newCount >= MAX_ATTEMPT ? now : null;
                attempts.put(key, new AttemptInfo(newCount, info.firstAttemptAt(), lockedAt));
            }
        }
    }

    /** Gọi khi đăng nhập thành công — xóa counter. */
    public void loginSucceeded(String key) {
        attempts.remove(key);
    }

    /** Kiểm tra IP có đang bị khóa không. */
    public boolean isBlocked(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null || info.lockedAt() == null) return false;
        // Hết thời gian khóa thì tự mở
        if (info.lockedAt().plusMinutes(LOCK_DURATION_MINUTES).isBefore(LocalDateTime.now())) {
            attempts.remove(key);
            return false;
        }
        return true;
    }

    /** Trả về số phút còn lại bị khóa (0 nếu không bị khóa). */
    public long minutesUntilUnlock(String key) {
        AttemptInfo info = attempts.get(key);
        if (info == null || info.lockedAt() == null) return 0;
        long remaining = java.time.Duration.between(LocalDateTime.now(),
                info.lockedAt().plusMinutes(LOCK_DURATION_MINUTES)).toMinutes();
        return Math.max(0, remaining + 1);
    }
}

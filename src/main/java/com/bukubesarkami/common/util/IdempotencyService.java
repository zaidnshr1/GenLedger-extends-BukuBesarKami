package com.bukubesarkami.common.util;

import com.bukubesarkami.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${app.idempotency.ttl-hours:24}")
    private int ttlHours;

    private static final String PREFIX = "idempotency:journal:";

    /**
     * Cek apakah idempotency key sudah pernah diproses.
     * Jika sudah → lempar exception agar tidak double entry.
     * Jika belum → tandai sebagai "dalam proses" (PENDING).
     */
    public void checkAndMark(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new AppException.BusinessException(
                    "Header X-Idempotency-Key wajib diisi untuk operasi ini."
            );
        }

        String redisKey = PREFIX + idempotencyKey;
        Boolean isNew = redisTemplate.opsForValue()
                .setIfAbsent(redisKey, "PENDING", Duration.ofHours(ttlHours));

        if (Boolean.FALSE.equals(isNew)) {
            String status = (String) redisTemplate.opsForValue().get(redisKey);
            if ("COMMITTED".equals(status)) {
                throw new AppException.DuplicateException(
                        "Transaksi dengan idempotency key ini sudah berhasil diproses sebelumnya."
                );
            }
            // Jika PENDING → sedang diproses, tolak duplikasi
            throw new AppException.DuplicateException(
                    "Transaksi sedang atau sudah diproses. Gunakan idempotency key baru untuk transaksi berbeda."
            );
        }
    }

    /**
     * Tandai key sebagai COMMITTED setelah transaksi berhasil disimpan.
     */
    public void commit(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        String redisKey = PREFIX + idempotencyKey;
        redisTemplate.opsForValue().set(redisKey, "COMMITTED", Duration.ofHours(ttlHours));
    }

    /**
     * Hapus key jika terjadi error — agar client bisa retry dengan key yang sama.
     */
    public void rollback(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        redisTemplate.delete(PREFIX + idempotencyKey);
        log.warn("Idempotency key rolled back: {}", idempotencyKey);
    }
}
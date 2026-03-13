package com.bukubesarkami.config;

import com.bukubesarkami.common.exception.AppException;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@Slf4j
public class RateLimiterService {

    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final int capacity;
    private final int refillTokens;
    private final int refillSeconds;

    public RateLimiterService(
            LettuceConnectionFactory lettuceConnectionFactory,
            @Value("${app.rate-limit.login.capacity:5}") int capacity,
            @Value("${app.rate-limit.login.refill-tokens:5}") int refillTokens,
            @Value("${app.rate-limit.login.refill-seconds:60}") int refillSeconds) {

        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillSeconds = refillSeconds;

        String host = lettuceConnectionFactory.getHostName();
        int port = lettuceConnectionFactory.getPort();
        String password = lettuceConnectionFactory.getPassword();
        boolean useSsl = lettuceConnectionFactory.isUseSsl();

        String protocol = useSsl ? "rediss" : "redis";

        String redisUriString = String.format("%s://:%s@%s:%d",
                protocol,
                password != null ? password : "",
                host,
                port);

        log.info("RateLimiter connecting to Redis at {}:{} (SSL: {})", host, port, useSsl);

        RedisClient redisClient = RedisClient.create(redisUriString);

        StatefulRedisConnection<byte[], byte[]> connection =
                redisClient.connect(ByteArrayCodec.INSTANCE);

        this.proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .build();
    }

    /**
     * Cek rate limit per IP address.
     * Melempar TooManyRequestsException jika bucket habis.
     */
    public void checkLoginRateLimit(String ipAddress) {
        String bucketKey = "rate:login:" + ipAddress;
        Supplier<BucketConfiguration> configSupplier = () ->
                BucketConfiguration.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(capacity)
                                .refillGreedy(refillTokens, Duration.ofSeconds(refillSeconds))
                                .build())
                        .build();

        var bucket = proxyManager.builder().build(bucketKey.getBytes(), configSupplier);
        if (!bucket.tryConsume(1)) {
            log.warn("Rate limit exceeded for IP: {}", ipAddress);
            throw new AppException.TooManyRequestsException(
                    "Terlalu banyak percobaan login. Coba lagi dalam " + refillSeconds + " detik."
            );
        }
    }
}
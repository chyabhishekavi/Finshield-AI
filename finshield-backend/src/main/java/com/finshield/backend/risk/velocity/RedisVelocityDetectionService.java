package com.finshield.backend.risk.velocity;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class RedisVelocityDetectionService implements VelocityDetectionService {

    private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);
    private static final Duration ONE_HOUR = Duration.ofHours(1);
    private static final Duration TWENTY_FOUR_HOURS = Duration.ofHours(24);

    private static final long FIVE_MINUTE_TTL_SECONDS = Duration.ofMinutes(6).toSeconds();
    private static final long ONE_HOUR_TTL_SECONDS = Duration.ofMinutes(61).toSeconds();
    private static final long TWENTY_FOUR_HOUR_TTL_SECONDS = Duration.ofHours(25).toSeconds();

    private static final DefaultRedisScript<Long> ADD_AND_COUNT_SCRIPT = new DefaultRedisScript<>("""
            redis.call('ZADD', KEYS[1], ARGV[1], ARGV[2])
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[3])
            redis.call('EXPIRE', KEYS[1], ARGV[4])
            return redis.call('ZCARD', KEYS[1])
            """, Long.class);

    private static final DefaultRedisScript<Long> TRIM_AND_COUNT_SCRIPT = new DefaultRedisScript<>("""
            redis.call('ZREMRANGEBYSCORE', KEYS[1], '-inf', ARGV[1])
            if redis.call('EXISTS', KEYS[1]) == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[2])
            end
            return redis.call('ZCARD', KEYS[1])
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisVelocityDetectionService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public VelocityMetrics recordTransaction(
            UUID eventId,
            UUID customerId,
            String beneficiaryAccountNumber,
            BigDecimal amount,
            String deviceId,
            Instant occurredAt
    ) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(customerId, "customerId must not be null");
        Objects.requireNonNull(beneficiaryAccountNumber, "beneficiaryAccountNumber must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");

        long timestamp = occurredAt.toEpochMilli();
        String customerPrefix = "finshield:velocity:customer:" + customerId;

        long transactionCount = executeCount(
                customerPrefix + ":transactions:5m",
                timestamp,
                eventId.toString(),
                occurredAt.minus(FIVE_MINUTES).toEpochMilli(),
                FIVE_MINUTE_TTL_SECONDS
        );

        String amountKey = customerPrefix + ":amount:1h";
        String amountMember = eventId + "|" + amount.toPlainString();
        executeCount(
                amountKey,
                timestamp,
                amountMember,
                occurredAt.minus(ONE_HOUR).toEpochMilli(),
                ONE_HOUR_TTL_SECONDS
        );
        BigDecimal amountTotal = sumAmounts(amountKey);

        long distinctBeneficiaries = executeCount(
                customerPrefix + ":beneficiaries:24h",
                timestamp,
                sha256(beneficiaryAccountNumber),
                occurredAt.minus(TWENTY_FOUR_HOURS).toEpochMilli(),
                TWENTY_FOUR_HOUR_TTL_SECONDS
        );

        long suspiciousAttempts = deviceId == null || deviceId.isBlank()
                ? 0L
                : trimAndCountDeviceAttempts(deviceId, occurredAt);

        return new VelocityMetrics(
                transactionCount,
                amountTotal,
                distinctBeneficiaries,
                suspiciousAttempts,
                Instant.now()
        );
    }

    @Override
    public void recordFailedOrSuspiciousAttempt(
            UUID eventId,
            String deviceId,
            Instant occurredAt
    ) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(deviceId, "deviceId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        executeCount(
                deviceAttemptsKey(deviceId),
                occurredAt.toEpochMilli(),
                eventId.toString(),
                occurredAt.minus(TWENTY_FOUR_HOURS).toEpochMilli(),
                TWENTY_FOUR_HOUR_TTL_SECONDS
        );
    }

    private long trimAndCountDeviceAttempts(String deviceId, Instant occurredAt) {
        Long count = redisTemplate.execute(
                TRIM_AND_COUNT_SCRIPT,
                List.of(deviceAttemptsKey(deviceId)),
                String.valueOf(occurredAt.minus(TWENTY_FOUR_HOURS).toEpochMilli()),
                String.valueOf(TWENTY_FOUR_HOUR_TTL_SECONDS)
        );
        return count == null ? 0L : count;
    }

    private long executeCount(
            String key,
            long timestamp,
            String member,
            long cutoff,
            long ttlSeconds
    ) {
        Long count = redisTemplate.execute(
                ADD_AND_COUNT_SCRIPT,
                List.of(key),
                String.valueOf(timestamp),
                member,
                String.valueOf(cutoff),
                String.valueOf(ttlSeconds)
        );
        return count == null ? 0L : count;
    }

    private String deviceAttemptsKey(String deviceId) {
        return "finshield:velocity:device:" + sha256(deviceId) + ":suspicious:24h";
    }

    private BigDecimal sumAmounts(String key) {
        Set<String> members = redisTemplate.opsForZSet().range(key, 0, -1);
        if (members == null || members.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return members.stream()
                .map(member -> member.substring(member.indexOf('|') + 1))
                .map(BigDecimal::new)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}

package pe.victoryordi.paycore.adapter.out.redis;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import pe.victoryordi.paycore.application.port.out.IdempotencyPort;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class RedisIdempotencyAdapter implements IdempotencyPort {

    private static final String PREFIX = "paycore:idempotency:";
    private static final String PROCESSING = "PROCESSING";
    private static final String COMPLETED = "COMPLETED";
    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              redis.call('psetex', KEYS[1], ARGV[3], ARGV[2])
              return 1
            end
            return 0
            """,
            Long.class);
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
              return redis.call('del', KEYS[1])
            end
            return 0
            """,
            Long.class);

    private final StringRedisTemplate redis;

    public RedisIdempotencyAdapter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Reservation reserve(String key, String fingerprint, Duration ttl) {
        String redisKey = PREFIX + key;
        String leaseToken = UUID.randomUUID().toString();
        String processingValue = PROCESSING + "|" + fingerprint + "|" + leaseToken;
        Boolean acquired = redis.opsForValue().setIfAbsent(redisKey, processingValue, ttl);
        if (Boolean.TRUE.equals(acquired)) {
            return Reservation.acquired(leaseToken);
        }

        String existing = redis.opsForValue().get(redisKey);
        if (existing == null) {
            return reserve(key, fingerprint, ttl);
        }
        String[] parts = existing.split("\\|", -1);
        if (parts.length < 2 || !parts[1].equals(fingerprint)) {
            return Reservation.conflict();
        }
        if (COMPLETED.equals(parts[0]) && parts.length == 3) {
            return Reservation.replay(UUID.fromString(parts[2]));
        }
        return Reservation.inProgress();
    }

    @Override
    public void complete(
            String key,
            String fingerprint,
            String leaseToken,
            UUID paymentId,
            Duration ttl) {
        String expected = PROCESSING + "|" + fingerprint + "|" + leaseToken;
        String completed = COMPLETED + "|" + fingerprint + "|" + paymentId;
        Long updated = redis.execute(
                COMPLETE_SCRIPT,
                List.of(PREFIX + key),
                expected,
                completed,
                Long.toString(ttl.toMillis()));
        if (!Long.valueOf(1).equals(updated)) {
            throw new IllegalStateException("Idempotency lease expired before completion");
        }
    }

    @Override
    public void release(String key, String fingerprint, String leaseToken) {
        String expected = PROCESSING + "|" + fingerprint + "|" + leaseToken;
        redis.execute(RELEASE_SCRIPT, List.of(PREFIX + key), expected);
    }
}

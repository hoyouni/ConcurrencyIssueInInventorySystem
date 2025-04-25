package com.example.stock.repository;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Redis 사용을 위한 레포지토리 생성
 */
@Component
public class RedisLockRepository {

    // Redis 명령어 사용을 위한 템플릿 변수 추가
    private RedisTemplate<String, String> redisTemplate;

    public RedisLockRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Lock 메소드
    public Boolean lock(Long key) {
        return redisTemplate.opsForValue().setIfAbsent(generateKey(key), "lock", Duration.ofMillis(3_000));
    }

    // unLock 메소드
    public void unLock(Long key) {
        redisTemplate.delete(generateKey(key));
    }

    public String generateKey(Long key) {
        return key.toString();
    }

}

package com.example.stock.facade;

import com.example.stock.repository.RedisLockRepository;
import com.example.stock.service.StockService;
import org.springframework.stereotype.Component;

/**
 * 로직 실행 전 후로 Lock 획득 / 해제를 수행하기 위한 Facade 클래스
 * Redis Lettuce 라이브러리 사용
 */
@Component
public class LettuceLockStockFacade {

    // Redis 사용을 위한 변수
    private final RedisLockRepository redisLockRepository;

    // 재고 감소 서비스 클래스
    private final StockService stockService;

    public LettuceLockStockFacade(RedisLockRepository redisLockRepository, StockService stockService) {
        this.redisLockRepository = redisLockRepository;
        this.stockService = stockService;
    }

    // 재고 감소 메소드
    public void decrease(Long id, Long quantity) throws InterruptedException {
        // Lock 획득 실패한 경우 재시도
        while(!redisLockRepository.lock(id)) {
            // Redis 부하 줄이기 위한 텀 주기
            Thread.sleep(100);
        }

        // Lock 획득 성공한 경우 재고 감소 로직 수행 후 Lock 해제
        try {
            stockService.decrease(id, quantity);
        } finally {
            redisLockRepository.unLock(id);
        }

    }
}

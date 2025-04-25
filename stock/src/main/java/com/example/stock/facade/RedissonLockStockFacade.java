package com.example.stock.facade;

import com.example.stock.service.StockService;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis Redisson 사용을 위한 클래스
 */

@Component
public class RedissonLockStockFacade {

    // Lock 획득에 사용할 클라이언트 선언
    private final RedissonClient redissonClient;

    // 재고 감소 서비스 클래스
    private final StockService stockService;

    public RedissonLockStockFacade(RedissonClient redissonClient, StockService stockService) {
        this.redissonClient = redissonClient;
        this.stockService = stockService;
    }

    // 재고 감소 메소드
    public void decrease(Long id, Long quantity) {
        // Redisson client 를 활용하여 Lock 인스턴스 생성
        RLock rLock = redissonClient.getLock(id.toString());

        try {
            // 몇 초 동안 Lock 획득 시도할 건지, 몇 초 동안 점유할 건지 작성
            boolean available = rLock.tryLock(10, 1, TimeUnit.SECONDS);

            // Lock 획득 실패 시 로그
            if(!available) {
                System.out.println("Lock 획득 실패");
                return;
            }

            // 재고 감소 로직 수행
            stockService.decrease(id, quantity);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // Lock 해제
            rLock.unlock();
        }
    }

}

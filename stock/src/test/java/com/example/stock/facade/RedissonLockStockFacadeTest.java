package com.example.stock.facade;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis Redisson 테스트를 위한 클래스
 */
@SpringBootTest
class RedissonLockStockFacadeTest {

    @Autowired
    private RedissonLockStockFacade redissonLockStockFacade;  // Redis Redisson 사용을 위한 변수 선언

    @Autowired
    private StockRepository stockRepository;    // 상품재고 엔티티 CRUD 인터페이스

    /**
     * 테스트를 하기 위해서는 재고가 존재해야 하므로 @BeforeEach 어노테이션을 사용해
     * 테스트가 실행 되기 전에 데이터 생성 (상품아이디 1 , 재고 100)
     */
    @BeforeEach
    public void before() {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }

    /**
     * 테스트 종료 후 관련 데이터 삭제
     */
    @AfterEach
    public void after() {
        stockRepository.deleteAll();
    }

    /**
     * Test 2-3-2) Redis Redisson 을 활용한 재고 감소 로직 동시성 (동시에 여러 건의 요청) 제어 테스트
     * - 테스트 결과 기대하는 값인 재고 0 과 실제 데이터인 재고 0 이 일치하는 것을 확인
     *  . Redis Redisson 장점
     *   ㄴ pub-sub 기반으로 구현되어 Redis 부하를 줄여줌
     *  . Redis Redisson 단점
     *   ㄴ 구현이 조금 복잡함
     *   ㄴ 별도의 라이브러리 사용을 해야함
     */
    @Test
    public void decreaseStockQuantityConcurrencyTestUsingRedisRedisson() throws InterruptedException {
        // 동시에 여러개의 요청을 보내야 하기 때문에 멀티쓰레드 사용하여 100개의 요청을 보낼 것
        int threadCount = 100;

        // 멀티쓰레드 사용을 위한 ExecutorService 사용 (비동기로 실행하는 작업을 단순화 하여 사용할 수 있게 도와주는 자바 API)
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        /*
         * 100 건의 요청이 모두 끝날때 까지 기다려야 하므로 CountDownLatch 사용
         * CountDownLatch : 다른 쓰레드에서 수행중인 작업이 완료될 때 까지 대기할 수 있도록 도와주는 클래스
         */
        CountDownLatch latch = new CountDownLatch(threadCount);

        // 100 건 요청 수행 로직
        for(int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonLockStockFacade.decrease(1L, 1L);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // 상품 재고 조회
        Stock stock = stockRepository.findById(1L).orElseThrow();

        // 실제 데이터 확인 (왼쪽 파라미터 : 기대값 0 / 오른쪽 파라미터 : 실제값 ??)
        assertEquals(0, stock.getQuantity());
    }
}
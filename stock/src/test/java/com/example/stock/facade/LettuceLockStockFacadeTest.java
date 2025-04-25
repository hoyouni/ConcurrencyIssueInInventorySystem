package com.example.stock.facade;

import com.example.stock.domain.Stock;
import com.example.stock.repository.LockRepository;
import com.example.stock.repository.RedisLockRepository;
import com.example.stock.repository.StockRepository;
import com.example.stock.service.StockService;
import jakarta.transaction.Transactional;
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
 * Redis Lettuce 를 통한 Lock 생성 및 해제 테스트 클래스
 */
@SpringBootTest
class LettuceLockStockFacadeTest {

    @Autowired
    private LettuceLockStockFacade lettuceLockStockFacade;  // Redis Lettuce 사용을 위한 변수 선언

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
     * Test 2-3-1) Redis Lettuce 을 활용한 재고 감소 로직 동시성 (동시에 여러 건의 요청) 제어 테스트
     * - 테스트 결과 기대하는 값인 재고 0 과 실제 데이터인 재고 0 이 일치하는 것을 확인
     *  . Redis Lettuce 장점
     *   ㄴ 구현이 단순함
     *  . Redis Lettuce 단점
     *   ㄴ spin Lock 방식으로 Redis 에 부하를 줄 수 있어 Thread.sleep 을 통해 Lock 획득 재시도 간 텀을 주어야 함
     */
    @Test
    public void decreaseStockQuantityConcurrencyTestUsingRedisLettuce() throws InterruptedException {
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
                    lettuceLockStockFacade.decrease(1L, 1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
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
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
 * 네임드 락 (Named Lock) 테스트를 위한 클래스
 */
@SpringBootTest
class NamedLockStockFacadeTest {

    @Autowired
    private NamedLockStockFacade namedLockStockFacade;

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
     * Test 2-2-3) Database 네임드 락 (Named Lock) 을 활용한 재고 감소 로직 동시성 (동시에 여러 건의 요청) 테스트
     * - 테스트 결과 기대하는 값인 재고 0 과 실제 데이터인 재고 0 이 일치하는 것을 확인
     *  . 네임드 락 (Named Lock) 의 장점
     *   ㄴ Time Out 을 손쉽게 구현할 수 있음
     *   ㄴ 데이터 삽입 시 정합성을 맞추어야 하는 경우 사용
     *  . 네임드 락 (Named Lock) 의 단점
     *   ㄴ 트랜젝션 종료 시 Lock 해제 및 세션 관리를 잘 해주어야 하기 때문에 주의 해야함
     *   ㄴ 실제로 사용 시 구현 방법이 복잡함
     */
    @Test
    public void decreaseStockQuantityConcurrencyTestUsingNamedLock() throws InterruptedException {
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
                    namedLockStockFacade.decrease(1L, 1L);
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
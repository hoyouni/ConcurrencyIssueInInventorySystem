package com.example.stock.service;

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
 * 상품재고 서비스 로직 테스트
 */
@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;          // 상품재고 서비스 로직

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
     * Test 1-1) 재고 감소 로직 단 건 (1개의 요청) 테스트
     * - 로직은 수행될 수 있으나 발생 가능한 문제점이 있음.
     *   . 만약에 동시에 여러개의 요청이 들어온다면..?
     */
    @Test
    public void decreaseStockQuantityTest() {
        // 상품 재고 서비스의 재고 감소 로직 수행
        stockService.decrease(1L, 1L);

        // 상품 재고 조회
        Stock stock = stockRepository.findById(1L).orElseThrow();

        // 실제 데이터 확인 (왼쪽 파라미터 : 기대값 99 / 오른쪽 파라미터 : 실제값 ??)
        assertEquals(99L, stock.getQuantity());
    }

    /**
     * Test 1-2) 재고 감소 로직 동시성 (동시에 여러 건의 요청) 테스트
     * - 특정 상품의 재고가 100개이고 100건의 요청이 동시에 들어온다고 가정한 테스트케이스
     *  . 기대하는 값은 재고가 0 이어야 하지만 실제로 동작한 결과 96건의 재고가 확인됨
     * - Race Condition 이 발생했기 때문
     *  . Race Condition 이란 둘 이상의 쓰레드가 공유 데이터(Stock Entity)에 엑세스 할 수 있고
     *    동시에 변경을 하려고 할 때 발생하는 문제로
     *    A 쓰레드가 특정 상품의 재고가 100개 있는걸 확인하고 재고를 변경하기 전에
     *    B 쓰레드가 특정 상품의 재고를 확인하게 됨. A 쓰레드가 재고를 변경하기 전에 먼저 접근해버리는 바람에 B 쓰레드도 재고 100개로 확인함.
     *    결국 A 쓰레드도 100 - 1 = 99 개로 갱신하고 B 쓰레드도 100 - 1 = 99 개로 갱신 해버림.
     * - 해결방안
     *  . 하나의 쓰레드가 작업이 완료된 이 후에 다른 쓰레드가 데이터에 접근하도록 하면 될 것으로 확인.
     */
    @Test
    public void decreaseStockQuantityConcurrencyTest() throws InterruptedException {
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
                    stockService.decrease(1L, 1L);
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
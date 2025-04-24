package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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
     * Test 1-1) 재고 감소 로직 테스트
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
}
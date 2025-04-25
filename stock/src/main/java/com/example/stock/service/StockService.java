package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

/**
 * 상품재고 관련 서비스 로직
 */
@Service
public class StockService {

    // 상품재고 인터페이스 변수 선언
    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    /**
     * 상품재고 감소 메소드 1
     * @param id        상품 아이디
     * @param quantity  상품 수량
     */
    @Transactional
    public void decrease(Long id, Long quantity) {
        // 재고 조회 > 재고 감소 > 갱신된 값 저장
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }

    /**
     * 상품재고 감소 메소드 2
     * 자바의 Synchronized 를 활용하여 스레드 작업을 제어하도록 설정
     * @param id        상품 아이디
     * @param quantity  상품 수량
     */
    @Transactional
    public synchronized void decreaseUsingSynchronized(Long id, Long quantity) {
        // 재고 조회 > 재고 감소 > 갱신된 값 저장
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }

    /**
     * 상품재고 감소 메소드 3
     * 네임드 락 (Named Lock) 을 활용하여 동시성 제어
     * 부모의 트랜젝션과 별도로 실행 되어야 하므로 Propagation 변경
     * @param id        상품 아이디
     * @param quantity  상품 수량
     */
    @org.springframework.transaction.annotation.Transactional(propagation = Propagation.REQUIRES_NEW)
    public void decreaseUsingNamedLock(Long id, Long quantity) {
        // 재고 조회 > 재고 감소 > 갱신된 값 저장
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);
        stockRepository.saveAndFlush(stock);
    }

}

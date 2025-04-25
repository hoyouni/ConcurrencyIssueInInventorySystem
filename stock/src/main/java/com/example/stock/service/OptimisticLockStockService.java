package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.repository.StockRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * 낙관적 락 (Optimistic Lock) 을 이용한 상품재고 서비스 로직
 */
@Service
public class OptimisticLockStockService {

    // 상품재고 CRUD 를 위한 변수 선언
    private final StockRepository stockRepository;

    public OptimisticLockStockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    // 낙관적 락 (Optimistic Lock) 을 이용한 상품재고 감소 로직 구현
    @Transactional
    public void decrease(Long id, Long quantity) {
        Stock stock = stockRepository.findByIdWithOptimisticLock(id);
        stock.decrease(quantity);
        stockRepository.save(stock);
    }
}

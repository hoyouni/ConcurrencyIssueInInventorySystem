package com.example.stock.facade;

import com.example.stock.repository.LockRepository;
import com.example.stock.service.StockService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

/**
 * 실제 로직 전 후로 Lock 획득 및 해제를 위한 네임드 락 관련 클래스
 */
@Component
public class NamedLockStockFacade {

    // 네임드 락 관련 서비스 인터페이스
    private final LockRepository lockRepository;

    // 재고 감소 서비스 클래스
    private final StockService stockService;

    public NamedLockStockFacade(LockRepository lockRepository, StockService stockService) {
        this.lockRepository = lockRepository;
        this.stockService = stockService;
    }

    // 재고 감소 메소드
    @Transactional
    public void decrease(Long id, Long quantity) {
        try {
            // Lock 획득
            lockRepository.getLock(id.toString());
            // 재고 감소
            stockService.decreaseUsingNamedLock(id, quantity);
        } finally {
            // Lock 해제
            lockRepository.releaseLock(id.toString());
        }
    }

}

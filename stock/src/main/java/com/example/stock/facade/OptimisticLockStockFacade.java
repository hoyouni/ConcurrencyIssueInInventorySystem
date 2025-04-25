package com.example.stock.facade;

import com.example.stock.service.OptimisticLockStockService;
import org.springframework.stereotype.Component;

/**
 * 낙관적 락 (Optimistic Lock) 실패 했을때 재시도를 위한 클래스
 */
@Component
public class OptimisticLockStockFacade {

    // 낙관적 락 서비스 클래스 필드 추가
    private final OptimisticLockStockService optimisticLockStockService;

    public OptimisticLockStockFacade(OptimisticLockStockService optimisticLockStockService) {
        this.optimisticLockStockService = optimisticLockStockService;
    }

    // 재고 감소 실패 시 재시도를 위한 메소드
    public void decrease(Long id, Long quantity) throws InterruptedException {
        // 성공할 때 까지 반복문을 통해 업데이트 수행
        while(true) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                break;
            } catch(Exception e) {
                Thread.sleep(50);
            }
        }
    }

}

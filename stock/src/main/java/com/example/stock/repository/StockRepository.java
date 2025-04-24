package com.example.stock.repository;

import com.example.stock.domain.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/**
 * 상품재고 엔티티 CRUD 를 위한 인터페이스
 */
public interface StockRepository extends JpaRepository<Stock, Long> {
    // 비관적 락 (Pessimistic Lock) 을 활용한 데이터 조작
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Stock s where s.id = :id")
    Stock findByIdWithPessimisticLock(Long id);
}

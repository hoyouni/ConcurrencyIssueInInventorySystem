package com.example.stock.repository;

import com.example.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 상품재고 엔티티 CRUD 를 위한 인터페이스
 */
public interface StockRepository extends JpaRepository<Stock, Long> {

}

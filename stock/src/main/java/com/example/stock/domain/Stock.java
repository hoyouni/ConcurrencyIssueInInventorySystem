package com.example.stock.domain;

import jakarta.persistence.*;

/**
 * 상품재고 엔티티
 */
@Entity
public class Stock {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;     // 상품 아이디

    private Long quantity;      // 상품 수량

    @Version
    private Long version;       // 낙관적 락 (Optimistic Lock) 을 사용하기 위한 버전

    public Stock() {

    }

    public Stock(Long productId, Long quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public Long getQuantity() {
        return quantity;
    }

    // 재고 감소 메소드
    public void decrease(Long quantity) {
        if(this.quantity - quantity < 0) {
            throw new RuntimeException("재고는 0개 미만이 될 수 없습니다.");
        }

        this.quantity -= quantity;
    }
}

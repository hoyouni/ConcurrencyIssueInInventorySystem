package com.example.stock.repository;

import com.example.stock.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 네임드 락 테스트를 위한 레포지토리 생성
 * 편의성을 위해 Stock 엔티티를 사용하지만 실제로는 별도의 JDBC 를 사용해주어야 함
 */
public interface LockRepository extends JpaRepository<Stock, Long> {
    // Lock 획득 메소드
    @Query(value = "select get_lock(:key, 3000)", nativeQuery = true)
    void getLock(String key);

    // Lock 해제 메소드
    @Query(value = "select release_lock(:key)", nativeQuery = true)
    void releaseLock(String key);
}

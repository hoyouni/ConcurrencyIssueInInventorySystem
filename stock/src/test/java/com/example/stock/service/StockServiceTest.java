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
    private PessimisticLockStockService pessimisticLockStockService;    // 비관적 락을 이용한 상품재고 서비스 로직

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
     * - 문제점
     *  . Race Condition 이 발생했기 때문
     *   ㄴ Race Condition 이란 둘 이상의 쓰레드가 공유 데이터(Stock Entity)에 엑세스 할 수 있고
     *     동시에 변경을 하려고 할 때 발생하는 문제로
     *     A 쓰레드가 특정 상품의 재고가 100개 있는걸 확인하고 재고를 변경하기 전에
     *     B 쓰레드가 특정 상품의 재고를 확인하게 됨. A 쓰레드가 재고를 변경하기 전에 먼저 접근해버리는 바람에 B 쓰레드도 재고 100개로 확인함.
     *     결국 A 쓰레드도 100 - 1 = 99 개로 갱신하고 B 쓰레드도 100 - 1 = 99 개로 갱신 해버림.
     * - 해결방안
     *   . 방안 1) 자바의 Synchronized 사용하여 쓰레드 작업 제어
     *    ㄴ 하단 decreaseStockQuantityConcurrencyTestUsingSynchronized 메소드 로직 참고
     *
     *   . 방안 2) DataBase 를 사용하여 데이터 정합성 제어
     *    ㄴ 2-1) 비관적 락 (Pessimistic Lock)
     *     . 실제로 데이터에 Lock 을 걸어서 정합성을 맞추는 방법으로
     *       독점 락 (exclusive lock) 을 걸게되며 다른 트랜잭션에서는 Lock 이 해제되기전에 데이터를 가져갈 수 없게됨.
     *       데드락이 걸릴 수 있기 때문에 주의하여 사용하여야 함.
     *       쓰레드 1 이 락을 걸고 데이터를 점유 하고 있다면 쓰레드 2 는 쓰레드 1의 작업이 끝나고 락을 해제해야 데이터에 접근 가능
     *    ㄴ 2-2) 낙관적 락 (Optimistic Lock)
     *     . 실제로 Lock 을 이용하지 않고 버전을 이용함으로써 정합성을 맞추는 방법으로
     *       먼저 데이터를 읽은 후에 update 를 수행할 때 현재 내가 읽은 버전이 맞는지 확인하며 업데이트 수행,
     *       내가 읽은 버전에서 수정사항이 생겼을 경우에는 application 에서 다시 읽은 후에 작업을 수행해야함.
     *       쓰레드 1 과 쓰레드 2 가 하나의 데이터에 동시에 접근했을 경우 쓰레드 1 이 데이터를 업데이트 수행할 때 해당 데이터의 버전을 올려줌
     *       쓰레드 2 가 읽을 시점에서의 버전이 1 이었다면 쓰레드 1 은 업데이트를 하면서 버전을 2 로 올려줌. 그렇게 되면 쓰레드 2 가 해당 데이터를
     *       업데이트 할 경우 이미 버전이 올라가 정상적으로 수행되지 않을 것임.
     *       단, 낙관적 락은 실패 했을 때 재시도가 필요함
     *    ㄴ 2-3) 네임드 락 (Named Lock)
     *     . 이름을 가진 metadata locking
     *       이름을 가진 Lock 을 획득한 후 해제할 때까지 다른 세션은 이 Lock 을 획득할 수 없도록 제어함.
     *       주의할 점은 트랜잭션이 종료될 때 Lock 이 자동으로 해제되지 않아 별도의 명령어로 해제를 수행해주거나 선점시간이 끝나야 해제됨.
     *       이전에 비관적 락 (Pessimistic Lock) 이 공유 데이터 (Stock) 에 Lock 을 걸었다면 네임드 락은 별도의 공간에 Lock 을 걸어줌.
     *       주로 분산락을 구현할 때 사용
     *
     *   . 방안 3) Redis 를 활용하여 동시성 문제 해결
     *    ㄴ 분산 락을 구현할 때 사용하는 대표적인 라이브러리
     *     . Lettuce
     *      ㄴ setnx 명령어를 활용하여 분산 락 구현
     *        : key 와 value 를 set 할 때 기존의 값이 없을 때만 set 하는 명령어
     *          setnx 를 활용하는 방식은 spin Lock 방식으로 retry 로직 개발자가 작성해야함.
     *          spin Lock : Lock 을 획득하려는 쓰레드가 Lock 을 사용할 수 있는지 반복적으로 확인하면서 Lock 획득을 시도하는 방식
     *                      예를 들어 쓰레드 1 이 key 가 1 인 데이터를 Redis 에 Set 하려고 할 때 최초 요청 시 Redis 에 데이터가 없기 때문에
     *                      정상적으로 Set 하게 됨.
     *                      그 후에 쓰레드 2 가 똑같이 key 가 1 인 데이터를 Set 하려고 할 때 Redis 에는 이미 key 가 1 인
     *                      데이터가 존재하기 때문에 실패를 리턴하게 되고 쓰레드 2 는 일정 시간 후에 Lock 획득을 위해 재시도 하게 되는 방식
     *      ㄴ 구현이 간단하며 Spring data redis 를 주입해주면 Lettuce 가 기본이기 때문에 별도 라이브러리 사용하지 않아도 됨.
     *      ㄴ 동시에 많은 쓰레드가 Lock 점유 중이라면 Redis 에 부하가 가기 때문에 재시도가 필요하지 않은 Lock 의 경우에 Lettuce 사용 권장
     *
     *     . Redisson
     *      ㄴ pub-sub 기반으로 Lock 구현
     *        : 채널 하나를 만들고 Lock 을 점유 중인 쓰레드가 Lock 획득 하려고 대기 중인 쓰레드에게
     *          해제를 알려주면 안내를 받은 쓰레드가 Lock 획득 시도를 하는 방식으로
     *          Lettuce 와 다르게 대부분의 경우에 retry 로직을 작성하지 않아도 됨.
     *          예를 들어 채널이 하나가 있고 쓰레드 1 이 먼저 Lock 을 점유를 하고 있는 상황에서 쓰레드 2 가 점유를 시도 하려고 하면
     *          쓰레드 1 이 Lock 을 해제할 때 끝났다는 메시지를 채널로 보내게 됨. 그 후 채널은 쓰레드 2 에게 락 획득 시도 안내를 전달 해주며
     *          쓰레드 2 는 그 때 락 획득을 시도하게 됨.
     *          Lettuce 는 지속해서 Lock 획득을 시도하는 반면 Redisson 은 Lock 해제가 되었을 때 한 번 혹은 몇 번만 시도를 하기 때문에
     *          Redis 의 부하를 보다 줄여줌
     *      ㄴ Lock 획득 재시도를 기본적으로 제공
     *      ㄴ pub-sub 방식이라 Redis 의 부하가 Lettuce 에 비해 덜 들어감
     *      ㄴ 별도의 라이브러리를 사용해야하며 재시도가 필요한 Lock 의 경우에 Redisson 사용 권장
     *
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

    /**
     * Test 2-1) 자바 Synchronized 를 활용한 재고 감소 로직 동시성 (동시에 여러 건의 요청) 테스트
     * - Synchronized 를 통해 한 스레드가 작업이 끝날때 까지 기다리고 그 이후에 다른 스레드가 작업을 하게 하여
     *   기대하는 값은 재고가 0 이어야 하지만 실제로 동작한 결과 49건의 재고가 확인됨
     *  . Spring 의 Transactional 어노테이션 동작 방식으로 인한 문제로 Transaction 어노테이션이 내부에서
     *    begin tran ~~~ commit; 가 같은 과정을 거치게 되는데
     *    commit 하여 데이터를 갱신하기 전에 다른 쓰레드가 공유 데이터 (Stock Entity) 에 접근 가능하여 발생하는 문제임.
     *    따라서 Transactional 어노테이션을 삭제하고 진행하면 재고 카운트가 0 이 되어 정상 수행되는 것을 확인.
     * - 문제점
     *  . 자바의 Synchronized 는 하나의 프로세스 안에서만 보장됨.
     *    서버가 1대일 때는 데이터의 접근을 서버 한 대만 해서 괜찮겠지만 서버가 여러 대인 경우 데이터 접근을 여러 서버에서 할 수 있음.
     *    예를 들어
     *    A 서버에서 10 : 00 에 재고 감소 로직을 수행하고 10 : 05 에 재고 감소 로직을 종료 한다고 가정하면
     *    B 서버에서 10 : 00 ~ 10 : 05 사이에 갱신되지 않은 정보에 대한 접근이 가능하고 그렇게 되면 다시 Race Condition 이 발생하게 됨.
     *    실제 실무 환경에서는 거의 두 대 이상의 서버를 사용하기 때문에 Synchronized 는 잘 사용하지 않음.
     */
    @Test
    public void decreaseStockQuantityConcurrencyTestUsingSynchronized() throws InterruptedException {
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
                    stockService.decreaseUsingSynchronized(1L, 1L);
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

    /**
     * Test 2-2-1) Database 비관적 락 (Pessimistic Lock) 을 활용한 재고 감소 로직 동시성 (동시에 여러 건의 요청) 테스트
     * - 테스트 결과 기대하는 값인 재고 0 과 실제 데이터인 재고 0 이 일치하는 것을 확인
     *  . 비관적 락 (Pessimistic Lock) 의 장점
     *   ㄴ 충돌이 빈번하게 일어난다면 낙관적 락 (Optimistic Lock) 보다 성능이 좋을 수 있음.
     *   ㄴ Lock 을 통해 Update 를 제어하기 때문에 데이터 정합성을 보장함.
     *  . 비관적 락 (Pessimistic Lock) 의 단점
     *   ㄴ 별도의 Lock 을 잡기 때문에 성능 감소 이슈가 생길 수 있음.
     *   ㄴ Dead Lock 주의 필요
     */
    @Test
    public void decreaseStockQuantityConcurrencyTestUsingPessimisticLock() throws InterruptedException {
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
                    pessimisticLockStockService.decrease(1L, 1L);
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
# 재고 시스템을 활용한 동시성 이슈 발생 및 해결방안
 - 목적
   1)  간단한 재고 시스템 로직 개발을 통해 동시성 이슈에 대한 내용을 이해하고 이를 해결하고자 함
   2)  Database Lock 기법과 Redis 대한 개념과 사용 이유에 대해 이해하며 활용 방안을 습득하고자 함
       - 비관적 락 (Pessimistic Lock) / 낙관적 락 (Optimistic Lock) / 네임드 락 (Named Lock) 에 대한 이해 및 활용
   3)  Redis 제공 라이브러리를 활용한 동시성 제어에 대해 이해하며 활용 방안을 습득하고자 함
       - Lettuce / Redisson Library 에 대한 이해 및 활용
   4) 실무에서 적용할 때 동시성 이슈를 경험하게 된다면 어떤 상황에서 어떤 방식을 채택할 지 고려해보도록 함 (Java Synchronized vs DB Lock vs Redis Library)
       - 비용적 측면 / 트래픽 측면 확인이 필요하며 Redis 에서 처리할 수 없는 트래픽 부하가 생긴다면 Kafka 를 고려해볼 수 있을까?
      
 - 요구사항
   1)  특정 상품의 재고가 100개라고 가정
   2)  동시에 100명의 사용자가 해당 상품을 구매하며 그에 따른 재고가 실시간으로 감소하게 됨
   3)  재고는 0개 미만이 될 수 없음
   4)  1명이 1개의 상품을 구매한다고 가정하며 기대 값인 재고 수량과 실제 재고 수량 비교
      
# Language
 - Java 17
# Database
 - MySQL
# Framework
 - Spring Boot 3
 - JPA (Hibernate)
# Container
 - Docker
# Cache / Message broker
 - Redis


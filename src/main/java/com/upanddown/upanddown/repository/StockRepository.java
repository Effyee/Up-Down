package com.upanddown.upanddown.repository;

import com.upanddown.upanddown.domain.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    // 종목 코드(ticker)를 이용해 주식 정보를 찾아오는 메소드를 정의합니다.
    // Spring Data JPA가 메소드 이름을 분석해서 실제 SQL 쿼리를 자동으로 만들어줍니다.
    Optional<Stock> findByTicker(String ticker);
}

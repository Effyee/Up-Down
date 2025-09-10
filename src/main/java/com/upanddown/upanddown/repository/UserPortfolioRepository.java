package com.upanddown.upanddown.repository;

import com.upanddown.upanddown.domain.UserPortfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface UserPortfolioRepository extends JpaRepository<UserPortfolio, Long> {

    // 낙관적 락 시나리오에서 사용될 일반 조회 메서드
    @Query("select p from UserPortfolio p where p.userId = :userId and p.ticker = :ticker")
    Optional<UserPortfolio> findByUserIdAndTicker(@Param("userId") Long userId, @Param("ticker") String ticker);

    // 비관적 락 시나리오에서 사용될 조회 메서드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserPortfolio p where p.userId = :userId and p.ticker = :ticker")
    Optional<UserPortfolio> findByUserIdAndTickerWithPessimisticLock(@Param("userId") Long userId, @Param("ticker") String ticker);
}

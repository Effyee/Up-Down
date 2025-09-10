package com.upanddown.upanddown.repository;

import com.upanddown.upanddown.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    // 낙관적 락 시나리오에서 사용될 일반 조회 메서드
    @Query("select u from UserAccount u where u.userId = :userId")
    Optional<UserAccount> findByUserId(@Param("userId") Long userId);

    // 비관적 락 시나리오에서 사용될 조회 메서드
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UserAccount u where u.userId = :userId")
    Optional<UserAccount> findByUserIdWithPessimisticLock(@Param("userId") Long userId);
}

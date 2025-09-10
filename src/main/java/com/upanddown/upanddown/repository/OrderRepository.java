package com.upanddown.upanddown.repository;

import com.upanddown.upanddown.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    /**
     * 비관적 락(PESSIMISTIC_WRITE)을 적용하여 ID로 주문을 조회하는 메서드.
     * 동일한 주문에 대한 동시 접근을 막기 위해 사용됩니다.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdWithPessimisticLock(Long id);
}

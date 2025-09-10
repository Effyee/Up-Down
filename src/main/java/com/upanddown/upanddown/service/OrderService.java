package com.upanddown.upanddown.service;

import com.upanddown.upanddown.domain.Order;
import com.upanddown.upanddown.domain.UserAccount;
import com.upanddown.upanddown.domain.UserPortfolio;
import com.upanddown.upanddown.dto.OrderRequestDto;
import com.upanddown.upanddown.repository.OrderRepository;
import com.upanddown.upanddown.repository.UserAccountRepository;
import com.upanddown.upanddown.repository.UserPortfolioRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserPortfolioRepository userPortfolioRepository;

    @Transactional
    public void placeOrder_NoLock(OrderRequestDto request) {
        Order sellOrder = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("판매 주문을 찾을 수 없습니다."));

        validateOrder(sellOrder, request);

        UserAccount buyerAccount = userAccountRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new NoSuchElementException("구매자 계좌를 찾을 수 없습니다."));
        UserPortfolio buyerPortfolio = userPortfolioRepository.findByUserIdAndTicker(request.getUserId(), sellOrder.getTicker())
                .orElseGet(() -> UserPortfolio.builder()
                        .userId(request.getUserId())
                        .ticker(sellOrder.getTicker())
                        .quantity(0L)
                        .build());

        BigDecimal transactionAmount = request.getPrice().multiply(new BigDecimal(request.getRequestedQuantity()));

        // 구매자 계좌, 포트폴리오 업데이트
        buyerAccount.setBalance(buyerAccount.getBalance().subtract(transactionAmount));
        buyerPortfolio.setQuantity(buyerPortfolio.getQuantity() + request.getRequestedQuantity());

        // 판매 주문 수량 업데이트
        sellOrder.decreaseQuantity(request.getRequestedQuantity());

        // No-Lock 방식에서는 수동으로 저장해야 합니다.
        userAccountRepository.save(buyerAccount);
        userPortfolioRepository.save(buyerPortfolio);
        orderRepository.save(sellOrder);
    }

    @Transactional
    public void placeOrder_PessimisticLock(OrderRequestDto request) {
        Order sellOrder = orderRepository.findByIdWithPessimisticLock(request.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("판매 주문을 찾을 수 없습니다."));

        validateOrder(sellOrder, request);

        UserAccount buyerAccount = userAccountRepository.findByUserIdWithPessimisticLock(request.getUserId())
                .orElseThrow(() -> new NoSuchElementException("구매자 계좌를 찾을 수 없습니다."));
        UserPortfolio buyerPortfolio = userPortfolioRepository.findByUserIdAndTickerWithPessimisticLock(request.getUserId(), sellOrder.getTicker())
                .orElseGet(() -> UserPortfolio.builder()
                        .userId(request.getUserId())
                        .ticker(sellOrder.getTicker())
                        .quantity(0L)
                        .build());

        BigDecimal transactionAmount = request.getPrice().multiply(new BigDecimal(request.getRequestedQuantity()));
        buyerAccount.setBalance(buyerAccount.getBalance().subtract(transactionAmount));
        buyerPortfolio.setQuantity(buyerPortfolio.getQuantity() + request.getRequestedQuantity());

        sellOrder.decreaseQuantity(request.getRequestedQuantity());
    }

    @Transactional
    @Retryable(
            value = {ObjectOptimisticLockingFailureException.class, OptimisticLockException.class},
            maxAttempts = 10,
            backoff = @Backoff(delay = 25)
    )
    public void placeOrder_OptimisticLockWithRetry(OrderRequestDto request) {
        System.out.println("재시도 시도! userId=" + request.getUserId() + ", orderId=" + request.getOrderId());
        placeOrder_OptimisticLock(request);
    }


    public void placeOrder_OptimisticLock(OrderRequestDto request) {
        Order sellOrder = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NoSuchElementException("판매 주문을 찾을 수 없습니다."));

        validateOrder(sellOrder, request);

        UserAccount buyerAccount = userAccountRepository.findByUserId(request.getUserId())
                .orElseThrow(() -> new NoSuchElementException("구매자 계좌를 찾을 수 없습니다."));
        UserPortfolio buyerPortfolio = userPortfolioRepository.findByUserIdAndTicker(request.getUserId(), sellOrder.getTicker())
                .orElseGet(() -> UserPortfolio.builder()
                        .userId(request.getUserId())
                        .ticker(sellOrder.getTicker())
                        .quantity(0L)
                        .build());

        BigDecimal transactionAmount = request.getPrice().multiply(new BigDecimal(request.getRequestedQuantity()));
        buyerAccount.setBalance(buyerAccount.getBalance().subtract(transactionAmount));
        buyerPortfolio.setQuantity(buyerPortfolio.getQuantity() + request.getRequestedQuantity());
        sellOrder.decreaseQuantity(request.getRequestedQuantity());
    }

    @Recover
    public void recover(ObjectOptimisticLockingFailureException e, OrderRequestDto request) {
        System.err.println("주문 처리 실패: " + request.getUserId() + "의 주문이 5회 재시도 후에도 실패했습니다.");
        throw e;
    }

    /**
     * 판매 주문 등록 로직
     * @param request 판매 주문 정보
     * @return 등록된 주문 ID
     */
    @Transactional
    public Long createSellOrder(OrderRequestDto request) {
        Order sellOrder = Order.builder()
                .userId(request.getUserId())
                .ticker(request.getTicker())
                .orderType("SELL")
                .price(request.getPrice())
                .quantity(request.getRequestedQuantity())
                .build();
        Order savedOrder = orderRepository.save(sellOrder);
        return savedOrder.getId();
    }

    private void validateOrder(Order sellOrder, OrderRequestDto request) {
        if (!sellOrder.getTicker().equals("005930")) {
            throw new IllegalArgumentException("올바른 종목 코드가 아닙니다.");
        }
        if (sellOrder.getQuantity() < request.getRequestedQuantity()) {
            throw new IllegalArgumentException("매물 수량이 부족합니다.");
        }
    }
}
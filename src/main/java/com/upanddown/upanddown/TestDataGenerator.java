package com.upanddown.upanddown;

import com.github.javafaker.Faker;
import com.upanddown.upanddown.domain.Order;
import com.upanddown.upanddown.domain.UserAccount;
import com.upanddown.upanddown.domain.UserPortfolio;
import com.upanddown.upanddown.repository.OrderRepository;
import com.upanddown.upanddown.repository.UserAccountRepository;
import com.upanddown.upanddown.repository.UserPortfolioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class TestDataGenerator implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final UserPortfolioRepository userPortfolioRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("데이터베이스 스키마가 자동으로 초기화됩니다. 데이터 생성 시작...");

        Faker faker = new Faker();

        // 1. "판매자X"의 매도 주문 생성 (Order Book)
        Order sellOrder = Order.builder()
                .userId(9999L)
                .ticker("005930")
                .orderType("SELL")
                .price(BigDecimal.valueOf(80000))
                .quantity(1000L)
                .build();
        orderRepository.save(sellOrder);
        System.out.println("매도 주문 생성 완료. 주문 ID: " + sellOrder.getId());

        // 2. 100명의 가상 구매자 계정 및 포트폴리오 생성
        // 각 계정은 초기 예치금 1,000,000원과 보유 주식 0으로 시작합니다.
        IntStream.rangeClosed(1, 100).forEach(i -> {
            UserAccount account = UserAccount.builder()
                    .userId((long) i)
                    .balance(BigDecimal.valueOf(1000000))
                    .build();
            userAccountRepository.save(account);

            UserPortfolio portfolio = UserPortfolio.builder()
                    .userId((long) i)
                    .ticker("005930")
                    .quantity(0L)
                    .build();
            userPortfolioRepository.save(portfolio);
        });

        System.out.println("100명의 가상 사용자 계정 및 포트폴리오 생성 완료.");
    }
}

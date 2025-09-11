package com.upanddown.upanddown;

import com.github.javafaker.Faker;
import com.upanddown.upanddown.domain.*;
import com.upanddown.upanddown.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class TestDataGenerator implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final OrderRepository orderRepository;
    private final UserPortfolioRepository userPortfolioRepository;
    private final StockRepository stockRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("데이터베이스 스키마가 자동으로 초기화됩니다. 데이터 생성 시작...");

        Faker faker = new Faker();

        // 1. 테스트용 주식 정보 생성
        Stock stock = Stock.builder()
                .ticker("005930")
                .name("삼성전자")
                .currentPrice(80000.0)
                .openPrice(80000.0)
                .highPrice(80000.0)
                .lowPrice(80000.0)
                .volume(0L)
                .build();

        stockRepository.save(stock);

        System.out.println("테스트용 주식('삼성전자') 생성 완료.");
        // 2. "판매자:9999"의 매도 주문 생성 (Order Book)
        Order sellOrder = Order.builder()
                .userId(9999L)
                .ticker("005930")
                .orderType("SELL")
                .price(BigDecimal.valueOf(80000))
                .quantity(1000L)
                .build();
        orderRepository.save(sellOrder);
        System.out.println("매도 주문 생성 완료. 주문 ID: " + sellOrder.getId());

        // 3. 100명의 가상 구매자, 계정, 포트폴리오 일괄 생성
        List<User> usersToSave = new ArrayList<>();
        for (int i = 1; i <= 10000; i++) {
            usersToSave.add(User.builder()
                    .username("testuser" + i)
                    .password("password123") // 실제 앱에서는 반드시 인코딩해야 합니다.
                    .email("testuser" + i + "@example.com")
                    .build());
        }
        List<User> savedUsers = userRepository.saveAll(usersToSave);
        System.out.println("10000명의 가상 사용자 생성 완료.");

        List<UserAccount> accountsToSave = new ArrayList<>();
        List<UserPortfolio> portfoliosToSave = new ArrayList<>();

        savedUsers.forEach(user -> {
            // 각 계정은 초기 예치금 1억원으로 시작
            UserAccount account = UserAccount.builder()
                    .userId(user.getId())
                    .balance(new BigDecimal("100000000"))
                    .build();
            accountsToSave.add(account);

            // 각 사용자는 '삼성전자' 주식을 0개 가진 포트폴리오로 시작
            UserPortfolio portfolio = UserPortfolio.builder()
                    .userId(user.getId())
                    .ticker("005930")
                    .quantity(0L)
                    .build();
            portfoliosToSave.add(portfolio);
        });

        userAccountRepository.saveAll(accountsToSave);
        userPortfolioRepository.saveAll(portfoliosToSave);

        System.out.println("10000명의 가상 사용자 계정 및 포트폴리오 생성 완료.");
        System.out.println("테스트 데이터 생성이 최종 완료되었습니다.");
    }
}
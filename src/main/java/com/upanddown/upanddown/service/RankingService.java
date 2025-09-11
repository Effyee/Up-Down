package com.upanddown.upanddown.service;

import com.upanddown.upanddown.domain.*;
import com.upanddown.upanddown.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RankingService {

    private final UserRepository userRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserPortfolioRepository userPortfolioRepository;
    private final StockRepository stockRepository; // Stock 엔티티와 Repository가 있다고 가정합니다.
    private final RankingRepository rankingRepository;

    // 매일 0시, 12시에 실행 (12시간 간격)
    @Scheduled(cron = "0 0 0,12 * * *")
    @Transactional
    public void updateUserRankings() {
        // 0. 필요한 데이터 미리 로드 (성능 최적화)
        // 0-1. 모든 주식의 현재가
        Map<String, Double> currentStockPrices = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getTicker, Stock::getCurrentPrice));

        // 0-2. 모든 유저의 포트폴리오
        Map<Long, List<UserPortfolio>> userPortfoliosMap = userPortfolioRepository.findAll().stream()
                .collect(Collectors.groupingBy(UserPortfolio::getUserId));

        // 0-3. 모든 유저의 계좌
        Map<Long, UserAccount> userAccountsMap = userAccountRepository.findAll().stream()
                .collect(Collectors.toMap(UserAccount::getUserId, ua -> ua));

        // 0-4. 가장 최근 랭킹 정보 (기준 자산 계산용)
        // 주의: 유저가 매우 많을 경우 이 부분도 성능 튜닝이 필요할 수 있습니다.
        Map<Long, Ranking> previousRankingsMap = rankingRepository.findLatestRankingsGroupedByUser()
                .stream().collect(Collectors.toMap(Ranking::getUserId, r -> r));

        // 1. 모든 유저 정보 조회
        List<User> allUsers = userRepository.findAll();
        List<Ranking> newRankings = new ArrayList<>();

        // 2. 각 유저별로 수익률 계산
        for (User user : allUsers) {
            // 2-1. 현재 총 자산 계산
            // [수정된 부분] userAccountsMap에서 직접 조회하고, 계좌가 없는 경우 잔액을 0으로 처리합니다.
            // 이렇게 하면 존재하지 않는 생성자를 호출하는 오류를 피하고 코드가 더 명확해집니다.
            UserAccount account = userAccountsMap.get(user.getId());
            BigDecimal balance = (account != null) ? account.getBalance() : BigDecimal.ZERO;

            List<UserPortfolio> portfolios = userPortfoliosMap.getOrDefault(user.getId(), new ArrayList<>());

            BigDecimal stockAssets = portfolios.stream()
                    .map(p -> {
                        double currentPrice = currentStockPrices.getOrDefault(p.getTicker(), 0.0);
                        return BigDecimal.valueOf(currentPrice).multiply(BigDecimal.valueOf(p.getQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal currentTotalAssets = balance.add(stockAssets);

            // 2-2. 수익률 계산
            Ranking previousRanking = previousRankingsMap.get(user.getId());
            // 최초 기록 시점에는 기준 자산을 현재 자산으로 설정 (수익률 0%)
            BigDecimal baseAssets = (previousRanking != null) ? previousRanking.getTotalAssets() : currentTotalAssets;

            double profitRate = 0.0;
            if (baseAssets.compareTo(BigDecimal.ZERO) > 0) {
                // 수익률 = (현재 총자산 - 기준 총자산) / 기준 총자산 * 100
                profitRate = currentTotalAssets.subtract(baseAssets)
                        .divide(baseAssets, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .doubleValue();
            }

            newRankings.add(Ranking.builder()
                    .userId(user.getId())
                    .totalAssets(currentTotalAssets)
                    .profitRate(profitRate)
                    .build());
        }

        // 3. 수익률 기준으로 정렬
        newRankings.sort(Comparator.comparing(Ranking::getProfitRate).reversed());

        // 4. 순위 부여 및 저장
        // 참고: 현재 로직은 랭킹 데이터를 계속 추가만 합니다.
        // 운영 시에는 오래된 랭킹 데이터를 삭제하는 로직도 고려하는 것이 좋습니다.
        int rank = 1;
        for (Ranking ranking : newRankings) {
            ranking.setCurrentRank(rank++);
        }

        rankingRepository.saveAll(newRankings);
    }
}

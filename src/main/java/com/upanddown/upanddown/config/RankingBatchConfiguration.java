package com.upanddown.upanddown.config;

import com.upanddown.upanddown.domain.Ranking;
import com.upanddown.upanddown.domain.Stock;
import com.upanddown.upanddown.domain.User;
import com.upanddown.upanddown.domain.UserAccount;
import com.upanddown.upanddown.domain.UserPortfolio;
import com.upanddown.upanddown.repository.RankingRepository;
import com.upanddown.upanddown.repository.StockRepository;
import com.upanddown.upanddown.repository.UserAccountRepository;
import com.upanddown.upanddown.repository.UserPortfolioRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope; // [추가]
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger; // [추가]
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RankingBatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final UserAccountRepository userAccountRepository; // (Processor에서 제거되었지만, 혹시 모를 다른 용도를 위해 유지)
    private final UserPortfolioRepository userPortfolioRepository; // (Processor에서 제거되었지만, 혹시 모를 다른 용도를 위해 유지)
    private final RankingRepository rankingRepository; // (Tasklet에서 제거되었지만, Step 2에서 사용)
    private final StockRepository stockRepository;

    private static final int CHUNK_SIZE = 100;
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("10000000");

    // =========================================================================
    // == Job 설정
    // =========================================================================
    // == Job 설정
    // =========================================================================
    @Bean
    public Job rankingJob(JobExecutionListener jobCompletionNotificationListener) {
        return new JobBuilder("rankingJob", jobRepository)
                .listener(jobCompletionNotificationListener)
                .start(calculateAssetsStep()) // Step 1: 자산 계산
                .next(applyRanksStep())       // Step 2: 순위 부여
                .build();
    }

    // =========================================================================
    // == Step 1: 자산 계산 (N+1 문제 해결)
    // =========================================================================
    @Bean
    public Step calculateAssetsStep() {
        return new StepBuilder("calculateAssetsStep", jobRepository)
                .<User, Ranking>chunk(CHUNK_SIZE, transactionManager)
                .reader(userItemReader())
                .processor(rankingItemProcessor())
                .writer(rankingItemWriter()) // Step 1용 Writer
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> userItemReader() {
        String jpqlQuery = "SELECT DISTINCT u FROM User u " +
                "JOIN FETCH u.userAccount ua " +
                "LEFT JOIN FETCH u.portfolios p " +
                "ORDER BY u.id ASC";

        return new JpaPagingItemReaderBuilder<User>()
                .name("userItemReader") // [정상] Reader는 상태 저장을 위해 name이 필수
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString(jpqlQuery)
                .build();
    }

    @Bean
    public ItemProcessor<User, Ranking> rankingItemProcessor() {
        // ... (Processor 코드는 이전과 동일) ...
        Map<String, BigDecimal> stockPrices = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getTicker, stock -> BigDecimal.valueOf(stock.getCurrentPrice())));

        return user -> {
            UserAccount userAccount = user.getUserAccount();
            BigDecimal balance = userAccount != null ? userAccount.getBalance() : BigDecimal.ZERO;
            List<UserPortfolio> portfolios = user.getPortfolios();

            BigDecimal stockAssets = portfolios.stream()
                    .map(p -> {
                        BigDecimal currentPrice = stockPrices.getOrDefault(p.getTicker(), BigDecimal.ZERO);
                        return currentPrice.multiply(BigDecimal.valueOf(p.getQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalAssets = balance.add(stockAssets);
            double profitRate = totalAssets.subtract(INITIAL_CAPITAL)
                    .divide(INITIAL_CAPITAL, 4, RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .doubleValue();

            return Ranking.builder()
                    .userId(user.getId())
                    .totalAssets(totalAssets)
                    .profitRate(profitRate)
                    .build();
        };
    }


    @Bean
    public JpaItemWriter<Ranking> rankingItemWriter() {
        return new JpaItemWriterBuilder<Ranking>()
                // [★수정★] .name() 메서드 제거
                .entityManagerFactory(entityManagerFactory)
                .build();
    }


    // =========================================================================
    // == Step 2: 순위 부여 (OOM 문제 해결)
    // =========================================================================
    @Bean
    public Step applyRanksStep() {
        return new StepBuilder("applyRanksStep", jobRepository)
                .<Ranking, Ranking>chunk(CHUNK_SIZE, transactionManager)
                .reader(rankingItemReaderStep2())
                .processor(rankingItemProcessorStep2())
                .writer(rankingItemWriterStep2()) // Step 2용 Writer
                .build();
    }

    @Bean
    public JpaPagingItemReader<Ranking> rankingItemReaderStep2() {
        String jpqlQuery = "SELECT r FROM Ranking r ORDER BY r.totalAssets DESC";

        return new JpaPagingItemReaderBuilder<Ranking>()
                .name("rankingItemReaderStep2") // [정상] Reader는 상태 저장을 위해 name이 필수
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString(jpqlQuery)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Ranking, Ranking> rankingItemProcessorStep2() {
        AtomicInteger rankCounter = new AtomicInteger(1);

        return ranking -> {
            ranking.setCurrentRank(rankCounter.getAndIncrement());
            return ranking;
        };
    }

    @Bean
    public JpaItemWriter<Ranking> rankingItemWriterStep2() {
        return new JpaItemWriterBuilder<Ranking>()
                // [★수정★] .name() 메서드 제거
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    // =========================================================================
    // == Job 리스너 (실행 시간 로깅)
    // =========================================================================
    @Bean
    public JobExecutionListener jobCompletionNotificationListener() {
        // ... (리스너 코드는 이전과 동일) ...
        return new JobExecutionListener() {
            private long startTime;

            @Override
            public void beforeJob(@NonNull JobExecution jobExecution) {
                startTime = System.currentTimeMillis();
                log.info("========================================================================");
                log.info(">>>>>>>>>> [Ranking Job] BATCH JOB STARTED >>>>>>>>>>");
                log.info("========================================================================");
            }

            @Override
            public void afterJob(@NonNull JobExecution jobExecution) {
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                long minutes = (duration / 1000) / 60;
                long seconds = (duration / 1000) % 60;
                long millis = duration % 1000;
                String formattedDuration = String.format("%d분 %d초 %dms", minutes, seconds, millis);

                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    log.info("<<<<<<<<<< [Ranking Job] BATCH JOB FINISHED SUCCESSFULLY <<<<<<<<<<");
                } else {
                    log.warn("<<<<<<<<<< [Ranking Job] BATCH JOB FINISHED WITH STATUS: {} <<<<<<<<<<", jobExecution.getStatus());
                }
                log.info("========================================================================");
                log.info("  Total Execution Time: {} ms ({})", duration, formattedDuration);
                log.info("========================================================================");
            }
        };
    }
}
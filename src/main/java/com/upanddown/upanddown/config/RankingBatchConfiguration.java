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
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RankingBatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final UserAccountRepository userAccountRepository;
    private final UserPortfolioRepository userPortfolioRepository;
    private final RankingRepository rankingRepository;
    private final StockRepository stockRepository;

    private static final int CHUNK_SIZE = 100;
    private static final BigDecimal INITIAL_CAPITAL = new BigDecimal("10000000");

    @Bean
    public Job rankingJob(JobExecutionListener jobCompletionNotificationListener) { // [수정] JobExecutionListener를 파라미터로 주입받습니다.
        return new JobBuilder("rankingJob", jobRepository)
                .listener(jobCompletionNotificationListener) // [수정] 주입받은 리스너를 사용합니다.
                .start(calculateAssetsStep())
                .next(applyRanksStep())
                .build();
    }

    // (이하 다른 Bean 설정들은 변경 없음)
    @Bean
    public Step calculateAssetsStep() {
        return new StepBuilder("calculateAssetsStep", jobRepository)
                .<User, Ranking>chunk(CHUNK_SIZE, transactionManager)
                .reader(userItemReader())
                .processor(rankingItemProcessor())
                .writer(rankingItemWriter())
                .build();
    }

    @Bean
    public Step applyRanksStep() {
        return new StepBuilder("applyRanksStep", jobRepository)
                .tasklet(applyRankTasklet(), transactionManager)
                .build();
    }

    @Bean
    public JpaPagingItemReader<User> userItemReader() {
        return new JpaPagingItemReaderBuilder<User>()
                .name("userItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString("SELECT u FROM User u ORDER BY u.id ASC")
                .build();
    }

    @Bean
    public ItemProcessor<User, Ranking> rankingItemProcessor() {
        Map<String, BigDecimal> stockPrices = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getTicker, stock -> BigDecimal.valueOf(stock.getCurrentPrice())));

        return user -> {
            BigDecimal balance = userAccountRepository.findByUserId(user.getId())
                    .map(UserAccount::getBalance)
                    .orElse(BigDecimal.ZERO);

            List<UserPortfolio> portfolios = userPortfolioRepository.findAllByUserId(user.getId());
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
                .entityManagerFactory(entityManagerFactory)
                .build();
    }

    @Bean
    public Tasklet applyRankTasklet() {
        return (contribution, chunkContext) -> {
            log.info(">>>>> [Step 2] Ranking 순위 부여 Tasklet 시작");
            List<Ranking> rankings = rankingRepository.findAllByOrderByTotalAssetsDesc();
            int rank = 1;
            for (Ranking ranking : rankings) {
                ranking.setCurrentRank(rank++);
            }
            rankingRepository.saveAll(rankings);
            log.info("<<<<< [Step 2] Ranking 순위 부여 Tasklet 종료");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public JobExecutionListener jobCompletionNotificationListener() {
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


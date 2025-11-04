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
        // User를 조회할 때 userAccount와 portfolios를 미리가져옴
        String jpqlQuery = "SELECT DISTINCT u FROM User u " +
                "JOIN FETCH u.userAccount ua " +
                "LEFT JOIN FETCH u.portfolios p " + // User 1명당 Portfolio가 여러 개일 수 있으니 LEFT JOIN
                "ORDER BY u.id ASC";

        return new JpaPagingItemReaderBuilder<User>()
                .name("userItemReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(CHUNK_SIZE)
                .queryString(jpqlQuery) // 3개 테이블을 조인
                .build();
    }

    @Bean
    public ItemProcessor<User, Ranking> rankingItemProcessor() {
        Map<String, BigDecimal> stockPrices = stockRepository.findAll().stream()
                .collect(Collectors.toMap(Stock::getTicker, stock -> BigDecimal.valueOf(stock.getCurrentPrice())));

        return user -> {
            //1. User에 연관된 UserAccount 바로 참조
            UserAccount userAccount = user.getUserAccount();
            BigDecimal balance = userAccount != null ? userAccount.getBalance() : BigDecimal.ZERO;

            //2. User에 연관된 Portfolio 컬렉션 바로 참조
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


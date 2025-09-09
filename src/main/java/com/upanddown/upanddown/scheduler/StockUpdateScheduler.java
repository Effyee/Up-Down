package com.upanddown.upanddown.scheduler;

import com.upanddown.upanddown.service.StockDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StockUpdateScheduler {

    private final StockDataService stockDataService;

    // 한국 주식 시장 시간(월-금, 09:00-16:59)에 15분 간격으로 실행
    @Scheduled(cron = "0 */15 9-16 * * MON-FRI", zone = "Asia/Seoul")
    public void scheduleStockUpdates() {
        System.out.println("주기적인 주가 업데이트를 시작합니다...");
        stockDataService.updateStockPrices();
        System.out.println("주가 업데이트 완료.");
    }
}


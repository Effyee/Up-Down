package com.upanddown.upanddown.controller;

import com.upanddown.upanddown.service.StockDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발 및 테스트 목적으로 수동 트리거를 제공하는 컨트롤러입니다.
 * 운영 환경에서는 프로필(Profile)을 분리하여 비활성화하는 것이 좋습니다.
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final StockDataService stockDataService;

    /**
     * 이 엔드포인트에 접속하면 즉시 주가 업데이트 로직을 실행합니다.
     * @return 작업 시작을 알리는 간단한 메시지
     */
    @GetMapping("/update-stocks")
    public ResponseEntity<String> manualUpdateStocks() {
        System.out.println("수동 주가 업데이트 요청을 받았습니다...");

        // 비동기 실행을 위해 새 스레드에서 서비스 호출 (선택 사항이지만 권장)
        // 이렇게 하면 웹 요청이 즉시 응답하고, 데이터 업데이트는 백그라운드에서 진행됩니다.
        new Thread(() -> stockDataService.updateStockPrices()).start();

        return ResponseEntity.ok("Stock price update process has been triggered successfully in the background.");
    }
}


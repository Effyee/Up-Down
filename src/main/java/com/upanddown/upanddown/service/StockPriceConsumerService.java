package com.upanddown.upanddown.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.upanddown.upanddown.config.RabbitMQConfig;
import com.upanddown.upanddown.domain.Stock;
import com.upanddown.upanddown.dto.RealtimeStockPriceDto;
import com.upanddown.upanddown.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockPriceConsumerService {

    private final StockRepository stockRepository;
    private final ObjectMapper objectMapper;

    /**
     * 'stock_price_update_queue'를 구독(리스닝)하여 실시간 주가 메시지를 처리합니다.
     * @param message RabbitMQ로부터 받은 JSON 형태의 메시지 문자열
     */
    @RabbitListener(queues = RabbitMQConfig.REALTIME_QUEUE_NAME) // 설정 파일(RabbitMQConfig)에 정의된 정확한 큐 이름을 사용합니다.
    @Transactional // DB 업데이트 작업을 하나의 트랜잭션으로 묶어 안전하게 처리합니다.
    public void receiveRealtimeMessage(String message) {
        try {
            // 1. 받은 JSON 메시지를 DTO로 변환합니다.
            RealtimeStockPriceDto dto = objectMapper.readValue(message, RealtimeStockPriceDto.class);

            String ticker = dto.getTicker();
            Double price = dto.getPrice();

            if (ticker == null || price == null) {
                log.warn("Received invalid message format: {}", message);
                return;
            }

            log.info("RECEIVED - Ticker: {}, Price: {}", ticker, price);

            // 2. DB에서 해당 주식을 찾아 가격을 업데이트합니다.
            stockRepository.findByTicker(ticker)
                    .ifPresentOrElse(
                            stock -> { // Ticker를 찾았을 때 실행
                                log.info("UPDATING DB for Ticker: {}", ticker);
                                stock.updateCurrentPrice(price);
                                                            },
                            () -> { // Ticker를 찾지 못했을 때 실행
                                log.warn("Ticker '{}' not found in DB. Ignoring message.", ticker);
                            }
                    );

        } catch (Exception e) {
            log.error("Failed to process message: {}", message, e);
        }
    }
}
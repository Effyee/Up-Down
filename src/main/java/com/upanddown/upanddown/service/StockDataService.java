package com.upanddown.upanddown.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upanddown.upanddown.domain.Stock;
import com.upanddown.upanddown.dto.StockPriceDto;
import com.upanddown.upanddown.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // final 필드를 위한 생성자
public class StockDataService {

    private static final Logger log = LoggerFactory.getLogger(StockDataService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StockRepository stockRepository; // DB와 대화할 Repository를 주입

    @Transactional // 이 메소드 전체를 하나의 트랜잭션으로 묶어 데이터 정합성을 보장
    public void updateStockPrices() {
        log.info("updateStockPrices() called - Starting stock data fetch process...");

        // TODO: 나중에는 DB의 stocks 테이블에서 모든 티커를 조회하도록 수정
        List<String> tickers = List.of("005930.KS", "AAPL", "MSFT", "GOOGL");

        try {
            // Python 스크립트 실행 로직 (이전과 동일)
            String scriptPath = "scripts/get_stock_data.py";
            List<String> command;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                String fullPythonCommand = "python " + scriptPath + " " + String.join(" ", tickers);
                command = List.of("cmd.exe", "/c", fullPythonCommand);
            } else {
                command = new java.util.ArrayList<>();
                command.add("python3");
                command.add(scriptPath);
                command.addAll(tickers);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new File("."));
            Process process = processBuilder.start();

            String output = new BufferedReader(new InputStreamReader(process.getInputStream())).lines().collect(Collectors.joining("\n"));
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorOutput = new BufferedReader(new InputStreamReader(process.getErrorStream())).lines().collect(Collectors.joining("\n"));
                log.error("Python script exited with code: {} and error: {}", exitCode, errorOutput);
                return;
            }

            log.info("Successfully fetched stock data JSON from Python script.");
            List<StockPriceDto> stockPrices = objectMapper.readValue(output, new TypeReference<>() {});


            for (StockPriceDto priceDto : stockPrices) {
                // DB에서 해당 티커의 주식이 있는지 찾아봅니다.
                stockRepository.findByTicker(priceDto.getTicker())
                        .ifPresentOrElse(
                                // 1. 주식이 DB에 이미 존재할 경우:
                                stock -> {
                                    log.info("Updating existing stock: {}", stock.getTicker());
                                    stock.updatePrice(priceDto); // Entity의 업데이트 메소드를 호출하여 값 변경
                                    // @Transactional 어노테이션 덕분에, 메소드가 끝나면 변경된 내용을 자동으로 DB에 반영(UPDATE)해 줍니다.
                                },
                                // 2. 주식이 DB에 없을 경우
                                () -> {
                                    log.info("Creating new stock: {}", priceDto.getTicker());
                                    Stock newStock = Stock.builder() // Builder 패턴으로 새 Entity 생성
                                            .ticker(priceDto.getTicker())
                                            .name(priceDto.getTicker()) // TODO: 종목 이름은 별도로 가져와야 함
                                            .currentPrice(priceDto.getClosePrice())
                                            .openPrice(priceDto.getOpenPrice())
                                            .highPrice(priceDto.getHighPrice())
                                            .lowPrice(priceDto.getLowPrice())
                                            .volume(priceDto.getVolume())
                                            .build();
                                    stockRepository.save(newStock); // 새 Entity를 DB에 저장 (INSERT)
                                }
                        );
            }
            log.info("DB stock price update process finished successfully.");

        } catch (Exception e) {
            log.error("An error occurred during stock price update process", e);
        }
    }

    /**
     * RabbitMQ에서 수신한 주가 리스트를 DB에 반영하는 메서드
     */
    @Transactional
    public void updateStockPricesFromQueue(List<StockPriceDto> stockPrices) {
        for (StockPriceDto priceDto : stockPrices) {
            stockRepository.findByTicker(priceDto.getTicker())
                    .ifPresentOrElse(
                            stock -> {
                                stock.updatePrice(priceDto);
                            },
                            () -> {
                                Stock newStock = Stock.builder()
                                        .ticker(priceDto.getTicker())
                                        .name(priceDto.getTicker())
                                        .currentPrice(priceDto.getClosePrice())
                                        .openPrice(priceDto.getOpenPrice())
                                        .highPrice(priceDto.getHighPrice())
                                        .lowPrice(priceDto.getLowPrice())
                                        .volume(priceDto.getVolume())
                                        .build();
                                stockRepository.save(newStock);
                            }
                    );
        }
    }
}
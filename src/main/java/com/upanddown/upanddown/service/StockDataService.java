package com.upanddown.upanddown.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upanddown.upanddown.dto.StockPriceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockDataService {

    private static final Logger log = LoggerFactory.getLogger(StockDataService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void updateStockPrices() {
        log.info("updateStockPrices() called - Starting stock data fetch process...");

        List<String> tickers = List.of("005930.KS", "AAPL", "MSFT", "GOOGL");

        try {
            String scriptPath = "scripts/get_stock_data.py";
            List<String> command;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows 환경: cmd.exe 사용
                String tickersAsString = String.join(" ", tickers);
                String fullPythonCommand = "python " + scriptPath + " " + tickersAsString;
                command = List.of("cmd.exe", "/c", fullPythonCommand);
            } else {
                // Linux/Unix 환경: python3 직접 실행
                command = new ArrayList<>();
                command.add("python3"); // 또는 "python" (컨테이너에 따라 다름)
                command.add(scriptPath);
                command.addAll(tickers);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(new java.io.File("."));

            Process process = processBuilder.start();
            String output = new BufferedReader(new InputStreamReader(process.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String errorOutput = new BufferedReader(new InputStreamReader(process.getErrorStream()))
                        .lines().collect(Collectors.joining("\n"));
                log.error("Python script exited with code: {} and error: {}", exitCode, errorOutput);
                return;
            }

            log.info("Successfully fetched stock data JSON from Python script.");
            List<StockPriceDto> stockPrices = objectMapper.readValue(output, new TypeReference<>() {});
            for (StockPriceDto price : stockPrices) {
                log.info("Ticker: {}, Close Price: {}", price.getTicker(), price.getClosePrice());
                // TODO: DB 업데이트 로직
            }
        } catch (Exception e) {
            log.error("An error occurred during stock price update process", e);
        }
    }
}

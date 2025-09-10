package com.upanddown.upanddown.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upanddown.upanddown.dto.StockPriceDto;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class StockPriceService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<StockPriceDto> getStockPrices(List<String> tickers) {
        List<StockPriceDto> result = new ArrayList<>();
        try {
            List<String> command = new ArrayList<>();
            command.add("python");
            command.add("scripts/get_stock_data.py");
            command.addAll(tickers);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python script failed with exit code " + exitCode);
            }
            // JSON 파싱
            result = objectMapper.readValue(output.toString(), new TypeReference<List<StockPriceDto>>() {});
        } catch (Exception e) {
            throw new RuntimeException("Failed to get stock prices: " + e.getMessage(), e);
        }
        return result;
    }
}


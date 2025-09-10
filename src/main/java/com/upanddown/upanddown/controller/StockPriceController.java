package com.upanddown.upanddown.controller;

import com.upanddown.upanddown.dto.StockPriceDto;
import com.upanddown.upanddown.service.StockPriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-prices")
public class StockPriceController {
    private final StockPriceService stockPriceService;

    @Autowired
    public StockPriceController(StockPriceService stockPriceService) {
        this.stockPriceService = stockPriceService;
    }

    @PostMapping
    public List<StockPriceDto> getStockPrices(@RequestBody List<String> tickers) {
        return stockPriceService.getStockPrices(tickers);
    }
}


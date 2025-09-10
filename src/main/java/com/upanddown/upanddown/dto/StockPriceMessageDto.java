package com.upanddown.upanddown.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class StockPriceMessageDto {
    private String ticker;
    private Double price;
    private Long timestamp;
}


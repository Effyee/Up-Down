package com.upanddown.upanddown.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RabbitMQ로부터 수신하는 실시간 주가 메시지를 위한 DTO
 */
@Data
@NoArgsConstructor
public class RealtimeStockPriceDto {
    private String ticker;
    private Double price;
    private Double timestamp;
}

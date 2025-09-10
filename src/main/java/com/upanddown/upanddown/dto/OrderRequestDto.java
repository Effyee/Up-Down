package com.upanddown.upanddown.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OrderRequestDto {
    private Long userId; // 주문자 ID
    private Long orderId; // 경쟁 대상이 되는 기존 주문(판매자X)의 ID
    private Long requestedQuantity; // 구매 요청 수량
    private BigDecimal price; // 구매 가격
    private String ticker; // 주식 티커 심볼
}
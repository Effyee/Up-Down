package com.upanddown.upanddown.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "bidding_orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 주문자
    private String ticker; // 종목 코드
    private String orderType; // 'BUY' or 'SELL'
    private BigDecimal price; // 주문 가격
    private Long quantity; // 주문 수량

    @Version
    private Long version;

    @Builder
    public Order(Long userId, String ticker, String orderType, BigDecimal price, Long quantity) {
        this.userId = userId;
        this.ticker = ticker;
        this.orderType = orderType;
        this.price = price;
        this.quantity = quantity;
    }

    /**
     * 주문 수량 차감
     * @param requestedQuantity 구매 요청 수량
     */
    public void decreaseQuantity(Long requestedQuantity) {
        if (this.quantity < requestedQuantity) {
            throw new IllegalArgumentException("주문 가능한 수량이 부족합니다.");
        }
        this.quantity -= requestedQuantity;
    }
}
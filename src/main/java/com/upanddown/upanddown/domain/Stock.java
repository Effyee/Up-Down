package com.upanddown.upanddown.domain;

import com.upanddown.upanddown.dto.StockPriceDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "stocks")
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String ticker; // 종목 코드

    private String name; // 종목명

    private Double currentPrice; // 현재가

    private Double openPrice; // 시가

    private Double highPrice; // 고가

    private Double lowPrice; // 저가

    private Long volume; // 거래량

    @UpdateTimestamp // 데이터가 업데이트될 때마다 자동으로 시간이 기록
    private LocalDateTime updatedAt;

    @Builder
    public Stock(String ticker, String name, Double currentPrice, Double openPrice, Double highPrice, Double lowPrice, Long volume) {
        this.ticker = ticker;
        this.name = name;
        this.currentPrice = currentPrice;
        this.openPrice = openPrice;
        this.highPrice = highPrice;
        this.lowPrice = lowPrice;
        this.volume = volume;
    }

    // 주가 정보를 업데이트 메소드
    public void updatePrice(StockPriceDto priceDto) {
        this.currentPrice = priceDto.getClosePrice();
        this.openPrice = priceDto.getOpenPrice();
        this.highPrice = priceDto.getHighPrice();
        this.lowPrice = priceDto.getLowPrice();
        this.volume = priceDto.getVolume();
    }

    public void updateCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }



}

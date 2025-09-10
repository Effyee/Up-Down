package com.upanddown.upanddown.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

// JSON 데이터를 Java 객체로 변환하기 위한 DTO
@Data
@NoArgsConstructor
public class StockPriceDto {

    private String ticker;

    @JsonProperty("closePrice") // JSON의 키 이름과 Java 필드 이름을 매핑
    private Double closePrice;

    @JsonProperty("openPrice")
    private Double openPrice;

    @JsonProperty("highPrice")
    private Double highPrice;

    @JsonProperty("lowPrice")
    private Double lowPrice;

    private Long volume;
}

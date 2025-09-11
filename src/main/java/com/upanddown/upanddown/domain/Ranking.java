package com.upanddown.upanddown.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Setter(AccessLevel.PROTECTED)
@Table(name = "rankings")
public class Ranking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Setter
    private Integer currentRank; // 현재 랭킹

    private BigDecimal totalAssets; // 총 자산 (예치금 + 보유 주식 평가액)

    private Double profitRate; // 수익률 (%)

    @CreationTimestamp
    private LocalDateTime createdAt; // 랭킹 생성 시점

    @Builder
    public Ranking(Long userId, Integer currentRank, BigDecimal totalAssets, Double profitRate) {
        this.userId = userId;
        this.currentRank = currentRank;
        this.totalAssets = totalAssets;
        this.profitRate = profitRate;
    }

}
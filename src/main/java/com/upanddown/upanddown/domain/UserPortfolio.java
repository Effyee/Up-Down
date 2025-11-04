package com.upanddown.upanddown.domain;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPortfolio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Long id;

    private Long userId; // 사용자 식별자
    private String ticker; // 종목 코드
    private Long quantity; // 보유 수량

    @Version
    private Long version;

}

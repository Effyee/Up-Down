package com.upanddown.upanddown.repository;

import com.upanddown.upanddown.domain.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {
    /**
     * 각 유저별로 가장 최근 랭킹만 반환하는 커스텀 쿼리 (예시)
     * 실제 DB 구조에 따라 수정 필요
     */
    @Query("SELECT r FROM Ranking r WHERE r.id IN (SELECT MAX(r2.id) FROM Ranking r2 GROUP BY r2.userId)")
    List<Ranking> findLatestRankingsGroupedByUser();
}


package com.upanddown.upanddown.repository;

import com.upanddown.upanddown.domain.Ranking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RankingRepository extends JpaRepository<Ranking, Long> {

    /**
     * [추가된 메서드]
     * 총 자산(totalAssets)을 기준으로 모든 Ranking 데이터를 내림차순으로 정렬하여 조회합니다.
     * Spring Data JPA의 쿼리 메서드 규칙에 따라 메서드 이름만으로 자동으로 쿼리가 생성됩니다.
     *
     * @return 정렬된 Ranking 리스트
     */
    List<Ranking> findAllByOrderByTotalAssetsDesc();

    /**
     * 각 유저별로 가장 최근 랭킹만 반환하는 커스텀 쿼리입니다.
     */
    @Query("SELECT r FROM Ranking r WHERE r.id IN (SELECT MAX(r2.id) FROM Ranking r2 GROUP BY r2.userId)")
    List<Ranking> findLatestRankingsGroupedByUser();
}


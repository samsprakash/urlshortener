package com.example.agentic.url.repo;

import com.example.agentic.url.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

    List<ClickEvent> findByUrlIdOrderByOccurredAtDesc(UUID urlId);

    long countByUrlId(UUID urlId);

    @Query("select count(distinct c.visitorHash) from ClickEvent c where c.urlId = :urlId")
    long countDistinctVisitors(@Param("urlId") UUID urlId);

    @Query("select c from ClickEvent c where c.urlId = :urlId and c.occurredAt >= :since order by c.occurredAt desc")
    List<ClickEvent> findRecent(@Param("urlId") UUID urlId, @Param("since") Instant since);
}

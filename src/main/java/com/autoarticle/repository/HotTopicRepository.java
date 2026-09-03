package com.autoarticle.repository;

import com.autoarticle.entity.HotTopic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HotTopicRepository extends JpaRepository<HotTopic, Long> {

    @Query("SELECT h FROM HotTopic h WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR h.title LIKE %:keyword%) AND " +
           "(:source IS NULL OR :source = '' OR h.source = :source) AND " +
           "(:status IS NULL OR :status = '' OR h.status = :status) " +
           "ORDER BY h.rank ASC")
    Page<HotTopic> findByFilters(@Param("keyword") String keyword,
                                 @Param("source") String source,
                                 @Param("status") String status,
                                 Pageable pageable);

    boolean existsByTitle(String title);

    boolean existsByTitleIgnoreCase(String title);
}

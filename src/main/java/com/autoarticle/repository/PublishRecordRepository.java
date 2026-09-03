package com.autoarticle.repository;

import com.autoarticle.entity.PublishRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PublishRecordRepository extends JpaRepository<PublishRecord, Long> {

    Page<PublishRecord> findByArticleIdOrderByCreatedAtDesc(Long articleId, Pageable pageable);

    long countByStatus(String status);
}

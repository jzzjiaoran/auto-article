package com.autoarticle.repository;

import com.autoarticle.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    @Query("SELECT a FROM Article a WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR a.title LIKE %:keyword%) AND " +
           "(:status IS NULL OR :status = '' OR a.status = :status)")
    Page<Article> findByFilters(@Param("keyword") String keyword,
                                @Param("status") String status,
                                Pageable pageable);

    Page<Article> findByStatusIn(List<String> statuses, Pageable pageable);

    long countByStatus(String status);

    List<Article> findTop5ByOrderByUpdatedAtDesc();

    @Query("SELECT a FROM Article a WHERE a.status IN ('draft', 'generated') ORDER BY a.updatedAt DESC")
    List<Article> findPublishableArticles();
}

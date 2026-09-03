package com.autoarticle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "articles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Article {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String contentHtml;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private String status;

    private Integer wordCount;

    private String aiProvider;

    private String aiModel;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    private String style;

    private String length;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hot_topic_id")
    private HotTopic hotTopic;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

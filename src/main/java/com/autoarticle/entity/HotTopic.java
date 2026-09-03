package com.autoarticle.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hot_topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotTopic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String source;

    private Integer rank;

    private String hotLevel;

    private String status;

    private String sourceUrl;

    @OneToMany(mappedBy = "hotTopic", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Article> articles = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime collectedAt;
}

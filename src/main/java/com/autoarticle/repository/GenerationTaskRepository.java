package com.autoarticle.repository;

import com.autoarticle.entity.GenerationTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenerationTaskRepository extends JpaRepository<GenerationTask, Long> {

    Optional<GenerationTask> findByTaskId(String taskId);
}

package com.example.backend.repositatory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.IngestionJob;

public interface IngestionJobRepository extends JpaRepository<IngestionJob, UUID> {
    List<IngestionJob> findByUserIdOrderByCreatedAtDesc(String userId);
}

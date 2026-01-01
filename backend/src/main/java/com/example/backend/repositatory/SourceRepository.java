package com.example.backend.repositatory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Source;

public interface SourceRepository extends JpaRepository<Source, UUID> {
    List<Source> findByUserId(String userId);
}


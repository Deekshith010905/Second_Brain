package com.example.backend.repositatory;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.entity.Chunk;

public interface ChunkRepository extends JpaRepository<Chunk, UUID> {
    List<Chunk> findBySourceId(UUID sourceId);
    long countByUserIdAndModality(String userId, String modality);
}

package com.example.backend.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
@Entity
@Table(name="chunks")
public class Chunk {
  @Id
  private UUID id;

  @Column(name="source_id", nullable=false)
  private UUID sourceId;

  @Column(name="user_id", nullable=false)
  private String userId;

  @Column(nullable=false)
  private String modality;

  @Column(name="chunk_index", nullable=false)
  private int chunkIndex;

  @Column(columnDefinition="text", nullable=false)
  private String content;

  // embedding stored via SQL insert (vector type) - keep null in JPA entity for MVP
  // timestamps:
  @Column(name="created_at")
  private OffsetDateTime createdAt;

  @Column(name="ingested_at")
  private OffsetDateTime ingestedAt;

  @Column(name="start_time_ms")
  private Integer startTimeMs;

  @Column(name="end_time_ms")
  private Integer endTimeMs;

  @Column(columnDefinition="jsonb")
  private String meta;

  
}

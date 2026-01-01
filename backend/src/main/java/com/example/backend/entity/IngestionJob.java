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
@Table(name="ingestion_jobs")
public class IngestionJob {
  @Id
  private UUID id;

  @Column(name="user_id", nullable=false)
  private String userId;

  @Column(nullable=false)
  private String modality;

  @Column(nullable=false)
  private String status; // QUEUED/RUNNING/DONE/FAILED

  @Column(columnDefinition="text")
  private String error;

  @Column(name="created_at")
  private OffsetDateTime createdAt;

  @Column(name="updated_at")
  private OffsetDateTime updatedAt;
}


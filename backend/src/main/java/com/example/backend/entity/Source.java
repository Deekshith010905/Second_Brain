package com.example.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "sources")
public class Source {

  @Id
  private UUID id;

  @Column(name="user_id", nullable=false)
  private String userId;

  @Column(nullable=false)
  private String modality;

  @Column(name="source_uri")
  private String sourceUri;

  private String title;

  @Column(name="created_at")
  private OffsetDateTime createdAt;

  @Column(name="ingested_at")
  private OffsetDateTime ingestedAt;

  @Column(columnDefinition = "jsonb")
  private String meta;
}

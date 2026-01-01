package com.example.backend.controller;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.dto.IngestUrlRequest;
import com.example.backend.service.IngestionService;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;



@RestController
@RequestMapping("api/ingest")
public class IngestController {
  private final JdbcTemplate jdbc;
  private final IngestionService ingestion;

  @Value("${storage.uploadDir}")
  private String uploadDir;

  public IngestController(JdbcTemplate jdbc, IngestionService ingestion) {
    this.jdbc = jdbc;
    this.ingestion = ingestion;
  }

  @PostMapping("/url")
  public Map<String,Object> ingestUrl(@RequestBody IngestUrlRequest req) {
    UUID jobId = UUID.randomUUID();
    jdbc.update("""
      INSERT INTO ingestion_jobs(id,user_id,modality,status)
      VALUES (?,?,?,'QUEUED')
    """, jobId, req.userId(), "web");

    OffsetDateTime createdAt = req.createdAt() == null ? null : OffsetDateTime.parse(req.createdAt());
    ingestion.ingestWeb(jobId, req.userId(), req.url(), createdAt);

    return Map.of("jobId", jobId, "status", "QUEUED");
  }

  @PostMapping(value="/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String,Object> ingestAudio(
      @RequestParam String userId,
      @RequestParam(required=false) String createdAt,
      @RequestPart("file") MultipartFile file
  ) throws IOException {
    Files.createDirectories(Path.of(uploadDir));
    Path out = Path.of(uploadDir, System.currentTimeMillis() + "_" + file.getOriginalFilename());
    Files.copy(file.getInputStream(), out);

    UUID jobId = UUID.randomUUID();
    jdbc.update("""
      INSERT INTO ingestion_jobs(id,user_id,modality,status)
      VALUES (?,?,?,'QUEUED')
    """, jobId, userId, "audio");

    OffsetDateTime created = createdAt == null ? null : OffsetDateTime.parse(createdAt);
    ingestion.ingestAudio(jobId, userId, out, created);

    return Map.of("jobId", jobId, "status", "QUEUED");
  }

  @GetMapping("/jobs/{jobId}")
  public Map<String,Object> jobStatus(@PathVariable UUID jobId) {
    return jdbc.queryForMap("SELECT id, user_id, modality, status, error, created_at, updated_at FROM ingestion_jobs WHERE id=?",
        jobId);
  }
}


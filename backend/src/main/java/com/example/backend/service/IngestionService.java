package com.example.backend.service;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import org.jsoup.Jsoup;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class IngestionService {
  private final JdbcTemplate jdbc;
  private final OpenAiClient openai;
  private final Chunker chunker;

  public IngestionService(JdbcTemplate jdbc, OpenAiClient openai, Chunker chunker) {
    this.jdbc = jdbc;
    this.openai = openai;
    this.chunker = chunker;
  }

  @Async("ingestExecutor")
  public void ingestWeb(UUID jobId, String userId, String url, OffsetDateTime createdAt) {
    markJob(jobId, "RUNNING", null);
    try {
      String text = Jsoup.connect(url).get().text();

      UUID sourceId = UUID.randomUUID();
      jdbc.update("""
        INSERT INTO sources(id,user_id,modality,source_uri,title,created_at,meta)
        VALUES (?,?,?,?,?,?,?::jsonb)
      """, sourceId, userId, "web", url, url, createdAt, "{\"url\":\""+url+"\"}");

      List<String> chunks = chunker.chunk(text, 1800, 200);

      for (int i=0;i<chunks.size();i++) {
        String c = chunks.get(i);
        List<Double> emb = openai.embed(c);
        UUID chunkId = UUID.randomUUID();

        jdbc.update("""
          INSERT INTO chunks(id,source_id,user_id,modality,chunk_index,content,embedding,created_at,meta)
          VALUES (?,?,?,?,?,?,?::vector,?,'{}'::jsonb)
        """,
          chunkId, sourceId, userId, "web", i, c, toVectorLiteral(emb), createdAt
        );

        jdbc.update("UPDATE chunks SET content_tsv = to_tsvector('english', content) WHERE id = ?", chunkId);
      }

      markJob(jobId, "DONE", null);
    } catch (Exception e) {
      markJob(jobId, "FAILED", e.getMessage());
    }
  }

  @Async("ingestExecutor")
  public void ingestAudio(UUID jobId, String userId, Path audioPath, OffsetDateTime createdAt) {
    markJob(jobId, "RUNNING", null);
    try {
      String transcript = openai.transcribe(audioPath);

      UUID sourceId = UUID.randomUUID();
      jdbc.update("""
        INSERT INTO sources(id,user_id,modality,source_uri,title,created_at,meta)
        VALUES (?,?,?,?,?,?,?::jsonb)
      """, sourceId, userId, "audio", audioPath.toString(), audioPath.getFileName().toString(),
          createdAt, "{\"filename\":\""+audioPath.getFileName()+"\"}");

      List<String> chunks = chunker.chunk(transcript, 1800, 200);

      for (int i=0;i<chunks.size();i++) {
        String c = chunks.get(i);
        List<Double> emb = openai.embed(c);
        UUID chunkId = UUID.randomUUID();

        jdbc.update("""
          INSERT INTO chunks(id,source_id,user_id,modality,chunk_index,content,embedding,created_at,meta)
          VALUES (?,?,?,?,?,?,?::vector,?,'{}'::jsonb)
        """,
          chunkId, sourceId, userId, "audio", i, c, toVectorLiteral(emb), createdAt
        );
        jdbc.update("UPDATE chunks SET content_tsv = to_tsvector('english', content) WHERE id = ?", chunkId);
      }

      markJob(jobId, "DONE", null);
    } catch (Exception e) {
      markJob(jobId, "FAILED", e.getMessage());
    }
  }

  private void markJob(UUID jobId, String status, String error) {
    jdbc.update("""
      UPDATE ingestion_jobs SET status=?, error=?, updated_at=now() WHERE id=?
    """, status, error, jobId);
  }

  private String toVectorLiteral(List<Double> emb) {
    // pgvector literal format: '[0.1,0.2,...]'
    StringJoiner sj = new StringJoiner(",", "[", "]");
    for (Double f : emb) sj.add(String.valueOf(f));
    return sj.toString();
  }
}


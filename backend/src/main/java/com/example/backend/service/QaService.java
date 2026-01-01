package com.example.backend.service;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.springframework.stereotype.Service;

import com.example.backend.dto.QueryRequest;
import com.example.backend.dto.QueryResponse;
import com.example.backend.retrieval.RetrievalService;

@Service
public class QaService {
  private final RetrievalService retrieval;
  private final OpenAiClient openai;

  // ✅ allow only 1 query at a time to reduce rate-limit bursts
  private final Semaphore queryGate = new Semaphore(1);

  public QaService(RetrievalService retrieval, OpenAiClient openai) {
    this.retrieval = retrieval;
    this.openai = openai;
  }

  public QueryResponse answer(QueryRequest req) {
    boolean acquired = false;

    try {
      queryGate.acquire();
      acquired = true;

      int k = (req.topK() == null) ? 8 : req.topK();

      OffsetDateTime start = (req.startTime() == null || req.startTime().isBlank())
          ? null
          : OffsetDateTime.parse(req.startTime());

      OffsetDateTime end = (req.endTime() == null || req.endTime().isBlank())
          ? null
          : OffsetDateTime.parse(req.endTime());

      var chunks = retrieval.hybridRetrieve(req.userId(), req.query(), k, req.modality(), start, end);

      StringBuilder ctx = new StringBuilder();
      int i = 1;
      for (var c : chunks) {
        ctx.append("[").append(i++).append("] ")
            .append("modality=").append(c.get("modality"))
            .append(" created_at=").append(c.get("created_at"))
            .append(" source_id=").append(c.get("source_id"))
            .append("\n")
            .append(c.get("content"))
            .append("\n\n");
      }

      String answer = openai.chatAnswer(
          req.query(),
          (ctx.length() == 0) ? "NO CONTEXT FOUND." : ctx.toString()
      );

      List<Map<String, Object>> sources =
          ((Collection<Map<String, Object>>) chunks).stream()
              .map(c -> Map.of(
                  "source_id", c.get("source_id"),
                  "chunk_id", c.get("chunk_id"),
                  "modality", c.get("modality"),
                  "created_at", c.get("created_at"),
                  "vec_score", c.get("vec_score"),
                  "fts_score", c.get("fts_score")
              ))
              .toList();

      return new QueryResponse(answer, sources);

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Query interrupted", e);

    } finally {
      if (acquired) queryGate.release();
    }
  }
}

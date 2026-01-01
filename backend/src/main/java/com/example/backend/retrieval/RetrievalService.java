package com.example.backend.retrieval;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.backend.service.OpenAiClient;

@Service
public class RetrievalService {
  private final JdbcTemplate jdbc;
  private final OpenAiClient openai;

  public RetrievalService(JdbcTemplate jdbc, OpenAiClient openai) {
    this.jdbc = jdbc;
    this.openai = openai;
  }

  public List<Map<String,Object>> hybridRetrieve(
      String userId, String query, int topK,
      String modality, OffsetDateTime start, OffsetDateTime end
  ) {
    List<Double> qEmb = openai.embed(query);
    String qVec = toVectorLiteral(qEmb);

    StringBuilder where = new StringBuilder(" user_id = ? ");
    List<Object> params = new ArrayList<>();
    params.add(userId);

    if (modality != null && !modality.isBlank()) {
      where.append(" AND modality = ? ");
      params.add(modality);
    }
    if (start != null) {
      where.append(" AND created_at >= ? ");
      params.add(start);
    }
    if (end != null) {
      where.append(" AND created_at < ? ");
      params.add(end);
    }

    // Vector search
    String vecSql = """
    		  SELECT id::text AS chunk_id,
    		         source_id::text AS source_id,
    		         modality,
    		         content,
    		         created_at,
    		         (1 - (embedding <=> CAST(? AS vector))) AS vec_score
    		  FROM chunks
    		  WHERE %s AND embedding IS NOT NULL
    		  ORDER BY embedding <=> CAST(? AS vector)
    		  LIMIT ?
    		""".formatted(where.toString());

    		List<Object> vecParams = new ArrayList<>(params);
    		vecParams.add(qVec);
    		vecParams.add(qVec);
    		vecParams.add(topK);

    		List<Map<String, Object>> vec =
    		    jdbc.queryForList(vecSql, vecParams.toArray());


    // Full-text search
    		String ftsSql = """
    				  SELECT id::text AS chunk_id,
    				         source_id::text AS source_id,
    				         modality,
    				         content,
    				         created_at,
    				         ts_rank(
    				           to_tsvector('english', content),
    				           plainto_tsquery('english', ?)
    				         ) AS fts_score
    				  FROM chunks
    				  WHERE %s
    				    AND to_tsvector('english', content)
    				        @@ plainto_tsquery('english', ?)
    				  ORDER BY fts_score DESC
    				  LIMIT ?
    				""".formatted(where.toString());

    				List<Object> ftsParams = new ArrayList<>(params);
    				ftsParams.add(query);
    				ftsParams.add(query);
    				ftsParams.add(topK);

    				List<Map<String, Object>> fts =
    				    jdbc.queryForList(ftsSql, ftsParams.toArray());


    // Merge + score
    Map<String, Map<String,Object>> merged = new HashMap<>();
    for (var r : vec) {
      r.putIfAbsent("fts_score", 0.0);
      merged.put((String) r.get("chunk_id"), r);
    }
    for (var r : fts) {
      String id = (String) r.get("chunk_id");
      merged.compute(id, (k, existing) -> {
        if (existing == null) {
          r.putIfAbsent("vec_score", 0.0);
          return r;
        }
        existing.put("fts_score", r.get("fts_score"));
        return existing;
      });
    }

    List<Map<String,Object>> out = new ArrayList<>(merged.values());
    out.sort((a,b) -> Double.compare(score(b), score(a)));
    if (out.size() > topK) out = out.subList(0, topK);
    return out;
  }

  private double score(Map<String,Object> r) {
    double v = r.get("vec_score")==null ? 0.0 : ((Number)r.get("vec_score")).doubleValue();
    double f = r.get("fts_score")==null ? 0.0 : ((Number)r.get("fts_score")).doubleValue();
    return 0.65*v + 0.35*f;
  }

  private String toVectorLiteral(List<Double> qEmb) {
    StringJoiner sj = new StringJoiner(",", "[", "]");
    for (Double f : qEmb) sj.add(String.valueOf(f));
    return sj.toString();
  }

  private Object[] concat(List<Object> a, List<Object> b) {
    List<Object> all = new ArrayList<>(a);
    all.addAll(b);
    return all.toArray();
  }
}


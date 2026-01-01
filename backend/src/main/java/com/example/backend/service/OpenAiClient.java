package com.example.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.beans.factory.annotation.Value;


@Service
public class OpenAiClient {
  private final WebClient client;
  private final String embedModel;
  private final String chatModel;
  private final String transcribeModel;
 private final Map<String, List<Double>> queryEmbedCache = new ConcurrentHashMap<>();

  public OpenAiClient(
      @Value("${openai.apiKey}") String apiKey,
      @Value("${openai.embedModel}") String embedModel,
      @Value("${openai.chatModel}") String chatModel,
      @Value("${openai.transcribeModel}") String transcribeModel
  ) {
    this.client = WebClient.builder()
        .baseUrl("https://api.openai.com/v1")
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
        .build();
    this.embedModel = embedModel;
    this.chatModel = chatModel;
    this.transcribeModel = transcribeModel;
  }

  public List<Double> embed(String text) {
	  if (text == null) text = "";
	  String cleaned = text.trim();
	  if (cleaned.isEmpty()) return List.of();

	  // Cache only for short query-like inputs (prevents repeated calls)
	  // (We don't cache huge chunks to avoid memory blow)
	  if (cleaned.length() <= 500) {
	    return queryEmbedCache.computeIfAbsent(cleaned, this::embedWithRetry);
	  }
	  return embedWithRetry(cleaned);
	}

	private List<Double> embedWithRetry(String input) {
	  int maxRetries = 6;
	  long backoffMs = 600; // start
	  for (int attempt = 1; attempt <= maxRetries; attempt++) {
	    try {
	      Map<String, Object> body = Map.of(
	          "model", embedModel,
	          "input", input
	      );

	      Map resp = client.post()
	          .uri("/embeddings")
	          .bodyValue(body)
	          .retrieve()
	          .bodyToMono(Map.class)
	          .block();

	      List data = (List) resp.get("data");
	      Map first = (Map) data.get(0);
	      List emb = (List) first.get("embedding");

	      // Convert to List<Double>
	      List<Double> out = new ArrayList<>(emb.size());
	      for (Object v : emb) out.add(((Number) v).doubleValue());
	      return out;

	    } catch (WebClientResponseException e) {
	      int code = e.getStatusCode().value();
	      boolean retryable = (code == 429 || code >= 500);

	      if (retryable && attempt < maxRetries) {
	        long wait = backoffMs + (long) (Math.random() * 250); // jitter
	        System.out.println("Embeddings rate-limited (" + code + "). Retry in " + wait + "ms. Attempt " + attempt);
	        sleepMs(wait);
	        backoffMs *= 2; // exponential backoff
	        continue;
	      }

	      // Print OpenAI response for debugging
	      System.out.println("OpenAI embed error body: " + e.getResponseBodyAsString());
	      throw e;
	    }
	  }
	  throw new RuntimeException("Embeddings failed after retries");
	}

	private void sleepMs(long ms) {
	  try { Thread.sleep(ms); }
	  catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
	}


  public String chatAnswer(String query, String context) {
    Map<String,Object> body = Map.of(
        "model", chatModel,
        "temperature", 0.2,
        "messages", List.of(
            Map.of("role","system","content",
                "Answer using ONLY the provided context. If insufficient, say you don't have enough info."),
            Map.of("role","user","content","QUESTION:\n"+query+"\n\nCONTEXT:\n"+context)
        )
    );

    Map resp = client.post().uri("/chat/completions")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(body)
        .retrieve().bodyToMono(Map.class).block();

    List choices = (List) resp.get("choices");
    Map c0 = (Map) choices.get(0);
    Map msg = (Map) c0.get("message");
    return (String) msg.get("content");
  }

  public String transcribe(Path audioPath) throws IOException {
	    MultipartBodyBuilder mb = new MultipartBodyBuilder();
	    mb.part("model", transcribeModel);
	    mb.part("file", new FileSystemResource(audioPath.toFile()));

	    Map resp = client.post()
	        .uri("/audio/transcriptions")
	        .contentType(MediaType.MULTIPART_FORM_DATA)
	        .body(BodyInserters.fromMultipartData(mb.build()))
	        .retrieve()
	        .bodyToMono(Map.class)
	        .block();

	    return (String) resp.get("text");
	}
}


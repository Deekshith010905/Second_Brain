package com.example.backend.config;



import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(WebClientResponseException.class)
  public ResponseEntity<?> handleWebClient(WebClientResponseException e) {
    return ResponseEntity.status(e.getStatusCode()).body(
        Map.of(
            "error", "Upstream API error",
            "status", e.getStatusCode().value(),
            "message", e.getResponseBodyAsString()
        )
    );
  }
}


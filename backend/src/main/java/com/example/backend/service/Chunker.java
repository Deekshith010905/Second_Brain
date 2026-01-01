package com.example.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class Chunker {
  public List<String> chunk(String text, int maxChars, int overlap) {
    text = text == null ? "" : text.trim();
    if (text.isEmpty()) return List.of();

    List<String> chunks = new ArrayList<>();
    int i = 0;
    while (i < text.length()) {
      int end = Math.min(text.length(), i + maxChars);
      chunks.add(text.substring(i, end));
      if (end == text.length()) break;
      i = Math.max(0, end - overlap);
    }
    return chunks;
  }
}


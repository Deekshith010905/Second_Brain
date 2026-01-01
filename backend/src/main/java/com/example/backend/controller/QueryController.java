package com.example.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.QueryRequest;
import com.example.backend.dto.QueryResponse;
import com.example.backend.service.QaService;

@RestController
public class QueryController {
  private final QaService qa;

  public QueryController(QaService qa) { this.qa = qa; }

  @PostMapping("api/query")
  public QueryResponse query(@RequestBody QueryRequest req) {
    return qa.answer(req);
  }
}

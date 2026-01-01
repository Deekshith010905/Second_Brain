package com.example.backend.dto;

import java.util.List;
import java.util.Map;

public record QueryResponse(
		  String answer,
		  List<Map<String, Object>> sources
		) {}

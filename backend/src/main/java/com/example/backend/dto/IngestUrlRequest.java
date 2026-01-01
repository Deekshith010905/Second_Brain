package com.example.backend.dto;

public record IngestUrlRequest(
		  String userId,
		  String url,
		  String createdAt 
		) {}
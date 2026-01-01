package com.example.backend.dto;

public record QueryRequest(
		  String userId,
		  String query,
		  Integer topK,
		  String modality,     
		  String startTime,    
		  String endTime       
		) {}

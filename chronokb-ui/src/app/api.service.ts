import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface IngestResponse {
  jobId: string;
  status: string;
}

export interface JobStatus {
  id: string;
  user_id: string;
  modality: string;
  status: string;
  error?: string | null;
  created_at?: string;
  updated_at?: string;
}

export interface QueryRequest {
  userId: string;
  query: string;
  topK?: number;
  modality?: string | null;
  startTime?: string | null;
  endTime?: string | null;
}

export interface QueryResponse {
  answer: string;
  sources: Array<{
    source_id: string;
    chunk_id: string;
    modality: string;
    created_at?: string | null;
    vec_score?: number;
    fts_score?: number;
  }>;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  // proxy prefix:
  private base = '/api';

  constructor(private http: HttpClient) {
    console.log(`${this.base}`);
  }

  ingestUrl(userId: string, url: string, createdAt?: string): Observable<IngestResponse> {
    return this.http.post<IngestResponse>(`${this.base}/ingest/url`, { userId, url, createdAt });
  }

  ingestAudio(userId: string, file: File, createdAt?: string): Observable<IngestResponse> {
    const form = new FormData();
    form.append('userId', userId);
    if (createdAt) form.append('createdAt', createdAt);
    form.append('file', file);
    return this.http.post<IngestResponse>(`${this.base}/ingest/audio`, form);
  }

  getJob(jobId: string): Observable<JobStatus> {
    return this.http.get<JobStatus>(`${this.base}/ingest/jobs/${jobId}`);
  }

  query(req: QueryRequest): Observable<QueryResponse> {
    return this.http.post<QueryResponse>(`${this.base}/query`, req);
  }
}

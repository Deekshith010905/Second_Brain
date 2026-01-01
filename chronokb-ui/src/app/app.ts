import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, QueryResponse } from './api.service';

type ChatMsg = { who: 'user' | 'bot'; text: string };
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  title = 'ChronoKB UI';

  userId = 'demo-user';

  urlToIngest = '';
  audioFile: File | null = null;

  queryText = '';
  topK = 8;

  jobText = '';
  loading = false;

  messages: ChatMsg[] = [
    { who: 'bot', text: 'Hi! Ingest a URL or audio, then ask a question.' }
  ];

  constructor(private api: ApiService) {}

  onAudioSelected(evt: Event) {
    const input = evt.target as HTMLInputElement;
    this.audioFile = input.files && input.files.length ? input.files[0] : null;
  }

  addMsg(who: 'user' | 'bot', text: string) {
    this.messages.push({ who, text });
    setTimeout(() => {
      const el = document.getElementById('chatEnd');
      el?.scrollIntoView({ behavior: 'smooth' });
    }, 0);
  }

  async pollJob(jobId: string) {
    this.jobText = `Job ${jobId}: QUEUED`;
    for (let i = 0; i < 30; i++) { // ~60s
      const job = await this.api.getJob(jobId).toPromise();
      if (!job) return;
      this.jobText = `Job ${jobId}: ${job.status}` + (job.error ? ` | error: ${job.error}` : '');
      if (job.status === 'DONE' || job.status === 'FAILED') return;
      await new Promise(r => setTimeout(r, 2000));
    }
    this.jobText = `Job ${jobId}: still running (UI timeout).`;
  }

  ingestUrl() {
    const url = this.urlToIngest.trim();
    if (!url) return;

    this.loading = true;
    this.api.ingestUrl(this.userId, url).subscribe({
      next: async (res) => {
        this.addMsg('user', `Queued URL ingestion: ${url}\nJobId: ${res.jobId}`);
        await this.pollJob(res.jobId);
        this.loading = false;
        
      },
      error: (err) => {
        this.loading = false;
        this.addMsg('bot', `Ingest URL failed: ${err?.error || err?.message || err}`);
      }
    });
  }

  ingestAudio() {
    if (!this.audioFile) return;

    this.loading = true;
    this.api.ingestAudio(this.userId, this.audioFile).subscribe({
      next: async (res) => {
        this.addMsg('user', `Queued audio ingestion: ${this.audioFile?.name}\nJobId: ${res.jobId}`);
        await this.pollJob(res.jobId);
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.addMsg('bot', `Ingest audio failed: ${err?.error || err?.message || err}`);
      }
    });
  }

  ask() {
    const q = this.queryText.trim();
    if (!q) return;

    this.addMsg('user', q);
    this.queryText = '';
    this.loading = true;

    this.api.query({
      userId: this.userId,
      query: q,
      topK: this.topK
    }).subscribe({
      next: (res: QueryResponse) => {
        this.addMsg('bot', res.answer || '(no answer)');

        if (res.sources?.length) {
          const cite = res.sources.map((s, i) =>
            `#${i + 1} modality=${s.modality} source_id=${s.source_id} chunk_id=${s.chunk_id}`
          ).join('\n');
          this.addMsg('bot', `Citations:\n${cite}`);
        }
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.addMsg('bot', `Query failed: ${err?.error || err?.message || err}`);
      }
    });
  }

}

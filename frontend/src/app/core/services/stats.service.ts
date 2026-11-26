import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Stats } from '../models/stats.model';

// Статистика пари: /api/stats
@Injectable({ providedIn: 'root' })
export class StatsService {
  private http = inject(HttpClient);

  get(): Observable<Stats> {
    return this.http.get<Stats>('/api/stats');
  }
}

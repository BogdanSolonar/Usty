import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Dream } from '../models/dream.model';
import { EntryRequest } from '../models/entry.model';

// Мрії: /api/dreams
@Injectable({ providedIn: 'root' })
export class DreamService {
  private http = inject(HttpClient);

  // author = null -> усі мрії пари, інакше — лише мрії користувача з цим id
  list(author: number | null): Observable<Dream[]> {
    return this.http.get<Dream[]>('/api/dreams', { params: author ? { author } : {} });
  }

  create(request: EntryRequest): Observable<Dream> {
    return this.http.post<Dream>('/api/dreams', request);
  }

  update(id: number, request: EntryRequest): Observable<Dream> {
    return this.http.put<Dream>(`/api/dreams/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/dreams/${id}`);
  }

  setAchieved(id: number, value: boolean): Observable<Dream> {
    return this.http.put<Dream>(`/api/dreams/${id}/achieved`, { value });
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { DateIdea, DatePhoto } from '../models/date.model';
import { EntryRequest } from '../models/entry.model';

// Побачення: /api/dates
@Injectable({ providedIn: 'root' })
export class DateService {
  private http = inject(HttpClient);

  // Активні (ідеї + заплановані)
  active(): Observable<DateIdea[]> {
    return this.http.get<DateIdea[]>('/api/dates');
  }

  // Реалізовані (Історія)
  history(): Observable<DateIdea[]> {
    return this.http.get<DateIdea[]>('/api/dates', { params: { view: 'history' } });
  }

  create(request: EntryRequest): Observable<DateIdea> {
    return this.http.post<DateIdea>('/api/dates', request);
  }

  update(id: number, request: EntryRequest): Observable<DateIdea> {
    return this.http.put<DateIdea>(`/api/dates/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/dates/${id}`);
  }

  // "Реалізувати": дата + примітка. Бекенд надсилає сповіщення в Telegram.
  plan(id: number, plannedDate: string, note: string): Observable<DateIdea> {
    return this.http.post<DateIdea>(`/api/dates/${id}/plan`, { plannedDate, note });
  }

  cancelPlan(id: number): Observable<DateIdea> {
    return this.http.delete<DateIdea>(`/api/dates/${id}/plan`);
  }

  // "Реалізовано": побачення переходить в Історію
  complete(id: number, completedAt: string): Observable<DateIdea> {
    return this.http.post<DateIdea>(`/api/dates/${id}/complete`, { completedAt });
  }

  // Одне фото за запит — так можна показувати прогрес і бал кожного фото одразу
  uploadPhoto(dateId: number, file: File): Observable<DatePhoto[]> {
    const form = new FormData();
    form.append('files', file);
    return this.http.post<DatePhoto[]>(`/api/dates/${dateId}/photos`, form);
  }

  deletePhoto(dateId: number, photoId: number): Observable<void> {
    return this.http.delete<void>(`/api/dates/${dateId}/photos/${photoId}`);
  }
}

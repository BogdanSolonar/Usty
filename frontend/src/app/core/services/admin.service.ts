import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AdminOverview, Couple, CreateCoupleRequest } from '../models/admin.model';

// Адмінка: /api/admin/couples (бекенд пускає лише адміна, решті — 403)
@Injectable({ providedIn: 'root' })
export class AdminService {
  private http = inject(HttpClient);

  // Короткі лічильники по системі
  overview(): Observable<AdminOverview> {
    return this.http.get<AdminOverview>('/api/admin/couples/overview');
  }

  list(): Observable<Couple[]> {
    return this.http.get<Couple[]>('/api/admin/couples');
  }

  create(request: CreateCoupleRequest): Observable<Couple> {
    return this.http.post<Couple>('/api/admin/couples', request);
  }

  // Новий пароль і нове посилання на бота (старі перестають діяти)
  resetPassword(coupleId: number, userId: number): Observable<Couple> {
    return this.http.post<Couple>(`/api/admin/couples/${coupleId}/users/${userId}/reset-password`, {});
  }

  // Лише нове посилання на бота (пароль не змінюється) — щоб прив'язати Telegram для сповіщень
  newInvite(coupleId: number, userId: number): Observable<Couple> {
    return this.http.post<Couple>(`/api/admin/couples/${coupleId}/users/${userId}/invite`, {});
  }

  // Пара разом з обома користувачами і всім їхнім контентом
  delete(coupleId: number): Observable<void> {
    return this.http.delete<void>(`/api/admin/couples/${coupleId}`);
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { EntryRequest } from '../models/entry.model';
import { WishlistItem } from '../models/wishlist.model';

// Вішліст: /api/wishlist
@Injectable({ providedIn: 'root' })
export class WishlistService {
  private http = inject(HttpClient);

  // owner — id користувача з моєї пари
  byOwner(owner: number): Observable<WishlistItem[]> {
    return this.http.get<WishlistItem[]>('/api/wishlist', { params: { owner } });
  }

  // Бажання завжди додається у вішліст того, хто увійшов
  create(request: EntryRequest): Observable<WishlistItem> {
    return this.http.post<WishlistItem>('/api/wishlist', request);
  }

  update(id: number, request: EntryRequest): Observable<WishlistItem> {
    return this.http.put<WishlistItem>(`/api/wishlist/${id}`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/wishlist/${id}`);
  }

  setFulfilled(id: number, value: boolean): Observable<WishlistItem> {
    return this.http.put<WishlistItem>(`/api/wishlist/${id}/fulfilled`, { value });
  }
}

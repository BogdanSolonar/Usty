import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PhotoSuggestion } from '../models/photo.model';

// Підбір фото за назвою: /api/photos
@Injectable({ providedIn: 'root' })
export class PhotoService {
  private http = inject(HttpClient);

  searchPhotos(query: string): Observable<PhotoSuggestion[]> {
    return this.http.get<PhotoSuggestion[]>('/api/photos', { params: { query } });
  }
}

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, of } from 'rxjs';
import { PhotoChoice } from '../models/photo.model';

// Збереження фото на бекенді: /api/uploads
@Injectable({ providedIn: 'root' })
export class UploadService {
  private http = inject(HttpClient);

  // Перетворює вибір у формі на готове значення для imageUrl:
  //   без фото          -> null
  //   поточне фото      -> як є (редагування без зміни фото)
  //   фото з Pixabay     -> бекенд скачує його собі, повертає "/uploads/..."
  //   файл з пристрою   -> бекенд зберігає його, повертає "/uploads/..."
  resolveImageUrl(choice: PhotoChoice): Observable<string | null> {
    switch (choice.kind) {
      case 'none':
        return of(null);
      case 'current':
        return of(choice.url);
      case 'suggestion':
        return this.copyFromPixabay(choice.url);
      case 'file':
        return this.uploadFile(choice.file);
    }
  }

  private uploadFile(file: File): Observable<string> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<{ url: string }>('/api/uploads', form).pipe(map((r) => r.url));
  }

  private copyFromPixabay(url: string): Observable<string> {
    return this.http.post<{ url: string }>('/api/uploads/from-url', { url }).pipe(map((r) => r.url));
  }
}

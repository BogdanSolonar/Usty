import { Component, DestroyRef, computed, inject, input, output, signal } from '@angular/core';
import { Observable, catchError, concatMap, from, map, of, tap, toArray } from 'rxjs';
import { DatePhoto } from '../../../core/models/date.model';
import { DateService } from '../../../core/services/date.service';
import { ScoreBadge } from '../score-badge/score-badge';

const MAX_FILE_BYTES = 10 * 1024 * 1024;

type UploadState = 'pending' | 'uploading' | 'done' | 'error';

interface UploadItem {
  id: number;
  file: File;
  preview: string;          // тимчасове локальне посилання для прев'ю
  state: UploadState;
  photo?: DatePhoto;        // що повернув бекенд (з балом якості)
}

let nextId = 1;

// Мультизавантаження фото з побачення: вибір або перетягування кількох файлів, прев'ю, видалення до відправки,
// а після відправки — бал якості й вердикт для кожного фото.
//
// Два режими:
//   - "Реалізовано": кнопки немає, сторінка сама викликає uploadAll() після збереження дати
//   - Історія: [withButton]="true" — своя кнопка "Завантажити"
@Component({
  selector: 'app-photo-uploader',
  imports: [ScoreBadge],
  templateUrl: './photo-uploader.html',
  styleUrl: './photo-uploader.css',
})
export class PhotoUploader {
  private dateService = inject(DateService);

  dateId = input<number | null>(null);
  withButton = input(false);
  uploaded = output<DatePhoto>();

  items = signal<UploadItem[]>([]);
  error = signal('');
  dragging = signal(false);
  pendingCount = computed(() => this.items().filter((i) => i.state === 'pending' || i.state === 'error').length);
  busy = computed(() => this.items().some((i) => i.state === 'uploading'));

  constructor() {
    inject(DestroyRef).onDestroy(() => this.items().forEach((i) => URL.revokeObjectURL(i.preview)));
  }

  onFilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    this.addFiles(files);
  }

  // Перетягування файлів у зону
  onDragOver(event: DragEvent) {
    event.preventDefault(); // без цього браузер не дозволить "кинути" файл
    if (!this.busy()) this.dragging.set(true);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.dragging.set(false);
    if (this.busy()) return;
    const files = Array.from(event.dataTransfer?.files ?? []).filter((f) => f.type.startsWith('image/'));
    this.addFiles(files);
  }

  private addFiles(files: File[]) {
    const tooBig = files.filter((f) => f.size > MAX_FILE_BYTES);
    this.error.set(tooBig.length ? `Пропущено завеликі файли (макс. 10 МБ): ${tooBig.map((f) => f.name).join(', ')}` : '');
    const added = files
      .filter((f) => f.size <= MAX_FILE_BYTES)
      .map((file) => ({ id: nextId++, file, preview: URL.createObjectURL(file), state: 'pending' as UploadState }));
    this.items.update((list) => [...list, ...added]);
  }

  remove(item: UploadItem) {
    URL.revokeObjectURL(item.preview);
    this.items.update((list) => list.filter((i) => i.id !== item.id));
  }

  // Відправляє всі невідправлені фото по одному. Помилка одного фото не зупиняє інші.
  uploadAll(dateId: number): Observable<DatePhoto[]> {
    const queue = this.items().filter((i) => i.state === 'pending' || i.state === 'error');
    return from(queue).pipe(
      concatMap((item) => {
        this.setState(item.id, 'uploading');
        return this.dateService.uploadPhoto(dateId, item.file).pipe(
          map((photos) => photos[0]),
          tap((photo) => {
            this.setState(item.id, 'done', photo);
            this.uploaded.emit(photo);
          }),
          catchError(() => {
            this.setState(item.id, 'error');
            return of(null);
          }),
        );
      }),
      toArray(),
      map((photos) => photos.filter((p): p is DatePhoto => p !== null)),
    );
  }

  uploadNow() {
    const id = this.dateId();
    if (id === null) return;
    this.uploadAll(id).subscribe(() => {
      // відправлені фото вже є в галереї — прибираємо їх зі списку завантаження
      const done = this.items().filter((i) => i.state === 'done');
      done.forEach((i) => URL.revokeObjectURL(i.preview));
      this.items.update((list) => list.filter((i) => i.state !== 'done'));
    });
  }

  private setState(id: number, state: UploadState, photo?: DatePhoto) {
    this.items.update((list) => list.map((i) => (i.id === id ? { ...i, state, photo } : i)));
  }
}

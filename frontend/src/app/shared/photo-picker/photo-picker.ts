import { Component, DestroyRef, computed, inject, input, model, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { Observable, catchError, debounceTime, distinctUntilChanged, map, of, skip, switchMap, tap } from 'rxjs';
import { PhotoChoice, PhotoSuggestion } from '../../core/models/photo.model';
import { PhotoService } from '../../core/services/photo.service';

// Стан блоку пропозицій фото
type SearchState = 'idle' | 'loading' | 'done' | 'disabled' | 'error';

const MAX_FILE_BYTES = 10 * 1024 * 1024; // 10 МБ, як на бекенді

// Вибір фото у формі — лише два способи:
//   1. одне із запропонованих фото (за назвою) — підсвічується прямо в сітці, без окремого прев'ю
//   2. свій файл з пристрою — під сіткою; лише тоді з'являється прев'ю, а вибір у сітці знімається
// У режимі редагування вже збережене фото показується як "Поточне фото".
// Назовні віддає, що саме обрано: [(choice)].
@Component({
  selector: 'app-photo-picker',
  templateUrl: './photo-picker.html',
  styleUrl: './photo-picker.css',
})
export class PhotoPicker {
  private photoService = inject(PhotoService);

  title = input('');                                  // назва запису — за нею шукаємо фото
  choice = model<PhotoChoice>({ kind: 'none' });      // двостороннє зв'язування з формою

  suggestions = signal<PhotoSuggestion[]>([]);
  searchState = signal<SearchState>('idle');
  fileError = signal('');
  dragging = signal(false);

  private filePreview = signal<string | null>(null);  // тимчасове локальне посилання на файл

  hasSuggestionChoice = computed(() => this.choice().kind === 'suggestion');

  // Нижнє прев'ю — лише для власного файлу (або вже збереженого фото при редагуванні)
  ownPhoto = computed<{ url: string; label: string; name?: string } | null>(() => {
    const choice = this.choice();
    if (choice.kind === 'file') {
      const url = this.filePreview();
      return url ? { url, label: 'Ваше фото', name: choice.file.name } : null;
    }
    if (choice.kind === 'current') return { url: choice.url, label: 'Поточне фото' };
    return null;
  });

  constructor() {
    // Пошук фото: кожна зміна назви (крім початкової) -> пауза 0.6 с -> запит
    toObservable(this.title)
      .pipe(
        skip(1),                       // початкову назву (у режимі редагування) не шукаємо
        map((text) => text.trim()),
        debounceTime(600),             // чекаємо 0.6 с після останньої натиснутої клавіші
        distinctUntilChanged(),        // той самий текст вдруге не шукаємо
        tap((q) => this.searchState.set(q.length >= 3 ? 'loading' : 'idle')),
        switchMap((q) => (q.length >= 3 ? this.search(q) : of(null))), // старий запит скасовується
        takeUntilDestroyed(),
      )
      .subscribe((photos) => {
        if (photos !== null) this.suggestions.set(photos);
      });

    inject(DestroyRef).onDestroy(() => this.revokeFilePreview());
  }

  isSelected(photo: PhotoSuggestion): boolean {
    const choice = this.choice();
    return choice.kind === 'suggestion' && choice.url === photo.imageUrl;
  }

  // Повторний клік по вибраному фото знімає вибір
  pickSuggestion(photo: PhotoSuggestion) {
    this.setChoice(this.isSelected(photo) ? { kind: 'none' } : { kind: 'suggestion', url: photo.imageUrl });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = ''; // щоб той самий файл можна було вибрати ще раз
    if (file) this.useFile(file);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault(); // без цього браузер не дозволить "кинути" файл
    this.dragging.set(true);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.dragging.set(false);
    const file = Array.from(event.dataTransfer?.files ?? []).find((f) => f.type.startsWith('image/'));
    if (file) this.useFile(file);
  }

  clear() {
    this.setChoice({ kind: 'none' });
  }

  private useFile(file: File) {
    if (file.size > MAX_FILE_BYTES) {
      this.fileError.set('Файл завеликий (максимум 10 МБ).');
      return;
    }
    this.fileError.set('');
    this.setChoice({ kind: 'file', file }); // вибір у сітці пропозицій знімається сам
    this.filePreview.set(URL.createObjectURL(file)); // показуємо фото ще до завантаження на сервер
  }

  private setChoice(choice: PhotoChoice) {
    this.revokeFilePreview();
    this.choice.set(choice);
  }

  private revokeFilePreview() {
    const preview = this.filePreview();
    if (preview) URL.revokeObjectURL(preview);
    this.filePreview.set(null);
  }

  private search(query: string): Observable<PhotoSuggestion[] | null> {
    return this.photoService.searchPhotos(query).pipe(
      tap(() => this.searchState.set('done')),
      catchError((err) => {
        this.searchState.set(err.status === 503 ? 'disabled' : 'error');
        return of([]);
      }),
    );
  }
}

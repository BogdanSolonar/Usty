import { Component, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Observable, switchMap } from 'rxjs';
import { EntryRequest } from '../../core/models/entry.model';
import { PhotoChoice } from '../../core/models/photo.model';
import { UploadService } from '../../core/services/upload.service';
import { Modal } from '../modal/modal';
import { PhotoPicker } from '../photo-picker/photo-picker';

// Модальне вікно створення / редагування: назва, фото (PhotoPicker), опис.
// Спільне для побачень, бажань і мрій — яку саме API викликати, передає сторінка через saveFn.
@Component({
  selector: 'app-entry-form',
  imports: [Modal, FormsModule, PhotoPicker],
  templateUrl: './entry-form.html',
})
export class EntryForm implements OnInit {
  private uploadService = inject(UploadService);

  heading = input('Новий запис');
  initial = input<EntryRequest | null>(null);   // null = створення нового
  placeholder = input('Наприклад: пікнік у парку');
  saveFn = input.required<(request: EntryRequest) => Observable<unknown>>();

  closed = output<void>();
  saved = output<unknown>();                     // те, що повернув бекенд

  title = signal('');
  description = '';
  photo = signal<PhotoChoice>({ kind: 'none' });

  saving = signal(false);
  error = signal('');

  canSave = computed(() => this.title().trim().length > 0 && !this.saving());

  ngOnInit() {
    // Режим редагування: заповнюємо поля поточними значеннями
    const initial = this.initial();
    if (initial) {
      this.title.set(initial.title);
      this.description = initial.description ?? '';
      if (initial.imageUrl) this.photo.set({ kind: 'current', url: initial.imageUrl });
    }
  }

  save() {
    if (!this.canSave()) return;
    this.saving.set(true);
    this.error.set('');

    // Крок 1: фото -> готове значення imageUrl (за потреби завантажуємо на сервер)
    // Крок 2: зберігаємо сам запис з цим imageUrl
    this.uploadService
      .resolveImageUrl(this.photo())
      .pipe(
        switchMap((imageUrl) =>
          this.saveFn()({ title: this.title().trim(), description: this.description.trim() || null, imageUrl }),
        ),
      )
      .subscribe({
        next: (saved) => this.saved.emit(saved),
        error: () => {
          this.saving.set(false);
          this.error.set('Не вдалося зберегти. Перевірте, що бекенд запущено.');
        },
      });
  }
}

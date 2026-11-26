import { Component, HostListener, computed, inject, input, linkedSignal, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { AuthService } from '../../../core/auth/auth.service';
import { DateIdea, DatePhoto, QualityBreakdown } from '../../../core/models/date.model';
import { planned } from '../../../core/models/user-names';
import { DateService } from '../../../core/services/date.service';
import { EntryDetails } from '../../../shared/entry-details/entry-details';
import { UserAvatar } from '../../../shared/user-avatar/user-avatar';
import { CompleteDialog } from '../complete-dialog/complete-dialog';
import { PhotoUploader } from '../photo-uploader/photo-uploader';
import { ScoreBadge } from '../score-badge/score-badge';

// Вікно побачення.
//   Ідея / заплановане: інфо про план + зелена кнопка "✓ Реалізовано" ->
//     поверх відкривається окреме вікно (дата + фото) -> побачення йде в Історію.
//   Реалізоване (з Історії): галерея фото з балами якості + дозавантаження нових.
@Component({
  selector: 'app-date-details',
  imports: [EntryDetails, DatePipe, UserAvatar, CompleteDialog, PhotoUploader, ScoreBadge],
  templateUrl: './date-details.html',
  styleUrl: './date-details.css',
})
export class DateDetails {
  private dateService = inject(DateService);
  private auth = inject(AuthService);

  date = input.required<DateIdea>();

  closed = output<void>();
  edit = output<void>();
  replan = output<void>();
  deleted = output<number>();
  changed = output<DateIdea>();     // план скасовано, додано/видалено фото
  completed = output<DateIdea>();   // побачення перейшло в Історію

  // Локальна копія, щоб одразу показувати нові фото / зміни
  current = linkedSignal(() => this.date());
  me = this.auth.user;
  readonly plannedVerb = planned;

  completing = signal(false);                 // відкрите вікно "Реалізовано"
  error = signal('');
  viewing = signal<DatePhoto | null>(null);   // фото, відкрите на весь екран

  isCompleted = computed(() => this.current().status === 'COMPLETED');
  deleteFn = () => this.dateService.delete(this.current().id);

  // Підписи до показників оцінки фото
  readonly criteria: { key: keyof QualityBreakdown; label: string }[] = [
    { key: 'sharpness', label: 'Чіткість' },
    { key: 'lighting', label: 'Освітлення' },
    { key: 'people', label: 'Люди в кадрі' },
    { key: 'composition', label: 'Композиція' },
    { key: 'location', label: 'Локація і кольори' },
    { key: 'resolution', label: 'Роздільна здатність' },
  ];

  // Esc при відкритому фото закриває лише фото, а не все вікно.
  // Цей обробник підключається раніше за обробник вікна всередині, тож встигає його зупинити.
  @HostListener('document:keydown.escape', ['$event'])
  onEscape(event: Event) {
    if (this.viewing()) {
      this.viewing.set(null);
      event.stopImmediatePropagation();
    }
  }

  cancelPlan() {
    this.dateService.cancelPlan(this.current().id).subscribe({
      next: (updated) => {
        this.current.set(updated);
        this.changed.emit(updated);
      },
      error: () => this.error.set('Не вдалося скасувати план.'),
    });
  }

  // Нове фото з Історії — одразу в галерею
  onPhotoUploaded(photo: DatePhoto) {
    this.current.update((d) => ({ ...d, photos: [...d.photos, photo] }));
    this.changed.emit(this.current());
  }

  deletePhoto(photo: DatePhoto) {
    this.dateService.deletePhoto(this.current().id, photo.id).subscribe({
      next: () => {
        this.viewing.set(null);
        this.current.update((d) => ({ ...d, photos: d.photos.filter((p) => p.id !== photo.id) }));
        this.changed.emit(this.current());
      },
      error: () => this.error.set('Не вдалося видалити фото.'),
    });
  }
}

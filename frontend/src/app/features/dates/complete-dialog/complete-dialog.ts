import { Component, OnInit, inject, input, output, signal, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { map, of, switchMap } from 'rxjs';
import { DateIdea } from '../../../core/models/date.model';
import { DateService } from '../../../core/services/date.service';
import { Calendar } from '../../../shared/calendar/calendar';
import { Modal } from '../../../shared/modal/modal';
import { todayIso } from '../../../shared/utils/dates';
import { PhotoUploader } from '../photo-uploader/photo-uploader';

// "✓ Реалізовано": окреме вікно поверх вікна побачення.
// Дата реалізації (за замовчуванням — запланована, якщо вже настала) + фото ->
// "Зберегти" -> статус COMPLETED, фото завантажуються по одному й отримують оцінку.
@Component({
  selector: 'app-complete-dialog',
  imports: [Modal, Calendar, DatePipe, PhotoUploader],
  templateUrl: './complete-dialog.html',
  styleUrl: './complete-dialog.css',
})
export class CompleteDialog implements OnInit {
  private dateService = inject(DateService);

  date = input.required<DateIdea>();
  closed = output<void>();
  completed = output<DateIdea>();

  readonly today = todayIso();
  day = signal<string | null>(null);
  saving = signal(false);
  error = signal('');

  private uploader = viewChild(PhotoUploader);

  ngOnInit() {
    const planned = this.date().plannedDate;
    this.day.set(planned && planned <= this.today ? planned : this.today);
  }

  close() {
    if (!this.saving()) this.closed.emit();
  }

  save() {
    const day = this.day();
    if (!day || this.saving()) return;
    const id = this.date().id;
    const uploader = this.uploader();
    this.saving.set(true);
    this.error.set('');

    // 1) статус COMPLETED + дата  2) фото по одному (кожне отримує оцінку)
    this.dateService
      .complete(id, day)
      .pipe(
        switchMap((updated) =>
          (uploader ? uploader.uploadAll(id) : of([])).pipe(
            map((photos) => ({ ...updated, photos: [...updated.photos, ...photos] })),
          ),
        ),
      )
      .subscribe({
        next: (updated) => this.completed.emit(updated),
        error: () => {
          this.saving.set(false);
          this.error.set('Не вдалося зберегти. Спробуйте ще раз.');
        },
      });
  }
}

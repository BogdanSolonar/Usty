import { Component, OnInit, inject, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DateIdea } from '../../../core/models/date.model';
import { DateService } from '../../../core/services/date.service';
import { Calendar } from '../../../shared/calendar/calendar';
import { Modal } from '../../../shared/modal/modal';
import { todayIso } from '../../../shared/utils/dates';

// "Реалізувати": календарик для вибору дати + примітка.
// Після підтвердження бекенд ставить статус PLANNED, запам'ятовує ініціатора
// і надсилає сповіщення в Telegram.
@Component({
  selector: 'app-plan-dialog',
  imports: [Modal, Calendar, FormsModule, DatePipe],
  templateUrl: './plan-dialog.html',
  styleUrl: './plan-dialog.css',
})
export class PlanDialog implements OnInit {
  private dateService = inject(DateService);

  date = input.required<DateIdea>();
  closed = output<void>();
  planned = output<DateIdea>();

  readonly today = todayIso();
  day = signal<string | null>(null);
  note = '';
  saving = signal(false);
  error = signal('');

  ngOnInit() {
    // Перепланування: підставляємо поточні дату й примітку
    const d = this.date();
    if (d.plannedDate && d.plannedDate >= this.today) this.day.set(d.plannedDate);
    this.note = d.plannedNote ?? '';
  }

  confirm() {
    const day = this.day();
    if (!day || this.saving()) return;
    this.saving.set(true);
    this.error.set('');
    this.dateService.plan(this.date().id, day, this.note.trim()).subscribe({
      next: (updated) => this.planned.emit(updated),
      error: () => {
        this.saving.set(false);
        this.error.set('Не вдалося запланувати. Спробуйте ще раз.');
      },
    });
  }
}

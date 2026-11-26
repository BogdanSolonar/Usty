import { Component, OnInit, computed, input, model, signal } from '@angular/core';
import { parseIsoDate, toIsoDate, todayIso } from '../utils/dates';

interface Day {
  iso: string;
  day: number;
  disabled: boolean;
  marks: string[]; // назви подій цього дня (для точок і підказки)
}

const MONTHS = ['Січень', 'Лютий', 'Березень', 'Квітень', 'Травень', 'Червень',
  'Липень', 'Серпень', 'Вересень', 'Жовтень', 'Листопад', 'Грудень'];

// Календарик на місяць: стрілками гортаємо місяці.
//   - звичайний режим: клік по дню — вибір; [(value)] = обрана дата "2026-12-05";
//     min / max — межі (дні поза ними неактивні)
//   - [readonly]="true": дні не клікаються, лише гортання місяців;
//     [marks] = { "2026-12-05": ["Пікнік"] } — на цих днях з'являються точки
@Component({
  selector: 'app-calendar',
  templateUrl: './calendar.html',
  styleUrl: './calendar.css',
  host: { '[class.readonly]': 'readonly()' },
})
export class Calendar implements OnInit {
  value = model<string | null>(null);
  min = input<string | null>(null);
  max = input<string | null>(null);
  readonly = input(false);
  marks = input<Record<string, string[]>>({});

  readonly weekDays = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Нд'];
  readonly today = todayIso();

  // Який місяць показуємо (перше число місяця)
  private shown = signal(firstOfMonth(new Date()));

  ngOnInit() {
    // Якщо дата вже обрана — відкриваємо календар на її місяці
    const value = this.value();
    if (value) this.shown.set(firstOfMonth(parseIsoDate(value)));
  }

  title = computed(() => `${MONTHS[this.shown().getMonth()]} ${this.shown().getFullYear()}`);

  // Клітинки сітки: порожні на початку (щоб 1-ше число стало під свій день тижня) + дні місяця
  cells = computed<(Day | null)[]>(() => {
    const first = this.shown();
    const marks = this.marks();
    const offset = (first.getDay() + 6) % 7; // понеділок = 0
    const daysInMonth = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate();
    const cells: (Day | null)[] = Array(offset).fill(null);
    for (let d = 1; d <= daysInMonth; d++) {
      const iso = toIsoDate(new Date(first.getFullYear(), first.getMonth(), d));
      const disabled = (!!this.min() && iso < this.min()!) || (!!this.max() && iso > this.max()!);
      cells.push({ iso, day: d, disabled, marks: marks[iso] ?? [] });
    }
    return cells;
  });

  move(months: number) {
    const s = this.shown();
    this.shown.set(new Date(s.getFullYear(), s.getMonth() + months, 1));
  }

  pick(day: Day) {
    if (!day.disabled && !this.readonly()) this.value.set(day.iso);
  }
}

function firstOfMonth(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), 1);
}

import { Component, computed, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DateIdea } from '../../../core/models/date.model';
import { planned } from '../../../core/models/user-names';
import { Calendar } from '../../../shared/calendar/calendar';
import { UserAvatar } from '../../../shared/user-avatar/user-avatar';

// Права колонка сторінки "Побачення":
//   - зверху календарик лише для перегляду (гортаються місяці, точки — заплановані дні)
//   - під ним — заплановані побачення: дата, назва і хто запланував (аватарка + ім'я)
@Component({
  selector: 'app-planned-panel',
  imports: [Calendar, UserAvatar, DatePipe],
  templateUrl: './planned-panel.html',
  styleUrl: './planned-panel.css',
})
export class PlannedPanel {
  dates = input.required<DateIdea[]>(); // лише заплановані, впорядковані за датою
  open = output<DateIdea>();

  readonly plannedVerb = planned;

  // { "2026-12-05": ["Пікнік на даху"] } — для точок у календарі
  marks = computed(() => {
    const marks: Record<string, string[]> = {};
    for (const d of this.dates()) {
      if (d.plannedDate) (marks[d.plannedDate] ??= []).push(d.title);
    }
    return marks;
  });
}

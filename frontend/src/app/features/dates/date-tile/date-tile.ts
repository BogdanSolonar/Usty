import { Component, computed, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { DateIdea } from '../../../core/models/date.model';
import { UserAvatar } from '../../../shared/user-avatar/user-avatar';

// Картка побачення в стилі "полароїд":
//   - у правому верхньому куті — аватарка автора ідеї (без фото — літера: М рожева / Б синя)
//   - заплановане — стрічка з датою
//   - при наведенні з'являється кнопка "Реалізувати" (на телефоні видно завжди)
//   - у реалізованого (Історія) — дата і кількість фото
@Component({
  selector: 'app-date-tile',
  imports: [DatePipe, UserAvatar],
  templateUrl: './date-tile.html',
  styleUrl: './date-tile.css',
  host: { '[style.--i]': 'index()' },
})
export class DateTile {
  date = input.required<DateIdea>();
  index = input(0);

  open = output<void>();
  realize = output<void>();

  // Обкладинка; у реалізованого без обкладинки — перше фото з побачення
  cover = computed(() => this.date().coverImageUrl ?? this.date().photos[0]?.photoUrl ?? null);
}

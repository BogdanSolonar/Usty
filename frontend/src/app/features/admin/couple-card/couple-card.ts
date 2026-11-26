import { Component, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Couple, Member } from '../../../core/models/admin.model';
import { User } from '../../../core/models/user.model';
import { UserAvatar } from '../../../shared/user-avatar/user-avatar';
import { timeAgo } from '../../../shared/utils/dates';

// Картка пари в адмінці: двоє користувачів — чи входили, остання активність, скільки чого створили,
// статус Telegram, посилання на бота і ще не доставлений пароль.
// Та сама картка показується одразу після створення пари ([justCreated] — паролі видно відразу).
@Component({
  selector: 'app-couple-card',
  imports: [UserAvatar, DatePipe],
  templateUrl: './couple-card.html',
  styleUrl: './couple-card.css',
})
export class CoupleCard {
  couple = input.required<Couple>();
  justCreated = input(false);

  reset = output<Member>();   // "Новий пароль і посилання"
  invite = output<Member>();  // "Посилання для Telegram" (без зміни пароля)
  remove = output<void>();    // "Видалити пару"

  revealed = signal<Set<number>>(new Set());   // id тих, чий пароль зараз показано
  copied = signal<string | null>(null);        // що щойно скопійовано (для підказки "Скопійовано")
  readonly ago = timeAgo;

  // UserAvatar чекає на User — беремо з Member лише потрібне
  asUser(m: Member): User {
    return { id: m.id, name: m.name, nameGenitive: m.nameGenitive, role: m.role, color: m.color, avatarUrl: m.avatarUrl, admin: m.admin };
  }

  isRevealed(m: Member): boolean {
    return this.justCreated() || this.revealed().has(m.id);
  }

  toggleReveal(m: Member) {
    this.revealed.update((set) => {
      const next = new Set(set);
      if (!next.delete(m.id)) next.add(m.id);
      return next;
    });
  }

  copy(text: string, key: string) {
    navigator.clipboard?.writeText(text).then(() => {
      this.copied.set(key);
      setTimeout(() => this.copied.set(null), 1500);
    });
  }
}

import { Component, inject, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Couple, PartnerForm } from '../../../core/models/admin.model';
import { AdminService } from '../../../core/services/admin.service';
import { Modal } from '../../../shared/modal/modal';

// Форма "Нова пара": хлопець і дівчина.
// Для кожного: ім'я (обов'язково), "чий?" для підписів, колір-маркер, пароль (порожньо — згенерується).
// Мова й часовий пояс не потрібні: увесь застосунок український і живе за київським часом.
@Component({
  selector: 'app-couple-form',
  imports: [Modal, FormsModule],
  templateUrl: './couple-form.html',
  styleUrl: './couple-form.css',
})
export class CoupleForm {
  private adminService = inject(AdminService);

  closed = output<void>();
  created = output<Couple>();

  he: PartnerForm = { name: '', nameGenitive: '', color: '#3b82f6', password: '' };
  she: PartnerForm = { name: '', nameGenitive: '', color: '#ec4899', password: '' };

  readonly partners = [
    { key: 'he' as const, title: '👨 Хлопець', namePlaceholder: 'Андрій', genitivePlaceholder: 'Андрія' },
    { key: 'she' as const, title: '👩 Дівчина', namePlaceholder: 'Олена', genitivePlaceholder: 'Олени' },
  ];

  saving = signal(false);
  error = signal('');

  form(key: 'he' | 'she'): PartnerForm {
    return key === 'he' ? this.he : this.she;
  }

  canSave(): boolean {
    return !!this.he.name.trim() && !!this.she.name.trim() && !this.saving();
  }

  // Ті самі правила, що й на бекенді — щоб підказати одразу, без запиту
  private validate(): string | null {
    const he = this.he.password.trim(), she = this.she.password.trim();
    if ((he && he.length < 6) || (she && she.length < 6)) return 'Пароль має бути щонайменше з 6 символів.';
    if (he && he === she) return 'Паролі хлопця і дівчини мають відрізнятися — за паролем застосунок визначає, хто входить.';
    return null;
  }

  save() {
    if (!this.canSave()) return;
    const problem = this.validate();
    if (problem) {
      this.error.set(problem);
      return;
    }
    this.saving.set(true);
    this.error.set('');
    this.adminService.create({ he: this.he, she: this.she }).subscribe({
      next: (couple) => this.created.emit(couple),
      error: (err: HttpErrorResponse) => {
        this.saving.set(false);
        // 400 / 409 — зрозуміле пояснення від бекенду (однакові паролі, пароль уже зайнятий...)
        this.error.set(err.status === 400 || err.status === 409 ? (err.error?.message ?? 'Перевірте поля форми.')
          : 'Не вдалося створити пару. Перевірте, що бекенд запущено.');
      },
    });
  }
}

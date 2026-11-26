import { Component, computed, input, output, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Observable } from 'rxjs';
import { Modal } from '../modal/modal';
import { splitLinks } from '../utils/split-links';

// Модальне вікно перегляду запису (побачення, бажання, мрії):
// фото, назва, дата, опис + "Редагувати" / "Видалити" з підтвердженням.
// Додатковий вміст передається всередину:
//   <div detailsMeta>…</div>     — під заголовком (автор, статус)
//   <div>…</div>                 — під описом (наприклад, фото з побачення)
//   <button detailsActions>…     — додаткові кнопки поруч із "Редагувати"
@Component({
  selector: 'app-entry-details',
  imports: [Modal, DatePipe],
  templateUrl: './entry-details.html',
  styleUrl: './entry-details.css',
})
export class EntryDetails {
  title = input.required<string>();
  imageUrl = input<string | null>(null);
  emoji = input('✨');
  createdAt = input.required<string>();
  description = input<string | null>(null);
  canEdit = input(true);                                  // false — кнопок змін немає
  deleteFn = input<(() => Observable<unknown>) | null>(null); // як видалити (виклик API)
  wide = input(false);

  closed = output<void>();
  edit = output<void>();
  deleted = output<void>();

  confirmingDelete = signal(false);
  deleting = signal(false);
  error = signal('');

  // Опис, розбитий на текст і клікабельні посилання
  descriptionParts = computed(() => splitLinks(this.description() ?? ''));

  delete() {
    const deleteFn = this.deleteFn();
    if (!deleteFn) return;
    this.deleting.set(true);
    deleteFn().subscribe({
      next: () => this.deleted.emit(),
      error: () => {
        this.deleting.set(false);
        this.error.set('Не вдалося видалити. Спробуйте ще раз.');
      },
    });
  }
}

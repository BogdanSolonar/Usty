import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { category } from '../../../core/models/category.model';
import { DateIdea } from '../../../core/models/date.model';
import { EntryRequest } from '../../../core/models/entry.model';
import { DateService } from '../../../core/services/date.service';
import { EntryForm } from '../../../shared/entry-form/entry-form';
import { PageHero } from '../../../shared/page-hero/page-hero';
import { DateDetails } from '../date-details/date-details';
import { DateTile } from '../date-tile/date-tile';
import { PlanDialog } from '../plan-dialog/plan-dialog';
import { PlannedPanel } from '../planned-panel/planned-panel';
import { StatsDialog } from '../stats-dialog/stats-dialog';

type ModalKind = 'none' | 'details' | 'form' | 'plan' | 'stats';

// Сторінка "Побачення": ліворуч ідеї плитками, праворуч календарик і заплановані;
// кнопки "Історія", "Статистика", "Створити".
// Тримає список і вирішує, яке модальне вікно зараз відкрите.
@Component({
  selector: 'app-dates-page',
  imports: [RouterLink, PageHero, DateTile, DateDetails, EntryForm, PlanDialog, PlannedPanel, StatsDialog],
  templateUrl: './dates-page.html',
  styleUrls: ['../dates-shared.css', './dates-page.css'],
})
export class DatesPage implements OnInit {
  private dateService = inject(DateService);

  readonly info = category('dates');

  dates = signal<DateIdea[]>([]);
  loaded = signal(false);
  loadError = signal(false);

  planned = computed(() => this.dates().filter((d) => d.status === 'PLANNED'));
  ideas = computed(() => this.dates().filter((d) => d.status === 'IDEA'));

  modal = signal<ModalKind>('none');
  selected = signal<DateIdea | null>(null);
  fromDetails = signal(false); // форму / планування відкрито з вікна перегляду -> туди й повертаємось
  toast = signal('');

  // Що викликати при збереженні форми (створення чи редагування)
  saveFn = (request: EntryRequest) => {
    const selected = this.selected();
    return selected ? this.dateService.update(selected.id, request) : this.dateService.create(request);
  };

  ngOnInit() {
    this.dateService.active().subscribe({
      next: (dates) => {
        this.dates.set(dates);
        this.loaded.set(true);
      },
      error: () => this.loadError.set(true),
    });
  }

  open(kind: ModalKind, date: DateIdea | null = null) {
    this.selected.set(date);
    this.fromDetails.set(false);
    this.modal.set(kind);
    if (kind !== 'none') this.hideToast(); // повідомлення не налазить на нове вікно
  }

  // "Редагувати" / "Змінити дату" у вікні перегляду
  openFromDetails(kind: 'form' | 'plan') {
    this.fromDetails.set(true);
    this.modal.set(kind);
  }

  closeModal() {
    if (this.fromDetails() && (this.modal() === 'form' || this.modal() === 'plan')) {
      this.fromDetails.set(false);
      this.modal.set('details');
      return;
    }
    this.modal.set('none');
    this.selected.set(null);
  }

  onSaved(result: unknown) {
    const saved = result as DateIdea;
    if (this.dates().some((d) => d.id === saved.id)) {
      this.replace(saved);
      this.open(this.fromDetails() ? 'details' : 'none', saved);
    } else {
      this.dates.update((list) => [saved, ...list]);
      this.open('none');
    }
  }

  onPlanned(date: DateIdea) {
    this.replace(date);
    this.open(this.fromDetails() ? 'details' : 'none', date);
    this.showToast(`📨 Заплановано! Сповіщення відправлено в Telegram`);
  }

  onChanged(date: DateIdea) {
    this.replace(date);
    this.selected.set(date);
  }

  onCompleted(date: DateIdea) {
    this.dates.update((list) => list.filter((d) => d.id !== date.id));
    this.open('none');
    this.showToast(`✅ «${date.title}» перенесено в Історію`);
  }

  onDeleted(id: number) {
    this.dates.update((list) => list.filter((d) => d.id !== id));
    this.open('none');
  }

  // Заплановані побачення впорядковані за датою, тому після змін просто сортуємо наново
  private replace(date: DateIdea) {
    this.dates.update((list) =>
      list
        .map((d) => (d.id === date.id ? date : d))
        .sort((a, b) => (a.plannedDate ?? '9999').localeCompare(b.plannedDate ?? '9999')),
    );
  }

  private toastTimer?: ReturnType<typeof setTimeout>;

  private showToast(text: string) {
    this.hideToast();
    this.toast.set(text);
    this.toastTimer = setTimeout(() => this.toast.set(''), 4000);
  }

  private hideToast() {
    clearTimeout(this.toastTimer);
    this.toast.set('');
  }
}

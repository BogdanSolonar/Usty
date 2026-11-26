import { Component, OnInit, inject, signal } from '@angular/core';
import { category } from '../../../core/models/category.model';
import { DateIdea } from '../../../core/models/date.model';
import { EntryRequest } from '../../../core/models/entry.model';
import { DateService } from '../../../core/services/date.service';
import { EntryForm } from '../../../shared/entry-form/entry-form';
import { PageHero } from '../../../shared/page-hero/page-hero';
import { DateDetails } from '../date-details/date-details';
import { DateTile } from '../date-tile/date-tile';

// "Історія": реалізовані побачення з датою і фото.
// Клік — перегляд усіх фото і дозавантаження нових.
@Component({
  selector: 'app-date-history',
  imports: [PageHero, DateTile, DateDetails, EntryForm],
  templateUrl: './date-history.html',
  styleUrl: '../dates-shared.css',
})
export class DateHistory implements OnInit {
  private dateService = inject(DateService);

  readonly info = category('dates');

  dates = signal<DateIdea[]>([]);
  loaded = signal(false);
  loadError = signal(false);

  modal = signal<'none' | 'details' | 'form'>('none');
  selected = signal<DateIdea | null>(null);

  saveFn = (request: EntryRequest) => this.dateService.update(this.selected()!.id, request);

  ngOnInit() {
    this.dateService.history().subscribe({
      next: (dates) => {
        this.dates.set(dates);
        this.loaded.set(true);
      },
      error: () => this.loadError.set(true),
    });
  }

  openDetails(date: DateIdea) {
    this.selected.set(date);
    this.modal.set('details');
  }

  close() {
    if (this.modal() === 'form') {
      this.modal.set('details');
      return;
    }
    this.modal.set('none');
    this.selected.set(null);
  }

  onChanged(date: DateIdea) {
    this.dates.update((list) => list.map((d) => (d.id === date.id ? date : d)));
    this.selected.set(date);
  }

  onSaved(result: unknown) {
    this.onChanged(result as DateIdea);
    this.modal.set('details');
  }

  onDeleted(id: number) {
    this.dates.update((list) => list.filter((d) => d.id !== id));
    this.close();
  }
}

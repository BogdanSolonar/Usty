import { Component, inject, signal } from '@angular/core';
import { toObservable, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { category } from '../../core/models/category.model';
import { Dream } from '../../core/models/dream.model';
import { EntryRequest } from '../../core/models/entry.model';
import { whose } from '../../core/models/user-names';
import { DreamService } from '../../core/services/dream.service';
import { UserService } from '../../core/services/user.service';
import { EntryDetails } from '../../shared/entry-details/entry-details';
import { EntryForm } from '../../shared/entry-form/entry-form';
import { EntryTile } from '../../shared/entry-tile/entry-tile';
import { PageHero } from '../../shared/page-hero/page-hero';
import { UserAvatar } from '../../shared/user-avatar/user-avatar';

// Мрії пари: перемикач "Усі / Мрії Боді / Мрії Марічки", на кожній плитці — аватарка автора.
// Змінювати й видаляти може автор, позначити "Здійснилось" — будь-хто.
@Component({
  selector: 'app-dreams-page',
  imports: [PageHero, EntryTile, EntryDetails, EntryForm, UserAvatar],
  templateUrl: './dreams-page.html',
  styleUrl: '../../shared/styles/collection-page.css',
})
export class DreamsPage {
  private dreamService = inject(DreamService);
  private userService = inject(UserService);

  readonly info = category('dreams');
  readonly whose = whose;
  me = inject(AuthService).user;
  users = this.userService.users;

  filter = signal<number | null>(null);   // id автора; null = усі

  dreams = signal<Dream[]>([]);
  loaded = signal(false);
  loadError = signal(false);

  modal = signal<'none' | 'details' | 'form'>('none');
  selected = signal<Dream | null>(null);

  saveFn = (request: EntryRequest) => {
    const selected = this.selected();
    return selected ? this.dreamService.update(selected.id, request) : this.dreamService.create(request);
  };
  deleteFn = () => this.dreamService.delete(this.selected()!.id);

  constructor() {
    this.userService.load().subscribe();
    toObservable(this.filter)
      .pipe(
        tap(() => this.loaded.set(false)),
        switchMap((author) => this.dreamService.list(author).pipe(catchError(() => of(null)))),
        takeUntilDestroyed(),
      )
      .subscribe((dreams) => {
        this.loadError.set(dreams === null);
        this.dreams.set(dreams ?? []);
        this.loaded.set(true);
      });
  }

  isAuthor(dream: Dream): boolean {
    return dream.createdBy.id === this.me()?.id;
  }

  openCreate() {
    this.selected.set(null);
    this.modal.set('form');
  }

  openDetails(dream: Dream) {
    this.selected.set(dream);
    this.modal.set('details');
  }

  close() {
    if (this.modal() === 'form' && this.selected()) {
      this.modal.set('details');
      return;
    }
    this.modal.set('none');
    this.selected.set(null);
  }

  onSaved(result: unknown) {
    const saved = result as Dream;
    if (this.dreams().some((d) => d.id === saved.id)) {
      this.replace(saved);
      this.openDetails(saved);
      return;
    }
    this.modal.set('none');
    this.selected.set(null);
    // нова мрія видна у "Усі" і в мріях автора; в чужому фільтрі — перемикаємось на "Усі"
    if (this.filter() === null || this.filter() === saved.createdBy.id) this.dreams.update((list) => [saved, ...list]);
    else this.filter.set(null);
  }

  toggleAchieved(dream: Dream) {
    this.dreamService.setAchieved(dream.id, !dream.achieved).subscribe((updated) => {
      this.replace(updated);
      this.selected.set(updated);
    });
  }

  onDeleted() {
    const id = this.selected()?.id;
    this.dreams.update((list) => list.filter((d) => d.id !== id));
    this.modal.set('none');
    this.selected.set(null);
  }

  private replace(dream: Dream) {
    this.dreams.update((list) => list.map((d) => (d.id === dream.id ? dream : d)));
  }
}

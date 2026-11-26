import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap, tap } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { category } from '../../core/models/category.model';
import { EntryRequest } from '../../core/models/entry.model';
import { whose } from '../../core/models/user-names';
import { WishlistItem } from '../../core/models/wishlist.model';
import { UserService } from '../../core/services/user.service';
import { WishlistService } from '../../core/services/wishlist.service';
import { EntryDetails } from '../../shared/entry-details/entry-details';
import { EntryForm } from '../../shared/entry-form/entry-form';
import { EntryTile } from '../../shared/entry-tile/entry-tile';
import { PageHero } from '../../shared/page-hero/page-hero';
import { UserAvatar } from '../../shared/user-avatar/user-avatar';

// Вішліст: дві вкладки — вішліст кожного з пари ("Вішліст Боді", "Вішліст Марічки").
// Бачать обидва; нове бажання автоматично потрапляє у вішліст того, хто увійшов.
// Змінювати/видаляти можна лише свої, а позначити "Подаровано" — будь-хто.
@Component({
  selector: 'app-wishlist-page',
  imports: [PageHero, EntryTile, EntryDetails, EntryForm, UserAvatar],
  templateUrl: './wishlist-page.html',
  styleUrl: '../../shared/styles/collection-page.css',
})
export class WishlistPage {
  private wishlistService = inject(WishlistService);
  private userService = inject(UserService);

  readonly info = category('wishlist');
  readonly whose = whose;
  me = inject(AuthService).user;
  users = this.userService.users;

  tab = signal<number>(this.me()?.id ?? 0);   // id власника вкладки; спершу — свій вішліст
  isMine = computed(() => this.tab() === this.me()?.id);
  tabUser = computed(() => this.users().find((u) => u.id === this.tab()));

  items = signal<WishlistItem[]>([]);
  loaded = signal(false);
  loadError = signal(false);

  modal = signal<'none' | 'details' | 'form'>('none');
  selected = signal<WishlistItem | null>(null);

  saveFn = (request: EntryRequest) => {
    const selected = this.selected();
    return selected ? this.wishlistService.update(selected.id, request) : this.wishlistService.create(request);
  };
  deleteFn = () => this.wishlistService.delete(this.selected()!.id);

  constructor() {
    this.userService.load().subscribe();
    // При кожній зміні вкладки завантажуємо відповідний вішліст
    toObservable(this.tab)
      .pipe(
        tap(() => this.loaded.set(false)),
        switchMap((owner) => this.wishlistService.byOwner(owner).pipe(catchError(() => of(null)))),
        takeUntilDestroyed(),
      )
      .subscribe((items) => {
        this.loadError.set(items === null);
        this.items.set(items ?? []);
        this.loaded.set(true);
      });
  }

  isOwner(item: WishlistItem): boolean {
    return item.owner.id === this.me()?.id;
  }

  openCreate() {
    this.selected.set(null);
    this.modal.set('form');
  }

  openDetails(item: WishlistItem) {
    this.selected.set(item);
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
    const saved = result as WishlistItem;
    if (this.items().some((i) => i.id === saved.id)) {
      this.replace(saved);
      this.openDetails(saved);
      return;
    }
    // нове бажання — завжди у свій вішліст: перемикаємось на нього
    this.modal.set('none');
    this.selected.set(null);
    if (this.isMine()) this.items.update((list) => [saved, ...list]);
    else this.tab.set(saved.owner.id);
  }

  toggleFulfilled(item: WishlistItem) {
    this.wishlistService.setFulfilled(item.id, !item.fulfilled).subscribe((updated) => {
      this.replace(updated);
      this.selected.set(updated);
    });
  }

  onDeleted() {
    const id = this.selected()?.id;
    this.items.update((list) => list.filter((i) => i.id !== id));
    this.modal.set('none');
    this.selected.set(null);
  }

  private replace(item: WishlistItem) {
    this.items.update((list) => list.map((i) => (i.id === item.id ? item : i)));
  }
}

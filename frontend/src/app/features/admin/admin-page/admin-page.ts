import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AdminOverview, Couple, Member } from '../../../core/models/admin.model';
import { CategoryInfo } from '../../../core/models/category.model';
import { AdminService } from '../../../core/services/admin.service';
import { Modal } from '../../../shared/modal/modal';
import { PageHero } from '../../../shared/page-hero/page-hero';
import { CoupleCard } from '../couple-card/couple-card';
import { CoupleForm } from '../couple-form/couple-form';
import { UsageSummary } from '../usage-summary/usage-summary';

// Що підтверджуємо у вікні "Ви впевнені?"
interface Confirmation {
  title: string;
  text: string;
  button: string;
  danger: boolean;
  run: () => void;
}

// Адмінка (лише для адміна): короткі лічильники використання, список усіх пар з активністю кожного,
// створення нової пари, новий пароль, видалення пари.
// /admin?new=1 — одразу відкриває форму створення (пункт "Створити нову пару" в меню профілю).
@Component({
  selector: 'app-admin-page',
  imports: [PageHero, CoupleCard, CoupleForm, Modal, UsageSummary],
  templateUrl: './admin-page.html',
  styleUrl: './admin-page.css',
})
export class AdminPage implements OnInit {
  private adminService = inject(AdminService);
  private route = inject(ActivatedRoute);
  private router = inject(Router);

  readonly hero: CategoryInfo = {
    slug: 'dreams',
    title: 'Керування парами',
    subtitle: 'Створення пар, паролі та запрошення в Telegram',
    image: '/images/dreams.jpg',
    focus: '72% 40%',
    emoji: '💞',
  };

  overview = signal<AdminOverview | null>(null);
  couples = signal<Couple[]>([]);
  loaded = signal(false);
  loadError = signal(false);

  creating = signal(false);
  created = signal<Couple | null>(null);       // щойно створена пара — показуємо паролі й посилання
  confirmation = signal<Confirmation | null>(null);
  busy = signal(false);
  error = signal('');

  usersCount = computed(() => this.couples().reduce((n, c) => n + c.members.length, 0));

  ngOnInit() {
    this.load();
    if (this.route.snapshot.queryParamMap.has('new')) this.openCreate();
  }

  load() {
    this.loadOverview();
    this.adminService.list().subscribe({
      next: (couples) => {
        this.couples.set(couples);
        this.loaded.set(true);
      },
      error: () => this.loadError.set(true),
    });
  }

  // Лічильники перераховуються після кожної зміни (нова пара, видалення...)
  loadOverview() {
    this.adminService.overview().subscribe({ next: (o) => this.overview.set(o), error: () => this.overview.set(null) });
  }

  openCreate() {
    this.creating.set(true);
  }

  closeCreate() {
    this.creating.set(false);
    this.router.navigate([], { queryParams: {} }); // прибираємо ?new=1 з адреси
  }

  onCreated(couple: Couple) {
    this.closeCreate();
    this.couples.update((list) => [...list, couple]);
    this.created.set(couple);
    this.loadOverview();
  }

  askReset(couple: Couple, member: Member) {
    this.confirmation.set({
      title: `Новий пароль для ${member.nameGenitive}?`,
      text: 'Старий пароль і старе посилання на бота одразу перестануть діяти. Нове посилання і пароль з’являться в картці пари.',
      button: '🔄 Згенерувати',
      danger: false,
      run: () => this.adminService.resetPassword(couple.id, member.id).subscribe({
        next: (updated) => this.done(() => this.replace(updated)),
        error: () => this.failed('Не вдалося згенерувати новий пароль.'),
      }),
    });
  }

  // Посилання на бота без зміни пароля — підтвердження не потрібне, нічого не ламається
  newInvite(couple: Couple, member: Member) {
    this.adminService.newInvite(couple.id, member.id).subscribe((updated) => this.replace(updated));
  }

  askDelete(couple: Couple) {
    const names = couple.members.map((m) => m.name).join(' і ');
    this.confirmation.set({
      title: `Видалити пару ${names}?`,
      text: 'Буде видалено обох користувачів і все, що вони створили: побачення з фото, вішлісти, мрії, аватарки. Це не можна скасувати.',
      button: 'Так, видалити пару',
      danger: true,
      run: () => this.adminService.delete(couple.id).subscribe({
        next: () => this.done(() => this.couples.update((list) => list.filter((c) => c.id !== couple.id))),
        error: () => this.failed('Не вдалося видалити пару.'),
      }),
    });
  }

  confirm() {
    const confirmation = this.confirmation();
    if (!confirmation || this.busy()) return;
    this.busy.set(true);
    this.error.set('');
    confirmation.run();
  }

  closeConfirmation() {
    if (!this.busy()) this.confirmation.set(null);
  }

  private done(apply: () => void) {
    apply();
    this.loadOverview();
    this.busy.set(false);
    this.confirmation.set(null);
  }

  private failed(message: string) {
    this.busy.set(false);
    this.error.set(message);
  }

  private replace(couple: Couple) {
    this.couples.update((list) => list.map((c) => (c.id === couple.id ? couple : c)));
  }
}

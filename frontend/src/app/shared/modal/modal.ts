import { Component, HostListener, OnDestroy, OnInit, input, output } from '@angular/core';

// Відкриті зараз вікна — від нижнього до верхнього (вікно може відкритись поверх іншого)
const openModals: Modal[] = [];

// Спільна "рамка" модального вікна: затемнений фон + біла картка по центру.
// Вміст передається всередину тегу: <app-modal> ...тут... </app-modal>
// Закривається кліком по фону, хрестиком або клавішею Esc.
//
// Картка ніколи не виходить за межі екрана: її висота обмежена, а вміст прокручується
// всередині — тому хрестик завжди видно. Вікна можна відкривати одне поверх одного:
// Esc закриває лише верхнє.
@Component({
  selector: 'app-modal',
  styleUrl: './modal.css',
  host: { '[style.--layer]': 'layer' },
  template: `
    <div class="modal-backdrop" (click)="closed.emit()">
      <div
        class="modal-card"
        [class.wide]="size() === 'wide'"
        [class.narrow]="size() === 'narrow'"
        role="dialog"
        aria-modal="true"
        (click)="$event.stopPropagation()"
      >
        <button class="modal-close" type="button" aria-label="Закрити" (click)="closed.emit()">✕</button>
        <div class="modal-scroll">
          <ng-content />
        </div>
      </div>
    </div>
  `,
})
export class Modal implements OnInit, OnDestroy {
  // narrow — короткі форми (планування, реалізація), wide — статистика, фото з побачення
  size = input<'narrow' | 'normal' | 'wide'>('normal');
  closed = output<void>();

  layer = 0; // скільки вікон уже відкрито під цим

  @HostListener('document:keydown.escape')
  onEscape() {
    if (openModals.at(-1) === this) this.closed.emit();
  }

  // Поки відкрите хоча б одне вікно, сторінка під ним не прокручується
  ngOnInit() {
    this.layer = openModals.length;
    openModals.push(this);
    document.body.style.overflow = 'hidden';
  }

  ngOnDestroy() {
    openModals.splice(openModals.indexOf(this), 1);
    if (!openModals.length) document.body.style.overflow = '';
  }
}

import { Component, input, output } from '@angular/core';

// Плитка в сітці (вішліст, мрії): фото (або емодзі) і назва.
// Додаткові значки передаються всередину:
//   <span tileCorner>…</span>  — правий верхній кут (маркер автора)
//   <span tileBadge>…</span>   — лівий верхній кут ("Подаровано", "Здійснилось")
@Component({
  selector: 'app-entry-tile',
  styleUrl: './entry-tile.css',
  // --i = порядковий номер; плитки з'являються по черзі з невеликою затримкою
  host: { '[style.--i]': 'index()', '[class.done]': 'done()' },
  template: `
    <button class="tile" type="button" (click)="open.emit()">
      @if (imageUrl()) {
        <img [src]="imageUrl()" [alt]="title()" loading="lazy" />
      } @else {
        <div class="tile-placeholder">{{ emoji() }}</div>
      }
      <span class="tile-title">{{ title() }}</span>
    </button>
    <div class="tile-corner"><ng-content select="[tileCorner]" /></div>
    <div class="tile-badge"><ng-content select="[tileBadge]" /></div>
  `,
})
export class EntryTile {
  title = input.required<string>();
  imageUrl = input<string | null>(null);
  emoji = input('✨');
  index = input(0);
  done = input(false); // виконане бажання / здійснена мрія — трохи приглушене

  open = output<void>();
}

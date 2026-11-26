import { Component, computed, input } from '@angular/core';

// Кружечок з балом якості фото: зелений (70+), жовтий (45–69), червоний (до 45)
@Component({
  selector: 'app-score-badge',
  host: { '[class]': 'level()', '[attr.title]': '"Оцінка якості: " + score() + " зі 100"' },
  template: `{{ score() }}`,
  styles: `
    :host {
      display: inline-grid;
      place-items: center;
      min-width: 34px;
      height: 34px;
      padding: 0 6px;
      border-radius: 999px;
      font-weight: 800;
      font-size: 14px;
      color: white;
      box-shadow: 0 3px 10px rgba(0, 0, 0, 0.3);
    }
    :host(.good) { background: linear-gradient(135deg, #22c55e, #15803d); }
    :host(.ok) { background: linear-gradient(135deg, #fbbf24, #d97706); }
    :host(.bad) { background: linear-gradient(135deg, #f87171, #b91c1c); }
  `,
})
export class ScoreBadge {
  score = input.required<number>();
  level = computed(() => (this.score() >= 70 ? 'good' : this.score() >= 45 ? 'ok' : 'bad'));
}

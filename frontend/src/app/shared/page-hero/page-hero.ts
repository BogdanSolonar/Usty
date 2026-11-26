import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CategoryInfo } from '../../core/models/category.model';

// Тонка шапка розділу: розмите фото категорії, "назад", заголовок і кнопки дій в один рядок
// (на телефоні кнопки дій — окремим рядком на всю ширину).
// Кнопки дій передаються всередину: <app-page-hero> <button>…</button> </app-page-hero>
// Значок у кнопці варто загорнути в <span class="ico"> — на дуже вузьких екранах він ховається.
@Component({
  selector: 'app-page-hero',
  imports: [RouterLink],
  templateUrl: './page-hero.html',
  styleUrl: './page-hero.css',
})
export class PageHero {
  category = input.required<CategoryInfo>();
  title = input<string | null>(null);     // за замовчуванням — назва категорії
  backLink = input('/');
  backLabel = input('Головна');
  // анімація "панель з головної -> шапка"; false — для підсторінок (історія, адмінка),
  // щоб не було двох шапок з однаковою назвою переходу
  morph = input(true);
}

import { Component, computed, input, signal } from '@angular/core';
import { User } from '../../core/models/user.model';

// Кружечок користувача: фото-аватарка в рамці його кольору (задається при створенні пари).
// Якщо фото ще не завантажене (або не відкрилось) — перша літера імені на фоні його кольору.
@Component({
  selector: 'app-user-avatar',
  host: {
    '[style.--size.px]': 'size()',
    '[style.--color]': 'user()?.color ?? "#9a8fa0"',
    '[class.has-photo]': 'showPhoto()',
    '[attr.title]': 'user()?.name',
  },
  template: `
    @if (showPhoto()) {
      <img [src]="user()!.avatarUrl" [alt]="user()!.name" (error)="brokenUrl.set(user()!.avatarUrl)" />
    } @else {
      <span class="letter">{{ letter() }}</span>
    }
  `,
  styles: `
    :host {
      display: inline-grid;
      place-items: center;
      flex: none;
      width: var(--size);
      height: var(--size);
      border-radius: 50%;
      overflow: hidden;
      background: var(--color);
      color: white;
      box-shadow: 0 0 0 2px white, 0 2px 8px rgba(0, 0, 0, 0.25);
    }
    /* з фото: кольорова рамка всередині + біла обвідка зовні */
    :host(.has-photo) {
      box-shadow: inset 0 0 0 calc(2px + var(--size) * 0.03) var(--color), 0 0 0 2px white, 0 2px 8px rgba(0, 0, 0, 0.25);
    }
    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      /* картинка трохи менша за кружечок, щоб кольорову рамку було видно */
      clip-path: circle(calc(50% - 2px - var(--size) * 0.03));
    }
    .letter {
      font-weight: 700;
      font-size: calc(var(--size) * 0.46);
      line-height: 1;
    }
  `,
})
export class UserAvatar {
  user = input<User | null>(null);
  size = input(40);

  brokenUrl = signal<string | null>(null); // фото не завантажилось — показуємо літеру

  showPhoto = computed(() => {
    const url = this.user()?.avatarUrl;
    return !!url && url !== this.brokenUrl();
  });
  letter = computed(() => this.user()?.name.charAt(0).toUpperCase() ?? '?');
}

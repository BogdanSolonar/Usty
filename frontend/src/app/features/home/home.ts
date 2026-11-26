import { Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CATEGORIES } from '../../core/models/category.model';
import { UserService } from '../../core/services/user.service';
import { UserAvatar } from '../../shared/user-avatar/user-avatar';
import { ProfileMenu } from './profile-menu/profile-menu';

// Головний екран: три вузькі вертикальні панелі по центру, справа зверху — шапка пари
// (я ♥ моя половинка). Фон сторінки (.home-backdrop) складається з тих самих трьох фото,
// сильно розмитих, тому він завжди в тон карткам — навіть якщо фото замінити.
@Component({
  selector: 'app-home',
  imports: [RouterLink, ProfileMenu, UserAvatar],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  private auth = inject(AuthService);
  private userService = inject(UserService);

  categories = CATEGORIES;

  // друга людина з моєї пари
  partner = computed(() => this.userService.users().find((u) => u.id !== this.auth.user()?.id) ?? null);

  ngOnInit() {
    this.auth.refresh().subscribe(); // свіжі дані про себе (аватарка, права адміна)
    this.userService.load().subscribe();
  }
}
